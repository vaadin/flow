/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.communication.FaviconHandler;
import com.vaadin.flow.server.communication.IndexHtmlRequestHandler;
import com.vaadin.flow.server.communication.PushRequestHandler;
import com.vaadin.flow.server.frontend.FallbackChunk;
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
        addBootstrapHandler(handlers);
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
        return handlers;
    }

    private void addBootstrapHandler(List<RequestHandler> handlers) {
        if (getDeploymentConfiguration().isClientSideMode()) {
            handlers.add(0, new IndexHtmlRequestHandler());
            getLogger().debug("Using '{}' in clientSideMode",
                    IndexHtmlRequestHandler.class.getName());
        } else {
            handlers.add(0, new BootstrapHandler());
            getLogger().debug("Using '{}' in default mode",
                    BootstrapHandler.class.getName());
        }
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

    @Override
    public void init() throws ServiceException {
        DeploymentConfiguration deploymentConfiguration = getDeploymentConfiguration();
        Properties initParameters = deploymentConfiguration.getInitParameters();
        Object object = initParameters
                .get(DeploymentConfigurationFactory.FALLBACK_CHUNK);
        if (object instanceof FallbackChunk) {
            VaadinContext context = getContext();
            context.setAttribute(object);
        }
        super.init();
    }

    private boolean isOtherRequest(VaadinRequest request) {
        String type = request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER);
        return type == null
                || ApplicationConstants.REQUEST_TYPE_INIT.equals(type);
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
            @SuppressWarnings("deprecation")
            URL appUrl = getServlet()
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
            return getServlet().getServletContext().getResource(path);
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
}
