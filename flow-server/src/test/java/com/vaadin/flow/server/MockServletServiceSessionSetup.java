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
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Supplier;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.ResponseWriterTest.CapturingServletOutputStream;
import com.vaadin.flow.router.DefaultRoutePathProvider;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RoutePathProvider;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.TestRouteRegistry;
import com.vaadin.flow.server.AppShellRegistry.AppShellRegistryWrapper;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.tests.util.MockDeploymentConfiguration;

public class MockServletServiceSessionSetup {

    public class TestVaadinServletService extends VaadinServletService {

        private List<DependencyFilter> dependencyFilterOverride;
        private TestRouteRegistry routeRegistry;
        private Router router;
        private List<IndexHtmlRequestListener> indexHtmlRequestListeners = new ArrayList<>();
        private VaadinContext context;
        private List<RequestHandler> handlers;
        private Instantiator overriddenInstantiator;

        public TestVaadinServletService(TestVaadinServlet testVaadinServlet,
                DeploymentConfiguration deploymentConfiguration) {
            super(testVaadinServlet, deploymentConfiguration);
        }

        @Override
        public Iterable<DependencyFilter> getDependencyFilters() {
            if (dependencyFilterOverride != null) {
                return dependencyFilterOverride;
            }
            return super.getDependencyFilters();
        }

        public void setDependencyFilters(
                List<DependencyFilter> dependencyFilters) {
            dependencyFilterOverride = dependencyFilters;
        }

        public void setRouteRegistry(TestRouteRegistry routeRegistry) {
            this.routeRegistry = routeRegistry;
        }

        @Override
        protected List<RequestHandler> createRequestHandlers()
                throws ServiceException {
            List<RequestHandler> requestHandlers = super.createRequestHandlers();
            handlers = requestHandlers;
            return requestHandlers;
        }

        public List<RequestHandler> getRequestHandlers() {
            return handlers;
        }

        @Override
        protected RouteRegistry getRouteRegistry() {
            if (routeRegistry != null) {
                return routeRegistry;
            }
            return super.getRouteRegistry();
        }

        @Override
        public Router getRouter() {
            if (router != null) {
                return router;
            }
            return super.getRouter();
        }

        public void setRouter(Router router) {
            this.router = router;
        }

        public void addIndexHtmlRequestListener(
                IndexHtmlRequestListener listener) {
            indexHtmlRequestListeners.add(listener);
        }

        @Override
        public void modifyIndexHtmlResponse(IndexHtmlResponse response) {
            indexHtmlRequestListeners.forEach(
                    listener -> listener.modifyIndexHtmlResponse(response));

            super.modifyIndexHtmlResponse(response);
        }

        @Override
        public VaadinContext getContext() {
            if (context != null) {
                return context;
            }
            return super.getContext();
        }

        public void setContext(VaadinContext context) {
            this.context = context;
        }

        @Override
        protected Instantiator createInstantiator() throws ServiceException {
            return Mockito.spy(super.createInstantiator());
        }

        @Override
        public Instantiator getInstantiator() {
            if (this.overriddenInstantiator != null) {
                return overriddenInstantiator;
            }
            return super.getInstantiator();
        }

        public void setInstantiator(Instantiator instantiator) {
            this.overriddenInstantiator = instantiator;
        }
    }

    public class TestVaadinServlet extends VaadinServlet {

        @Override
        protected VaadinServletService createServletService()
                throws ServletException, ServiceException {
            service = createTestVaadinServletService();
            service.init();
            return service;
        }

        public TestVaadinServletService createTestVaadinServletService() {
            return new TestVaadinServletService(this, deploymentConfiguration);
        }

        @Override
        public ServletContext getServletContext() {
            return servletContext;
        }

        public void addServletContextResource(String path) {
            addServletContextResource(path, "This is " + path);
        }

