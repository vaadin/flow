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

package com.vaadin.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.vaadin.annotations.AnnotationReader;
import com.vaadin.annotations.Viewport;
import com.vaadin.annotations.ViewportGeneratorClass;
import com.vaadin.annotations.WebComponents;
import com.vaadin.annotations.WebComponents.PolyfillVersion;
import com.vaadin.external.jsoup.nodes.DataNode;
import com.vaadin.external.jsoup.nodes.Document;
import com.vaadin.external.jsoup.nodes.DocumentType;
import com.vaadin.external.jsoup.nodes.Element;
import com.vaadin.external.jsoup.nodes.Node;
import com.vaadin.external.jsoup.parser.Tag;
import com.vaadin.server.communication.AtmospherePushConnection;
import com.vaadin.server.communication.UidlWriter;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.VaadinUriResolver;
import com.vaadin.shared.Version;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.ui.ComponentUtil;
import com.vaadin.ui.DependencyList;
import com.vaadin.ui.UI;
import com.vaadin.util.ReflectTools;

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
 * @since 7.0.0
 */
public class BootstrapHandler extends SynchronizedRequestHandler {

    static final String PRE_RENDER_INFO_TEXT = "This is only a pre-rendered version. Remove ?prerender=only to see the full version";

    private static final CharSequence GWT_STAT_EVENTS_JS = "if (typeof window.__gwtStatsEvent != 'function') {"
            + "flow.gwtStatsEvents = [];"
            + "window.__gwtStatsEvent = function(event) {"
            + "flow.gwtStatsEvents.push(event); " + "return true;};};";
    private static final String CONTENT_ATTRIBUTE = "content";
    private static final String DEFER_ATTRIBUTE = "defer";
    private static final String META_TAG = "meta";
    /**
     * Location of client nocache file, relative to the context root.
     */
    private static final String CLIENT_ENGINE_NOCACHE_FILE = ApplicationConstants.CLIENT_ENGINE_PATH
            + "/client.nocache.js";
    private static final Pattern SCRIPT_END_TAG_PATTERN = Pattern
            .compile("</(script)", Pattern.CASE_INSENSITIVE);
    private static final String BOOTSTRAP_JS;

    static String clientEngineFile;

    private static Logger getLogger() {
        return Logger.getLogger(BootstrapHandler.class.getName());
    }

