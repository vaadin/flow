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

import java.net.MalformedURLException;
import java.net.URL;

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

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class VaadinServletTest {

    private VaadinRequest request;
    private VaadinSession session;
    private ServletContext context;
    private VaadinUriResolverFactory factory;
    private VaadinServletService service;

    private VaadinServlet servlet = new VaadinServlet() {
        @Override
        public ServletContext getServletContext() {
            return context;
        }

        @Override
        protected VaadinServletService createServletService() {
            return service;
        }
    };

    @Before
    public void setUp() {
        request = Mockito.mock(VaadinRequest.class);
        CurrentInstance.set(VaadinRequest.class, request);
        session = Mockito.mock(VaadinSession.class);
        CurrentInstance.set(VaadinSession.class, session);

        context = Mockito.mock(ServletContext.class);
        factory = Mockito.mock(VaadinUriResolverFactory.class);

        context = Mockito.mock(ServletContext.class);

        Mockito.when(session.getAttribute(VaadinUriResolverFactory.class))
                .thenReturn(factory);
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
    public void resolveResource_noWebJars_resolveViaResolverFactory() {
        String path = "foo";
        String resolved = "bar";
        Mockito.when(factory.toServletContextPath(request, path))
                .thenReturn(resolved);

        Assert.assertEquals(resolved, servlet.resolveResource(path));
    }

    @Test
    public void resolveResource_webJarResource_resolvedAsWebJarsResource()
            throws ServletException, MalformedURLException {
        String path = "foo";
        String frontendPrefix = "context://baz/";
        String resolved = "/baz/bower_components/";
        Mockito.when(factory.toServletContextPath(request, path))
                .thenReturn(resolved);

        service = Mockito.mock(VaadinServletService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(configuration.getDevelopmentFrontendPrefix())
                .thenReturn(frontendPrefix);

        Mockito.when(configuration.areWebJarsEnabled()).thenReturn(true);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);

        ServletConfig config = Mockito.mock(ServletConfig.class);
        servlet.init(config);

        CurrentInstance.set(VaadinRequest.class, request);
        CurrentInstance.set(VaadinSession.class, session);

        URL url = new URL("http://example.com");
        String webjars = "/webjars/";
        Mockito.when(context.getResource(webjars)).thenReturn(url);

        Assert.assertEquals(webjars, servlet.resolveResource(path));
    }
}
