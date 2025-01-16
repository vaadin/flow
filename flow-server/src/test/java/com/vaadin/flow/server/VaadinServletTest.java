/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.ApplicationClassLoaderAccess;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.VaadinContextInitializer;

@NotThreadSafe
public class VaadinServletTest {

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
    public void init_superInitCalledOnce() throws ServletException {
        AtomicBoolean called = new AtomicBoolean();
        VaadinServlet servlet = new VaadinServlet() {

            @Override
            public void init() throws ServletException {
                Assert.assertFalse(called.get());
                called.set(true);
            }
        };

        ServletConfig config = mockConfig();
        servlet.init(config);

        Assert.assertTrue(called.get());

        servlet.init(config);

        Assert.assertSame(config, servlet.getServletConfig());
    }

    @Test(expected = IllegalArgumentException.class)
    public void init_passDifferentConfigInstance_throws()
            throws ServletException {
        VaadinServlet servlet = new VaadinServlet();

        ServletConfig config = mockConfig();
        servlet.init(config);

        servlet.init(mockConfig());
    }

    @Test
    public void init_noLookup_servletIsNotInitialized()
            throws ServletException {
        AtomicBoolean called = new AtomicBoolean();
        VaadinServlet servlet = new VaadinServlet() {

            @Override
            protected void servletInitialized() throws ServletException {
                called.set(true);
            }
        };

        ServletConfig config = mockConfig();
        servlet.init(config);

        Assert.assertFalse(called.get());
    }

    @Test
    public void init_contextHasLookup_servletIsInitialized()
            throws ServletException {
        AtomicBoolean called = new AtomicBoolean();
        VaadinServlet servlet = new VaadinServlet() {

            @Override
            protected VaadinServletService createServletService()
                    throws ServletException, ServiceException {
                return Mockito.mock(VaadinServletService.class);
            }

            @Override
            protected StaticFileHandler createStaticFileHandler(
                    VaadinService servletService) {
                return Mockito.mock(StaticFileHandler.class);
            }

            @Override
            protected void servletInitialized() throws ServletException {
                called.set(true);
            }
        };

        ServletConfig config = mockConfig();
        ServletContext servletContext = config.getServletContext();
        Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(Mockito.mock(Lookup.class));
        servlet.init(config);

        Assert.assertTrue(called.get());
    }

    @Test
    public void init_initServlet_CurrentInstanceClearAllIsCalled()
            throws ServletException {
        try {
            VaadinServlet servlet = new VaadinServlet() {

                @Override
                public void init() throws ServletException {
                    VaadinSession.setCurrent(Mockito.mock(VaadinSession.class));
                }

                @Override
                protected VaadinServletService createServletService()
                        throws ServletException, ServiceException {
                    VaadinService.setCurrent(Mockito.mock(VaadinService.class));
                    return Mockito.mock(VaadinServletService.class);
                }

                @Override
                protected StaticFileHandler createStaticFileHandler(
                        VaadinService servletService) {
                    return Mockito.mock(StaticFileHandler.class);
                }

                @Override
                protected void servletInitialized() throws ServletException {
                    UI.setCurrent(new UI());
                }
            };

            ServletConfig config = mockConfig();
            ServletContext servletContext = config.getServletContext();
            Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                    .thenReturn(Mockito.mock(Lookup.class));
            servlet.init(config);

            Assert.assertNull(VaadinService.getCurrent());
            Assert.assertNull(UI.getCurrent());
            Assert.assertNull(VaadinSession.getCurrent());
        } finally {
            CurrentInstance.clearAll();
        }
    }

    @Test
    public void init_initOnlyConfig_CurrentInstanceClearAllIsCalled()
            throws ServletException {
        try {
            VaadinServlet servlet = new VaadinServlet() {

                @Override
                public void init() throws ServletException {
                    VaadinService.setCurrent(Mockito.mock(VaadinService.class));
                }

            };

            ServletConfig config = mockConfig();
            servlet.init(config);

            Assert.assertNull(VaadinService.getCurrent());
        } finally {
            CurrentInstance.clearAll();
        }
    }