    static {
        // read bootstrap javascript template
        try (InputStream stream = BootstrapHandler.class
                .getResourceAsStream("BootstrapHandler.js");
                BufferedReader bf = new BufferedReader(new InputStreamReader(
                        stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            bf.lines().forEach(sb::append);
            BOOTSTRAP_JS = sb.toString();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        // read client engine file name
        try (InputStream prop = BootstrapHandler.class.getResourceAsStream(
                "/META-INF/resources/" + ApplicationConstants.CLIENT_ENGINE_PATH
                        + "/compile.properties")) {
            // null when running SDM or tests
            if (prop != null) {
                Properties p = new Properties();
                p.load(prop);
                clientEngineFile = ApplicationConstants.CLIENT_ENGINE_PATH + "/"
                        + p.getProperty("jsFile");
            } else {
                getLogger().warning(
                        "No compile.properties available on initialization, "
                                + "could not read client engine file name.");
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected static class BootstrapContext {

        private final VaadinRequest request;
        private final VaadinResponse response;
        private final VaadinSession session;
        private final UI ui;

        private String appId;
        private PushMode pushMode;
        private JsonObject applicationParameters;
        private VaadinUriResolver uriResolver;

        protected BootstrapContext(VaadinRequest request,
                VaadinResponse response, VaadinSession session, UI ui) {
            this.request = request;
            this.response = response;
            this.session = session;
            this.ui = ui;
        }

        public VaadinResponse getResponse() {
            return response;
        }

        public VaadinRequest getRequest() {
            return request;
        }

        public VaadinSession getSession() {
            return session;
        }

        public UI getUI() {
            return ui;
        }

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

        public String getAppId() {
            if (appId == null) {
                appId = getRequest().getService().getMainDivId(getSession(),
                        getRequest());
            }
            return appId;
        }

        public JsonObject getApplicationParameters() {
            if (applicationParameters == null) {
                applicationParameters = BootstrapHandler
                        .getApplicationParameters(this);
            }

            return applicationParameters;
        }

        public VaadinUriResolver getUriResolver() {
            if (uriResolver == null) {
                uriResolver = new BootstrapUriResolver(this);
            }

            return uriResolver;
        }

        /**
         * Gets the pre-rendering mode.
         * <p>
         * The pre-rendering mode can be "pre-render only", "pre-render and live
         * " or "live only" and is only meant for testing.
         *
         * @return the mode to use for pre-rendering
         */
        public PreRenderMode getPreRenderMode() {
            String preParam = request.getParameter("prerender");
            if (preParam != null) {
                if ("only".equals(preParam)) {
                    return PreRenderMode.PRE_ONLY;
                } else if ("no".equals(preParam)) {
                    return PreRenderMode.LIVE_ONLY;
                }
            }
            return PreRenderMode.PRE_AND_LIVE;
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

    }

    enum PreRenderMode {
        PRE_AND_LIVE, PRE_ONLY, LIVE_ONLY;

        /**
         * Checks if a live version of the application should be rendered.
         *
         * @return <code>true</code> if a live version should be rendered,
         *         <code>false</code> otherwise
         */
        public boolean includeLiveVersion() {
            return this == PRE_AND_LIVE || this == LIVE_ONLY;
        }

        /**
         * Checks if a pre-render version of the application should be included.
         *
         * @return <code>true</code> if a pre-render version should be included,
         *         <code>false</code> otherwise
         */
        public boolean includePreRenderVersion() {
            return this == PRE_AND_LIVE || this == PRE_ONLY;
        }
    }

    private static class BootstrapUriResolver extends VaadinUriResolver {
        private final BootstrapContext context;
        private final String es6BuildUrl;
        private final String es5BuildUrl;

        protected BootstrapUriResolver(BootstrapContext bootstrapContext) {
            context = bootstrapContext;

            DeploymentConfiguration config = context.getSession()
                    .getConfiguration();
            if (config.isProductionMode()) {
                es6BuildUrl = config.getApplicationOrSystemProperty(
                        Constants.FRONTEND_URL_ES6,
                        ApplicationConstants.FRONTEND_URL_ES6_DEFAULT_VALUE);
                es5BuildUrl = config.getApplicationOrSystemProperty(
                        Constants.FRONTEND_URL_ES5,
                        ApplicationConstants.FRONTEND_URL_ES5_DEFAULT_VALUE);
            } else {
                es6BuildUrl = config.getApplicationOrSystemProperty(
                        Constants.FRONTEND_URL_ES6,
                        ApplicationConstants.CONTEXT_PROTOCOL_PREFIX);
                es5BuildUrl = config.getApplicationOrSystemProperty(
                        Constants.FRONTEND_URL_ES5,
                        ApplicationConstants.CONTEXT_PROTOCOL_PREFIX);
            }
        }

        @Override
        protected String getContextRootUrl() {
            String root = context.getApplicationParameters()
                    .getString(ApplicationConstants.CONTEXT_ROOT_URL);
            assert root.endsWith("/");
            return root;
        }

        @Override
        protected String getFrontendRootUrl() {
            String root;
            if (context.getSession().getBrowser().isEs6Supported()) {
                root = es6BuildUrl;
            } else {
                root = es5BuildUrl;
            }
            assert root.endsWith("/");
            return root;
        }

    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        // Find UI class
        Class<? extends UI> uiClass = getUIClass(request);

        UI ui = createAndInitUI(uiClass, request, session);

        BootstrapContext context = new BootstrapContext(request, response,
                session, ui);

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
        Element head = html.appendElement("head");

        setupDocumentHead(head, context);
        setupDocumentBody(document, context);

        document.outputSettings().prettyPrint(false);

        BootstrapPageResponse response = new BootstrapPageResponse(
                context.getRequest(), context.getSession(),
                context.getResponse(), document, context.getUI());
        context.getSession().getService().modifyBootstrapPage(response);

        return document;
    }

    private static void writeBootstrapPage(VaadinResponse response, String html)
            throws IOException {
        response.setContentType("text/html");
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
        writer.append(html);
        writer.close();
    }

    private static void setupDocumentHead(Element head,
            BootstrapContext context) {
        head.appendElement(META_TAG).attr("http-equiv", "Content-Type")
                .attr(CONTENT_ATTRIBUTE, "text/html; charset=utf-8");

        head.appendElement("base").attr("href", getServiceUrl(context));

        Class<? extends UI> uiClass = context.getUI().getClass();

        String viewportContent = getViewportContent(uiClass,
                context.getRequest());
        if (viewportContent != null) {
            head.appendElement(META_TAG).attr("name", "viewport")
                    .attr(CONTENT_ATTRIBUTE, viewportContent);
        }

        Optional<String> title = resolvePageTitle(context);
        if (title.isPresent() && !title.get().isEmpty()) {
            head.appendElement("title").appendText(title.get());
        }

        JsonObject initialUIDL = getInitialUidl(context.getUI());
        includeDependenciesInPreRender(head, initialUIDL,
                context.getUriResolver());

        Element styles = head.appendElement("style").attr("type", "text/css");
        styles.appendText("html, body {height:100%;margin:0;}");
        // Basic reconnect dialog style just to make it visible and outside of
        // normal flow
        styles.appendText(".v-reconnect-dialog {" //
                + "position: absolute;" //
                + "top: 1em;" //
                + "right: 1em;" //
                + "border: 1px solid black;" //
                + "padding: 1em;" //
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
                + "}");

        // Collections polyfill needed for PhantomJS and maybe googlebot
        head.appendChild(
                createJavaScriptElement(
                        context.getUriResolver()
                                .resolveVaadinUri("context://"
                                        + ApplicationConstants.VAADIN_STATIC_FILES_PATH
                                        + "server/es6-collections.js"),
                        false));

        appendWebComponentsElements(head, context);

        if (context.getPushMode().isEnabled()) {
            head.appendChild(getPushScript(context));
        }

        if (context.getPreRenderMode().includeLiveVersion()) {
            head.appendChild(getBootstrapScript(initialUIDL, context));
            head.appendChild(
                    createJavaScriptElement(getClientEngineUrl(context)));
        }

    }

    private static void appendWebComponentsElements(Element head,
            BootstrapContext context) {
        Optional<WebComponents> webComponents = AnnotationReader
                .getAnnotationFor(context.getUI().getClass(),
                        WebComponents.class);

        boolean isVersion1;
        boolean forceShadyDom;
        boolean loadEs5Adapter;

        DeploymentConfiguration config = context.getSession()
                .getConfiguration();

        isVersion1 = getUserDefinedProperty(config, Constants.WEB_COMPONENTS,
                version -> String.valueOf(1).equals(version),
                webComponents.isPresent()
                        && webComponents.get().value() == PolyfillVersion.V1);
        forceShadyDom = getUserDefinedProperty(config,
                Constants.FORCE_SHADY_DOM, Boolean::parseBoolean,
                webComponents.isPresent()
                        && webComponents.get().forceShadyDom());

        loadEs5Adapter = getUserDefinedProperty(config,
                Constants.LOAD_ES5_ADAPTER, Boolean::parseBoolean,
                webComponents.isPresent()
                        && webComponents.get().loadEs5Adapter());

        if (loadEs5Adapter) {
            head.appendChild(createJavaScriptElement(
                    context.getUriResolver()
                            .resolveVaadinUri("context://"
                                    + ApplicationConstants.VAADIN_STATIC_FILES_PATH
                                    + "server/custom-elements-es5-adapter.js"),
                    false));
        }

        if (isVersion1) {
            head.appendChild(createJavaScriptElement(
                    context.getUriResolver()
                            .resolveVaadinUri("context://"
                                    + ApplicationConstants.VAADIN_STATIC_FILES_PATH
                                    + "server/v1/webcomponents-lite.js"),
                    false).attr("shadydom", forceShadyDom));
        } else {
            head.appendChild(createJavaScriptElement(context.getUriResolver()
                    .resolveVaadinUri("context://"
                            + ApplicationConstants.VAADIN_STATIC_FILES_PATH
                            + "server/webcomponents-lite.min.js")));
        }
    }

    private static Element createJavaScriptElement(String sourceUrl,
            boolean defer) {
        Element jsElement = new Element(Tag.valueOf("script"), "")
                .attr("type", "text/javascript").attr(DEFER_ATTRIBUTE, defer);
        if (sourceUrl != null) {
            jsElement = jsElement.attr("src", sourceUrl);
        }
        return jsElement;
    }

    private static Element createJavaScriptElement(String sourceUrl) {
        return createJavaScriptElement(sourceUrl, true);
    }

    private static <T> T getUserDefinedProperty(DeploymentConfiguration config,
            String propertyName, Function<String, T> converter,
            T defaultValue) {

        // application or system properties have priority
        String value = config.getApplicationOrSystemProperty(propertyName,
                null);

        // null means that the property wasn't set
        if (value == null) {
            return defaultValue;
        }

        // converts the String to the desired type
        return converter.apply(value);
    }

    private static void includeDependenciesInPreRender(Element head,
            JsonObject initialUIDL, VaadinUriResolver resolver) {
        // Extract style sheets and load them eagerly
        JsonArray dependencies = initialUIDL
                .getArray(DependencyList.DEPENDENCY_KEY);
        if (dependencies == null || dependencies.length() == 0) {
            // No dependencies at all
            return;
        }

        JsonArray loadedAtClientDependencies = Json.createArray();
        int uidlDependenciesIndex = 0;
        for (int i = 0; i < dependencies.length(); i++) {
            JsonObject dependency = dependencies.getObject(i);
            LoadMode loadMode = LoadMode.valueOf(
                    dependency.getString(DependencyList.KEY_LOAD_MODE));
            String dependencyKey = dependency
                    .getString(DependencyList.KEY_TYPE);
            if (loadMode == LoadMode.EAGER) {
                switch (dependencyKey) {
                case DependencyList.TYPE_STYLESHEET:
                    addStyleSheet(head, resolver, dependency);
                    break;
                case DependencyList.TYPE_JAVASCRIPT:
                    addJavaScript(head, resolver, dependency);
                    break;
                case DependencyList.TYPE_HTML_IMPORT:
                    addHtmlImport(head, resolver, dependency);
                    break;
                default:
                    throw new IllegalStateException(
                            "Unsupported dependency type: " + dependencyKey);
                }
            } else {
                loadedAtClientDependencies.set(uidlDependenciesIndex,
                        dependency);
                uidlDependenciesIndex += 1;
            }
        }

        // Remove from initial UIDL
        initialUIDL.put(DependencyList.DEPENDENCY_KEY,
                loadedAtClientDependencies);
    }

    private static void addHtmlImport(Element head, VaadinUriResolver resolver,
            JsonObject dependency) {
        String url = dependency.getString(DependencyList.KEY_URL);

        head.appendElement("link").attr("rel", "import").attr("href",
                resolver.resolveVaadinUri(url));
    }

    private static void addStyleSheet(Element head, VaadinUriResolver resolver,
            JsonObject styleSheet) {
        Element link = head.appendElement("link").attr("rel", "stylesheet")
                .attr("type", "text/css");
        String url = styleSheet.getString(DependencyList.KEY_URL);
        link.attr("href", resolver.resolveVaadinUri(url));
    }

    private static void addJavaScript(Element head, VaadinUriResolver resolver,
            JsonObject javaScript) {
        String url = javaScript.getString(DependencyList.KEY_URL);
        head.appendChild(
                createJavaScriptElement(resolver.resolveVaadinUri(url)));
    }

    private static void setupDocumentBody(Document document,
            BootstrapContext context) {
        Element body;
        if (!context.getPreRenderMode().includePreRenderVersion()) {
            document.head().after("<body></body>");
            body = document.body();
        } else {
            Optional<Node> uiElement = ComponentUtil.prerender(context.getUI());

            if (uiElement.isPresent()) {
                Node prerenderedUIElement = uiElement.get();
                assert prerenderedUIElement instanceof Element;
                assert "body"
                        .equals(((Element) prerenderedUIElement).tagName());

                document.head().after(prerenderedUIElement);
                body = document.body();
                assert body == prerenderedUIElement;

                // Mark body and children so we know what to remove when
                // transitioning to the live version
                body.attr(ApplicationConstants.PRE_RENDER_ATTRIBUTE, true);
                body.children().forEach(element -> element
                        .attr(ApplicationConstants.PRE_RENDER_ATTRIBUTE, true));
            } else {
                document.head().after("<body></body>");
                body = document.body();
            }
        }

        if (context.getPreRenderMode() == PreRenderMode.PRE_ONLY
                && !context.isProductionMode()) {
            Element preOnlyInfo = body.appendElement("div");
            preOnlyInfo.addClass("v-system-error");
            preOnlyInfo.text(PRE_RENDER_INFO_TEXT);
            preOnlyInfo.attr("onclick", "this.remove()");
        }

        body.appendElement("noscript").append(
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
        String scriptData = "//<![CDATA[\n"
                + getBootstrapJS(initialUIDL, context) + "//]]>";
        Element mainScript = createJavaScriptElement(null);
        mainScript.appendChild(new DataNode(scriptData, mainScript.baseUri()));
        return mainScript;
    }

    private static String getBootstrapJS(JsonValue initialUIDL,
            BootstrapContext context) {
        boolean productionMode = context.getSession().getConfiguration()
                .isProductionMode();
        String result = getBootstrapJS();
        JsonObject appConfig = context.getApplicationParameters();

        int indent = 0;
        if (!productionMode) {
            indent = 4;
        }
        String appConfigString = JsonUtil.stringify(appConfig, indent);

        String initialUIDLString = JsonUtil.stringify(initialUIDL, indent);
        // Browser interpret </script> as end of script no matter if it is
        // inside a string or not so we must escape it
        initialUIDLString = SCRIPT_END_TAG_PATTERN.matcher(initialUIDLString)
                .replaceAll("<\\\\x2F$1");

        if (!productionMode) {
            // only used in debug mode by profiler
            result = result.replace("{{GWT_STAT_EVENTS}}", GWT_STAT_EVENTS_JS);
        } else {
            result = result.replace("{{GWT_STAT_EVENTS}}", "");
        }

        result = result.replace("{{APP_ID}}", context.getAppId());
        result = result.replace("{{INITIAL_UIDL}}", initialUIDLString);
        result = result.replace("{{CONFIG_JSON}}", appConfigString);
        return result;
    }

    protected static JsonObject getApplicationParameters(
            BootstrapContext context) {
        VaadinRequest request = context.getRequest();
        VaadinSession session = context.getSession();
        VaadinService vaadinService = request.getService();
        final boolean productionMode = session.getConfiguration()
                .isProductionMode();

        JsonObject appConfig = Json.createObject();

        if (productionMode) {
            appConfig.put(Constants.FRONTEND_URL_ES6, context.getSession()
                    .getConfiguration()
                    .getApplicationOrSystemProperty(Constants.FRONTEND_URL_ES6,
                            ApplicationConstants.FRONTEND_URL_ES6_DEFAULT_VALUE));
            appConfig.put(Constants.FRONTEND_URL_ES5, context.getSession()
                    .getConfiguration()
                    .getApplicationOrSystemProperty(Constants.FRONTEND_URL_ES5,
                            ApplicationConstants.FRONTEND_URL_ES5_DEFAULT_VALUE));
        } else {
            appConfig.put(Constants.FRONTEND_URL_ES6, context.getSession()
                    .getConfiguration()
                    .getApplicationOrSystemProperty(Constants.FRONTEND_URL_ES6,
                            ApplicationConstants.CONTEXT_PROTOCOL_PREFIX));
            appConfig.put(Constants.FRONTEND_URL_ES5, context.getSession()
                    .getConfiguration()
                    .getApplicationOrSystemProperty(Constants.FRONTEND_URL_ES5,
                            ApplicationConstants.CONTEXT_PROTOCOL_PREFIX));
        }

        appConfig.put(ApplicationConstants.UI_ID_PARAMETER,
                context.getUI().getUIId());

        JsonObject versionInfo = Json.createObject();
        versionInfo.put("vaadinVersion", Version.getFullVersion());
        String atmosphereVersion = AtmospherePushConnection
                .getAtmosphereVersion();
        if (atmosphereVersion != null) {
            versionInfo.put("atmosphereVersion", atmosphereVersion);
        }

        appConfig.put("versionInfo", versionInfo);

        // Use locale from session if set, else from the request
        Locale locale = ServletHelper.findLocale(context.getSession(),
                context.getRequest());
        // Get system messages
        SystemMessages systemMessages = vaadinService.getSystemMessages(locale,
                request);
        if (systemMessages != null) {
            // Write the CommunicationError -message to client
            JsonObject comErrMsg = Json.createObject();
            putValueOrNull(comErrMsg, "caption",
                    systemMessages.getCommunicationErrorCaption());
            putValueOrNull(comErrMsg, "message",
                    systemMessages.getCommunicationErrorMessage());
            putValueOrNull(comErrMsg, "url",
                    systemMessages.getCommunicationErrorURL());

            appConfig.put("comErrMsg", comErrMsg);

            JsonObject authErrMsg = Json.createObject();
            putValueOrNull(authErrMsg, "caption",
                    systemMessages.getAuthenticationErrorCaption());
            putValueOrNull(authErrMsg, "message",
                    systemMessages.getAuthenticationErrorMessage());
            putValueOrNull(authErrMsg, "url",
                    systemMessages.getAuthenticationErrorURL());

            appConfig.put("authErrMsg", authErrMsg);

            JsonObject sessExpMsg = Json.createObject();
            putValueOrNull(sessExpMsg, "caption",
                    systemMessages.getSessionExpiredCaption());
            putValueOrNull(sessExpMsg, "message",
                    systemMessages.getSessionExpiredMessage());
            putValueOrNull(sessExpMsg, "url",
                    systemMessages.getSessionExpiredURL());

            appConfig.put("sessExpMsg", sessExpMsg);
        }

        String contextRoot = ServletHelper.getContextRootRelativePath(request)
                + "/";
        appConfig.put(ApplicationConstants.CONTEXT_ROOT_URL, contextRoot);

        if (!productionMode) {
            appConfig.put("debug", true);
        }

        appConfig.put("heartbeatInterval", vaadinService
                .getDeploymentConfiguration().getHeartbeatInterval());

        boolean sendUrlsAsParameters = vaadinService
                .getDeploymentConfiguration().isSendUrlsAsParameters();
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
        // check for explicitly set page title, eg. by PageTitleGenerator or
        // View level title or page.setTitle
        String title = context.getUI().getInternals().getTitle();
        if (title != null) {
            // cancel the unnecessary execute javascript
            context.getUI().getInternals().cancelPendingTitleUpdate();
        }
        return Optional.ofNullable(title);
    }

    protected UI createAndInitUI(Class<? extends UI> uiClass,
            VaadinRequest request, VaadinSession session) {
        UI ui = ReflectTools.createInstance(uiClass);

        // Initialize some fields for a newly created UI
        ui.getInternals().setSession(session);

        PushMode pushMode = AnnotationReader.getPushMode(uiClass).orElseGet(
                session.getService().getDeploymentConfiguration()::getPushMode);
        ui.getPushConfiguration().setPushMode(pushMode);

        AnnotationReader.getPushTransport(uiClass)
                .ifPresent(ui.getPushConfiguration()::setTransport);

        // Set thread local here so it is available in init
        UI.setCurrent(ui);
        ui.doInit(request, session.getNextUIid());
        session.addUI(ui);
        return ui;
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
        if (getLogger().isLoggable(Level.FINE)) {
            getLogger().fine("Initial UIDL:" + json.asString());
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
        if (BOOTSTRAP_JS == null) {
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

        boolean resolveNow = !productionMode || clientEngineFile == null;
        if (resolveNow && BootstrapHandler.class.getResource(
                "/META-INF/resources/" + CLIENT_ENGINE_NOCACHE_FILE) != null) {
            return context.getUriResolver().resolveVaadinUri(
                    "context://" + CLIENT_ENGINE_NOCACHE_FILE);
        }

        if (clientEngineFile == null) {
            throw new BootstrapException(
                    "Client engine file name has not been resolved during initialization");
        }
        return context.getUriResolver()
                .resolveVaadinUri("context://" + clientEngineFile);
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

    /**
     * Returns the specified viewport content for the given UI class, specified
     * with {@link Viewport} or {@link ViewportGeneratorClass} annotations.
     *
     * @param uiClass
     *            the ui class whose viewport to get
     * @param request
     *            the request for the ui
     * @return the content value string for viewport meta tag
     */
    private static String getViewportContent(Class<? extends UI> uiClass,
            VaadinRequest request) {
        String viewportContent = null;
        Optional<Viewport> viewportAnnotation = AnnotationReader
                .getAnnotationFor(uiClass, Viewport.class);
        Optional<ViewportGeneratorClass> viewportGeneratorClassAnnotation = AnnotationReader
                .getAnnotationFor(uiClass, ViewportGeneratorClass.class);
        if (viewportAnnotation.isPresent()
                && viewportGeneratorClassAnnotation.isPresent()) {
            throw new IllegalStateException(uiClass.getCanonicalName()
                    + " cannot be annotated with both @"
                    + Viewport.class.getSimpleName() + " and @"
                    + ViewportGeneratorClass.class.getSimpleName());
        }

        if (viewportAnnotation.isPresent()) {
            viewportContent = viewportAnnotation.get().value();
        } else if (viewportGeneratorClassAnnotation.isPresent()) {
            Class<? extends ViewportGenerator> viewportGeneratorClass = viewportGeneratorClassAnnotation
                    .get().value();
            viewportContent = ReflectTools
                    .createInstance(viewportGeneratorClass)
                    .getViewport(request);
        }
        return viewportContent;
    }
}
