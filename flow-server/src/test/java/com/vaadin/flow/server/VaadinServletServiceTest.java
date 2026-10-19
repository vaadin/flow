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

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletService;
import com.vaadin.flow.server.communication.FaviconHandler;
import com.vaadin.flow.server.communication.WebComponentProvider;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.theme.AbstractTheme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for testing es6 resolution by browser capability. This is valid
 * only for bower mode where we need to decide ourselves.
 */
class VaadinServletServiceTest {

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

    @BeforeEach
    void setup() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        service = mocks.getService();

        servlet = mocks.getServlet();
    }

    @AfterEach
    void tearDown() {
        mocks.cleanup();
    }

    @Test
    void resolveNullThrows() {
        try {
            service.resolveResource(null);
            fail("null should not resolve");
        } catch (NullPointerException e) {
            assertEquals("Url cannot be null", e.getMessage());
        }
    }

    @Test
    void resolveResource() {
        assertEquals("", service.resolveResource(""));
        assertEquals("foo", service.resolveResource("foo"));
        assertEquals("/foo", service.resolveResource("context://foo"));
    }

    @Test
    void resolveResourceNPM_production() {
        mocks.setProductionMode(true);

        assertEquals("", service.resolveResource(""));
        assertEquals("foo", service.resolveResource("foo"));
        assertEquals("/foo", service.resolveResource("context://foo"));
    }

    @Test
    void getContextRootRelativePath_useVariousContextPathAndServletPathsAndPathInfo()
            throws Exception {
        String location;

        /* SERVLETS */
        // http://dummy.host:8080/contextpath/servlet
        // should return . (relative url resolving to /contextpath)
        location = testLocation("http://dummy.host:8080", "/contextpath",
                "/servlet", "");
        assertEquals("./../", location);

        // http://dummy.host:8080/contextpath/servlet/
        // should return ./.. (relative url resolving to /contextpath)
        location = testLocation("http://dummy.host:8080", "/contextpath",
                "/servlet", "/");
        assertEquals("./../", location);

        // http://dummy.host:8080/servlet
        // should return "."
        location = testLocation("http://dummy.host:8080", "", "/servlet", "");
        assertEquals("./../", location);

        // http://dummy.host/contextpath/servlet/extra/stuff
        // should return ./../.. (relative url resolving to /contextpath)
        location = testLocation("http://dummy.host", "/contextpath", "/servlet",
                "/extra/stuff");
        assertEquals("./../", location);

        // http://dummy.host/context/path/servlet/extra/stuff
        // should return ./../.. (relative url resolving to /context/path)
        location = testLocation("http://dummy.host", "/context/path",
                "/servlet", "/extra/stuff");
        assertEquals("./../", location);

    }

    @Test
    void init_classLoaderIsSetUsingServletContext() throws ServiceException {
        VaadinServlet vaadinServlet = mock(VaadinServlet.class);
        ServletContext context = mock(ServletContext.class);
        when(vaadinServlet.getServletContext()).thenReturn(context);
        Lookup lookup = mock(Lookup.class);
        when(context.getAttribute(Lookup.class.getName())).thenReturn(lookup);

        ClassLoader loader = mock(ClassLoader.class);
        when(context.getClassLoader()).thenReturn(loader);

        VaadinServletService servletService = new VaadinServletService(
                vaadinServlet, mocks.getDeploymentConfiguration()) {
            @Override
            protected Instantiator createInstantiator()
                    throws ServiceException {
                return mock(Instantiator.class);
            }

            @Override
            protected List<RequestHandler> createRequestHandlers()
                    throws ServiceException {
                return Collections.emptyList();
            }
        };

        servletService.init();

        assertSame(loader, servletService.getClassLoader());
    }

    @Test
    void getPwaRegistry_servletInitialized_getsRegistry() {
        MockServletServiceSessionSetup.TestVaadinServlet vaadinServlet = spy(
                mocks.getServlet());
        // Restore original behavior of getServletContext
        when(vaadinServlet.getServletContext()).thenAnswer(
                i -> vaadinServlet.getServletConfig().getServletContext());
        VaadinServletService service = new VaadinServletService(vaadinServlet,
                mocks.getDeploymentConfiguration());
        assertNotNull(service.getPwaRegistry());
    }

    @Test
    void getPwaRegistry_servletNotInitialized_getsNull() {
        MockServletServiceSessionSetup.TestVaadinServlet vaadinServlet = spy(
                mocks.getServlet());
        // Restore original behavior of getServletContext
        when(vaadinServlet.getServletContext()).thenAnswer(
                i -> vaadinServlet.getServletConfig().getServletContext());
        VaadinServletService service = new VaadinServletService(vaadinServlet,
                mocks.getDeploymentConfiguration());
        vaadinServlet.destroy();
        assertNull(service.getPwaRegistry());
    }

    @Test
    void requestCanCreateSession_webComponentScript_doesNotCreateSession() {
        WebComponentConfigurationRegistry registry = mock(
                WebComponentConfigurationRegistry.class);
        when(registry.hasConfigurations()).thenReturn(true);
        VaadinRequest request = createWebComponentRequest(registry);

        when(request.getPathInfo())
                .thenReturn("/web-component/my-component.js");
        assertFalse(service.requestCanCreateSession(request));

        when(request.getPathInfo())
                .thenReturn("/web-component/web-component-bootstrap.js");
        assertTrue(service.requestCanCreateSession(request));

        for (String path : new String[] { "/web-component/",
                "/web-component/my-component",
                "/web-component/my-component.html" }) {
            when(request.getPathInfo()).thenReturn(path);
            assertTrue(service.requestCanCreateSession(request), path);
        }

        when(registry.hasConfigurations()).thenReturn(false);
        when(request.getPathInfo())
                .thenReturn("/web-component/my-component.js");
        assertTrue(service.requestCanCreateSession(request));

        // Without a provider, nothing serves the script without a session
        when(registry.hasConfigurations()).thenReturn(true);
        service.getRequestHandlers()
                .removeIf(WebComponentProvider.class::isInstance);
        assertTrue(service.requestCanCreateSession(request));
    }

    @Test
    void handleSessionExpired_webComponentScript_servedBeforeCustomHandlers()
            throws Exception {
        WebComponentConfigurationRegistry registry = mock(
                WebComponentConfigurationRegistry.class);
        when(registry.hasConfigurations()).thenReturn(true);
        when(registry.getConfiguration("my-component"))
                .thenReturn(Optional.empty());
        VaadinRequest request = createWebComponentRequest(registry);
        when(request.getPathInfo())
                .thenReturn("/web-component/my-component.js");
        VaadinResponse response = mock(VaadinResponse.class);
        // Handlers added through ServiceInitEvent come first
        CatchAllSessionExpiredHandler customHandler = mock(
                CatchAllSessionExpiredHandler.class);
        service.getRequestHandlers().add(0, customHandler);

        service.handleSessionExpired(request, response);

        verify(customHandler, never()).handleSessionExpired(any(), any());
        verify(response).sendError(eq(HttpStatusCode.NOT_FOUND.getCode()),
                anyString());
    }

    @Test
    void handleSessionExpired_notServedByProvider_reachesCustomHandlers()
            throws Exception {
        WebComponentConfigurationRegistry registry = mock(
                WebComponentConfigurationRegistry.class);
        when(registry.hasConfigurations()).thenReturn(true);
        VaadinRequest request = createWebComponentRequest(registry);
        VaadinResponse response = mock(VaadinResponse.class);
        CatchAllSessionExpiredHandler customHandler = mock(
                CatchAllSessionExpiredHandler.class);
        when(customHandler.handleSessionExpired(request, response))
                .thenReturn(true);
        service.getRequestHandlers().add(0, customHandler);

        when(request.getPathInfo())
                .thenReturn("/web-component/web-component-bootstrap.js");
        service.handleSessionExpired(request, response);
        verify(customHandler).handleSessionExpired(request, response);

        // Without a provider, nothing else serves the script
        service.getRequestHandlers()
                .removeIf(WebComponentProvider.class::isInstance);
        when(request.getPathInfo())
                .thenReturn("/web-component/my-component.js");
        service.handleSessionExpired(request, response);
        verify(customHandler, times(2)).handleSessionExpired(request, response);
    }

    @Test
    void requestCanCreateSession_favicon_doesNotCreateSession() {
        VaadinServletRequest request = createFaviconRequest();
        assertFalse(service.requestCanCreateSession(request));

        // Below a servlet path, the favicon is a request like any other
        when(request.getServletPath()).thenReturn("/vaadin");
        assertTrue(service.requestCanCreateSession(request));

        // Below a context path as well
        when(request.getServletPath()).thenReturn("");
        when(request.getContextPath()).thenReturn("/ctx");
        assertTrue(service.requestCanCreateSession(request));

        // Without the handler, nothing answers the favicon without a session
        when(request.getContextPath()).thenReturn("");
        service.getRequestHandlers().removeIf(FaviconHandler.class::isInstance);
        assertTrue(service.requestCanCreateSession(request));
    }

    @Test
    void handleSessionExpired_favicon_notFoundBeforeCustomHandlers()
            throws Exception {
        VaadinServletRequest request = createFaviconRequest();
        VaadinResponse response = mock(VaadinResponse.class);
        // Handlers added through ServiceInitEvent come first
        CatchAllSessionExpiredHandler customHandler = mock(
                CatchAllSessionExpiredHandler.class);
        service.getRequestHandlers().add(0, customHandler);

        service.handleSessionExpired(request, response);

        verify(customHandler, never()).handleSessionExpired(any(), any());
        verify(response).setStatus(HttpStatusCode.NOT_FOUND.getCode());

        // Without the handler, the custom handlers answer it
        service.getRequestHandlers().removeIf(FaviconHandler.class::isInstance);
        service.handleSessionExpired(request, response);
        verify(customHandler).handleSessionExpired(request, response);
        verify(response).setStatus(HttpStatusCode.NOT_FOUND.getCode());
    }

    private static VaadinServletRequest createFaviconRequest() {
        VaadinServletRequest request = mock(VaadinServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getServletPath()).thenReturn("");
        when(request.getPathInfo()).thenReturn("/favicon.ico");
        return request;
    }

    private interface CatchAllSessionExpiredHandler
            extends RequestHandler, SessionExpiredHandler {
    }

    private static VaadinRequest createWebComponentRequest(
            WebComponentConfigurationRegistry registry) {
        VaadinContext context = mock(VaadinContext.class);
        when(context.getAttribute(eq(WebComponentConfigurationRegistry.class),
                any())).thenReturn(registry);
        VaadinService requestService = mock(VaadinService.class);
        when(requestService.getContext()).thenReturn(context);
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getService()).thenReturn(requestService);
        return request;
    }

    private String testLocation(String base, String contextPath,
            String servletPath, String pathInfo) throws Exception {

        HttpServletRequest request = createNonIncludeRequest(base, contextPath,
                servletPath, pathInfo);

        VaadinServletService servletService = mock(VaadinServletService.class);
        doCallRealMethod().when(servletService)
                .getContextRootRelativePath(any());
        return servletService.getContextRootRelativePath(
                servlet.createVaadinRequest(request));
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
        HttpServletRequest request = mock(HttpServletRequest.class);
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
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        stubSessionAttributes(session, attributes);
        stubAttributes(request, attributes);
        return request;
    }

    private static void stubSessionAttributes(HttpSession session,
            Map<String, Object> attributes) {
        when(session.getAttribute(anyString())).thenAnswer(
                invocation -> attributes.get(invocation.getArgument(0)));
        doAnswer(invocation -> attributes.put(invocation.getArgument(0),
                invocation.getArgument(1))).when(session)
                .setAttribute(anyString(), anyString());
    }

    private static void stubAttributes(HttpServletRequest request,
            Map<String, Object> attributes) {
        when(request.getAttribute(anyString())).thenAnswer(
                invocation -> attributes.get(invocation.getArgument(0)));
        doAnswer(invocation -> attributes.put(invocation.getArgument(0),
                invocation.getArgument(1))).when(request)
                .setAttribute(anyString(), any());
    }

    @Test
    void filtersAreCalledAndEventsFiredWhenHandlingARequest() throws Exception {
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
        VaadinResponse response = mock(VaadinResponse.class);
        service.getRequestHandlers().clear();
        RequestHandler handler = new ExceptionThrowingRequestHandler();
        service.getRequestHandlers().add(handler);
        List<RequestStartedEvent> started = new ArrayList<>();
        List<RequestEndedEvent> ended = new ArrayList<>();
        service.getEventBus().addListener(RequestStartedEvent.class,
                started::add);
        service.getEventBus().addListener(RequestEndedEvent.class, ended::add);

        try {
            service.handleRequest(request, response);
        } catch (ServiceException ex) {
            assertTrue(ex.getMessage().contains("BOOM!"),
                    "The exception was the one coming from RequestHandler");
        }

        assertEquals("true", request.getAttribute("started"),
                "Filter was called on request start");
        assertEquals("true", request.getAttribute("exception handled"),
                "Filter was called on exception handling");
        assertEquals("true", request.getAttribute("ended"),
                "Filter was called in the finally block");

        assertEquals(1, started.size());
        assertSame(request, started.get(0).getRequest());
        assertEquals(1, ended.size());
        RequestEndedEvent end = ended.get(0);
        assertSame(request, end.getRequest());
        assertSame(response, end.getResponse().orElseThrow());
        assertSame(handler, end.getHandler().orElseThrow());
        assertEquals("BOOM!", end.getFailure().orElseThrow().getMessage());
        assertTrue(end.getSession().isPresent());
    }

    @Test
    void getStaticResource_jarUrlOnJetty12_returnsUrlInsteadOfThrowing(
            @TempDir Path tempDir) throws Exception {
        // Reproduces the regression caused by Jetty 12.1.9 where
        // ServletContext.getResource() returns a jar:file:... URL for
        // resources packaged inside a JAR (e.g. vaadinPush.js in flow-push).
        // Path.of(jar:URI) throws FileSystemNotFoundException because no
        // FileSystem has been opened for the JAR, and the existence check in
        // VaadinServletService.getStaticResource catches only
        // URISyntaxException, so the unchecked exception escapes and the
        // request fails with HTTP 500.
        String resourcePath = "META-INF/resources/VAADIN/static/push/vaadinPush.js";
        Path jarFile = tempDir
                .resolve("flow-push-" + UUID.randomUUID() + ".jar");
        try (JarOutputStream jar = new JarOutputStream(
                Files.newOutputStream(jarFile))) {
            jar.putNextEntry(new JarEntry(resourcePath));
            jar.write("// push".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        URL jarResourceUrl = URI
                .create("jar:" + jarFile.toUri() + "!/" + resourcePath).toURL();

        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getServerInfo()).thenReturn("jetty/12.1.9");
        when(servletContext.getResource("/VAADIN/static/push/vaadinPush.js"))
                .thenReturn(jarResourceUrl);

        URL resolved = VaadinServletService.getStaticResource(servletContext,
                "/VAADIN/static/push/vaadinPush.js");

        assertNotNull(resolved,
                "An existing resource served from a JAR by Jetty 12 must be returned, "
                        + "not lost to FileSystemNotFoundException");
        assertEquals(jarResourceUrl, resolved);
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
                assertEquals("BOOM!", ex.getMessage());
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