    @Test
    public void init_appClassLoaderIsSet() throws ServletException {
        VaadinServlet servlet = new VaadinServlet();

        ServletConfig config = mockConfig();
        ServletContext servletContext = config.getServletContext();
        ClassLoader loader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletContext.getClassLoader()).thenReturn(loader);
        servlet.init(config);

        ArgumentCaptor<ApplicationClassLoaderAccess> captor = ArgumentCaptor
                .forClass(ApplicationClassLoaderAccess.class);
        Mockito.verify(servletContext).setAttribute(
                Mockito.eq(ApplicationClassLoaderAccess.class.getName()),
                captor.capture());

        ApplicationClassLoaderAccess access = captor.getValue();
        Assert.assertSame(loader, access.getClassloader());
    }

    @Test
    public void init_contextInitializationIsExecuted() throws ServletException {
        VaadinServlet servlet = new VaadinServlet();

        ServletConfig config = mockConfig();
        ServletContext servletContext = config.getServletContext();
        ClassLoader loader = Mockito.mock(ClassLoader.class);

        VaadinContextInitializer initializer = Mockito
                .mock(VaadinContextInitializer.class);

        Mockito.when(servletContext
                .getAttribute(VaadinContextInitializer.class.getName()))
                .thenReturn(initializer);

        Mockito.when(servletContext.getClassLoader()).thenReturn(loader);
        servlet.init(config);

        Mockito.verify(initializer)
                .initialize(Mockito.any(VaadinContext.class));
    }

    @Test
    public void init_initIsCalledAfterDestroy_passDifferentConfigInstance_servletIsInitialized()
            throws ServletException {
        VaadinServlet servlet = new VaadinServlet();

        ServletConfig config = mockConfig();

        servlet.init(config);

        Assert.assertSame(config, servlet.getServletConfig());

        servlet.destroy();

        ServletConfig newConfig = mockConfig();
        servlet.init(newConfig);
        Assert.assertSame(newConfig, servlet.getServletConfig());
    }

    @Test
    public void destroy_servletIsInitializedBeforeDestroy_servletConfigIsNullAfterDestroy()
            throws ServletException {
        VaadinServlet servlet = new VaadinServlet();

        ServletConfig config = mockConfig();

        servlet.init(config);

        servlet.destroy();

        Assert.assertNull(servlet.getServletConfig());
    }

    @Test
    public void createStaticFileHandler_delegateToStaticFileHandlerFactory() {
        VaadinServlet servlet = new VaadinServlet();
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(context);
        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);

        StaticFileHandlerFactory factory = Mockito
                .mock(StaticFileHandlerFactory.class);

        Mockito.when(lookup.lookup(StaticFileHandlerFactory.class))
                .thenReturn(factory);

        StaticFileHandler handler = Mockito.mock(StaticFileHandler.class);
        Mockito.when(factory.createHandler(service)).thenReturn(handler);

        StaticFileHandler result = servlet.createStaticFileHandler(service);

        Mockito.verify(factory).createHandler(service);
        Assert.assertSame(handler, result);
    }

    @Test
    public void destroy_servletConfigAvailableInServbiceDestroy()
            throws ServletException {
        VaadinServletService service = Mockito.mock(VaadinServletService.class);
        VaadinServlet servlet = new VaadinServlet() {
            @Override
            public VaadinServletService getService() {
                return service;
            }
        };

        AtomicReference<ServletConfig> configDuringDestroy = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            configDuringDestroy.set(servlet.getServletConfig());
            return null;
        }).when(service).destroy();

        ServletConfig config = mockConfig();

        servlet.init(config);

        servlet.destroy();

        Assert.assertSame(config, configDuringDestroy.get());
    }

    private ServletConfig mockConfig() {
        ServletConfig config = Mockito.mock(ServletConfig.class);
        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(config.getServletContext()).thenReturn(context);
        return config;
    }

    private HttpServletRequest createRequest(
            MockServletServiceSessionSetup mocks, String path) {
        HttpServletRequest httpServletRequest = Mockito
                .mock(HttpServletRequest.class);
        return new VaadinServletRequest(httpServletRequest,
                mocks.getService()) {
            @Override
            public String getPathInfo() {
                return path;
            }

            @Override
            public String getServletPath() {
                return "";
            }

            @Override
            public ServletContext getServletContext() {
                return mocks.getServletContext();
            }
        };
    }
}