        public void addServletContextResource(String path, String contents) {
            try {
                Supplier<InputStream> streamSupplier = new Supplier<InputStream>() {
                    @Override
                    public InputStream get() {
                        return new ByteArrayInputStream(
                                contents.getBytes(StandardCharsets.UTF_8));
                    }
                };
                URL url = new URL(null, "file://" + path,
                        new URLStreamHandler() {
                            @Override
                            protected URLConnection openConnection(URL u)
                                    throws IOException {
                                return new URLConnection(u) {
                                    @Override
                                    public void connect() throws IOException {
                                    }

                                    @Override
                                    public InputStream getInputStream()
                                            throws IOException {
                                        return streamSupplier.get();
                                    }
                                };
                            }
                        });
                Mockito.when(getServletContext().getResource(path))
                        .thenReturn(url);
                Mockito.when(getServletContext().getResourceAsStream(path))
                        .thenAnswer(i -> streamSupplier.get());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        public void addWebJarResource(String webjarContent) {
            // Webjars map /frontend/bower_components/foo/bar.html to
            // /webjars/foo/bar.html
            addServletContextResource("/webjars/" + webjarContent);
        }

        public void verifyServletContextResourceLoadedOnce(String resource) {
            Mockito.verify(servlet.getServletContext())
                    .getResourceAsStream(resource);

        }

        public void verifyServletContextResourceNotLoaded(String resource) {
            Mockito.verify(servlet.getServletContext(), Mockito.never())
                    .getResourceAsStream(resource);
        }
    }

    public static class TestVaadinServletResponse
            extends VaadinServletResponse {
        private int errorCode;
        private String errorMessage;

        private CapturingServletOutputStream output = new CapturingServletOutputStream();

        private String type;

        private TestVaadinServletResponse(HttpServletResponse response,
                VaadinServletService vaadinService) {
            super(response, vaadinService);
        }

        @Override
        public void sendError(int errorCode, String message)
                throws java.io.IOException {
            this.errorCode = errorCode;
            errorMessage = message;
        }

        @Override
        public void sendError(int sc) throws IOException {
            errorCode = sc;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public void setStatus(int sc) {
            errorCode = sc;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return output;
        }

        public String getPayload() {
            return new String(output.getOutput());
        }

        @Override
        public void setContentType(String type) {
            this.type = type;
        }

        @Override
        public String getContentType() {
            return type;
        }
    }

    @Mock
    private ServletContext servletContext;
    @Mock
    private VaadinServletRequest request;
    @Mock
    private VaadinSession session;
    @Mock
    private WebBrowser browser;
    @Mock
    private WrappedHttpSession wrappedSession;
    @Mock
    private HttpSession httpSession;
    @Mock
    private ServletConfig servletConfig;
    @Mock
    private Lookup lookup;
    @Mock
    private ResourceProvider resourceProvider;
    private TestVaadinServlet servlet;
    private TestVaadinServletService service;
    private MockDeploymentConfiguration deploymentConfiguration = new MockDeploymentConfiguration();
    @Mock
    private StaticFileHandlerFactory staticFileHandlerFactory;

    public MockServletServiceSessionSetup() throws RuntimeException {
        this(true);
    }

    public MockServletServiceSessionSetup(boolean sessionAvailable)
            throws RuntimeException {
        MockitoAnnotations.initMocks(this);
        servlet = createVaadinServlet();

        deploymentConfiguration.setXsrfProtectionEnabled(false);
        deploymentConfiguration.setProjectFolder(new File("./"));
        Mockito.doAnswer(
                invocation -> servletContext.getClass().getClassLoader())
                .when(servletContext).getClassLoader();
        Mockito.when(servletConfig.getServletContext())
                .thenReturn(servletContext);

        Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);
        DefaultRoutePathProvider routePathProvider = new DefaultRoutePathProvider();
        Mockito.when(lookup.lookup(RoutePathProvider.class))
                .thenReturn(routePathProvider);
        Mockito.when(lookup.lookup(StaticFileHandlerFactory.class))
                .thenReturn(staticFileHandlerFactory);

        try {
            Mockito.when(resourceProvider
                    .getClientResourceAsStream("META-INF/resources/"
                            + ApplicationConstants.CLIENT_ENGINE_PATH
                            + "/compile.properties"))
                    .thenAnswer(invocation -> new ByteArrayInputStream(
                            "jsFile=foo".getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Mockito.when(
                resourceProvider.getApplicationResource(Mockito.anyString()))
                .thenAnswer(invocation -> {
                    return MockServletServiceSessionSetup.class
                            .getResource("/" + invocation.getArgument(0));
                });

        configureLookup(lookup);

        try {
            servlet.init(servletConfig);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }

        if (sessionAvailable) {
            Mockito.when(session.getConfiguration())
                    .thenReturn(deploymentConfiguration);

            Mockito.when(session.getBrowser()).thenReturn(browser);
            Mockito.when(session.getPushId()).thenReturn("fake push id");
            Mockito.when(session.getLocale()).thenReturn(Locale.ENGLISH);

            Mockito.when(wrappedSession.getHttpSession())
                    .thenReturn(httpSession);
            Mockito.when(session.getState())
                    .thenReturn(VaadinSessionState.OPEN);

            Mockito.when(session.getService()).thenAnswer(i -> service);
            Mockito.when(session.hasLock()).thenReturn(true);
            Mockito.when(session.getPendingAccessQueue())
                    .thenReturn(new LinkedBlockingDeque<>());
            Mockito.when(request.getWrappedSession())
                    .thenReturn(wrappedSession);
            SessionRouteRegistry sessionRegistry = (SessionRouteRegistry) SessionRouteRegistry
                    .getSessionRegistry(session);
            Mockito.when(session.getAttribute(SessionRouteRegistry.class))
                    .thenReturn(sessionRegistry);
        } else {
            session = null;
        }

        CurrentInstance.set(VaadinRequest.class, request);
        CurrentInstance.set(VaadinService.class, service);
        if (sessionAvailable) {
            CurrentInstance.set(VaadinSession.class, session);
        }

        Mockito.when(request.getServletPath()).thenReturn("");
    }

    public TestVaadinServlet createVaadinServlet() {
        return new TestVaadinServlet();
    }

    protected void configureLookup(Lookup lookup) {

    }

    public TestVaadinServletService getService() {
        return service;
    }

    public TestVaadinServlet getServlet() {
        return servlet;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public HttpSession getHttpSession() {
        return httpSession;
    }

    public WrappedHttpSession getWrappedSession() {
        return wrappedSession;
    }

    public VaadinSession getSession() {
        return session;
    }

    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    public MockDeploymentConfiguration getDeploymentConfiguration() {
        return deploymentConfiguration;
    }

    public WebBrowser getBrowser() {
        return browser;
    }

    public void cleanup() {
        CurrentInstance.clearAll();
    }

    public void setProductionMode(boolean productionMode) {
        deploymentConfiguration.setProductionMode(productionMode);
    }

    public void setAppShellRegistry(AppShellRegistry appShellRegistry) {
        Mockito.when(servletContext
                .getAttribute(AppShellRegistryWrapper.class.getName()))
                .thenReturn(new AppShellRegistryWrapper(appShellRegistry));
    }

    public TestVaadinServletResponse createResponse() throws IOException {
        HttpServletResponse httpServletResponse = Mockito
                .mock(HttpServletResponse.class);
        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(httpServletResponse.getOutputStream()).thenReturn(out);
        return new TestVaadinServletResponse(httpServletResponse, getService());

    }

    public VaadinRequest createRequest(MockServletServiceSessionSetup mocks,
            String path, String queryString) {
        return createRequest(mocks, path, "", queryString);
    }

    public VaadinRequest createRequest(MockServletServiceSessionSetup mocks,
            String path, String servletPath, String queryString) {

        QueryParameters queryParams = QueryParameters.fromString(queryString);
        Map<String, List<String>> params = queryParams.getParameters();
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
                return servletPath;
            }

            @Override
            public ServletContext getServletContext() {
                return mocks.getServletContext();
            }

            @Override
            public String getParameter(String name) {
                if (!params.containsKey(name)) {
                    return null;
                }
                return params.get(name).get(0);
            }

            @Override
            public StringBuffer getRequestURL() {
                return new StringBuffer(
                        "http://localhost:8888" + servletPath + getPathInfo());
            }
        };
    }
}
