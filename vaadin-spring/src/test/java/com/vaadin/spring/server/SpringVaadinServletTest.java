/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.server;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;

public class SpringVaadinServletTest {

    @Test
    public void getStaticFilePath() {
        SpringVaadinServlet servlet = new DebugSpringVaadinServlet();

        // Mapping: /VAADIN/*
        // /VAADIN
        Assert.assertNull(servlet.getStaticFilePath(
                createServletRequestUsingServletPathAndPathInfo("/VAADIN",
                        null)));
        // /VAADIN/ - not really sensible but still interpreted as a resource
        // request
        Assert.assertEquals("/VAADIN/",
                servlet.getStaticFilePath(
                        createServletRequestUsingServletPathAndPathInfo(
                                "/VAADIN", "/")));
        // /VAADIN/vaadinBootstrap.js
        Assert.assertEquals("/VAADIN/vaadinBootstrap.js",
                servlet.getStaticFilePath(
                        createServletRequestUsingServletPathAndPathInfo(
                                "/VAADIN", "/vaadinBootstrap.js")));
        // /VAADIN/foo bar.js
        Assert.assertEquals("/VAADIN/foo bar.js",
                servlet.getStaticFilePath(
                        createServletRequestUsingServletPathAndPathInfo(
                                "/VAADIN", "/foo bar.js")));
        // /VAADIN/.. - not normalized and disallowed in this method
        Assert.assertEquals("/VAADIN/..",
                servlet.getStaticFilePath(
                        createServletRequestUsingServletPathAndPathInfo(
                                "/VAADIN", "/..")));

        // Mapping: /*
        // /
        Assert.assertNull(servlet.getStaticFilePath(
                createServletRequestUsingServletPathAndPathInfo("", null)));
        // /VAADIN
        Assert.assertNull(servlet.getStaticFilePath(
                createServletRequestUsingServletPathAndPathInfo("",
                        "/VAADIN")));
        // /VAADIN/
        Assert.assertEquals("/VAADIN/",
                servlet.getStaticFilePath(
                        createServletRequestUsingServletPathAndPathInfo("",
                                "/VAADIN/")));
        // /VAADIN/foo bar.js
        Assert.assertEquals("/VAADIN/foo bar.js",
                servlet.getStaticFilePath(
                        createServletRequestUsingServletPathAndPathInfo("",
                                "/VAADIN/foo bar.js")));
        // /VAADIN/.. - not normalized and disallowed in this method
        Assert.assertEquals("/VAADIN/..",
                servlet.getStaticFilePath(
                        createServletRequestUsingServletPathAndPathInfo("",
                                "/VAADIN/..")));
        // /BAADIN/foo.js
        Assert.assertNull(servlet.getStaticFilePath(
                createServletRequestUsingServletPathAndPathInfo("",
                        "/BAADIN/foo.js")));

        // Mapping: /myservlet/*
        // /myservlet
        Assert.assertNull(servlet.getStaticFilePath(
                createServletRequestUsingServletPathAndPathInfo("/myservlet",
                        null)));
        // /myservlet/VAADIN
        Assert.assertNull(servlet.getStaticFilePath(
                createServletRequestUsingServletPathAndPathInfo("/myservlet",
                        "/VAADIN")));
        // /myservlet/VAADIN/
        Assert.assertEquals("/VAADIN/",
                servlet.getStaticFilePath(
                        createServletRequestUsingServletPathAndPathInfo(
                                "/myservlet", "/VAADIN/")));
        // /myservlet/VAADIN/foo bar.js
        Assert.assertEquals("/VAADIN/foo bar.js",
                servlet.getStaticFilePath(
                        createServletRequestUsingServletPathAndPathInfo(
                                "/myservlet", "/VAADIN/foo bar.js")));
        // /myservlet/VAADIN/.. - not normalized and disallowed in this method
        Assert.assertEquals("/VAADIN/..",
                servlet.getStaticFilePath(
                        createServletRequestUsingServletPathAndPathInfo(
                                "/myservlet", "/VAADIN/..")));
        // /myservlet/BAADIN/foo.js
        Assert.assertNull(servlet.getStaticFilePath(
                createServletRequestUsingServletPathAndPathInfo("/myservlet",
                        "/BAADIN/foo.js")));

    }

    @Test
    public void getStaticPath_requestURIAndContextPath() {
        SpringVaadinServlet servlet = new SpringVaadinServlet();

        // no /VAADIN/
        HttpServletRequest request = createServletRequestUsingURIAndContextPath(
                "", "");
        Assert.assertEquals(null, servlet.getStaticFilePath(request));

        // URI doesn't start with /VAADIN/
        request = createServletRequestUsingURIAndContextPath("/VAADIN", "foo");
        Assert.assertEquals(null, servlet.getStaticFilePath(request));

        // URI equals /VAADIN/
        request = createServletRequestUsingURIAndContextPath("/VAADIN/", "foo");
        Assert.assertEquals("/VAADIN/", servlet.getStaticFilePath(request));

        // URI starts with /VAADIN/
        request = createServletRequestUsingURIAndContextPath("/VAADIN/bar.css",
                "foo");
        Assert.assertEquals("/VAADIN/bar.css",
                servlet.getStaticFilePath(request));

        // URI doesn't start with "context/VAADIN/"
        request = createServletRequestUsingURIAndContextPath("foobar/VAADIN/",
                "foo");
        Assert.assertEquals(null, servlet.getStaticFilePath(request));

        // URI doesn't start with "context/VAADIN/"
        request = createServletRequestUsingURIAndContextPath("foo/VAADIN",
                "foo");
        Assert.assertEquals(null, servlet.getStaticFilePath(request));

        // URI equals "context/VAADIN/"
        request = createServletRequestUsingURIAndContextPath("foo/VAADIN/",
                "foo");
        Assert.assertEquals("/VAADIN/", servlet.getStaticFilePath(request));

        // URI starts with "context/VAADIN/"
        request = createServletRequestUsingURIAndContextPath(
                "foo/VAADIN/bar.css", "foo");
        Assert.assertEquals("/VAADIN/bar.css",
                servlet.getStaticFilePath(request));
    }

    private HttpServletRequest createServletRequestUsingServletPathAndPathInfo(
            String servletPath, String pathInfo) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getServletPath()).thenReturn(servletPath);
        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);
        Mockito.when(request.getRequestURI()).thenReturn("");
        Mockito.when(request.getContextPath()).thenReturn("");
        return request;
    }

    private HttpServletRequest createServletRequestUsingURIAndContextPath(
            String requestURI, String contextPath) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getServletPath()).thenReturn("");
        Mockito.when(request.getPathInfo()).thenReturn("/");
        Mockito.when(request.getRequestURI()).thenReturn(requestURI);
        Mockito.when(request.getContextPath()).thenReturn(contextPath);
        return request;
    }

    private static class DebugSpringVaadinServlet extends SpringVaadinServlet {
        @Override
        public String getStaticFilePath(HttpServletRequest request) {
            return super.getStaticFilePath(request);
        }
    }
}
