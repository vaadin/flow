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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.client.ClientResourcesUtils;
import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.BootstrapUtils.ThemeSettings;
import com.vaadin.flow.server.communication.AtmospherePushConnection;
import com.vaadin.flow.server.communication.PushConnectionFactory;
import com.vaadin.flow.server.communication.UidlWriter;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.VaadinUriResolver;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.theme.ThemeDefinition;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

/**
 * Request handler which handles bootstrapping of the application, i.e. the
 * initial GET request.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class BootstrapHandler extends SynchronizedRequestHandler {
    private static final CharSequence GWT_STAT_EVENTS_JS = "if (typeof window.__gwtStatsEvent != 'function') {"
            + "window.Vaadin.Flow.gwtStatsEvents = [];"
            + "window.__gwtStatsEvent = function(event) {"
            + "window.Vaadin.Flow.gwtStatsEvents.push(event); "
            + "return true;};};";
    static final String CONTENT_ATTRIBUTE = "content";
    private static final String DEFER_ATTRIBUTE = "defer";
    static final String VIEWPORT = "viewport";
    private static final String META_TAG = "meta";
    private static final String SCRIPT_TAG = "script";

    /**
     * Location of client nocache file, relative to the context root.
     */
    private static final String CLIENT_ENGINE_NOCACHE_FILE = ApplicationConstants.CLIENT_ENGINE_PATH
            + "/client.nocache.js";
    private static final String BOOTSTRAP_JS = readResource(
            "BootstrapHandler.js");
    private static final String BABEL_HELPERS_JS = readResource(
            "babel-helpers.min.js");
    private static final String ES6_COLLECTIONS = "//<![CDATA[\n"
            + readResource("es6-collections.js") + "//]]>";
    private static final String CSS_TYPE_ATTRIBUTE_VALUE = "text/css";

    private static final String CAPTION = "caption";
    private static final String MESSAGE = "message";
    private static final String URL = "url";

    static Supplier<String> clientEngineFile = () -> LazyClientEngineInit.CLIENT_ENGINE_FILE;

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

        private String appId;
        private PushMode pushMode;
        private JsonObject applicationParameters;
        private BootstrapUriResolver uriResolver;

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
         */
        protected BootstrapContext(VaadinRequest request,
                VaadinResponse response, VaadinSession session, UI ui) {
            this.request = request;
            this.response = response;
            this.session = session;
            this.ui = ui;

            pageConfigurationHolder = BootstrapUtils
                    .resolvePageConfigurationHolder(ui, request).orElse(null);

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
         * Gets the Vaadin session.
         *
         * @return the Vaadin session
         */
        public VaadinSession getSession() {
            return session;
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
                    pushMode = getRequest().getService()
                            .getDeploymentConfiguration().getPushMode();
                }

                if (pushMode.isEnabled()
                        && !getRequest().getService().ensurePushAvailable()) {
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
         * The application id is defined by
         * {@link VaadinService#getMainDivId(VaadinSession, VaadinRequest)}
         *
         * @return the application id
         */
        public String getAppId() {
            if (appId == null) {
                appId = getRequest().getService().getMainDivId(getSession(),
                        getRequest());
            }
            return appId;
        }

        /**
         * Gets the application parameters specified by the BootstrapHandler.
         *
         * @return the application parameters that will be written on the page
         */
        public JsonObject getApplicationParameters() {
            if (applicationParameters == null) {
                applicationParameters = BootstrapHandler
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
            return request.getService().getDeploymentConfiguration()
                    .isProductionMode();
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
         * Gets the {@link ThemeDefinition} associated with the
         * pageConfigurationHolder of this context, if any.
         *
         * @return the theme definition, or empty if none is found, or
         *         pageConfigurationHolder is <code>null</code>
         * @see UI#getThemeFor(Class, String)
         */
        protected Optional<ThemeDefinition> getTheme() {
            return ui.getThemeFor(pageConfigurationHolder, null);
        }
    }

    /**
     * The URI resolver used in the bootstrap process.
     */
    public static class BootstrapUriResolver extends VaadinUriResolver {
        private String frontendRootUrl;
        private String servletPathToContextRoot;

        /**
         * Creates a new bootstrap resolver based on the given request and
         * session.
         *
         * @param ui
         *            the ui to resolve for
         */
        protected BootstrapUriResolver(UI ui) {
            servletPathToContextRoot = ui.getInternals()
                    .getContextRootRelativePath();
            VaadinSession session = ui.getSession();
            DeploymentConfiguration config = session.getConfiguration();
            if (session.getBrowser().isEs6Supported()) {
                frontendRootUrl = config.getEs6FrontendPrefix();
            } else {
                frontendRootUrl = config.getEs5FrontendPrefix();
            }
            assert frontendRootUrl.endsWith("/");
            assert servletPathToContextRoot.endsWith("/");
        }

        /**
         * Translates a Vaadin URI to a URL that can be loaded by the browser.
         * The following URI schemes are supported:
         * <ul>
         * <li><code>{@value ApplicationConstants#CONTEXT_PROTOCOL_PREFIX}</code>
         * - resolves to the application context root</li>
         * <li><code>{@value ApplicationConstants#FRONTEND_PROTOCOL_PREFIX}</code>
         * - resolves to the build path where web components were compiled.
         * Browsers supporting ES6 can receive different, more optimized files
         * than browsers that only support ES5.</li>
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
            return super.resolveVaadinUri(uri, frontendRootUrl,
                    servletPathToContextRoot);
        }

    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        // Find UI class
        Class<? extends UI> uiClass = getUIClass(request);

        BootstrapContext context = createAndInitUI(uiClass, request, response,
                session);

        ServletHelper.setResponseNoCacheHeaders(response::setHeader,
                response::setDateHeader);

        Document document = getBootstrapPage(context);
        writeBootstrapPage(response, document.outerHtml());

        return true;
    }

    static Document getBootstrapPage(BootstrapContext context) {
        Document document = new Document("");
        DocumentType doctype = new DocumentType("html", "", "",
                document.baseUri());
        document.appendChild(doctype);
        Element html = document.appendElement("html");
        html.attr("lang", context.getUI().getLocale().getLanguage());
        Element head = html.appendElement("head");
        html.appendElement("body");

        List<Element> dependenciesToInlineInBody = setupDocumentHead(head,
                context);
        dependenciesToInlineInBody
                .forEach(dependency -> document.body().appendChild(dependency));
        setupDocumentBody(document);

        document.outputSettings().prettyPrint(false);

        BootstrapUtils.getInlineTargets(context)
                .ifPresent(targets -> handleInlineTargets(context, head,
                        document.body(), targets));

        BootstrapUtils.getInitialPageSettings(context).ifPresent(
                initialPageSettings -> handleInitialPageSettings(context, head,
                        initialPageSettings));

        /* Append any theme elements to initial page. */
        handleThemeContents(context, document);

        if (!context.isProductionMode()) {
            exportUsageStatistics(document);
        }

        setupPwa(document, context);

        BootstrapPageResponse response = new BootstrapPageResponse(
                context.getRequest(), context.getSession(),
                context.getResponse(), document, context.getUI(),
                context.getUriResolver());
        context.getSession().getService().modifyBootstrapPage(response);

        return document;
    }

    private static void exportUsageStatistics(Document document) {
        String registerScript = UsageStatistics.getEntries().map(entry -> {
            String name = entry.getName();
            String version = entry.getVersion();

            JsonObject json = Json.createObject();
            json.put("is", name);
            json.put("version", version);

            String escapedName = Json.create(name).toJson();

            // Registers the entry in a way that is picked up as a Vaadin
            // WebComponent by the usage stats gatherer
            return String.format("window.Vaadin[%s]=%s;", escapedName, json);
        }).collect(Collectors.joining("\n"));

        if (!registerScript.isEmpty()) {
            document.body().appendElement(SCRIPT_TAG).text(registerScript);
        }
    }

    private static void handleThemeContents(BootstrapContext context,
            Document document) {
        ThemeSettings themeSettings = BootstrapUtils.getThemeSettings(context);

        if (themeSettings == null) {
            // no theme configured for the application
            return;
        }

        List<JsonObject> themeContents = themeSettings.getHeadContents();
        if (themeContents != null) {
            themeContents.stream().map(
                    dependency -> createDependencyElement(context, dependency))
                    .forEach(element -> insertElements(element,
                            document.head()::appendChild));
        }

        JsonObject themeContent = themeSettings.getHeadInjectedContent();
        if (themeContent != null) {
            Element dependency = createDependencyElement(context, themeContent);
            insertElements(dependency, document.head()::appendChild);
        }

        if (themeSettings.getBodyAttributes() != null) {
            themeSettings.getBodyAttributes()
                    .forEach((key, value) -> document.body().attr(key, value));
        }
    }

    private static void handleInlineTargets(BootstrapContext context,
            Element head, Element body, InlineTargets targets) {
        targets.getInlineHead(Inline.Position.PREPEND).stream()
                .map(dependency -> createDependencyElement(context, dependency))
                .forEach(
                        element -> insertElements(element, head::prependChild));
        targets.getInlineHead(Inline.Position.APPEND).stream()
                .map(dependency -> createDependencyElement(context, dependency))
                .forEach(element -> insertElements(element, head::appendChild));

        targets.getInlineBody(Inline.Position.PREPEND).stream()
                .map(dependency -> createDependencyElement(context, dependency))
                .forEach(
                        element -> insertElements(element, body::prependChild));
        targets.getInlineBody(Inline.Position.APPEND).stream()
                .map(dependency -> createDependencyElement(context, dependency))
                .forEach(element -> insertElements(element, body::appendChild));
    }

    private static void handleInitialPageSettings(BootstrapContext context,
            Element head, InitialPageSettings initialPageSettings) {
        if (initialPageSettings.getViewport() != null) {
            Elements viewport = head.getElementsByAttributeValue("name",
                    VIEWPORT);
            if (!viewport.isEmpty() && viewport.size() == 1) {
                viewport.get(0).attr(CONTENT_ATTRIBUTE,
                        initialPageSettings.getViewport());
            } else {
                head.appendElement(META_TAG).attr("name", VIEWPORT).attr(
                        CONTENT_ATTRIBUTE, initialPageSettings.getViewport());
            }
        }

        initialPageSettings.getInline(InitialPageSettings.Position.PREPEND)
                .stream()
                .map(dependency -> createDependencyElement(context, dependency))
                .forEach(
                        element -> insertElements(element, head::prependChild));
        initialPageSettings.getInline(InitialPageSettings.Position.APPEND)
                .stream()
                .map(dependency -> createDependencyElement(context, dependency))
                .forEach(element -> insertElements(element, head::appendChild));

        initialPageSettings.getElement(InitialPageSettings.Position.PREPEND)
                .forEach(
                        element -> insertElements(element, head::prependChild));
        initialPageSettings.getElement(InitialPageSettings.Position.APPEND)
                .forEach(element -> insertElements(element, head::appendChild));
    }

    private static void insertElements(Element element,
            Consumer<Element> action) {
        if (element instanceof Document) {
            element.getAllElements().stream()
                    .filter(item -> !(item instanceof Document)
                            && element.equals(item.parent()))
                    .forEach(action::accept);
        } else {
            action.accept(element);
        }
    }

    private static void writeBootstrapPage(VaadinResponse response, String html)
            throws IOException {
        response.setContentType(
                ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), UTF_8))) {
            writer.append(html);
        }
    }

    private static List<Element> setupDocumentHead(Element head,
            BootstrapContext context) {
        setupMetaAndTitle(head, context);
        setupCss(head, context);

        JsonObject initialUIDL = getInitialUidl(context.getUI());
        Map<LoadMode, JsonArray> dependenciesToProcessOnServer = popDependenciesToProcessOnServer(
                initialUIDL);
        setupFrameworkLibraries(head, initialUIDL, context);
        return applyUserDependencies(head, context,
                dependenciesToProcessOnServer);
    }

    private static List<Element> applyUserDependencies(Element head,
            BootstrapContext context,
            Map<LoadMode, JsonArray> dependenciesToProcessOnServer) {
        List<Element> dependenciesToInlineInBody = new ArrayList<>();
        for (Map.Entry<LoadMode, JsonArray> entry : dependenciesToProcessOnServer
                .entrySet()) {
            dependenciesToInlineInBody.addAll(
                    inlineDependenciesInHead(head, context.getUriResolver(),
                            entry.getKey(), entry.getValue()));
        }
        return dependenciesToInlineInBody;
    }

    private static List<Element> inlineDependenciesInHead(Element head,
            BootstrapUriResolver uriResolver, LoadMode loadMode,
            JsonArray dependencies) {
        List<Element> dependenciesToInlineInBody = new ArrayList<>();

        for (int i = 0; i < dependencies.length(); i++) {
            JsonObject dependencyJson = dependencies.getObject(i);
            Dependency.Type dependencyType = Dependency.Type
                    .valueOf(dependencyJson.getString(Dependency.KEY_TYPE));
            Element dependencyElement = createDependencyElement(uriResolver,
                    loadMode, dependencyJson, dependencyType);

            if (loadMode == LoadMode.INLINE
                    && dependencyType == Dependency.Type.HTML_IMPORT) {
                dependenciesToInlineInBody.add(dependencyElement);
            } else {
                head.appendChild(dependencyElement);
            }
        }
        return dependenciesToInlineInBody;
    }

    private static Map<LoadMode, JsonArray> popDependenciesToProcessOnServer(
            JsonObject initialUIDL) {
        Map<LoadMode, JsonArray> result = new EnumMap<>(LoadMode.class);
        Stream.of(LoadMode.EAGER, LoadMode.INLINE).forEach(mode -> {
            if (initialUIDL.hasKey(mode.name())) {
                result.put(mode, initialUIDL.getArray(mode.name()));
                initialUIDL.remove(mode.name());
            }
        });
        return result;
    }

    private static void setupFrameworkLibraries(Element head,
            JsonObject initialUIDL, BootstrapContext context) {
        inlineEs6Collections(head, context);
        appendWebComponentsPolyfills(head, context);

        if (context.getPushMode().isEnabled()) {
            head.appendChild(getPushScript(context));
        }

        head.appendChild(getBootstrapScript(initialUIDL, context));
        head.appendChild(createJavaScriptElement(getClientEngineUrl(context)));
    }

    private static void inlineEs6Collections(Element head,
            BootstrapContext context) {
        if (!context.getSession().getBrowser().isEs6Supported()) {
            head.appendChild(createInlineJavaScriptElement(ES6_COLLECTIONS));
        }
    }

    private static void setupCss(Element head, BootstrapContext context) {
        Element styles = head.appendElement("style").attr("type",
                CSS_TYPE_ATTRIBUTE_VALUE);
        // Add any body style that is defined for the application using
        // @BodySize
        String bodySizeContent = BootstrapUtils.getBodySizeContent(context);
        styles.appendText(bodySizeContent);
        // Basic reconnect dialog style just to make it visible and outside of
        // normal flow
        styles.appendText(".v-reconnect-dialog {" //
                + "position: absolute;" //
                + "top: 1em;" //
                + "right: 1em;" //
                + "border: 1px solid black;" //
                + "padding: 1em;" //
                + "z-index: 10000;" //
                + "}");

        // Basic system error dialog style just to make it visible and outside
        // of normal flow
        styles.appendText(".v-system-error {" //
                + "color: red;" //
                + "background: white;" //
                + "position: absolute;" //
                + "top: 1em;" //
                + "right: 1em;" //
                + "border: 1px solid black;" //
                + "padding: 1em;" //
                + "z-index: 10000;" //
                + "pointer-events: auto;" //
                + "}");
    }

    private static void setupMetaAndTitle(Element head,
            BootstrapContext context) {
        head.appendElement(META_TAG).attr("http-equiv", "Content-Type").attr(
                CONTENT_ATTRIBUTE,
                ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);

        head.appendElement(META_TAG).attr("http-equiv", "X-UA-Compatible")
                .attr(CONTENT_ATTRIBUTE, "IE=edge");

        head.appendElement("base").attr("href", getServiceUrl(context));

        head.appendElement(META_TAG).attr("name", VIEWPORT)
                .attr(CONTENT_ATTRIBUTE, BootstrapUtils
                        .getViewportContent(context).orElse(Viewport.DEFAULT));

        if (!BootstrapUtils.getMetaTargets(context).isEmpty()) {
            BootstrapUtils.getMetaTargets(context)
                    .forEach((name, content) -> head.appendElement(META_TAG)
                            .attr("name", name)
                            .attr(CONTENT_ATTRIBUTE, content));
        }
        resolvePageTitle(context).ifPresent(title -> {
            if (!title.isEmpty()) {
                head.appendElement("title").appendText(title);
            }
        });
    }

    private static void setupPwa(Document document, BootstrapContext context) {
        VaadinService vaadinService = context.getSession().getService();
        if (vaadinService == null) {
            return;
        }

        PwaRegistry registry = vaadinService.getPwaRegistry();
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

            // Add service worker initialization
            head.appendElement(SCRIPT_TAG)
                    .text("if ('serviceWorker' in navigator) {\n"
                            + "  window.addEventListener('load', function() {\n"
                            + "    navigator.serviceWorker.register('"
                            + config.getServiceWorkerPath() + "');\n"
                            + "  });\n" + "}");

            // add body injections
            if (registry.getPwaConfiguration().isInstallPromptEnabled()) {
                // PWA Install prompt html/js
                document.body().append(registry.getInstallPrompt());
            }
        }
    }

    private static void appendWebComponentsPolyfills(Element head,
            BootstrapContext context) {
        VaadinSession session = context.getSession();
        DeploymentConfiguration config = session.getConfiguration();

        String webcomponentsLoaderUrl = "frontend://bower_components/webcomponentsjs/webcomponents-loader.js";
        String es5AdapterUrl = "frontend://bower_components/webcomponentsjs/custom-elements-es5-adapter.js";
        VaadinService service = session.getService();
        if (!service.isResourceAvailable(webcomponentsLoaderUrl,
                session.getBrowser(), null)) {
            // No webcomponents polyfill, load nothing
            return;
        }

        boolean loadEs5Adapter = config
                .getBooleanProperty(Constants.LOAD_ES5_ADAPTERS, true);
        if (loadEs5Adapter && !session.getBrowser().isEs6Supported()) {
            // This adapter is required since lots of our current customers
            // use polymer-cli to transpile sources,
            // this tool adds babel-helpers dependency into each file, see:
            // https://github.com/Polymer/polymer-cli/blob/master/src/build/build.ts#L64
            // and
            // https://github.com/Polymer/polymer-cli/blob/master/src/build/optimize-streams.ts#L119
            head.appendChild(createInlineJavaScriptElement(BABEL_HELPERS_JS));

            if (session.getBrowser().isEs5AdapterNeeded()) {
                head.appendChild(
                        createJavaScriptElement(context.getUriResolver()
                                .resolveVaadinUri(es5AdapterUrl), false));
            }
        }

        String resolvedUrl = context.getUriResolver()
                .resolveVaadinUri(webcomponentsLoaderUrl);
        head.appendChild(createJavaScriptElement(resolvedUrl, false));

    }

    private static Element createInlineJavaScriptElement(
            String javaScriptContents) {
        // defer makes no sense without src:
        // https://developer.mozilla.org/en/docs/Web/HTML/Element/script
        Element wrapper = createJavaScriptElement(null, false);
        wrapper.appendChild(
                new DataNode(javaScriptContents, wrapper.baseUri()));
        return wrapper;
    }

    private static Element createJavaScriptElement(String sourceUrl,
            boolean defer) {
        Element jsElement = new Element(Tag.valueOf(SCRIPT_TAG), "")
                .attr("type", "text/javascript").attr(DEFER_ATTRIBUTE, defer);
        if (sourceUrl != null) {
            jsElement = jsElement.attr("src", sourceUrl);
        }
        return jsElement;
    }

    private static Element createJavaScriptElement(String sourceUrl) {
        return createJavaScriptElement(sourceUrl, true);
    }

    private static Element createDependencyElement(
            BootstrapUriResolver resolver, LoadMode loadMode,
            JsonObject dependency, Dependency.Type type) {
        boolean inlineElement = loadMode == LoadMode.INLINE;
        String url = dependency.hasKey(Dependency.KEY_URL)
                ? resolver.resolveVaadinUri(
                        dependency.getString(Dependency.KEY_URL))
                : null;

        final Element dependencyElement;
        switch (type) {
        case STYLESHEET:
            dependencyElement = createStylesheetElement(url);
            break;
        case JAVASCRIPT:
            dependencyElement = createJavaScriptElement(url, !inlineElement);
            break;
        case HTML_IMPORT:
            dependencyElement = createHtmlImportElement(url);
            break;
        default:
            throw new IllegalStateException(
                    "Unsupported dependency type: " + type);
        }

        if (inlineElement) {
            dependencyElement.appendChild(
                    new DataNode(dependency.getString(Dependency.KEY_CONTENTS),
                            dependencyElement.baseUri()));
        }

        return dependencyElement;
    }

    private static Element createHtmlImportElement(String url) {
        final Element htmlImportElement;
        if (url != null) {
            htmlImportElement = new Element(Tag.valueOf("link"), "")
                    .attr("rel", "import").attr("href", url);
        } else {
            htmlImportElement = new Element(Tag.valueOf("span"), "")
                    .attr("hidden", true);
        }
        return htmlImportElement;
    }

    private static Element createStylesheetElement(String url) {
        final Element cssElement;
        if (url != null) {
            cssElement = new Element(Tag.valueOf("link"), "")
                    .attr("rel", "stylesheet")
                    .attr("type", CSS_TYPE_ATTRIBUTE_VALUE).attr("href", url);
        } else {
            cssElement = new Element(Tag.valueOf("style"), "").attr("type",
                    CSS_TYPE_ATTRIBUTE_VALUE);
        }
        return cssElement;
    }

    private static void setupDocumentBody(Document document) {
        document.body().appendElement("noscript").append(
                "You have to enable javascript in your browser to use this web site.");
    }

    private static Element getPushScript(BootstrapContext context) {
        VaadinRequest request = context.getRequest();

        // Parameter appended to JS to bypass caches after version upgrade.
        String versionQueryParam = "?v=" + Version.getFullVersion();

        // Load client-side dependencies for push support
        String pushJSPath = ServletHelper.getContextRootRelativePath(request)
                + "/";
        if (request.getService().getDeploymentConfiguration()
                .isProductionMode()) {
            pushJSPath += ApplicationConstants.VAADIN_PUSH_JS;
        } else {
            pushJSPath += ApplicationConstants.VAADIN_PUSH_DEBUG_JS;
        }

        pushJSPath += versionQueryParam;

        return createJavaScriptElement(pushJSPath);
    }

    private static Element getBootstrapScript(JsonValue initialUIDL,
            BootstrapContext context) {
        return createInlineJavaScriptElement("//<![CDATA[\n"
                + getBootstrapJS(initialUIDL, context) + "//]]>");
    }

    private static String getBootstrapJS(JsonValue initialUIDL,
            BootstrapContext context) {
        boolean productionMode = context.getSession().getConfiguration()
                .isProductionMode();
        String result = getBootstrapJS();
        JsonObject appConfig = context.getApplicationParameters();
appConfig.put(ApplicationConstants.UI_TAG, context.getUI().getElement().getTag());

        int indent = 0;
        if (!productionMode) {
            indent = 4;
        }
        String appConfigString = JsonUtil.stringify(appConfig, indent);

        String initialUIDLString = JsonUtil.stringify(initialUIDL, indent);

        /*
         * The < symbol is escaped to prevent two problems:
         *
         * 1 - The browser interprets </script> as end of script no matter if it
         * is inside a string
         *
         * 2 - Scripts can be injected with <!-- <script>, that can cause
         * unexpected behavior or complete crash of the app
         */
        initialUIDLString = initialUIDLString.replace("<", "\\x3C");

        if (!productionMode) {
            // only used in debug mode by profiler
            result = result.replace("{{GWT_STAT_EVENTS}}", GWT_STAT_EVENTS_JS);
        } else {
            result = result.replace("{{GWT_STAT_EVENTS}}", "");
        }

        result = result.replace("{{APP_ID}}", context.getAppId());
        result = result.replace("{{CONFIG_JSON}}", appConfigString);
        // {{INITIAL_UIDL}} should be the last replaced so that it may have
        // other patterns inside it (like {{CONFIG_JSON}})
        result = result.replace("{{INITIAL_UIDL}}", initialUIDLString);
        return result;
    }

    protected static JsonObject getApplicationParameters(
            BootstrapContext context) {
        JsonObject appConfig = getApplicationParameters(context.getRequest(),
                context.getSession());

        appConfig.put(ApplicationConstants.UI_ID_PARAMETER,
                context.getUI().getUIId());
        return appConfig;
    }

    private static JsonObject getApplicationParameters(VaadinRequest request,
            VaadinSession session) {
        DeploymentConfiguration deploymentConfiguration = session
                .getConfiguration();
        final boolean productionMode = deploymentConfiguration
                .isProductionMode();

        JsonObject appConfig = Json.createObject();

        appConfig.put(ApplicationConstants.FRONTEND_URL_ES6,
                deploymentConfiguration.getEs6FrontendPrefix());
        appConfig.put(ApplicationConstants.FRONTEND_URL_ES5,
                deploymentConfiguration.getEs5FrontendPrefix());
        appConfig.put(ApplicationConstants.UI_ELEMENT_ID,
                        deploymentConfiguration.getRootElementId());

        if (!productionMode) {
            JsonObject versionInfo = Json.createObject();
            versionInfo.put("vaadinVersion", Version.getFullVersion());
            String atmosphereVersion = AtmospherePushConnection
                    .getAtmosphereVersion();
            if (atmosphereVersion != null) {
                versionInfo.put("atmosphereVersion", atmosphereVersion);
            }
            appConfig.put("versionInfo", versionInfo);
        }

        // Use locale from session if set, else from the request
        Locale locale = ServletHelper.findLocale(session, request);
        // Get system messages
        SystemMessages systemMessages = session.getService()
                .getSystemMessages(locale, request);
        if (systemMessages != null) {
            JsonObject sessExpMsg = Json.createObject();
            putValueOrNull(sessExpMsg, CAPTION,
                    systemMessages.getSessionExpiredCaption());
            putValueOrNull(sessExpMsg, MESSAGE,
                    systemMessages.getSessionExpiredMessage());
            putValueOrNull(sessExpMsg, URL,
                    systemMessages.getSessionExpiredURL());

            appConfig.put("sessExpMsg", sessExpMsg);
        }

        String contextRoot = ServletHelper.getContextRootRelativePath(request)
                + "/";
        appConfig.put(ApplicationConstants.CONTEXT_ROOT_URL, contextRoot);

        if (!productionMode) {
            appConfig.put("debug", true);
        }

        if (deploymentConfiguration.isRequestTiming()) {
            appConfig.put("requestTiming", true);
        }

        appConfig.put("heartbeatInterval",
                deploymentConfiguration.getHeartbeatInterval());

        boolean sendUrlsAsParameters = deploymentConfiguration
                .isSendUrlsAsParameters();
        if (!sendUrlsAsParameters) {
            appConfig.put("sendUrlsAsParameters", false);
        }

        return appConfig;
    }

    /**
     * Gets the service URL as a URL relative to the request URI.
     *
     * @param context
     *            the bootstrap context
     * @return the relative service URL
     */
    protected static String getServiceUrl(BootstrapContext context) {
        String pathInfo = context.getRequest().getPathInfo();
        if (pathInfo == null) {
            return ".";
        } else {
            /*
             * Make a relative URL to the servlet by adding one ../ for each
             * path segment in pathInfo (i.e. the part of the requested path
             * that comes after the servlet mapping)
             */
            return ServletHelper.getCancelingRelativePath(pathInfo);
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
                ServletHelper.getContextRootRelativePath(request) + "/");

        PushConfiguration pushConfiguration = ui.getPushConfiguration();

        ui.getInternals().setSession(session);
        ui.setLocale(session.getLocale());

        BootstrapContext context = new BootstrapContext(request, response,
                session, ui);

        Optional<Push> push = context
                .getPageConfigurationAnnotation(Push.class);

        DeploymentConfiguration deploymentConfiguration = context.getSession()
                .getService().getDeploymentConfiguration();
        PushMode pushMode = push.map(Push::value)
                .orElseGet(deploymentConfiguration::getPushMode);
        setupPushConnectionFactory(pushConfiguration, context);
        pushConfiguration.setPushMode(pushMode);
        pushConfiguration.setPushUrl(deploymentConfiguration.getPushURL());
        push.map(Push::transport).ifPresent(pushConfiguration::setTransport);

        // Set thread local here so it is available in init
        UI.setCurrent(ui);
        ui.doInit(request, session.getNextUIid());
        session.addUI(ui);

        // After init and adding UI to session fire init listeners.
        session.getService().fireUIInitListeners(ui);

        if (ui.getRouter() != null) {
            ui.getRouter().initializeUI(ui, request);
        }

        return context;
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
     * Generates the initial UIDL message which is included in the initial
     * bootstrap page.
     *
     * @param ui
     *            the UI for which the UIDL should be generated
     * @return a JSON object with the initial UIDL message
     */
    protected static JsonObject getInitialUidl(UI ui) {
        JsonObject json = new UidlWriter().createUidl(ui, false);

        VaadinSession session = ui.getSession();
        if (session.getConfiguration().isXsrfProtectionEnabled()) {
            writeSecurityKeyUIDL(json, session);
        }
        writePushIdUIDL(json, session);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Initial UIDL: {}", json.asString());
        }
        return json;
    }

    /**
     * Writes the security key (and generates one if needed) to the given JSON
     * object.
     *
     * @param response
     *            the response JSON object to write security key into
     * @param session
     *            the vaadin session to which the security key belongs
     */
    private static void writeSecurityKeyUIDL(JsonObject response,
            VaadinSession session) {
        String seckey = session.getCsrfToken();
        response.put(ApplicationConstants.UIDL_SECURITY_TOKEN_ID, seckey);
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
    private static void writePushIdUIDL(JsonObject response,
            VaadinSession session) {
        String pushId = session.getPushId();
        response.put(ApplicationConstants.UIDL_PUSH_ID, pushId);
    }

    private static void putValueOrNull(JsonObject object, String key,
            String value) {
        assert object != null;
        assert key != null;
        if (value == null) {
            object.put(key, Json.createNull());
        } else {
            object.put(key, value);
        }
    }

    private static String getBootstrapJS() {
        if (BOOTSTRAP_JS.isEmpty()) {
            throw new BootstrapException(
                    "BootstrapHandler.js has not been loaded during initialization");
        }
        return BOOTSTRAP_JS;
    }

    private static String getClientEngineUrl(BootstrapContext context) {
        // use nocache version of client engine if it
        // has been compiled by SDM or eclipse
        // In production mode, this should really be loaded by the static block
        // so emit a warning if we get here (tests will always get here)
        final boolean productionMode = context.getSession().getConfiguration()
                .isProductionMode();

        boolean resolveNow = !productionMode || getClientEngine() == null;
        if (resolveNow && ClientResourcesUtils.getResource(
                "/META-INF/resources/" + CLIENT_ENGINE_NOCACHE_FILE) != null) {
            return context.getUriResolver().resolveVaadinUri(
                    "context://" + CLIENT_ENGINE_NOCACHE_FILE);
        }

        if (getClientEngine() == null) {
            throw new BootstrapException(
                    "Client engine file name has not been resolved during initialization");
        }
        return context.getUriResolver()
                .resolveVaadinUri("context://" + getClientEngine());
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

    private static Element createDependencyElement(BootstrapContext context,
            JsonObject dependencyJson) {
        String type = dependencyJson.getString(Dependency.KEY_TYPE);
        if (Dependency.Type.contains(type)) {
            Dependency.Type dependencyType = Dependency.Type.valueOf(type);
            return createDependencyElement(context.getUriResolver(),
                    LoadMode.INLINE, dependencyJson, dependencyType);
        }
        return Jsoup.parse(dependencyJson.getString(Dependency.KEY_CONTENTS),
                "", Parser.xmlParser());
    }

    private static String readClientEngine() {
        // read client engine file name
        try (InputStream prop = ClientResourcesUtils.getResource(
                "/META-INF/resources/" + ApplicationConstants.CLIENT_ENGINE_PATH
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

    private static String getClientEngine() {
        return clientEngineFile.get();
    }

    private static class LazyClientEngineInit {
        private static final String CLIENT_ENGINE_FILE = readClientEngine();
    }
}
