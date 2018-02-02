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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.router.legacy.RouterConfigurator;
import com.vaadin.flow.server.ServletHelper.RequestType;
import com.vaadin.flow.server.VaadinServletConfiguration.InitParameterName;
import com.vaadin.flow.server.webjar.WebJarServer;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.theme.AbstractTheme;

/**
 * The main servlet, which handles all incoming requests to the application.
 * <p>
 * This servlet is typically subclassed in all applications to provide servlet
 * mappings and init parameters. Together with a {@literal web.xml} file, it is
 * also possible to use this class directly.
 * <p>
 * Internally sets up a {@link VaadinService} through
 * {@link #createServletService(DeploymentConfiguration)} and delegates handling
 * of most requests to that.
 *
 * @author Vaadin Ltd
 */
public class VaadinServlet extends HttpServlet {
    private VaadinServletService servletService;
    private StaticFileServer staticFileServer;
    private WebJarServer webJarServer;

    private final ReflectionCache<AbstractTheme, ConcurrentHashMap<String, String>> themeTranslations = new ReflectionCache<>(
            type -> new ConcurrentHashMap<>());

    /**
     * Called by the servlet container to indicate to a servlet that the servlet
     * is being placed into service.
     *
     * @param servletConfig
     *            the object containing the servlet's configuration and
     *            initialization parameters
     * @throws ServletException
     *             if an exception has occurred that interferes with the
     *             servlet's normal operation.
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        CurrentInstance.clearAll();
        super.init(servletConfig);
        try {
            servletService = createServletService();
        } catch (ServiceException e) {
            throw new ServletException("Could not initialize VaadinServlet", e);
        }

        staticFileServer = new StaticFileServer(servletService);

        if (servletService.getDeploymentConfiguration().areWebJarsEnabled()) {
            webJarServer = new WebJarServer(
                    servletService.getDeploymentConfiguration());
        }

        // Sets current service even though there are no request and response
        servletService.setCurrentInstances(null, null);

        servletInitialized();
        CurrentInstance.clearAll();

    }

    private void readUiFromEnclosingClass(Properties initParameters) {
        Class<?> enclosingClass = getClass().getEnclosingClass();

        if (enclosingClass != null
                && UI.class.isAssignableFrom(enclosingClass)) {
            initParameters.put(VaadinSession.UI_PARAMETER,
                    enclosingClass.getName());
        }
    }

    private void readConfigurationAnnotation(Properties initParameters)
            throws ServletException {
        Optional<VaadinServletConfiguration> optionalConfigAnnotation = AnnotationReader
                .getAnnotationFor(getClass(), VaadinServletConfiguration.class);
        if (optionalConfigAnnotation.isPresent()) {
            VaadinServletConfiguration configuration = optionalConfigAnnotation
                    .get();
            if (configuration.ui() == UI.class
                    && configuration
                            .routerConfigurator() == RouterConfigurator.class
                    && !configuration.usingNewRouting()) {
                throw new ServletException(String.format(
                        "At least one of 'ui' and 'routerConfigurator' must be defined in @%s if 'usingNewRouting' is false",
                        VaadinServletConfiguration.class.getSimpleName()));
            }

            Method[] methods = VaadinServletConfiguration.class
                    .getDeclaredMethods();
            for (Method method : methods) {
                InitParameterName name = method
                        .getAnnotation(InitParameterName.class);
                assert name != null : "All methods declared in VaadinServletConfiguration should have a @InitParameterName annotation";

                try {
                    Object value = method.invoke(configuration);

                    String stringValue;
                    if (value instanceof Class<?>) {
                        stringValue = ((Class<?>) value).getName();
                    } else {
                        stringValue = value.toString();
                    }

                    initParameters.setProperty(name.value(), stringValue);
                } catch (Exception e) {
                    // This should never happen
                    throw new ServletException(
                            "Could not read @VaadinServletConfiguration value "
                                    + method.getName(),
                            e);
                }
            }
        }
    }

    protected void servletInitialized() throws ServletException {
        // Empty by default
    }

    /**
     * Gets the currently used Vaadin servlet. The current servlet is
     * automatically defined when initializing the servlet and when processing
     * requests to the server (see {@link ThreadLocal}) and in
     * {@link VaadinSession#access(Command)} and {@link UI#access(Command)}. In
     * other cases, (e.g. from background threads), the current servlet is not
     * automatically defined.
     * <p>
     * The current servlet is derived from the current service using
     * {@link VaadinService#getCurrent()}
     *
     * @return the current Vaadin servlet instance if available, otherwise
     *         <code>null</code>
     *
     * @since 7.0
     */
    public static VaadinServlet getCurrent() {
        VaadinService vaadinService = CurrentInstance.get(VaadinService.class);
        if (vaadinService instanceof VaadinServletService) {
            VaadinServletService vss = (VaadinServletService) vaadinService;
            return vss.getServlet();
        } else {
            return null;
        }
    }

