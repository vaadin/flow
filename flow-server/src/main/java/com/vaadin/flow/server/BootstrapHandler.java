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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.BootstrapHandlerHelper;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.UsageStatisticsExporter;
import com.vaadin.flow.router.InvalidLocationException;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.LocationUtil;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.communication.AtmospherePushConnection;
import com.vaadin.flow.server.communication.IndexHtmlRequestHandler;
import com.vaadin.flow.server.communication.PushConnectionFactory;
import com.vaadin.flow.server.communication.UidlWriter;
import com.vaadin.flow.server.frontend.CssBundler;
import com.vaadin.flow.server.frontend.DevBundleUtils;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.ThemeUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.VaadinUriResolver;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import static com.vaadin.flow.server.Constants.VAADIN_MAPPING;
import static com.vaadin.flow.server.frontend.FrontendUtils.EXPORT_CHUNK;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Request handler which handles bootstrapping of the application, i.e. the
 * initial GET request.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class BootstrapHandler extends SynchronizedRequestHandler {

    public static final String SERVICE_WORKER_HEADER = "Service-Worker";

    private static final CharSequence GWT_STAT_EVENTS_JS = "if (typeof window.__gwtStatsEvent != 'function') {"
            + "window.Vaadin.Flow.gwtStatsEvents = [];"
            + "window.__gwtStatsEvent = function(event) {"
            + "window.Vaadin.Flow.gwtStatsEvents.push(event); "
            + "return true;};};";

    //@formatter:off
    protected static final String SCRIPT_TEMPLATE_FOR_STYLESHEET_LINK_TAG =
            "const link = document.createElement('link');"
            + "link.rel = 'stylesheet';"
            + "link.type = 'text/css';"
            + "link.href = $0;"
            + "document.head.appendChild(link);";
    //@formatter:on

    static final String CONTENT_ATTRIBUTE = "content";
    private static final String DEFER_ATTRIBUTE = "defer";
    static final String VIEWPORT = "viewport";
    private static final String META_TAG = "meta";
    protected static final String SCRIPT_TAG = "script";

    /**
     * Location of client nocache file, relative to the context root.
     */
    private static final String CLIENT_ENGINE_NOCACHE_FILE = ApplicationConstants.CLIENT_ENGINE_PATH
            + "/client.nocache.js";
    private static final String BOOTSTRAP_JS = readResource(
            "BootstrapHandler.js");
    private static final String CSS_TYPE_ATTRIBUTE_VALUE = "text/css";

    private static final String CAPTION = "caption";
    private static final String MESSAGE = "message";
    private static final String URL = "url";

    private final PageBuilder pageBuilder;

    /**
     * Creates an instance of the handler with default {@link PageBuilder}.
     */
    public BootstrapHandler() {
        this(new BootstrapPageBuilder());
    }

    /**
     * Creates an instance of the handler using provided page builder.
     *
     * @param pageBuilder
     *            Page builder to use.
     */
    protected BootstrapHandler(PageBuilder pageBuilder) {
        this.pageBuilder = pageBuilder;
    }

    /**
     * Returns the current page builder object.
     *
     * @return Page builder in charge of constructing the resulting page.
     */
    protected PageBuilder getPageBuilder() {
        return pageBuilder;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BootstrapHandler.class.getName());
    }

    /**
     * Provides context information for the bootstrap process.
     */
    protected static class BootstrapContext {

        private final VaadinRequest request;
        private final VaadinResponse response;
        private final VaadinSession session;
        private final UI ui;
        private final Class<?> pageConfigurationHolder;
        private final ApplicationParameterBuilder parameterBuilder;
        private final Location route;

        private String appId;
        private PushMode pushMode;
        private ObjectNode applicationParameters;
        private BootstrapUriResolver uriResolver;

        private boolean initTheme = true;

        /**
         * Creates a new context instance using the given parameters.
         *
         * @param request
         *            the request object
         * @param response
         *            the response object
         * @param session
         *            the current session
         * @param ui
         *            the UI object
         * @param contextCallback
         *            a callback that is invoked to resolve the context root
         *            from the request
         */
        protected BootstrapContext(VaadinRequest request,
                VaadinResponse response, VaadinSession session, UI ui,
                Function<VaadinRequest, String> contextCallback) {
            this(request, response, session, ui, contextCallback,
                    req -> new Location(req.getPathInfo(),
                            QueryParameters.full(req.getParameterMap())));
        }

        /**
         * Creates a new context instance using the given parameters.
         *
         * @param request
         *            the request object
         * @param response
         *            the response object
         * @param session
         *            the current session
         * @param ui
         *            the UI object
         * @param contextCallback
         *            a callback that is invoked to resolve the context root
         *            from the request
         * @param routeCallback
         *            a callback that is invoked to resolve the route from the
         *            request
         */
        protected BootstrapContext(VaadinRequest request,
                VaadinResponse response, VaadinSession session, UI ui,
                Function<VaadinRequest, String> contextCallback,
                Function<VaadinRequest, Location> routeCallback) {
            this.request = request;
            this.response = response;
            this.session = session;
            this.ui = ui;
            this.route = routeCallback.apply(request);
            parameterBuilder = new ApplicationParameterBuilder(contextCallback);

            pageConfigurationHolder = BootstrapUtils
                    .resolvePageConfigurationHolder(ui, route).orElse(null);
        }

        /**
         * Gets the Vaadin/HTTP response.
         *
         * @return the Vaadin/HTTP response
         */
        public VaadinResponse getResponse() {
            return response;
        }

        /**
         * Gets the Vaadin/HTTP request.
         *
         * @return the Vaadin/HTTP request
         */
        public VaadinRequest getRequest() {
            return request;
        }

        /**
         * Gets the Vaadin service.
         *
         * @return the Vaadin/HTTP service
         */
        public VaadinService getService() {
            return request.getService();
        }

        /**
         * Gets the Vaadin session.
         *
         * @return the Vaadin session
         */
        public VaadinSession getSession() {
            return session;
        }

        /**
         * Should custom theme be initialized.
         *
         * @return true if theme should be initialized
         */
        public boolean isInitTheme() {
            return initTheme;
        }

        /**
         * Set if custom theme should be initialized.
         *
         * @param initTheme
         *            enable or disable theme initialisation
         */
        public void setInitTheme(boolean initTheme) {
            this.initTheme = initTheme;
        }

        /**
         * Gets the UI.
         *
         * @return the UI
         */
        public UI getUI() {
            return ui;
        }

        /**
         * Gets the push mode to use.
         *
         * @return the desired push mode
         */
        public PushMode getPushMode() {
            if (pushMode == null) {

                pushMode = getUI().getPushConfiguration().getPushMode();
                if (pushMode == null) {
                    pushMode = getService().getDeploymentConfiguration()
                            .getPushMode();
                }

                if (pushMode.isEnabled()
                        && !getService().ensurePushAvailable()) {
                    /*
                     * Fall back if not supported (ensurePushAvailable will log
                     * information to the developer the first time this happens)
                     */
                    pushMode = PushMode.DISABLED;
                }
            }
            return pushMode;
        }

        /**
         * Gets the application id.
         *
         * @return the application id
         */
        public String getAppId() {
            if (appId == null) {
                appId = getService().getMainDivId(getSession(), getRequest());
            }
            return appId;
        }

        /**
         * Gets the application parameters specified by the BootstrapHandler.
         *
         * @return the application parameters that will be written on the page
         */
        public ObjectNode getApplicationParameters() {
            if (applicationParameters == null) {
                applicationParameters = parameterBuilder
                        .getApplicationParameters(this);
            }

            return applicationParameters;
        }

        /**
         * Gets the URI resolver to use for bootstrap resources.
         *
         * @return the URI resolver
         */
        public BootstrapUriResolver getUriResolver() {
            if (uriResolver == null) {
                uriResolver = new BootstrapUriResolver(getUI());
            }

            return uriResolver;
        }

        /**
         * Checks if the application is running in production mode.
         *
         * @return <code>true</code> if in production mode, <code>false</code>
         *         otherwise.
         */
        public boolean isProductionMode() {
            return getService().getDeploymentConfiguration().isProductionMode();
        }

        /**
         * Gets an annotation from the topmost class in the current navigation
         * target hierarchy.
         *
         * @param <T>
         *            the type of the annotation
         * @param annotationType
         *            the type of the annotation to get
         * @return an annotation, or an empty optional if there is no current
         *         navigation target or if it doesn't have the annotation
         */
        public <T extends Annotation> Optional<T> getPageConfigurationAnnotation(
                Class<T> annotationType) {
            if (pageConfigurationHolder == null) {
                return Optional.empty();
            } else {
                return AnnotationReader.getAnnotationFor(
                        pageConfigurationHolder, annotationType);
            }
        }

        /**
         * Gets a a list of annotations from the topmost class in the current
         * navigation target hierarchy.
         *
         * @param <T>
         *            the type of the annotations
         * @param annotationType
         *            the type of the annotation to get
         * @return a list of annotation, or an empty list if there is no current
         *         navigation target or if it doesn't have the annotation
         */
        public <T extends Annotation> List<T> getPageConfigurationAnnotations(
                Class<T> annotationType) {
            if (pageConfigurationHolder == null) {
                return Collections.emptyList();
            } else {
                return AnnotationReader.getAnnotationsFor(
                        pageConfigurationHolder, annotationType);
            }
        }

        /**
         * Gets a pwa registry instance.
         *
         * @return an optional pwa registry instance, or an empty optional if no
         *         pwa registry available for the context
         */
        protected Optional<PwaRegistry> getPwaRegistry() {
            VaadinService vaadinService = getSession().getService();
            if (vaadinService == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(vaadinService.getPwaRegistry());
        }

        /**
         * Gets the location of the route that should be activated for this
         * bootstrap request.
         *
         * @return the route to activate
         */
        public Location getRoute() {
            return route;
        }

    }

    /**
     * The URI resolver used in the bootstrap process.
     */
    public static class BootstrapUriResolver extends VaadinUriResolver {
        private String servletPathToContextRoot;

        /**
         * Creates a new bootstrap resolver based on the given ui.
         *
         * @param ui
         *            the ui to resolve for
         */
        protected BootstrapUriResolver(UI ui) {
            this(ui.getInternals().getContextRootRelativePath(),
                    ui.getSession());
        }

        /**
         * Creates a new bootstrap resolver based on the given session.
         *
         * @param contextRootRelatiePath
         *            the relative path from the UI (servlet) path to the
         *            context root
         * @param session
         *            the vaadin session
         */
        public BootstrapUriResolver(String contextRootRelatiePath,
                VaadinSession session) {
            servletPathToContextRoot = contextRootRelatiePath;
            assert servletPathToContextRoot.endsWith("/");
        }

        /**
         * Translates a Vaadin URI to a URL that can be loaded by the browser.
         * The following URI schemes are supported:
         * <ul>
         * <li><code>{@value ApplicationConstants#CONTEXT_PROTOCOL_PREFIX}</code>
         * - resolves to the application context root</li>
         * <li><code>{@value ApplicationConstants#BASE_PROTOCOL_PREFIX}</code> -
         * resolves to the base URI of the page</li>
         * </ul>
         * Any other URI protocols, such as <code>http://</code> or
         * <code>https://</code> are passed through this method unmodified.
         *
         * @param uri
         *            the URI to resolve
         * @return the resolved URI
         */
        public String resolveVaadinUri(String uri) {
            return super.resolveVaadinUri(uri, servletPathToContextRoot);
        }

    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        if (isFrameworkInternalRequest(request)) {
            // Never accidentally send a bootstrap page for what is considered
            // an internal request
            return false;
        }

        if (isVaadinStaticFileRequest(request)) {
            // Do not allow routes inside /VAADIN/
            return false;
        }

        if (!isRequestForHtml(request)) {
            return false;
        }

        return super.canHandleRequest(request);
    }

    /**
     * Checks whether the request is an internal request.
     * <p>
     * Warning: This assumes that the VaadinRequest is targeted for a
     * VaadinServlet and does no further checks to validate this. You want to
     * use
     * {@link HandlerHelper#isFrameworkInternalRequest(String, jakarta.servlet.http.HttpServletRequest)}
     * instead.
     * <p>
     * This is public only so that
     * {@link com.vaadin.flow.server.communication.IndexHtmlRequestHandler} can
     * access it. If you are not IndexHtmlRequestHandler, go away.
     *
     * @param request
     *            the request
     * @return {@code true} if the request is Vaadin internal, {@code false}
     *         otherwise
     */
    public static boolean isFrameworkInternalRequest(VaadinRequest request) {
        if (request instanceof VaadinServletRequest) {
            // We can ignore the servlet path in this case as we know that
            // this is targeting a Vaadin servlet and not some other servlet
            return HandlerHelper.isInternalRequestInsideServlet(
                    request.getPathInfo(), request.getParameter(
                            ApplicationConstants.REQUEST_TYPE_PARAMETER));
        }
        return false;
    }

    /**
     * Checks whether the request is a request for /VAADIN/*.
     * <p>
     * Warning: This assumes that the VaadinRequest is targeted for a
     * VaadinServlet and does no further checks to validate this.
     * <p>
     * This is public only so that
     * {@link com.vaadin.flow.server.communication.IndexHtmlRequestHandler} can
     * access it. If you are not IndexHtmlRequestHandler, go away.
     *
     * @param request
     *            the request
     * @return {@code true} if the request is for /VAADIN/*, {@code false}
     *         otherwise
     */
    public static boolean isVaadinStaticFileRequest(VaadinRequest request) {
        return request.getPathInfo() != null
                && request.getPathInfo().startsWith("/" + VAADIN_MAPPING);
    }

    /**
     * Checks if the request is potentially a request for an HTML page.
     *
     * @param request
     *            the request to check
     * @return {@code true} if the request is potentially for HTML,
     *         {@code false} if it is certain that it is a request for a script,
     *         image or something else
     */
    protected boolean isRequestForHtml(VaadinRequest request) {
        if (request.getHeader(BootstrapHandler.SERVICE_WORKER_HEADER) != null) {
            return false;
        }
        return !HandlerHelper.isNonHtmlInitiatedRequest(request);
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        if (writeErrorCodeIfRequestLocationIsInvalid(request, response)) {
            return true;
        }

        // Find UI class
        Class<? extends UI> uiClass = getUIClass(request);

        BootstrapContext context = createAndInitUI(uiClass, request, response,
                session);

        HandlerHelper.setResponseNoCacheHeaders(response::setHeader,
                response::setDateHeader);

        Document document = pageBuilder.getBootstrapPage(context);

        writeBootstrapPage(response, document.outerHtml());

        return true;
    }

    /**
     * Checks whether the request is for a valid location, and if not, writes
     * the error code for the response.
     *
     * @param request
     *            the request to check
     * @param response
     *            the response to write
     * @return {@code true} if location was invalid and error code was written,
     *         {@code false} if not (location was valid)
     * @throws IOException
     *             in case writing to response fails
     */
    protected boolean writeErrorCodeIfRequestLocationIsInvalid(
            VaadinRequest request, VaadinResponse response) throws IOException {
        try {
            // #9443 Use error code 400 for bad location and don't create UI
            LocationUtil.verifyRelativePath(
                    LocationUtil.ensureRelativeNonNull(request.getPathInfo()));
        } catch (InvalidLocationException invalidLocationException) { // NOSONAR
            response.sendError(400, "Invalid location: "
                    + invalidLocationException.getMessage());
            return true;
        }
        return false;
    }

    private void writeBootstrapPage(VaadinResponse response, String html)
            throws IOException {
        response.setContentType(
                ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), UTF_8))) {
            writer.append(html);
        }
    }

    /**
     * Interface for objects capable of building the bootstrap page.
     */
    public interface PageBuilder extends Serializable {
        /**
         * Creates the bootstrap page.
         *
         * @param context
         *            Context to build page for.
         * @return A non-null {@link Document} with bootstrap page.
         */
        Document getBootstrapPage(BootstrapContext context);
    }

    /**
     * Builds bootstrap pages.
     *
     * Do not subclass this, unless you really know why you are doing it.
     */
    protected static class BootstrapPageBuilder implements PageBuilder {

        /**
         * Returns the bootstrap page for the given context.
         *
         * @param context
         *            Context to generate bootstrap page for.
         * @return A document with the corresponding HTML page.
         */
        @Override
        public Document getBootstrapPage(BootstrapContext context) {
            DeploymentConfiguration config = context.getSession()
                    .getConfiguration();

            Document document = new Document("");
            DocumentType doctype = new DocumentType("html", "", "");
            document.appendChild(doctype);

            Element html = document.appendElement("html");
            html.attr("lang", context.getUI().getLocale().getLanguage());
            Element head = html.appendElement("head");
            html.appendElement("body");

            List<Element> dependenciesToInlineInBody = setupDocumentHead(head,
                    context);
            dependenciesToInlineInBody.forEach(
                    dependency -> document.body().appendChild(dependency));
            setupDocumentBody(document);

            document.outputSettings().prettyPrint(false);

            BootstrapUtils.getInlineTargets(context)
                    .ifPresent(targets -> handleInlineTargets(context, head,
                            document.body(), targets));

            if (!config.isProductionMode()) {
                UsageStatisticsExporter
                        .exportUsageStatisticsToDocument(document);
                IndexHtmlRequestHandler.addLicenseChecker(document);
            }

            setupPwa(document, context);

            return document;
        }

        private Element createDependencyElement(BootstrapContext context,
                ObjectNode dependencyJson) {
            String type = dependencyJson.get(Dependency.KEY_TYPE).asString();
            if (Dependency.Type.contains(type)) {
                Dependency.Type dependencyType = Dependency.Type.valueOf(type);
                return createDependencyElement(context.getUriResolver(),
                        LoadMode.INLINE, dependencyJson, dependencyType);
            }
            return Jsoup.parse(
                    dependencyJson.get(Dependency.KEY_CONTENTS).asString(), "",
                    Parser.xmlParser());
        }

        private void handleInlineTargets(BootstrapContext context, Element head,
                Element body, InlineTargets targets) {
            targets.getInlineHead(Inline.Position.PREPEND).stream().map(
                    dependency -> createDependencyElement(context, dependency))
                    .forEach(element -> insertElements(element,
                            head::prependChild));
            targets.getInlineHead(Inline.Position.APPEND).stream().map(
                    dependency -> createDependencyElement(context, dependency))
                    .forEach(element -> insertElements(element,
                            head::appendChild));

            targets.getInlineBody(Inline.Position.PREPEND).stream().map(
                    dependency -> createDependencyElement(context, dependency))
                    .forEach(element -> insertElements(element,
                            body::prependChild));
            targets.getInlineBody(Inline.Position.APPEND).stream().map(
                    dependency -> createDependencyElement(context, dependency))
                    .forEach(element -> insertElements(element,
                            body::appendChild));
        }

        private void insertElements(Element element, Consumer<Element> action) {
            if (element instanceof Document) {
                element.getAllElements().stream()
                        .filter(item -> !(item instanceof Document)
                                && element.equals(item.parent()))
                        .forEach(action::accept);
            } else if (element != null) {
                action.accept(element);
            }
        }

        private List<Element> setupDocumentHead(Element head,
                BootstrapContext context) {
            setupMetaAndTitle(head, context);
            setupCss(head, context);

            ObjectNode initialUIDL = getInitialUidl(context.getUI());
            Map<LoadMode, ArrayNode> dependenciesToProcessOnServer = popDependenciesToProcessOnServer(
                    initialUIDL);
            setupFrameworkLibraries(head, initialUIDL, context);
            return applyUserDependencies(head, context,
                    dependenciesToProcessOnServer);
        }

        /**
         * Generates the initial UIDL message which is included in the initial
         * bootstrap page.
         *
         * @param ui
         *            the UI for which the UIDL should be generated
         * @return a JSON object with the initial UIDL message
         */
        private ObjectNode getInitialUidl(UI ui) {
            ObjectNode json = new UidlWriter().createUidl(ui, false);

            VaadinSession session = ui.getSession();
            if (session.getConfiguration().isXsrfProtectionEnabled()) {
                writeSecurityKeyUIDL(json, ui);
            }
            writePushIdUIDL(json, session);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Initial UIDL: {}", json.toPrettyString());
            }
            return json;
        }

        /**
         * Writes the push id (and generates one if needed) to the given JSON
         * object.
         *
         * @param response
         *            the response JSON object to write security key into
         * @param session
         *            the vaadin session to which the security key belongs
         */
        private void writePushIdUIDL(ObjectNode response,
                VaadinSession session) {
            String pushId = session.getPushId();
            response.put(ApplicationConstants.UIDL_PUSH_ID, pushId);
        }

        /**
         * Writes the security key (and generates one if needed) to the given
         * JSON object.
         *
         * @param response
         *            the response JSON object to write security key into
         * @param ui
         *            the UI to which the security key belongs
         */
        private void writeSecurityKeyUIDL(ObjectNode response, UI ui) {
            String seckey = ui.getCsrfToken();
            response.put(ApplicationConstants.UIDL_SECURITY_TOKEN_ID, seckey);
        }

        private List<Element> applyUserDependencies(Element head,
                BootstrapContext context,
                Map<LoadMode, ArrayNode> dependenciesToProcessOnServer) {
            List<Element> dependenciesToInlineInBody = new ArrayList<>();
            for (Map.Entry<LoadMode, ArrayNode> entry : dependenciesToProcessOnServer
                    .entrySet()) {
                dependenciesToInlineInBody.addAll(
                        inlineDependenciesInHead(head, context.getUriResolver(),
                                entry.getKey(), entry.getValue()));
            }
            return dependenciesToInlineInBody;
        }

        private List<Element> inlineDependenciesInHead(Element head,
                BootstrapUriResolver uriResolver, LoadMode loadMode,
                ArrayNode dependencies) {
            List<Element> dependenciesToInlineInBody = new ArrayList<>();

            for (int i = 0; i < dependencies.size(); i++) {
                ObjectNode dependencyJson = (ObjectNode) dependencies.get(i);
                Dependency.Type dependencyType = Dependency.Type.valueOf(
                        dependencyJson.get(Dependency.KEY_TYPE).asString());
                Element dependencyElement = createDependencyElement(uriResolver,
                        loadMode, dependencyJson, dependencyType);

                head.appendChild(dependencyElement);
            }
            return dependenciesToInlineInBody;
        }

        private Map<LoadMode, ArrayNode> popDependenciesToProcessOnServer(
                ObjectNode initialUIDL) {
            Map<LoadMode, ArrayNode> result = new EnumMap<>(LoadMode.class);
            Stream.of(LoadMode.EAGER, LoadMode.INLINE).forEach(mode -> {
                if (initialUIDL.has(mode.name())) {
                    result.put(mode, (ArrayNode) initialUIDL.get(mode.name()));
                    initialUIDL.remove(mode.name());
                }
            });
            return result;
        }

        private void setupFrameworkLibraries(Element head,
                ObjectNode initialUIDL, BootstrapContext context) {

            VaadinService service = context.getSession().getService();
            DeploymentConfiguration conf = service.getDeploymentConfiguration();

            conf.getPolyfills().forEach(
                    polyfill -> head.appendChild(createJavaScriptElement(
                            "./" + VAADIN_MAPPING + polyfill, false)));
            try {
                appendNpmBundle(head, service, context);
            } catch (IOException e) {
                throw new BootstrapException("Unable to append bundle", e);
            }

            if (context.getPushMode().isEnabled()) {
                head.appendChild(
                        createJavaScriptElement(getPushScript(context)));
            }

            head.appendChild(getBootstrapScript(initialUIDL, context));
            head.appendChild(
                    createJavaScriptElement(getClientEngineUrl(context)));
        }

        private void appendNpmBundle(Element head, VaadinService service,
                BootstrapContext context) throws IOException {
            appendViteNpmBundle(head, service, context);
        }

        private void appendViteNpmBundle(Element head, VaadinService service,
                BootstrapContext context) throws IOException {
            if (!service.getDeploymentConfiguration().isProductionMode()) {
                Element script = createJavaScriptModuleElement(
                        "VAADIN/@vite/client", false);
                head.appendChild(script);
                return;
            }

            // Get the index.html to get vite generated bundles
            String index = FrontendUtils.getIndexHtmlContent(service);

            // Get and add all javascriptbundles
            Matcher scriptMatcher = Pattern
                    .compile("src=\\\"VAADIN\\/build\\/(.*\\.js)\\\"")
                    .matcher(index);
            while (scriptMatcher.find()) {
                Element script = createJavaScriptModuleElement(
                        "VAADIN/build/" + scriptMatcher.group(1), false);
                head.appendChild(script.attr("async", true)
                        // Fixes basic auth in Safari #6560
                        .attr("crossorigin", true));
            }

            // Get and add all css bundle links
            Matcher cssMatcher = Pattern
                    .compile("href=\\\"VAADIN\\/build\\/(.*\\.css)\\\"")
                    .matcher(index);
            while (cssMatcher.find()) {
                Element link = createStylesheetElement(
                        "VAADIN/build/" + cssMatcher.group(1));
                head.appendChild(link);
            }
        }

        /**
         * Return the list of chunk keys that should be considered by the
         * bootstrap handler.
         *
         * @param chunks
         *            in the stat file
         * @return the list of chunk keys to process
         */
        protected List<String> getChunkKeys(ObjectNode chunks) {
            // include all chunks but the one used for exported
            // components.
            return JacksonUtils.getKeys(chunks).stream()
                    .filter(s -> !EXPORT_CHUNK.equals(s))
                    .collect(Collectors.toList());
        }

        private String getClientEngineUrl(BootstrapContext context) {
            // use nocache version of client engine if it
            // has been compiled by SDM or eclipse
            // In production mode, this should really be loaded by the static
            // block
            // so emit a warning if we get here (tests will always get here)
            final boolean productionMode = context.getSession()
                    .getConfiguration().isProductionMode();

            ResourceProvider resourceProvider = getResourceProvider(context);
            String clientEngine = getClientEngine(resourceProvider);
            boolean resolveNow = !productionMode || clientEngine == null;
            if (resolveNow
                    && resourceProvider.getClientResource("META-INF/resources/"
                            + CLIENT_ENGINE_NOCACHE_FILE) != null) {
                return context.getUriResolver().resolveVaadinUri(
                        "context://" + CLIENT_ENGINE_NOCACHE_FILE);
            }

            if (clientEngine == null) {
                throw new BootstrapException(
                        "Client engine file name has not been resolved during initialization");
            }
            return context.getUriResolver()
                    .resolveVaadinUri("context://" + clientEngine);
        }

        private ResourceProvider getResourceProvider(BootstrapContext context) {
            ResourceProvider resourceProvider = context.getSession()
                    .getService().getContext().getAttribute(Lookup.class)
                    .lookup(ResourceProvider.class);
            return resourceProvider;
        }

        private String getClientEngine(ResourceProvider resourceProvider) {
            // read client engine file name
            try (InputStream prop = resourceProvider
                    .getClientResourceAsStream("META-INF/resources/"
                            + ApplicationConstants.CLIENT_ENGINE_PATH
                            + "/compile.properties")) {
                // null when running SDM or tests
                if (prop != null) {
                    Properties properties = new Properties();
                    properties.load(prop);
                    return ApplicationConstants.CLIENT_ENGINE_PATH + "/"
                            + properties.getProperty("jsFile");
                } else {
                    getLogger().warn(
                            "No compile.properties available on initialization, "
                                    + "could not read client engine file name.");
                }
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
            return null;
        }

        protected void setupCss(Element head, BootstrapContext context) {
            Element styles = head.appendElement("style").attr("type",
                    CSS_TYPE_ATTRIBUTE_VALUE);
            // Add any body style that is defined for the application using
            // @BodySize
            String bodySizeContent = BootstrapUtils.getBodySizeContent(context);
            styles.appendText(bodySizeContent);

            // Basic reconnect and system error dialog styles just to make them
            // visible and outside of normal flow
            setupErrorDialogs(styles);

            setupHiddenElement(styles);
        }

        private void setupMetaAndTitle(Element head, BootstrapContext context) {
            head.appendElement(META_TAG).attr("http-equiv", "Content-Type")
                    .attr(CONTENT_ATTRIBUTE,
                            ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);

            head.appendElement(META_TAG).attr("http-equiv", "X-UA-Compatible")
                    .attr(CONTENT_ATTRIBUTE, "IE=edge");

            head.appendElement("base").attr("href",
                    BootstrapHandlerHelper.getServiceUrl(context.getRequest()));

            head.appendElement(META_TAG).attr("name", VIEWPORT).attr(
                    CONTENT_ATTRIBUTE,
                    BootstrapUtils.getViewportContent(context)
                            .orElse(Viewport.DEFAULT));

            BootstrapUtils.getMetaTargets(context)
                    .forEach((name, content) -> head.appendElement(META_TAG)
                            .attr("name", name)
                            .attr(CONTENT_ATTRIBUTE, content));

            resolvePageTitle(context).ifPresent(title -> {
                if (!title.isEmpty()) {
                    head.appendElement("title").appendText(title);
                }
            });
        }

        private void setupPwa(Document document, BootstrapContext context) {
            BootstrapHandler.setupPwa(document,
                    context.getPwaRegistry().orElse(null));
        }

        protected Element createInlineJavaScriptElement(
                String javaScriptContents) {
            // defer makes no sense without src:
            // https://developer.mozilla.org/en/docs/Web/HTML/Element/script
            Element wrapper = createJavaScriptElement(null, false);
            wrapper.appendChild(new DataNode(javaScriptContents));
            return wrapper;
        }

        protected static Element createJavaScriptElement(String sourceUrl,
                boolean defer) {
            return createJavaScriptElement(sourceUrl, defer, "text/javascript");
        }

        protected static Element createJavaScriptModuleElement(String sourceUrl,
                boolean defer) {
            return createJavaScriptElement(sourceUrl, defer, "module");
        }

        protected static Element createJavaScriptElement(String sourceUrl,
                boolean defer, String type) {
            Element jsElement = new Element(Tag.valueOf(SCRIPT_TAG), "")
                    .attr("type", type).attr(DEFER_ATTRIBUTE, defer);
            if (sourceUrl != null) {
                jsElement = jsElement.attr("src", sourceUrl);
            }
            return jsElement;
        }

        protected static Element createJavaScriptElement(String sourceUrl) {
            return createJavaScriptElement(sourceUrl, true);
        }

        private Element createDependencyElement(BootstrapUriResolver resolver,
                LoadMode loadMode, ObjectNode dependency,
                Dependency.Type type) {
            boolean inlineElement = loadMode == LoadMode.INLINE;
            String url = dependency.has(Dependency.KEY_URL)
                    ? resolver.resolveVaadinUri(
                            dependency.get(Dependency.KEY_URL).asString())
                    : null;

            final Element dependencyElement;
            switch (type) {
            case STYLESHEET:
                dependencyElement = createStylesheetElement(url);
                break;
            case JAVASCRIPT:
                dependencyElement = createJavaScriptElement(url,
                        !inlineElement);
                break;
            case JS_MODULE:
                dependencyElement = createJavaScriptModuleElement(url, false);
                break;
            default:
                throw new IllegalStateException(
                        "Unsupported dependency type: " + type);
            }

            if (inlineElement) {
                dependencyElement.appendChild(new DataNode(
                        dependency.get(Dependency.KEY_CONTENTS).asString()));
            }

            return dependencyElement;
        }

        private Element createStylesheetElement(String url) {
            final Element cssElement;
            if (url != null) {
                cssElement = new Element(Tag.valueOf("link"), "")
                        .attr("rel", "stylesheet")
                        .attr("type", CSS_TYPE_ATTRIBUTE_VALUE)
                        .attr("href", url);
            } else {
                cssElement = new Element(Tag.valueOf("style"), "").attr("type",
                        CSS_TYPE_ATTRIBUTE_VALUE);
            }
            return cssElement;
        }

        private void setupDocumentBody(Document document) {
            document.body().appendElement("noscript").append(
                    "You have to enable javascript in your browser to use this web site.");
        }

        protected Element getBootstrapScript(ObjectNode initialUIDL,
                BootstrapContext context) {
            return createInlineJavaScriptElement("//<![CDATA[\n"
                    + getBootstrapJS(initialUIDL, context) + "//]]>");
        }

        private String getBootstrapJS() {
            if (BOOTSTRAP_JS.isEmpty()) {
                throw new BootstrapException(
                        "BootstrapHandler.js has not been loaded during initialization");
            }
            return BOOTSTRAP_JS;
        }

        private String getBootstrapJS(ObjectNode initialUIDL,
                BootstrapContext context) {
            boolean productionMode = context.getSession().getConfiguration()
                    .isProductionMode();
            String result = getBootstrapJS();
            ObjectNode appConfig = context.getApplicationParameters();

            String appConfigString;

            String initialUIDLString;
            if (!productionMode) {
                appConfigString = appConfig.toPrettyString();
                initialUIDLString = initialUIDL.toPrettyString();
            } else {
                appConfigString = appConfig.toString();
                initialUIDLString = initialUIDL.toString();
            }

            /*
             * The < symbol is escaped to prevent two problems:
             *
             * 1 - The browser interprets </script> as end of script no matter
             * if it is inside a string
             *
             * 2 - Scripts can be injected with <!-- <script>, that can cause
             * unexpected behavior or complete crash of the app
             */
            initialUIDLString = initialUIDLString.replace("<", "\\x3C");

            if (!productionMode) {
                // only used in debug mode by profiler
                result = result.replace("{{GWT_STAT_EVENTS}}",
                        GWT_STAT_EVENTS_JS);
            } else {
                result = result.replace("{{GWT_STAT_EVENTS}}", "");
            }

            result = result.replace("{{APP_ID}}", context.getAppId());
            result = result.replace("{{CONFIG_JSON}}", appConfigString);
            // {{INITIAL_UIDL}} should be the last replaced so that it may have
            // other patterns inside it (like {{CONFIG_JSON}})
            result = result.replace("{{INITIAL_UIDL}}", initialUIDLString);

            // set productionMode early because WC detector might be run before
            // client initialization finishes.
            result = result.replace("{{PRODUCTION_MODE}}",
                    String.valueOf(productionMode));
            return result;
        }
    }

    private static final class ApplicationParameterBuilder {
        private final Function<VaadinRequest, String> contextCallback;

        private ApplicationParameterBuilder(
                Function<VaadinRequest, String> contextCallback) {
            this.contextCallback = contextCallback;
        }

        /**
         * Creates application parameters for the provided
         * {@link BootstrapContext}.
         *
         * @param context
         *            Non-null context to provide application parameters for.
         * @return A non-null {@link ObjectNode} with application parameters.
         */
        public ObjectNode getApplicationParameters(BootstrapContext context) {
            VaadinRequest request = context.getRequest();
            VaadinSession session = context.getSession();
            DeploymentConfiguration deploymentConfiguration = session
                    .getConfiguration();
            final boolean productionMode = deploymentConfiguration
                    .isProductionMode();

            ObjectNode appConfig = JacksonUtils.createObjectNode();

            if (!productionMode) {
                ObjectNode versionInfo = JacksonUtils.createObjectNode();
                versionInfo.put("vaadinVersion", Version.getFullVersion());
                String atmosphereVersion = AtmospherePushConnection
                        .getAtmosphereVersion();
                if (atmosphereVersion != null) {
                    versionInfo.put("atmosphereVersion", atmosphereVersion);
                }
                appConfig.set("versionInfo", versionInfo);
                appConfig.put(ApplicationConstants.DEV_TOOLS_ENABLED,
                        deploymentConfiguration.isDevToolsEnabled());
            }

            // Use locale from session if set, else from the request
            Locale locale = HandlerHelper.findLocale(session, request);
            // Get system messages
            SystemMessages systemMessages = session.getService()
                    .getSystemMessages(locale, request);
            if (systemMessages != null) {
                ObjectNode sessExpMsg = JacksonUtils.createObjectNode();
                putValueOrNull(sessExpMsg, CAPTION,
                        systemMessages.getSessionExpiredCaption());
                putValueOrNull(sessExpMsg, MESSAGE,
                        systemMessages.getSessionExpiredMessage());
                putValueOrNull(sessExpMsg, URL,
                        systemMessages.getSessionExpiredURL());

                appConfig.set("sessExpMsg", sessExpMsg);
            }

            String contextRoot = contextCallback.apply(request);
            appConfig.put(ApplicationConstants.CONTEXT_ROOT_URL, contextRoot);

            if (!productionMode) {
                appConfig.put("debug", true);
            }

            if (deploymentConfiguration.isRequestTiming()) {
                appConfig.put("requestTiming", true);
            }

            appConfig.put("heartbeatInterval",
                    deploymentConfiguration.getHeartbeatInterval());

            appConfig.put("maxMessageSuspendTimeout",
                    deploymentConfiguration.getMaxMessageSuspendTimeout());

            boolean sendUrlsAsParameters = deploymentConfiguration
                    .isSendUrlsAsParameters();
            if (!sendUrlsAsParameters) {
                appConfig.put("sendUrlsAsParameters", false);
            }

            appConfig.put(ApplicationConstants.UI_ID_PARAMETER,
                    context.getUI().getUIId());
            return appConfig;
        }

        private void putValueOrNull(ObjectNode object, String key,
                String value) {
            assert object != null;
            assert key != null;
            if (value == null) {
                object.set(key, JacksonUtils.nullNode());
            } else {
                object.put(key, value);
            }
        }

    }

    /**
     * Resolves the initial page title for the given bootstrap context and
     * cancels any pending JS execution for it.
     *
     * @param context
     *            the bootstrap context
     * @return the optional initial page title
     */
    protected static Optional<String> resolvePageTitle(
            BootstrapContext context) {
        // check for explicitly set page title, e.g. by PageTitleGenerator or
        // View level title or page.setTitle
        String title = context.getUI().getInternals().getTitle();
        if (title != null) {
            // cancel the unnecessary execute javascript
            context.getUI().getInternals().cancelPendingTitleUpdate();
        }
        return Optional.ofNullable(title);
    }

    protected BootstrapContext createAndInitUI(Class<? extends UI> uiClass,
            VaadinRequest request, VaadinResponse response,
            VaadinSession session) {

        UI ui = ReflectTools.createInstance(uiClass);
        ui.getInternals().setContextRoot(
                request.getService().getContextRootRelativePath(request));

        PushConfiguration pushConfiguration = ui.getPushConfiguration();

        ui.getInternals().setSession(session);
        ui.setLocale(session.getLocale());

        BootstrapContext context = createBootstrapContext(request, response, ui,
                request.getService()::getContextRootRelativePath);

        Optional<Push> push = context
                .getPageConfigurationAnnotation(Push.class);

        DeploymentConfiguration deploymentConfiguration = context.getSession()
                .getService().getDeploymentConfiguration();
        PushMode pushMode = push.map(Push::value)
                .orElseGet(deploymentConfiguration::getPushMode);
        setupPushConnectionFactory(pushConfiguration, context);
        pushConfiguration.setPushMode(pushMode);
        pushConfiguration.setPushServletMapping(
                BootstrapHandlerHelper.determinePushServletMapping(session));

        push.map(Push::transport).ifPresent(pushConfiguration::setTransport);

        // Set thread local here so it is available in init
        UI.setCurrent(ui);
        ui.doInit(request, session.getNextUIid(), context.getAppId());
        session.addUI(ui);

        // After init and adding UI to session fire init listeners.
        session.getService().fireUIInitListeners(ui);

        initializeUIWithRouter(context, ui);

        return context;
    }

    protected void initializeUIWithRouter(BootstrapContext context, UI ui) {
        if (ui.getInternals().getRouter() != null) {
            ui.getInternals().getRouter().initializeUI(ui, context.getRoute());
        }
    }

    /**
     * Creates a new instance of {@link BootstrapContext} for given
     * {@code request}, {@code response} and {@code ui}.
     *
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @param ui
     *            the UI object
     * @param contextPathCallback
     *            a callback that is invoked to resolve the context root from
     *            the request
     * @return a new bootstrap context instance
     */
    protected BootstrapContext createBootstrapContext(VaadinRequest request,
            VaadinResponse response, UI ui,
            Function<VaadinRequest, String> contextPathCallback) {
        return new BootstrapContext(request, response,
                ui.getInternals().getSession(), ui, contextPathCallback);
    }

    protected void setupPushConnectionFactory(
            PushConfiguration pushConfiguration, BootstrapContext context) {
        VaadinService service = context.getSession().getService();
        Iterator<PushConnectionFactory> iter = ServiceLoader
                .load(PushConnectionFactory.class, service.getClassLoader())
                .iterator();
        if (iter.hasNext()) {
            pushConfiguration.setPushConnectionFactory(iter.next());
            if (iter.hasNext()) {
                throw new BootstrapException(
                        "Multiple " + PushConnectionFactory.class.getName()
                                + " implementations found");
            }
        }
    }

    /**
     * Returns the UI class mapped for servlet that handles the given request.
     * <p>
     * This method is protected for testing purposes.
     *
     * @param request
     *            the request for the UI
     * @return the UI class for the request
     */
    protected static Class<? extends UI> getUIClass(VaadinRequest request) {
        String uiClassName = request.getService().getDeploymentConfiguration()
                .getUIClassName();
        if (uiClassName == null) {
            throw new BootstrapException(
                    "Could not determine the uiClassName for the request path "
                            + request.getPathInfo());
        }

        ClassLoader classLoader = request.getService().getClassLoader();
        try {
            return Class.forName(uiClassName, true, classLoader)
                    .asSubclass(UI.class);
        } catch (ClassNotFoundException e) {
            throw new BootstrapException(
                    "Vaadin Servlet mapped to the request path "
                            + request.getPathInfo()
                            + " cannot find the mapped UI class with name "
                            + uiClassName,
                    e);
        }
    }

    protected static String readResource(String fileName) {
        try (InputStream stream = BootstrapHandler.class
                .getResourceAsStream(fileName);
                BufferedReader bf = new BufferedReader(new InputStreamReader(
                        stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            bf.lines().forEach(builder::append);
            return builder.toString();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Generates the initial UIDL message which is included in the initial
     * bootstrap page.
     *
     * @param ui
     *            the UI for which the UIDL should be generated
     * @return a JSON object with the initial UIDL message
     */
    protected static ObjectNode getInitialUidl(UI ui) {
        ObjectNode json = new UidlWriter().createUidl(ui, false);

        VaadinSession session = ui.getSession();
        if (session.getConfiguration().isXsrfProtectionEnabled()) {
            writeSecurityKeyUIDL(json, ui);
        }
        writePushIdUIDL(json, session);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Initial UIDL: {}", json);
        }
        return json;
    }

    /**
     * Writes the push id (and generates one if needed) to the given JSON
     * object.
     *
     * @param response
     *            the response JSON object to write security key into
     * @param session
     *            the vaadin session to which the security key belongs
     */
    private static void writePushIdUIDL(ObjectNode response,
            VaadinSession session) {
        String pushId = session.getPushId();
        response.put(ApplicationConstants.UIDL_PUSH_ID, pushId);
    }

    /**
     * Writes the security key (and generates one if needed) to the given JSON
     * object.
     *
     * @param response
     *            the response JSON object to write security key into
     * @param ui
     *            the UI to which the security key belongs
     */
    private static void writeSecurityKeyUIDL(ObjectNode response, UI ui) {
        String seckey = ui.getCsrfToken();
        response.put(ApplicationConstants.UIDL_SECURITY_TOKEN_ID, seckey);
    }

    protected static String getPushScript(BootstrapContext context) {
        VaadinRequest request = context.getRequest();
        // Parameter appended to JS to bypass caches after version upgrade.
        String versionQueryParam = "?v=" + Version.getBuildHash();

        String pushJs;
        if (request.getService().getDeploymentConfiguration()
                .isProductionMode()) {
            pushJs = ApplicationConstants.VAADIN_PUSH_JS;
        } else {
            pushJs = ApplicationConstants.VAADIN_PUSH_DEBUG_JS;
        }

        // Use direct path - the <base href> already points to the servlet root,
        // so VAADIN/... resolves correctly to {context}/{servlet}/VAADIN/...
        return pushJs + versionQueryParam;
    }

    protected static void setupErrorDialogs(Element style) {
        // @formatter:off
        style.appendText(
                ".v-reconnect-dialog," +
                ".v-system-error {" +
                "position: absolute;" +
                "color: black;" +
                "background: white;" +
                "top: 1em;" +
                "right: 1em;" +
                "border: 1px solid black;" +
                "padding: 1em;" +
                "z-index: 10000;" +
                "max-width: calc(100vw - 4em);" +
                "max-height: calc(100vh - 4em);" +
                "overflow: auto;" +
                "} .v-system-error {" +
                "color: indianred;" +
                "pointer-events: auto;" +
                "} .v-system-error h3, .v-system-error b {" +
                "color: red;" +
                "}");
     // @formatter:on
    }

    protected static void setupHiddenElement(Element styles) {
        // Component::setVisible relies on hidden attribute.
        // Adds a global display:none style to elements with hidden attribute
        styles.appendText("[hidden] { display: none !important; }");
    }

    protected static void setupPwa(Document document, VaadinService service) {
        setupPwa(document, service.getPwaRegistry());
    }

    protected static ObjectNode getStatsJson(DeploymentConfiguration config)
            throws IOException {
        String statsJson = DevBundleUtils.findBundleStatsJson(
                config.getProjectFolder(), config.getBuildFolder());
        Objects.requireNonNull(statsJson,
                """
                        Frontend development bundle is expected to be in the project or on the classpath, but not found.
                        Add 'com.vaadin.vaadin-dev' dependency to let Vaadin build the development bundle automatically
                        or 'com.vaadin.vaadin-dev-server' for minimal working configuration.
                        
                        Maven:
                            <dependency>
                                <groupId>com.vaadin</groupId>
                                <artifactId>vaadin-dev</artifactId>
                                <optional>true</optional>
                            </dependency>
                        
                        Gradle:
                            dependencies {
                                implementation('com.vaadin:vaadin-dev')
                            }
                        
                        """);
        return JacksonUtils.readTree(statsJson);
    }

    /**
     * Gives link tags for referencing the custom theme stylesheet files
     * (typically styles.css or document.css), which are served in express build
     * mode by static file server directly from frontend/themes folder.
     *
     * @param context
     *            the vaadin context
     * @param fileName
     *            the stylesheet file name to add a reference to
     * @return the collection of link tags to be added to the page
     * @throws IOException
     *             if theme name cannot be extracted from file
     */
    protected static Collection<Element> getStylesheetTags(
            VaadinContext context, String fileName) throws IOException {
        ApplicationConfiguration config = ApplicationConfiguration.get(context);
        return ThemeUtils.getActiveThemes(context).stream()
                .map(theme -> getStyleTag(theme, fileName, config)).toList();
    }

    /**
     * Gives a links for referencing the custom theme stylesheet files
     * (typically styles.css or document.css), which are served in express build
     * mode by static file server directly from frontend/themes folder.
     * <p>
     *
     * This method does not verify that the style sheet exists, so it may end up
     * at runtime with broken links. Use
     * {@link #getStylesheetLinks(VaadinContext, String, File)} if you want only
     * links for existing files to be returned.
     *
     * @param context
     *            the vaadin context
     * @param fileName
     *            the stylesheet file name to add a reference to
     * @return the collection of links to be added to the page
     */
    protected static Collection<String> getStylesheetLinks(
            VaadinContext context, String fileName) {
        return getStylesheetLinks(context, fileName, null);
    }

    /**
     * Gives a links for referencing the custom theme stylesheet files
     * (typically styles.css or document.css), which are served in express build
     * mode by static file server directly from frontend/themes folder.
     * <p>
     *
     * This method return links only for existing style sheet files.
     *
     * @param context
     *            the vaadin context
     * @param fileName
     *            the stylesheet file name to add a reference to
     * @param frontendDirectory
     *            the directory where project's frontend files are located.
     *
     * @return the collection of links to be added to the page
     */
    protected static Collection<String> getStylesheetLinks(
            VaadinContext context, String fileName, File frontendDirectory) {
        return ThemeUtils.getActiveThemes(context).stream()
                .filter(theme -> frontendDirectory == null
                        || ThemeUtils.getThemeFolder(frontendDirectory, theme)
                                .toPath().resolve(fileName).toFile().exists())
                .map(theme -> ThemeUtils.getThemeFilePath(theme, fileName))
                .toList();
    }

    private static Element getStyleTag(String themeName, String fileName,
            AbstractConfiguration config) {
        Element element;
        try {
            String themeFilePath = ThemeUtils.getThemeFilePath(themeName,
                    fileName);
            if (config.isProductionMode()) {
                element = new Element("link");
                element.attr("rel", "stylesheet");
                element.attr("type", "text/css");
                element.attr("href", themeFilePath);
            } else {
                element = new Element("style");
                element.attr("data-file-path", themeFilePath);
                File frontendDirectory = FrontendUtils
                        .getProjectFrontendDir(config);
                File stylesCss = new File(
                        ThemeUtils.getThemeFolder(frontendDirectory, themeName),
                        fileName);
                JsonNode themeJson = ThemeUtils.getThemeJson(themeName, config)
                        .orElse(null);

                // Inline CSS into style tag to have hot module reload feature
                element.appendChild(new DataNode(CssBundler.inlineImports(
                        stylesCss.getParentFile(), stylesCss, themeJson)));
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to read theme file from " + fileName, e);
        }
        return element;
    }

    private static void setupPwa(Document document, PwaRegistry registry) {
        if (registry == null) {
            return;
        }

        PwaConfiguration config = registry.getPwaConfiguration();

        if (config.isEnabled()) {
            // Add header injections
            Element head = document.head();

            // Describe PWA capability for iOS devices
            head.appendElement(META_TAG)
                    .attr("name", "apple-mobile-web-app-capable")
                    .attr(CONTENT_ATTRIBUTE, "yes");
            head.appendElement(META_TAG).attr("name", "mobile-web-app-capable")
                    .attr(CONTENT_ATTRIBUTE, "yes");
            head.appendElement(META_TAG).attr("name", "apple-touch-fullscreen")
                    .attr(CONTENT_ATTRIBUTE, "yes");
            head.appendElement(META_TAG)
                    .attr("name", "apple-mobile-web-app-title")
                    .attr(CONTENT_ATTRIBUTE, config.getShortName());

            // Theme color
            head.appendElement(META_TAG).attr("name", "theme-color")
                    .attr(CONTENT_ATTRIBUTE, config.getThemeColor());
            head.appendElement(META_TAG)
                    .attr("name", "apple-mobile-web-app-status-bar-style")
                    .attr(CONTENT_ATTRIBUTE, config.getThemeColor());

            // Add manifest
            head.appendElement("link").attr("rel", "manifest").attr("href",
                    config.getManifestPath());

            // Add icons
            for (PwaIcon icon : registry.getHeaderIcons()) {
                head.appendChild(icon.asElement());
            }

            if (config.isOfflineEnabled()) {
                // Add service worker initialization
                head.appendElement(SCRIPT_TAG).text(String.format(
                        "if ('serviceWorker' in navigator) {\n"
                                + "  window.addEventListener('load', function() {\n"
                                + "    navigator.serviceWorker.register('%s')\n"
                                + "  });\n" + "}",
                        config.getServiceWorkerPath()));
            } else {
                head.appendElement(SCRIPT_TAG).text(String.format(
                        "if ('serviceWorker' in navigator) {\n"
                                + "  navigator.serviceWorker.getRegistration('%s').then(function(registration) {\n"
                                + "    if (registration) {\n"
                                + "      registration.unregister();\n"
                                + "    }\n" + "  });\n" + "}",
                        config.getServiceWorkerPath()));
            }
        }
    }
}
