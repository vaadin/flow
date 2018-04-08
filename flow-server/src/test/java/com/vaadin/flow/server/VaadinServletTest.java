/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.theme.AbstractTheme;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class VaadinServletTest {

    private VaadinSession session;
    private ServletContext context;
    private VaadinServletService service;
    private ServletContextUriResolver vaadinUriResolver;

    private CustomVaadinServlet servlet = new CustomVaadinServlet();

    @Before
    public void setUp() throws ServletException {
        service = Mockito.mock(VaadinServletService.class);
        session = Mockito.mock(VaadinSession.class);
        WebBrowser mockedEs6Browser = Mockito.mock(WebBrowser.class);
        Mockito.when(session.getBrowser()).thenReturn(mockedEs6Browser);
        Mockito.when(mockedEs6Browser.isEs6Supported()).thenReturn(true);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.getEs6FrontendPrefix())
                .thenReturn(Constants.FRONTEND_URL_DEV_DEFAULT);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        context = Mockito.mock(ServletContext.class);

        servlet.init(Mockito.mock(ServletConfig.class));
        VaadinSession.setCurrent(session);
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void testGetLastPathParameter() {
        Assert.assertEquals("",
                VaadinServlet.getLastPathParameter("http://myhost.com"));
        Assert.assertEquals(";a",
                VaadinServlet.getLastPathParameter("http://myhost.com;a"));
        Assert.assertEquals("",
                VaadinServlet.getLastPathParameter("http://myhost.com/hello"));
        Assert.assertEquals(";b=c", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello;b=c"));
        Assert.assertEquals("",
                VaadinServlet.getLastPathParameter("http://myhost.com/hello/"));
        Assert.assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello;a/"));
        Assert.assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello;a=1/"));
        Assert.assertEquals(";b", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello/;b"));
        Assert.assertEquals(";b=1", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello/;b=1"));
        Assert.assertEquals(";b=1,c=2", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello/;b=1,c=2"));
        Assert.assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello/;b=1,c=2/"));
        Assert.assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;a/"));
        Assert.assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;a=1/"));
        Assert.assertEquals(";b", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;b"));
        Assert.assertEquals(";b=1", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;b=1"));
        Assert.assertEquals(";b=1,c=2", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;b=1,c=2"));
        Assert.assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;b=1,c=2/"));
    }

    @Test
    public void resolveServletContextResource() {
        assertResolvedUrl(
                "/frontend/bower_components/vaadin-button/vaadin-button.html",
                "frontend://bower_components/vaadin-button/vaadin-button.html");
        assertResolvedUrl("/foo/bar", "/foo/bar");
        assertResolvedUrl(
                "/frontend/bower_components/vaadin-button/vaadin-button.html",
                "context://frontend/bower_components/vaadin-button/vaadin-button.html");
    }

    @Test
    public void urlTranslation() {
        Assert.assertEquals("/theme/foo",
                servlet.getUrlTranslation(new MyTheme(), "/src/foo"));
        Assert.assertEquals("/other/foo",
                servlet.getUrlTranslation(new MyTheme(), "/other/foo"));
//        Assert.assertEquals("/theme/src/foo",
//                servlet.getUrlTranslation(new MyTheme(), "/src/foo"));
    }

    private void assertResolvedUrl(String expected, String untranslated) {
        Assert.assertEquals(expected, servlet.resolveResource(untranslated));

    }

    private final class CustomVaadinServlet extends VaadinServlet {

        @Override
        public ServletContext getServletContext() {
            return context;
        }

        @Override
        protected VaadinServletService createServletService() {
            return service;
        }

        @Override
        public VaadinServletService getService() {
            return service;
        }

        @Override
        boolean isInServletContext(String resolvedUrl) {
            // For testing URL translations, assume all resources can be found
            return true;
        }
    }

    private static class MyTheme implements AbstractTheme {

        @Override
        public String getBaseUrl() {
            return "/src/";
        }

        @Override
        public String getThemeUrl() {
            return "/theme/";
        }
    }
}
