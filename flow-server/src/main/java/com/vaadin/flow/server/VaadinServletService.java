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

import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.communication.FaviconHandler;
import com.vaadin.flow.server.communication.IndexHtmlRequestHandler;
import com.vaadin.flow.server.communication.PushRequestHandler;
import com.vaadin.flow.server.communication.WebComponentProvider;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * A service implementation connected to a {@link VaadinServlet}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class VaadinServletService extends VaadinService {

    /**
     * Should never be used directly, always use {@link #getServlet()}.
     */
    private final VaadinServlet servlet;
    private final ServiceContextUriResolver contextResolver = new ServiceContextUriResolver();

    /**
     * Creates an instance connected to the given servlet and using the given
     * configuration.
     *
     * @param servlet
     *            the servlet which receives requests
     * @param deploymentConfiguration
     *            the configuration to use
     */
    public VaadinServletService(VaadinServlet servlet,
            DeploymentConfiguration deploymentConfiguration) {
        super(deploymentConfiguration);
        this.servlet = servlet;
    }

    /**
     * Creates a servlet service. This method is for use by dependency injection
     * frameworks etc. {@link #getServlet()} and {@link #getContext()} should be
     * overridden (or otherwise intercepted) to not return <code>null</code>.
     */
    protected VaadinServletService() {
        servlet = null;
    }

    @Override
    protected List<RequestHandler> createRequestHandlers()
            throws ServiceException {
        List<RequestHandler> handlers = super.createRequestHandlers();
        handlers.add(0, new FaviconHandler());

        Mode mode = getDeploymentConfiguration().getMode();
        if (mode == Mode.DEVELOPMENT_FRONTEND_LIVERELOAD
                || mode == Mode.DEVELOPMENT_BUNDLE) {
            Optional<DevModeHandler> handlerManager = DevModeHandlerManager
                    .getDevModeHandler(this);
            if (handlerManager.isPresent()) {
                DevModeHandler devModeHandler = handlerManager.get();
                // WebComponentProvider handler should run before DevModeHandler
                // to avoid responding with html contents when dev bundle is
                // not ready (e.g. dev-mode-not-ready.html)
                handlers.stream().filter(WebComponentProvider.class::isInstance)
                        .findFirst().map(handlers::indexOf)
                        .ifPresentOrElse(idx -> {
                            handlers.add(idx, devModeHandler);
                        }, () -> handlers.add(devModeHandler));
            } else if (mode == Mode.DEVELOPMENT_FRONTEND_LIVERELOAD) {
                getLogger().warn(
                        "DevModeHandlerManager not found, but dev server is enabled. "
                                + "Add 'com.vaadin.vaadin-dev-server' dependency or include it transitively via 'com.vaadin.vaadin-dev'.");
            }
        }

        // PushRequestHandler should run before DevModeHandler to avoid
        // responding with html contents when dev mode server is not ready
        // (e.g. dev-mode-not-ready.html)
        if (isAtmosphereAvailable()) {
            try {
                handlers.add(new PushRequestHandler(this));
            } catch (ServiceException e) {
                // Atmosphere init failed. Push won't work but we don't throw a
                // service exception as we don't want to prevent non-push
                // applications from working
                getLogger().warn(
                        "Error initializing Atmosphere. Push will not work.",
                        e);
            }
        }

        addBootstrapHandler(handlers);
        return handlers;
    }

    private void addBootstrapHandler(List<RequestHandler> handlers) {
        handlers.add(0, new IndexHtmlRequestHandler());
        getLogger().debug("Using '{}' in client mode bootstrapping",
                IndexHtmlRequestHandler.class.getName());
    }

    /**
     * Retrieves a reference to the servlet associated with this service. Should
     * be overridden (or otherwise intercepted) if the no-arg constructor is
     * used to prevent NPEs.
     *
     * @return A reference to the VaadinServlet this service is using
     */
    public VaadinServlet getServlet() {
        return servlet;
    }

    @Override
    public String getMimeType(String resourceName) {
        return getServlet().getServletContext().getMimeType(resourceName);
    }

    @Override
    protected boolean requestCanCreateSession(VaadinRequest request) {
        if (isOtherRequest(request)) {
            /*
             * I.e URIs that are not RPC calls or static file requests.
             */
            return true;
        }

        return false;
    }

    private boolean isOtherRequest(VaadinRequest request) {
        String type = request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER);
        return type == null
                || ApplicationConstants.REQUEST_TYPE_INIT.equals(type)
                || ApplicationConstants.REQUEST_TYPE_WEBCOMPONENT_RESYNC
                        .equals(type);
    }

    public static HttpServletRequest getCurrentServletRequest() {
        return VaadinServletRequest.getCurrent();
    }

    public static VaadinServletResponse getCurrentResponse() {
        return VaadinServletResponse.getCurrent();
    }

    @Override
    public String getServiceName() {
        return getServlet().getServletName();
    }

    @Override
    public String getMainDivId(VaadinSession session, VaadinRequest request) {
        String appId = null;
        try {
            URL appUrl = VaadinServlet
                    .getApplicationUrl((VaadinServletRequest) request);
            appId = appUrl.getPath();
        } catch (MalformedURLException e) {
            // Just ignore problem here
        }

        if (appId == null || "".equals(appId) || "/".equals(appId)) {
            appId = "ROOT";
        }
        appId = appId.replaceAll("[^a-zA-Z0-9]", "");
        // Add hashCode to the end, so that it is still (sort of)
        // predictable, but indicates that it should not be used in CSS
        // and such:
        int hashCode = appId.hashCode();
        if (hashCode < 0) {
            hashCode = -hashCode;
        }
        appId = appId + "-" + hashCode;
        return appId;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(VaadinServletService.class.getName());
    }

    @Override
    protected RouteRegistry getRouteRegistry() {
        return ApplicationRouteRegistry.getInstance(getContext());
    }

    @Override
    protected PwaRegistry getPwaRegistry() {
        return Optional.ofNullable(getServlet())
                // VaadinServlet.getServletConfig can return null if the servlet
                // is not yet initialized or has been destroyed
                // It may happen for example during Spring hot deploy restarts
                // and in this case getServletContext will throw an NPE
                .filter(s -> s.getServletConfig() != null)
                .map(GenericServlet::getServletContext)
                .map(PwaRegistry::getInstance).orElse(null);
    }

    @Override
    public String resolveResource(String url) {
        Objects.requireNonNull(url, "Url cannot be null");

        return contextResolver.resolveVaadinUri(url);
    }

    @Override
    public URL getStaticResource(String path) {
        try {
            return getStaticResource(getServlet().getServletContext(), path);
        } catch (MalformedURLException e) {
            getLogger().warn("Error finding resource for '{}'", path, e);
        }
        return null;
    }

    @Override
    public URL getResource(String path) {
        return getResourceInServletContext(resolveResource(path));
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return getResourceInServletContextAsStream(resolveResource(path));
    }

    /**
     * Finds the given resource in the servlet context.
     *
     * @param path
     *            the path inside servlet context
     * @return a URL for the resource or <code>null</code> if no resource was
     *         found
     */
    public URL getResourceInServletContext(String path) {
        ServletContext servletContext = getServlet().getServletContext();
        try {
            return servletContext.getResource(path);
        } catch (MalformedURLException e) {
            getLogger().warn("Error finding resource for '{}'", path, e);
        }
        return null;
    }

    /**
     * Opens a stream for the given resource found in the servlet context or in
     * a webjar.
     *
     * @param path
     *            the path inside servlet context, automatically translated as
     *            needed for webjars
     * @return a URL for the resource or <code>null</code> if no resource was
     *         found
     */
    private InputStream getResourceInServletContextAsStream(String path) {
        ServletContext servletContext = getServlet().getServletContext();
        return servletContext.getResourceAsStream(path);
    }

    @Override
    public String getContextRootRelativePath(VaadinRequest request) {
        assert request instanceof VaadinServletRequest;
        // Generate location from the request by finding how many "../" should
        // be added to the servlet path before we get to the context root

        // Should not take pathinfo into account because the base URI refers to
        // the servlet path

        String servletPath = ((VaadinServletRequest) request).getServletPath();
        assert servletPath != null;
        if (!servletPath.endsWith("/")) {
            servletPath += "/";
        }
        return HandlerHelper.getCancelingRelativePath(servletPath) + "/";
    }

    @Override
    protected VaadinContext constructVaadinContext() {
        return new VaadinServletContext(getServlet().getServletContext());
    }

    @Override
    protected void setDefaultClassLoader() {
        setClassLoader(getServlet().getServletContext().getClassLoader());
    }

    static URL getStaticResource(ServletContext servletContext, String path)
            throws MalformedURLException {
        URL url = servletContext.getResource(path);
        if (url != null && Optional.ofNullable(servletContext.getServerInfo())
                .orElse("").contains("jetty/12.")) {
            // Making sure that resource exists before returning it. Jetty
            // 12 may return URL for non-existing resource.
            try {
                if (!Files.exists(Path.of(url.toURI()))) {
                    url = null;
                }
            } catch (URISyntaxException e) {
                url = null;
            }
        }
        return url;
    }
}
