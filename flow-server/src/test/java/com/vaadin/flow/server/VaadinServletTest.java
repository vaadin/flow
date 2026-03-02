/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.ApplicationClassLoaderAccess;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.VaadinContextInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NotThreadSafe
class VaadinServletTest {

    @Test
    public void testGetLastPathParameter() {
        assertEquals("",
                VaadinServlet.getLastPathParameter("http://myhost.com"));
        assertEquals(";a",
                VaadinServlet.getLastPathParameter("http://myhost.com;a"));
        assertEquals("",
                VaadinServlet.getLastPathParameter("http://myhost.com/hello"));
        assertEquals(";b=c", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello;b=c"));
        assertEquals("",
                VaadinServlet.getLastPathParameter("http://myhost.com/hello/"));
        assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello;a/"));
        assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello;a=1/"));
        assertEquals(";b", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello/;b"));
        assertEquals(";b=1", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello/;b=1"));
        assertEquals(";b=1,c=2", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello/;b=1,c=2"));
        assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/hello/;b=1,c=2/"));
        assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;a/"));
        assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;a=1/"));
        assertEquals(";b", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;b"));
        assertEquals(";b=1", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;b=1"));
        assertEquals(";b=1,c=2", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;b=1,c=2"));
        assertEquals("", VaadinServlet
                .getLastPathParameter("http://myhost.com/a;hello/;b=1,c=2/"));
    }

    @Test
    public void init_superInitCalledOnce() throws ServletException {
        AtomicBoolean called = new AtomicBoolean();
        VaadinServlet servlet = new VaadinServlet() {

            @Override
            public void init() throws ServletException {
                assertFalse(called.get());
                called.set(true);
            }
        };

        ServletConfig config = mockConfig();
        servlet.init(config);

        assertTrue(called.get());

        servlet.init(config);

        assertSame(config, servlet.getServletConfig());
    }

    @Test
    public void init_passDifferentConfigInstance_throws()
            throws ServletException {
        VaadinServlet servlet = new VaadinServlet();

        ServletConfig config = mockConfig();
        servlet.init(config);

        assertThrows(IllegalArgumentException.class,
                () -> servlet.init(mockConfig()));
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

        assertFalse(called.get());
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

        assertTrue(called.get());
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

            assertNull(VaadinService.getCurrent());
            assertNull(UI.getCurrent());
            assertNull(VaadinSession.getCurrent());
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

            assertNull(VaadinService.getCurrent());
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
        assertSame(loader, access.getClassloader());
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

        assertSame(config, servlet.getServletConfig());

        servlet.destroy();

        ServletConfig newConfig = mockConfig();
        servlet.init(newConfig);
        assertSame(newConfig, servlet.getServletConfig());
    }

    @Test
    public void destroy_servletIsInitializedBeforeDestroy_servletConfigIsNullAfterDestroy()
            throws ServletException {
        VaadinServlet servlet = new VaadinServlet();

        ServletConfig config = mockConfig();

        servlet.init(config);

        servlet.destroy();

        assertNull(servlet.getServletConfig());
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
        assertSame(handler, result);
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

        assertSame(config, configDuringDestroy.get());
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