    /**
     * Creates a deployment configuration to be used for the creation of a
     * {@link VaadinService}. Intended to be used by dependency injection
     * frameworks.
     *
     * @return the created deployment configuration
     *
     * @throws ServletException
     *             if construction of the {@link Properties} for
     *             {@link #createDeploymentConfiguration(Properties)} fails
     */
    protected DeploymentConfiguration createDeploymentConfiguration()
            throws ServletException {
        Properties initParameters = new Properties();

        readUiFromEnclosingClass(initParameters);

        readConfigurationAnnotation(initParameters);

        // Read default parameters from server.xml
        final ServletContext context = getServletConfig().getServletContext();
        for (final Enumeration<String> e = context.getInitParameterNames(); e
                .hasMoreElements();) {
            final String name = e.nextElement();
            initParameters.setProperty(name, context.getInitParameter(name));
        }

        // Override with application config from web.xml
        for (final Enumeration<String> e = getServletConfig()
                .getInitParameterNames(); e.hasMoreElements();) {
            final String name = e.nextElement();
            initParameters.setProperty(name,
                    getServletConfig().getInitParameter(name));
        }

        return createDeploymentConfiguration(initParameters);
    }

    /**
     * Creates a deployment configuration to be used for the creation of a
     * {@link VaadinService}. Override this if you want to override certain
     * properties.
     *
     * @param initParameters
     *            the context-param and init-param values as properties
     * @return the created deployment configuration
     */
    protected DeploymentConfiguration createDeploymentConfiguration(
            Properties initParameters) {
        return new DefaultDeploymentConfiguration(getClass(), initParameters,
                this::scanForResources);
    }

    protected void scanForResources(String basePath,
            Predicate<String> consumer) {
        Deque<String> queue = new ArrayDeque<>();
        queue.add(basePath);

        ServletContext context = getServletContext();
        int prefixLength = basePath.length();

        while (!queue.isEmpty()) {
            String path = queue.removeLast();
            boolean visit = consumer.test(path.substring(prefixLength));

            if (visit && path.endsWith("/")) {
                Set<String> resourcePaths = context.getResourcePaths(path);
                if (resourcePaths != null) {
                    queue.addAll(resourcePaths);
                }
            }
        }
    }

    /**
     * Creates a vaadin servlet service. This method functions as a layer of
     * indirection between {@link #init(ServletConfig)} and
     * {@link #createServletService(DeploymentConfiguration)} so dependency
     * injection frameworks can call {@link #createDeploymentConfiguration()}
     * when creating a vaadin servlet service lazily.
     *
     * @return the created vaadin servlet service
     *
     * @throws ServletException
     *             if creating a deployment configuration fails
     * @throws ServiceException
     *             if creating the vaadin servlet service fails
     */
    protected VaadinServletService createServletService()
            throws ServletException, ServiceException {
        return createServletService(createDeploymentConfiguration());
    }

