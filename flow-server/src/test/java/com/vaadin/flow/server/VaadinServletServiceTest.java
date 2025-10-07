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

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletService;
import com.vaadin.flow.theme.AbstractTheme;

import static org.mockito.Mockito.when;

/**
 * Test class for testing es6 resolution by browser capability. This is valid
 * only for bower mode where we need to decide ourselves.
 */
public class VaadinServletServiceTest {

    private final class TestTheme implements AbstractTheme {
        @Override
        public String getBaseUrl() {
            return "/raw/";
        }

        @Override
        public String getThemeUrl() {
            return "/theme/";
        }
    }

    private MockServletServiceSessionSetup mocks;
    private TestVaadinServletService service;
    private VaadinServlet servlet;

    @Before
    public void setup() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        service = mocks.getService();

        servlet = mocks.getServlet();
    }

    @After
    public void tearDown() {
        mocks.cleanup();
    }

    @Test
    public void resolveNullThrows() {
        try {
            service.resolveResource(null);
            Assert.fail("null should not resolve");
        } catch (NullPointerException e) {
            Assert.assertEquals("Url cannot be null", e.getMessage());
        }
    }

    @Test
    public void resolveResource() {
        Assert.assertEquals("", service.resolveResource(""));
        Assert.assertEquals("foo", service.resolveResource("foo"));
        Assert.assertEquals("/foo", service.resolveResource("context://foo"));
    }

    @Test
    public void resolveResourceNPM_production() {
        mocks.setProductionMode(true);

        Assert.assertEquals("", service.resolveResource(""));
        Assert.assertEquals("foo", service.resolveResource("foo"));
        Assert.assertEquals("/foo", service.resolveResource("context://foo"));
    }

    @Test
    public void getContextRootRelativePath_useVariousContextPathAndServletPathsAndPathInfo()
            throws Exception {
        String location;

        /* SERVLETS */
        // http://dummy.host:8080/contextpath/servlet
        // should return . (relative url resolving to /contextpath)
        location = testLocation("http://dummy.host:8080", "/contextpath",
                "/servlet", "");
        Assert.assertEquals("./../", location);

        // http://dummy.host:8080/contextpath/servlet/
        // should return ./.. (relative url resolving to /contextpath)
        location = testLocation("http://dummy.host:8080", "/contextpath",
                "/servlet", "/");
        Assert.assertEquals("./../", location);

        // http://dummy.host:8080/servlet
        // should return "."
        location = testLocation("http://dummy.host:8080", "", "/servlet", "");
        Assert.assertEquals("./../", location);

        // http://dummy.host/contextpath/servlet/extra/stuff
        // should return ./../.. (relative url resolving to /contextpath)
        location = testLocation("http://dummy.host", "/contextpath", "/servlet",
                "/extra/stuff");
        Assert.assertEquals("./../", location);

        // http://dummy.host/context/path/servlet/extra/stuff
        // should return ./../.. (relative url resolving to /context/path)
        location = testLocation("http://dummy.host", "/context/path",
                "/servlet", "/extra/stuff");
        Assert.assertEquals("./../", location);

    }

    @Test
    public void init_classLoaderIsSetUsingServletContext()
            throws ServiceException {
        VaadinServlet servlet = Mockito.mock(VaadinServlet.class);
        ServletContext context = Mockito.mock(ServletContext.class);
        when(servlet.getServletContext()).thenReturn(context);
        when(context.getAttribute(Lookup.class.getName()))
                .thenReturn(Mockito.mock(Lookup.class));

        ClassLoader loader = Mockito.mock(ClassLoader.class);
        when(context.getClassLoader()).thenReturn(loader);

        VaadinServletService service = new VaadinServletService(servlet,
                mocks.getDeploymentConfiguration()) {
            @Override
            protected Instantiator createInstantiator()
                    throws ServiceException {
                return Mockito.mock(Instantiator.class);
            }

            @Override
            protected List<RequestHandler> createRequestHandlers()
                    throws ServiceException {
                return Collections.emptyList();
            }
        };

        service.init();

        Assert.assertSame(loader, service.getClassLoader());
    }

    @Test
    public void getPwaRegistry_servletInitialized_getsRegistry() {
        MockServletServiceSessionSetup.TestVaadinServlet vaadinServlet = Mockito
                .spy(mocks.getServlet());
        // Restore original behavior of getServletContext
        when(vaadinServlet.getServletContext()).thenAnswer(
                i -> vaadinServlet.getServletConfig().getServletContext());
        VaadinServletService service = new VaadinServletService(vaadinServlet,
                mocks.getDeploymentConfiguration());
        Assert.assertNotNull(service.getPwaRegistry());
    }

    @Test
    public void getPwaRegistry_servletNotInitialized_getsNull() {
        MockServletServiceSessionSetup.TestVaadinServlet vaadinServlet = Mockito
                .spy(mocks.getServlet());
        // Restore original behavior of getServletContext
        when(vaadinServlet.getServletContext()).thenAnswer(
                i -> vaadinServlet.getServletConfig().getServletContext());
        VaadinServletService service = new VaadinServletService(vaadinServlet,
                mocks.getDeploymentConfiguration());
        vaadinServlet.destroy();
        Assert.assertNull(service.getPwaRegistry());
    }

    private String testLocation(String base, String contextPath,
            String servletPath, String pathInfo) throws Exception {

        HttpServletRequest request = createNonIncludeRequest(base, contextPath,
                servletPath, pathInfo);

        VaadinServletService service = Mockito.mock(VaadinServletService.class);
        Mockito.doCallRealMethod().when(service)
                .getContextRootRelativePath(Mockito.any());
        String location = service.getContextRootRelativePath(
                servlet.createVaadinRequest(request));
        return location;
    }

    private HttpServletRequest createNonIncludeRequest(String base,
            String realContextPath, String realServletPath, String pathInfo)
            throws Exception {
        HttpServletRequest request = createRequest(base, realContextPath,
                realServletPath, pathInfo);
        when(request.getAttribute("jakarta.servlet.include.context_path"))
                .thenReturn(null);
        when(request.getAttribute("jakarta.servlet.include.servlet_path"))
                .thenReturn(null);

        return request;
    }

    /**
     * Creates a HttpServletRequest mock using the supplied parameters.
     *
     * @param base
     *            The base url, e.g. http://localhost:8080
     * @param contextPath
     *            The context path where the application is deployed, e.g.
     *            /mycontext
     * @param servletPath
     *            The servlet path to the servlet we are testing, e.g. /myapp
     * @param pathInfo
     *            Any text following the servlet path in the request, not
     *            including query parameters, e.g. /UIDL/
     * @return A mock HttpServletRequest object useful for testing
     * @throws MalformedURLException
     */
    private HttpServletRequest createRequest(String base, String contextPath,
            String servletPath, String pathInfo) throws MalformedURLException {
        URL url = new URL(base + contextPath + pathInfo);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("null.lock", new ReentrantLock()); // for session
        attributes.put("requestStartTime", System.currentTimeMillis()); // for
                                                                        // request
                                                                        // end
        when(request.isSecure())
                .thenReturn(url.getProtocol().equalsIgnoreCase("https"));
        when(request.getServerName()).thenReturn(url.getHost());
        when(request.getServerPort()).thenReturn(url.getPort());
        when(request.getRequestURI()).thenReturn(url.getPath());
        when(request.getContextPath()).thenReturn(contextPath);
        when(request.getPathInfo()).thenReturn(pathInfo);
        when(request.getServletPath()).thenReturn(servletPath);
        HttpSession session = Mockito.mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(Mockito.anyBoolean())).thenReturn(session);
        stubSessionAttributes(session, attributes);
        stubAttributes(request, attributes);
        return request;
    }

    private static void stubSessionAttributes(HttpSession session,
            Map<String, Object> attributes) {
        when(session.getAttribute(Mockito.anyString())).thenAnswer(
                invocation -> attributes.get(invocation.getArgument(0)));
        Mockito.doAnswer(invocation -> attributes.put(invocation.getArgument(0),
                invocation.getArgument(1))).when(session)
                .setAttribute(Mockito.anyString(), Mockito.anyString());
    }

    private static void stubAttributes(HttpServletRequest request,
            Map<String, Object> attributes) {
        when(request.getAttribute(Mockito.anyString())).thenAnswer(
                invocation -> attributes.get(invocation.getArgument(0)));
        Mockito.doAnswer(invocation -> attributes.put(invocation.getArgument(0),
                invocation.getArgument(1))).when(request)
                .setAttribute(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void filtersAreCalledWhenHandlingARequest() throws Exception {
        mocks = new MockServletServiceSessionSetup() {
            @Override
            public TestVaadinServlet createVaadinServlet() {
                return new TestVaadinServlet() {
                    @Override
                    public TestVaadinServletService createTestVaadinServletService() {
                        return new TestVaadinServletService(this,
                                getDeploymentConfiguration()) {
                            @Override
                            protected List<VaadinRequestInterceptor> createVaadinRequestInterceptors()
                                    throws ServiceException {
                                return Collections.singletonList(
                                        new MyRequestInterceptor());
                            }
                        };
                    }
                };
            }
        };
        service = mocks.getService();
        servlet = mocks.getServlet();

        VaadinRequest request = servlet.createVaadinRequest(createRequest(
                "http://dummy.host:8080/", "/contextpath", "/servlet", "/"));
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        service.getRequestHandlers().clear();
        service.getRequestHandlers().add(new ExceptionThrowingRequestHandler());

        try {
            service.handleRequest(request, response);
        } catch (ServiceException ex) {
            Assert.assertTrue(
                    "The exception was the one coming from RequestHandler",
                    ex.getMessage().contains("BOOM!"));
        }

        Assert.assertEquals("Filter was called on request start", "true",
                request.getAttribute("started"));
        Assert.assertEquals("Filter was called on exception handling", "true",
                request.getAttribute("exception handled"));
        Assert.assertEquals("Filter was called in the finally block", "true",
                request.getAttribute("ended"));
    }

    static class ExceptionThrowingRequestHandler implements RequestHandler {

        @Override
        public boolean handleRequest(VaadinSession session,
                VaadinRequest request, VaadinResponse response)
                throws IOException {
            throw new IllegalStateException("BOOM!");
        }
    }

    static class MyRequestInterceptor implements VaadinRequestInterceptor {

        @Override
        public void requestStart(VaadinRequest request,
                VaadinResponse response) {
            request.setAttribute("started", "true");
            // An exception thrown here will not be caught by other methods of
            // the filter!
        }

        @Override
        public void handleException(VaadinRequest request,
                VaadinResponse response, VaadinSession vaadinSession,
                Exception t) {
            if (t instanceof IllegalStateException ex) {
                Assert.assertEquals("BOOM!", ex.getMessage());
                request.setAttribute("exception handled", "true");
                return;
            }
            throw new AssertionError(
                    "Invalid exception thrown. Wanted <IllegalStateException> got <"
                            + t.getClass() + ">",
                    t);
        }

        @Override
        public void requestEnd(VaadinRequest request, VaadinResponse response,
                VaadinSession session) {
            request.setAttribute("ended", "true");
        }
    }
}
