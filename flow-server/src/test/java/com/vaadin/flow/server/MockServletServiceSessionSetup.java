package com.vaadin.flow.server;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.tests.util.MockDeploymentConfiguration;

public class MockServletServiceSessionSetup {

    public class TestVaadinServletService extends VaadinServletService {

        private List<DependencyFilter> dependencyFilterOverride;

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

    }

    public class TestVaadinServlet extends VaadinServlet {
        private Function<String, String> resolveOverride;
        private Function<String, Boolean> resourceFoundOverride;
        private Function<String, InputStream> resourceAsStreamOverride;

        @Override
        protected VaadinServletService createServletService()
                throws ServletException, ServiceException {
            service = new TestVaadinServletService(this,
                    deploymentConfiguration);
            service.init();
            return service;
        }

        @Override
        public ServletContext getServletContext() {
            return servletContext;
        }

        public void setResolveOverride(
                Function<String, String> resolveOverride) {
            this.resolveOverride = resolveOverride;
        }

        public void setResourceFoundOverride(
                Function<String, Boolean> resourceFoundOverride) {
            this.resourceFoundOverride = resourceFoundOverride;
        }

        public void setResourceAsStreamOverride(
                Function<String, InputStream> resourceAsStreamOverride) {
            this.resourceAsStreamOverride = resourceAsStreamOverride;
        }

        @Override
        public InputStream getResourceAsStream(String path) {
            if (resourceAsStreamOverride != null) {
                return resourceAsStreamOverride.apply(path);
            }

            return super.getResourceAsStream(path);
        }

        @Override
        public String resolveResource(String url) {
            if (resolveOverride != null) {
                return resolveOverride.apply(url);
            }
            return super.resolveResource(url);
        }

        @Override
        public boolean isResourceFound(String resolvedUrl) {
            if (resourceFoundOverride != null) {
                return resourceFoundOverride.apply(resolvedUrl);
            }
            return super.isResourceFound(resolvedUrl);
        }

        @Override
        boolean isInServletContext(String resolvedUrl) {
            if (resourceFoundOverride != null) {
                return resourceFoundOverride.apply(resolvedUrl);
            }
            return super.isInServletContext(resolvedUrl);
        }

        public void addServletContextResource(String path) {
            addServletContextResource(path, "This is " + path);
        }

        public void addServletContextResource(String path, String contents) {
            try {
                URL url = new URL("file://" + path);
                Mockito.when(getServletContext().getResource(path))
                        .thenReturn(url);
                Mockito.when(getServletContext().getResourceAsStream(path))
                        .thenAnswer(i -> {
                            return new ByteArrayInputStream(
                                    contents.getBytes(StandardCharsets.UTF_8));
                        });
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        public void addWebJarResource(String webjarContent) {
            // Webjars map /frontend/bower_components/foo/bar.html to
            // /webjars/foo/bar.html
            addServletContextResource("/webjars/" + webjarContent);
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
    private TestVaadinServlet servlet;
    private TestVaadinServletService service;
    private MockDeploymentConfiguration deploymentConfiguration = new MockDeploymentConfiguration();

    public MockServletServiceSessionSetup() throws Exception {
        MockitoAnnotations.initMocks(this);

        deploymentConfiguration.setXsrfProtectionEnabled(false);
        Mockito.when(session.getConfiguration())
                .thenReturn(deploymentConfiguration);

        Mockito.when(session.getBrowser()).thenReturn(browser);
        Mockito.when(session.getPushId()).thenReturn("fake push id");

        Mockito.when(wrappedSession.getHttpSession()).thenReturn(httpSession);
        Mockito.when(servletConfig.getServletContext())
                .thenReturn(servletContext);

        servlet = new TestVaadinServlet();
        Mockito.when(session.getService()).thenAnswer(i -> service);
        Mockito.when(session.hasLock()).thenReturn(true);
        Mockito.when(session.getPendingAccessQueue())
                .thenReturn(new LinkedBlockingDeque<>());
        Mockito.when(request.getWrappedSession()).thenReturn(wrappedSession);

        servlet.init(servletConfig);

        CurrentInstance.set(VaadinRequest.class, request);
        CurrentInstance.set(VaadinSession.class, session);
        CurrentInstance.set(VaadinService.class, service);

        Mockito.when(request.getServletPath()).thenReturn("");
        Mockito.when(browser.isEs6Supported()).thenReturn(true);

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

    public DeploymentConfiguration getDeploymentConfiguration() {
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

    public void setBrowserEs6(boolean browserEs6) {
        Mockito.when(browser.isEs6Supported()).thenReturn(browserEs6);
    }
}