    /**
     * Creates a vaadin servlet service.
     *
     * @param deploymentConfiguration
     *            the deployment configuration to be used
     *
     * @return the created vaadin servlet service
     *
     * @throws ServiceException
     *             if creating the vaadin servlet service fails
     */
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        VaadinServletService service = new VaadinServletService(this,
                deploymentConfiguration);
        service.init();
        return service;
    }

    /**
     * Receives standard HTTP requests from the public service method and
     * dispatches them.
     *
     * @param request
     *            the object that contains the request the client made of the
     *            servlet.
     * @param response
     *            the object that contains the response the servlet returns to
     *            the client.
     * @throws ServletException
     *             if an input or output error occurs while the servlet is
     *             handling the TRACE request.
     * @throws IOException
     *             if the request for the TRACE cannot be handled.
     */
    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        // Handle context root request without trailing slash, see #9921
        if (handleContextOrServletRootWithoutSlash(request, response)) {
            return;
        }

        if (serveStaticOrWebJarRequest(request, response)) {
            return;
        }

        CurrentInstance.clearAll();

        VaadinServletRequest vaadinRequest = createVaadinRequest(request);
        VaadinServletResponse vaadinResponse = createVaadinResponse(response);
        if (!ensureCookiesEnabled(vaadinRequest, vaadinResponse)) {
            return;
        }

        try {
            getService().handleRequest(vaadinRequest, vaadinResponse);
        } catch (ServiceException e) {
            throw new ServletException(e);
        }

    }

    /**
     * Handles a request by serving a static file or a file from a WebJar.
     *
     * @param request
     *            the HTTP servlet request object that contains the request the
     *            client made of the servlet
     *
     * @param response
     *            the HTTP servlet response object that contains the response
     *            the servlet returns to the client
     * @return <code>true</code> if the request was handled a response written;
     *         oterwise <code>false</code>
     *
     * @exception IOException
     *                if an input or output error occurs while the servlet is
     *                handling the HTTP request
     *
     * @exception ServletException
     *                if the HTTP request cannot be handled
     */
    protected boolean serveStaticOrWebJarRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        if (staticFileServer.isStaticResourceRequest(request)) {
            staticFileServer.serveStaticResource(request, response);
            return true;
        }
        
        return webJarServer != null && webJarServer
                .tryServeWebJarResource(request, response);
    }

    /**
     * Invoked for every request to this servlet to potentially send a redirect
     * to avoid problems with requests to the context root with no trailing
     * slash.
     *
     * @param request
     *            the processed request
     * @param response
     *            the processed response
     * @return <code>true</code> if a redirect has been sent and the request
     *         should not be processed further; <code>false</code> if the
     *         request should be processed as usual
     * @throws IOException
     *             If an input or output exception occurs
     */
    protected boolean handleContextOrServletRootWithoutSlash(
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Query parameters like "?a=b" are handled by the servlet container but
        // path parameter (e.g. ;jsessionid=) needs to be handled here
        String location = request.getRequestURI();

        String lastPathParameter = getLastPathParameter(location);
        location = location.substring(0,
                location.length() - lastPathParameter.length());

        if ((request.getPathInfo() == null || "/".equals(request.getPathInfo()))
                && !location.endsWith("/")) {
            /*
             * Path info is for the root but request URI doesn't end with a
             * slash -> redirect to the same URI but with an ending slash.
             */
            location = location + "/" + lastPathParameter;
            String queryString = request.getQueryString();
            if (queryString != null) {
                // Prevent HTTP Response splitting in case the server doesn't
                queryString = queryString.replaceAll("[\\r\\n]", "");
                location += '?' + queryString;
            }
            response.sendRedirect(location);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Finds any path parameter added to the last part of the uri. A path
     * parameter is any string separated by ";" from the path and ends in / or
     * at the end of the string.
     * <p>
     * For example the uri http://myhost.com/foo;a=1/bar;b=1 contains two path
     * parameters, {@literal a=1} related to {@literal /foo} and {@literal b=1}
     * related to /bar.
     * <p>
     * For http://myhost.com/foo;a=1/bar;b=1 this method will return ;b=1
     *
     * @since 7.2
     * @param uri
     *            a URI
     * @return the last path parameter of the uri including the semicolon or an
     *         empty string. Never null.
     */
    protected static String getLastPathParameter(String uri) {
        int lastPathStart = uri.lastIndexOf('/');
        if (lastPathStart == -1) {
            return "";
        }

        int semicolonPos = uri.indexOf(';', lastPathStart);
        if (semicolonPos < 0) {
            // No path parameter for the last part
            return "";
        } else {
            // This includes the semicolon.
            String semicolonString = uri.substring(semicolonPos);
            return semicolonString;
        }
    }

    private VaadinServletResponse createVaadinResponse(
            HttpServletResponse response) {
        return new VaadinServletResponse(response, getService());
    }

    /**
     * Creates a Vaadin request for a http servlet request. This method can be
     * overridden if the Vaadin request should have special properties.
     *
     * @param request
     *            the original http servlet request
     * @return a Vaadin request for the original request
     */
    protected VaadinServletRequest createVaadinRequest(
            HttpServletRequest request) {
        return new VaadinServletRequest(request, getService());
    }

    /**
     * Gets the Vaadin service for this servlet.
     *
     * @return the Vaadin service
     */
    public VaadinServletService getService() {
        return servletService;
    }

    /**
     * Check that cookie support is enabled in the browser. Only checks UIDL
     * requests.
     *
     * @param request
     *            The request from the browser
     * @param response
     *            The response to which an error can be written
     * @return false if cookies are disabled, true otherwise
     * @throws IOException
     */
    private boolean ensureCookiesEnabled(VaadinServletRequest request,
            VaadinServletResponse response) throws IOException {
        if (ServletHelper.isRequestType(request, RequestType.UIDL)) {
            // In all other but the first UIDL request a cookie should be
            // returned by the browser.
            // This can be removed if cookieless mode (#3228) is supported
            if (request.getRequestedSessionId() == null) {
                // User has cookies disabled
                SystemMessages systemMessages = getService().getSystemMessages(
                        ServletHelper.findLocale(null, request), request);
                getService().writeStringResponse(response,
                        JsonConstants.JSON_CONTENT_TYPE,
                        VaadinService.createCriticalNotificationJSON(
                                systemMessages.getCookiesDisabledCaption(),
                                systemMessages.getCookiesDisabledMessage(),
                                null, systemMessages.getCookiesDisabledURL()));
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the current application URL from request.
     *
     * @param request
     *            the HTTP request.
     * @throws MalformedURLException
     *             if the application is denied access to the persistent data
     *             store represented by the given URL.
     *
     * @deprecated As of 7.0. Will likely change or be removed in a future
     *             version
     */
    @Deprecated
    protected URL getApplicationUrl(HttpServletRequest request)
            throws MalformedURLException {
        final URL reqURL = new URL((request.isSecure() ? "https://" : "http://")
                + request.getServerName()
                + ((request.isSecure() && request.getServerPort() == 443)
                        || (!request.isSecure()
                                && request.getServerPort() == 80) ? ""
                                        : ":" + request.getServerPort())
                + request.getRequestURI());
        String servletPath;
        if (request
                .getAttribute("javax.servlet.include.servlet_path") != null) {
            // this is an include request
            servletPath = request
                    .getAttribute("javax.servlet.include.context_path")
                    .toString()
                    + request
                            .getAttribute("javax.servlet.include.servlet_path");

        } else {
            servletPath = request.getContextPath() + request.getServletPath();
        }

        if (servletPath.length() == 0
                || servletPath.charAt(servletPath.length() - 1) != '/') {
            servletPath = servletPath + "/";
        }
        URL u = new URL(reqURL, servletPath);
        return u;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.GenericServlet#destroy()
     */
    @Override
    public void destroy() {
        super.destroy();
        getService().destroy();
    }

    /**
     * Escapes characters to html entities. An exception is made for some "safe
     * characters" to keep the text somewhat readable.
     *
     * @param unsafe
     * @return a safe string to be added inside an html tag
     *
     * @deprecated As of 7.0. Will likely change or be removed in a future
     *             version
     */
    @Deprecated
    public static String safeEscapeForHtml(String unsafe) {
        if (null == unsafe) {
            return null;
        }
        StringBuilder safe = new StringBuilder();
        char[] charArray = unsafe.toCharArray();
        for (char c : charArray) {
            if (isSafe(c)) {
                safe.append(c);
            } else {
                safe.append("&#");
                safe.append((int) c);
                safe.append(";");
            }
        }

        return safe.toString();
    }

    private static boolean isSafe(char c) {
        return //
        c > 47 && c < 58 || // alphanum
                c > 64 && c < 91 || // A-Z
                c > 96 && c < 123 // a-z
        ;
    }

    /**
     * Get the validated theme url for given string import. Result will be
     * cached for future use if production mode is enabled.
     *
     * @param theme
     *            Theme to resolve url translation for
     * @param urlToTranslate
     *            url value to translate
     * @return translated url path
     */
    public String getUrlTranslation(AbstractTheme theme,
            String urlToTranslate) {
        if (getService().getDeploymentConfiguration().isProductionMode()) {
            return themeTranslations.get(theme.getClass()).computeIfAbsent(
                    urlToTranslate, key -> computeUrlTranslation(theme, key));
        } else {
            return computeUrlTranslation(theme, urlToTranslate);
        }
    }

    private final String computeUrlTranslation(AbstractTheme theme,
            String urlToTranslate) {
        String translatedUrl = theme.translateUrl(urlToTranslate);
        if (translatedUrl.equals(urlToTranslate)
                || resourceIsFound(translatedUrl)) {
            return translatedUrl;
        } else {
            return urlToTranslate;
        }
    }

    private boolean resourceIsFound(String url) {
        VaadinUriResolverFactory uriResolverFactory = VaadinSession.getCurrent()
                .getAttribute(VaadinUriResolverFactory.class);
        String resolvedUrl = uriResolverFactory
                .toServletContextPath(VaadinRequest.getCurrent(), url);

        try {
            return inServletContext(resolvedUrl) || inWebJar(resolvedUrl);
        } catch (IOException e) {
            LoggerFactory.getLogger(VaadinServlet.class)
                    .trace("Failed to parse url.", e);
            return false;
        }
    }

    private boolean inServletContext(String resolvedUrl)
            throws MalformedURLException {
        return getServletContext().getResource(resolvedUrl) != null;
    }

    private boolean inWebJar(String resolvedUrl) throws IOException {
        return webJarServer != null && webJarServer
                .hasWebJarResource(resolvedUrl, getServletContext());
    }
}
