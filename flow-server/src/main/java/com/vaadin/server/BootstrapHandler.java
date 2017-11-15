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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.server.communication.AtmospherePushConnection;
import com.vaadin.server.communication.UidlWriter;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.VaadinUriResolver;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.ui.UI;
import com.vaadin.ui.Viewport;
import com.vaadin.ui.ViewportGeneratorClass;
import com.vaadin.ui.WebComponents;
import com.vaadin.util.AnnotationReader;
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
    private static final String BOOTSTRAP_JS = readResource(
            "BootstrapHandler.js");
    private static final String BABEL_HELPERS_JS = readResource(
            "babel-helpers.min.js");
    private static final String ES6_COLLECTIONS = "//<![CDATA[\n"
            + readResource("es6-collections.js") + "//]]>";
    private static final String CSS_TYPE_ATTRIBUTE_VALUE = "text/css";

    static String clientEngineFile = readClientEngine();

    private static Logger getLogger() {
        return Logger.getLogger(BootstrapHandler.class.getName());
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
                uriResolver = new BootstrapUriResolver(getRequest(),
                        getSession());
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

    }

    private static class BootstrapUriResolver extends VaadinUriResolver {
        private final VaadinSession session;
        private final VaadinRequest request;
        private final String es6FrontendPrefix;
        private final String es5FrontendPrefix;

        protected BootstrapUriResolver(VaadinRequest request,
                VaadinSession session) {
            this.session = session;
            this.request = request;

            DeploymentConfiguration config = session.getConfiguration();
            es6FrontendPrefix = config.getEs6FrontendPrefix();
            es5FrontendPrefix = config.getEs5FrontendPrefix();
        }

        @Override
        protected String getContextRootUrl() {
            String root = getApplicationParameters(request, session)
                    .getString(ApplicationConstants.CONTEXT_ROOT_URL);
            assert root.endsWith("/");
            return root;
        }

        @Override
        protected String getFrontendRootUrl() {
            String root;
            if (session.getBrowser().isEs6Supported()) {
                root = es6FrontendPrefix;
            } else {
                root = es5FrontendPrefix;
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

        if (session.getAttribute(VaadinUriResolverFactory.class) == null) {
            session.setAttribute(VaadinUriResolverFactory.class,
                    vaadinRequest -> new BootstrapUriResolver(vaadinRequest,
                            session));
        }

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
        html.appendElement("body");

        List<Element> dependenciesToInlineInBody = setupDocumentHead(head,
                context);
        dependenciesToInlineInBody
                .forEach(dependency -> document.body().appendChild(dependency));
        setupDocumentBody(document);

        document.outputSettings().prettyPrint(false);

        BootstrapPageResponse response = new BootstrapPageResponse(
                context.getRequest(), context.getSession(),
                context.getResponse(), document, context.getUI(),
                context.getUriResolver());
        context.getSession().getService().modifyBootstrapPage(response);

        return document;
    }

    private static void writeBootstrapPage(VaadinResponse response, String html)
            throws IOException {
        response.setContentType("text/html");
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), "UTF-8"))) {
            writer.append(html);
        }
    }

    private static List<Element> setupDocumentHead(Element head,
            BootstrapContext context) {
        setupMetaAndTitle(head, context);
        setupCss(head);

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
            VaadinUriResolver uriResolver, LoadMode loadMode,
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

    private static void setupCss(Element head) {
        Element styles = head.appendElement("style").attr("type",
                CSS_TYPE_ATTRIBUTE_VALUE);
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
    }

    private static void setupMetaAndTitle(Element head,
            BootstrapContext context) {
        head.appendElement(META_TAG).attr("http-equiv", "Content-Type")
                .attr(CONTENT_ATTRIBUTE, "text/html; charset=utf-8");

        head.appendElement(META_TAG).attr("http-equiv", "X-UA-Compatible")
                .attr(CONTENT_ATTRIBUTE, "IE=edge");

        head.appendElement("base").attr("href", getServiceUrl(context));

        getViewportContent(context.getUI().getClass(), context.getRequest())
                .ifPresent(content -> head.appendElement(META_TAG)
                        .attr("name", "viewport")
                        .attr(CONTENT_ATTRIBUTE, content));

        resolvePageTitle(context).ifPresent(title -> {
            if (!title.isEmpty()) {
                head.appendElement("title").appendText(title);
            }
        });
    }

    private static void appendWebComponentsPolyfills(Element head,
            BootstrapContext context) {
        Optional<WebComponents> webComponents = AnnotationReader
                .getAnnotationFor(context.getUI().getClass(),
                        WebComponents.class);

        DeploymentConfiguration config = context.getSession()
                .getConfiguration();

        String webComponentsPolyfillBase = config.getWebComponentsPolyfillBase()
                .orElse(null);
        if (webComponentsPolyfillBase == null) {
            return;
        }
        assert webComponentsPolyfillBase.endsWith("/");

        boolean loadEs5Adapter = config.getBooleanProperty(
                Constants.LOAD_ES5_ADAPTERS,
                webComponents.map(WebComponents::loadEs5Adapter).orElse(true));
        if (loadEs5Adapter
                && !context.getSession().getBrowser().isEs6Supported()) {
            // This adapter is required since lots of our current customers use polymer-cli to transpile sources,
            // this tool adds babel-helpers dependency into each file, see:
            // https://github.com/Polymer/polymer-cli/blob/master/src/build/build.ts#L64
            // and
            // https://github.com/Polymer/polymer-cli/blob/master/src/build/optimize-streams.ts#L119
            head.appendChild(createInlineJavaScriptElement(BABEL_HELPERS_JS));
            // This adapter is required by all webcomponents in order to work correctly in ES5 mode, comes in webcomponentsjs distribution
            head.appendChild(createJavaScriptElement(
                    context.getUriResolver()
                            .resolveVaadinUri(webComponentsPolyfillBase
                                    + "custom-elements-es5-adapter.js"),
                    false));
        }

        boolean forceShadyDom = config.getBooleanProperty(
                Constants.FORCE_SHADY_DOM, webComponents.isPresent()
                        && webComponents.get().forceShadyDom());
        head.appendChild(createJavaScriptElement(
                context.getUriResolver().resolveVaadinUri(
                        webComponentsPolyfillBase + "webcomponents-loader.js"),
                false).attr("shadydom", forceShadyDom));
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

    private static Element createDependencyElement(VaadinUriResolver resolver,
            LoadMode loadMode, JsonObject dependency, Dependency.Type type) {
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
        VaadinService vaadinService = session.getService();
        final boolean productionMode = session.getConfiguration()
                .isProductionMode();

        JsonObject appConfig = Json.createObject();

        appConfig.put(ApplicationConstants.FRONTEND_URL_ES6,
                session.getConfiguration().getEs6FrontendPrefix());
        appConfig.put(ApplicationConstants.FRONTEND_URL_ES5,
                session.getConfiguration().getEs5FrontendPrefix());

        JsonObject versionInfo = Json.createObject();
        versionInfo.put("vaadinVersion", Version.getFullVersion());
        String atmosphereVersion = AtmospherePushConnection
                .getAtmosphereVersion();
        if (atmosphereVersion != null) {
            versionInfo.put("atmosphereVersion", atmosphereVersion);
        }

        appConfig.put("versionInfo", versionInfo);

        // Use locale from session if set, else from the request
        Locale locale = ServletHelper.findLocale(session, request);
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
        ui.setLocale(session.getLocale());

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
    private static Optional<String> getViewportContent(
            Class<? extends UI> uiClass, VaadinRequest request) {
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

        String viewportContent = null;
        if (viewportAnnotation.isPresent()) {
            viewportContent = viewportAnnotation.get().value();
        } else if (viewportGeneratorClassAnnotation.isPresent()) {
            Class<? extends ViewportGenerator> viewportGeneratorClass = viewportGeneratorClassAnnotation
                    .get().value();
            viewportContent = ReflectTools
                    .createInstance(viewportGeneratorClass)
                    .getViewport(request);
        }
        return Optional.ofNullable(viewportContent);
    }

    private static String readResource(String fileName) {
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

    private static String readClientEngine() {
        // read client engine file name
        try (InputStream prop = BootstrapHandler.class.getResourceAsStream(
                "/META-INF/resources/" + ApplicationConstants.CLIENT_ENGINE_PATH
                        + "/compile.properties")) {
            // null when running SDM or tests
            if (prop != null) {
                Properties properties = new Properties();
                properties.load(prop);
                return ApplicationConstants.CLIENT_ENGINE_PATH + "/"
                        + properties.getProperty("jsFile");
            } else {
                getLogger().warning(
                        "No compile.properties available on initialization, "
                                + "could not read client engine file name.");
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        return null;
    }
}
