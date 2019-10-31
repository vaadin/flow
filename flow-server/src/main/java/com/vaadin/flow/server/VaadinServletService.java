/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import com.vaadin.flow.server.communication.PushRequestHandler;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.theme.AbstractTheme;

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
        handlers.add(0, new BootstrapHandler());
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
        return request.getParameter(
                ApplicationConstants.REQUEST_TYPE_PARAMETER) == null;
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
        return ApplicationRouteRegistry
                .getInstance(getContext());
    }

    @Override
    protected PwaRegistry getPwaRegistry() {
        return Optional.ofNullable(getServlet())
                .map(GenericServlet::getServletContext)
                .map(PwaRegistry::getInstance).orElse(null);
    }

    @Override
    public String resolveResource(String url, WebBrowser browser) {
        Objects.requireNonNull(url, "Url cannot be null");
        Objects.requireNonNull(browser, "Browser cannot be null");

        String frontendRootUrl;
        DeploymentConfiguration config = getDeploymentConfiguration();
        if (config.isCompatibilityMode()) {
            if (browser.isEs6Supported()) {
                frontendRootUrl = config.getEs6FrontendPrefix();
            } else {
                frontendRootUrl = config.getEs5FrontendPrefix();
            }
        } else {
            frontendRootUrl = config.getNpmFrontendPrefix();
        }

        return contextResolver.resolveVaadinUri(url, frontendRootUrl);
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
    public URL getResource(String path, WebBrowser browser,
            AbstractTheme theme) {
        return getResourceInServletContextOrWebJar(
                getThemedOrRawPath(path, browser, theme));
    }

    @Override
    public InputStream getResourceAsStream(String path, WebBrowser browser,
            AbstractTheme theme) {
        return getResourceInServletContextOrWebJarAsStream(
                getThemedOrRawPath(path, browser, theme));
    }

    @Override
    public Optional<String> getThemedUrl(String url, WebBrowser browser,
            AbstractTheme theme) {
        if (theme != null && !resolveResource(url, browser)
                .equals(getThemedOrRawPath(url, browser, theme))) {
            return Optional.of(theme.translateUrl(url));
        }
        return Optional.empty();
    }

    /**
     * Resolves the given {@code url} resource and tries to find a themed or raw
     * version.
     * <p>
     * The themed version is always tried first, with the raw version used as a
     * fallback.
     *
     * @param url
     *            the untranslated URL to the resource to find
     * @param browser
     *            the web browser to resolve for, relevant for es5 vs es6
     *            resolving
     * @param theme
     *            the theme to use for resolving, or <code>null</code> to not
     *            use a theme
     * @return the path to the themed resource if such exists, otherwise the
     *         resolved raw path
     */
    private String getThemedOrRawPath(String url, WebBrowser browser,
            AbstractTheme theme) {
        String resourcePath = resolveResource(url, browser);

        Optional<String> themeResourcePath = getThemeResourcePath(resourcePath,
                theme);
        if (themeResourcePath.isPresent()) {
            URL themeResource = getResourceInServletContextOrWebJar(
                    themeResourcePath.get());
            if (themeResource != null) {
                return themeResourcePath.get();
            }
        }
        return resourcePath;
    }

    /**
     * Gets the theme specific path for the given resource.
     *
     * @param path
     *            the raw path
     * @param theme
     *            the theme to use for resolving, possibly <code>null</code>
     * @return the path to the themed version or an empty optional if no themed
     *         version could be determined
     */
    private Optional<String> getThemeResourcePath(String path,
            AbstractTheme theme) {
        if (theme == null) {
            return Optional.empty();
        }
        String themeUrl = theme.translateUrl(path);
        if (path.equals(themeUrl)) {
            return Optional.empty();
        }

        return Optional.of(themeUrl);
    }

    /**
     * Finds the given resource in the servlet context or in a webjar.
     *
     * @param path
     *            the path inside servlet context, automatically translated as
     *            needed for webjars
     * @return a URL for the resource or <code>null</code> if no resource was
     *         found
     */
    public URL getResourceInServletContextOrWebJar(String path) {
        ServletContext servletContext = getServlet().getServletContext();
        try {
            URL url = servletContext.getResource(path);
            if (url != null) {
                return url;
            }
            Optional<String> webJarPath = getWebJarPath(path);
            if (webJarPath.isPresent()) {
                return servletContext.getResource(webJarPath.get());
            }
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
    private InputStream getResourceInServletContextOrWebJarAsStream(
            String path) {
        ServletContext servletContext = getServlet().getServletContext();
        InputStream stream = servletContext.getResourceAsStream(path);
        if (stream != null) {
            return stream;
        }
        Optional<String> webJarPath = getWebJarPath(path);
        if (webJarPath.isPresent()) {
            return servletContext.getResourceAsStream(webJarPath.get());
        }
        return null;
    }

    /**
     * Finds a resource for the given path inside a webjar.
     *
     * @param path
     *            the resource path
     * @return the path to the resource inside a webjar or <code>null</code> if
     *         the resource was not found in a webjar
     */
    private Optional<String> getWebJarPath(String path) {
        return getServlet().getWebJarServer()
                .flatMap(server -> server.getWebJarResourcePath(path));
    }

    @Override
    public String getContextRootRelativePath(VaadinRequest request) {
        assert request instanceof VaadinServletRequest;
        return ServletHelper.getContextRootRelativePath(
                (VaadinServletRequest) request) + "/";
    }

    @Override
    protected VaadinContext constructVaadinContext() {
        return new VaadinServletContext(getServlet().getServletContext());
    }
}
