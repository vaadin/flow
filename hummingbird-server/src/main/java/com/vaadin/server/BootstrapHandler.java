/*
 * Copyright 2000-2014 Vaadin Ltd.
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;

import com.vaadin.annotations.Viewport;
import com.vaadin.annotations.ViewportGeneratorClass;
import com.vaadin.server.communication.AtmospherePushConnection;
import com.vaadin.server.communication.UidlWriter;
import com.vaadin.server.communication.UidlWriter.Dependency;
import com.vaadin.server.communication.UidlWriter.Dependency.Type;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.VaadinUriResolver;
import com.vaadin.shared.Version;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.shared.ui.ui.UIConstants;
import com.vaadin.ui.Component;
import com.vaadin.ui.PreRenderer;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

/**
 * Request handler which handles bootstrapping of the application, i.e. the
 * initial GET request
 *
 * @author Vaadin Ltd
 * @since 7.0.0
 */
public abstract class BootstrapHandler extends SynchronizedRequestHandler {

    private static final String PARAMETER_TEST_PRE_RENDERING = "pre-only";

    protected class BootstrapContext implements Serializable {

        private final VaadinResponse response;
        private final BootstrapFragmentResponse bootstrapResponse;

        private String appId;
        private PushMode pushMode;
        private JsonObject applicationParameters;
        private VaadinUriResolver uriResolver;

        public BootstrapContext(VaadinResponse response,
                BootstrapFragmentResponse bootstrapResponse) {
            this.response = response;
            this.bootstrapResponse = bootstrapResponse;
        }

        public VaadinResponse getResponse() {
            return response;
        }

        public VaadinRequest getRequest() {
            return bootstrapResponse.getRequest();
        }

        public VaadinSession getSession() {
            return bootstrapResponse.getSession();
        }

        public UI getUI() {
            return bootstrapResponse.getUI();
        }

        public Class<? extends UI> getUIClass() {
            return bootstrapResponse.getUI().getClass();
        }

        public PushMode getPushMode() {
            if (pushMode == null) {
                UICreateEvent event = new UICreateEvent(getRequest(),
                        getUI().getClass());

                pushMode = getBootstrapResponse().getUIProvider()
                        .getPushMode(event);
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
                        getRequest(), getUIClass());
            }
            return appId;
        }

        public BootstrapFragmentResponse getBootstrapResponse() {
            return bootstrapResponse;
        }

        public JsonObject getApplicationParameters() {
            if (applicationParameters == null) {
                applicationParameters = BootstrapHandler.this
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
    }

    private class BootstrapUriResolver extends VaadinUriResolver {
        private final BootstrapContext context;

        public BootstrapUriResolver(BootstrapContext bootstrapContext) {
            context = bootstrapContext;
        }

        @Override
        protected String getWebContextUrl() {
            // FIXME
            return context.getApplicationParameters()
                    .getString(ApplicationConstants.VAADIN_DIR_URL) + "../";
        }

        @Override
        protected String getServiceUrlParameterName() {
            return getConfigOrNull(
                    ApplicationConstants.SERVICE_URL_PARAMETER_NAME);
        }

        @Override
        protected String getServiceUrl() {
            String serviceUrl = getConfigOrNull(
                    ApplicationConstants.SERVICE_URL);
            if (serviceUrl == null) {
                return "./";
            } else if (!serviceUrl.endsWith("/")) {
                serviceUrl += "/";
            }
            return serviceUrl;
        }

        private String getConfigOrNull(String name) {
            JsonObject parameters = context.getApplicationParameters();
            if (parameters.hasKey(name)) {
                return parameters.getString(name);
            } else {
                return null;
            }
        }

        @Override
        protected String encodeQueryStringParameterValue(String queryString) {
            try {
                return URLEncoder.encode(queryString, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // Won't happen
                getLogger().log(Level.SEVERE,
                        "Error encoding query string parameters", e);
                return "";
            }
        }
    }

    private static String bootstrapJS;

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        // We do not want to handle /APP requests here, instead let it fall
        // through and produce a 404
        return !ServletPortletHelper.isAppRequest(request);
    }

    public Logger getLogger() {
        return Logger.getLogger(BootstrapHandler.class.getName());
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        try {
            List<UIProvider> uiProviders = session.getUIProviders();

            UIClassSelectionEvent classSelectionEvent = new UIClassSelectionEvent(
                    request);

            // Find UI provider and UI class
            Class<? extends UI> uiClass = null;
            UIProvider provider = null;
            for (UIProvider p : uiProviders) {
                uiClass = p.getUIClass(classSelectionEvent);
                // If we found something
                if (uiClass != null) {
                    provider = p;
                    break;
                }
            }

            if (provider == null) {
                // Can't generate bootstrap if no UI provider matches
                return false;
            }

            UI ui = createAndInitUI(provider, uiClass, request, session);

            BootstrapContext context = new BootstrapContext(response,
                    new BootstrapFragmentResponse(this, request, session, ui,
                            new ArrayList<Node>(), provider));

            // Required scripts, html files, stylesheets
            Set<Class<? extends ClientConnector>> dependencyClasses = new HashSet<>();
            addComponentClasses(ui, dependencyClasses);
            List<Dependency> deps = UidlWriter.collectDependencies(ui,
                    dependencyClasses);
            List<Dependency> builtInDeps = getBuiltInDeps(context);
            if (request.getParameter(PARAMETER_TEST_PRE_RENDERING) == null) {
                // Add deps unless we are testing only pre-rendering
                deps.addAll(0, builtInDeps);
            }

            setupMainDiv(context);

            BootstrapFragmentResponse fragmentResponse = context
                    .getBootstrapResponse();
            session.modifyBootstrapResponse(fragmentResponse);
            if (request.getParameter(PARAMETER_TEST_PRE_RENDERING) != null) {
                // Remove startup scripts if we are only testing pre-rendering
                fragmentResponse.getFragmentNodes().clear();
            }
            String html = getBootstrapHtml(context, deps);
            writeBootstrapPage(response, html);
        } catch (JsonException e) {
            writeError(response, e);
        }

        return true;
    }

    private List<Dependency> getBuiltInDeps(BootstrapContext context) {
        List<Dependency> deps = new ArrayList<>();
        VaadinRequest request = context.getRequest();

        deps.add(new Dependency(Type.SCRIPT,
                "context://bower_components/webcomponentsjs/webcomponents-lite.min.js"));

        VaadinService vaadinService = request.getService();
        String staticFileLocation = vaadinService
                .getStaticFileLocation(request);
        String vaadinLocation = staticFileLocation + "/VAADIN/";
        String vaadinServerLocation = vaadinLocation + "server/";

        deps.add(new Dependency(Type.SCRIPT,
                getClientEngineUrl(staticFileLocation)));

        // Parameter appended to JS to bypass caches after version upgrade.
        String versionQueryParam = "?v=" + Version.getFullVersion();

        // Push
        if (context.getPushMode().isEnabled()) {
            // Load client-side dependencies for push support
            String pushJS = vaadinLocation + "push/";
            if (context.getRequest().getService().getDeploymentConfiguration()
                    .isProductionMode()) {
                pushJS += ApplicationConstants.VAADIN_PUSH_JS;
            } else {
                pushJS += ApplicationConstants.VAADIN_PUSH_DEBUG_JS;
            }

            pushJS += versionQueryParam;
            deps.add(new Dependency(Type.SCRIPT, pushJS));
        }

        deps.add(new Dependency(Type.STYLESHEET,
                vaadinServerLocation + "server.css" + versionQueryParam));

        return deps;
    }

    private String getClientEngineUrl(String staticFileLocation) {

        String gwtModuleDir = "/VAADIN/client";
        String jsFile;

        try {
            InputStream prop = getClass()
                    .getResourceAsStream(gwtModuleDir + "/compile.properties");
            Properties p = new Properties();
            p.load(prop);
            jsFile = p.getProperty("jsFile");
        } catch (Exception e) {
            jsFile = "client.nocache.js";
            getLogger().severe(
                    "No compile.properties file found for ClientEngine");
        }

        return staticFileLocation + gwtModuleDir + "/" + jsFile;

    }

    private String getBootstrapHtml(BootstrapContext context,
            List<Dependency> deps) {
        VaadinRequest request = context.getRequest();
        VaadinResponse response = context.getResponse();
        VaadinService vaadinService = request.getService();

        BootstrapFragmentResponse fragmentResponse = context
                .getBootstrapResponse();

        if (vaadinService.isStandalone(request)) {
            Map<String, Object> headers = new LinkedHashMap<String, Object>();
            Document document = Document.createShell("");
            BootstrapPageResponse pageResponse = new BootstrapPageResponse(this,
                    request, context.getSession(), context.getUI(), document,
                    headers, fragmentResponse.getUIProvider());
            List<Node> fragmentNodes = fragmentResponse.getFragmentNodes();

            preRender(document, context);

            Element vaadinInternals = new Element(
                    Tag.valueOf("vaadin-internals"), "");
            document.body().appendChild(vaadinInternals);

            for (Node node : fragmentNodes) {
                vaadinInternals.appendChild(node);
            }

            setupStandaloneDocument(context, pageResponse, deps);
            context.getSession().modifyBootstrapResponse(pageResponse);

            sendBootstrapHeaders(response, headers);

            document.outputSettings().prettyPrint(false);
            return document.outerHtml();
        } else {
            StringBuilder sb = new StringBuilder();
            for (Node node : fragmentResponse.getFragmentNodes()) {
                if (sb.length() != 0) {
                    sb.append('\n');
                }
                sb.append(node.outerHtml());
            }

            return sb.toString();
        }
    }

    private void preRender(Document document, BootstrapContext context) {
        com.vaadin.hummingbird.kernel.Element preRenderedUI = context.getUI()
                .preRender();
        preRenderedUI.setAttribute("pre-render", true);

        // UI maps to the body element, copy attributes
        for (String attrKey : preRenderedUI.getAttributeNames()) {
            document.body().attr(attrKey, preRenderedUI.getAttribute(attrKey));
        }

        // Render UI children inside body
        for (int i = 0; i < preRenderedUI.getChildCount(); i++) {
            Node jsoupNode = PreRenderer.toJSoup(document,
                    preRenderedUI.getChild(i));
            // Add a pre-render attribute so we easily detect pre-rendered
            // elements when starting the client side engine
            jsoupNode.attr("pre-render", "");
            document.body().appendChild(jsoupNode);
        }
    }

    private void sendBootstrapHeaders(VaadinResponse response,
            Map<String, Object> headers) {
        Set<Entry<String, Object>> entrySet = headers.entrySet();
        for (Entry<String, Object> header : entrySet) {
            Object value = header.getValue();
            if (value instanceof String) {
                response.setHeader(header.getKey(), (String) value);
            } else if (value instanceof Long) {
                response.setDateHeader(header.getKey(),
                        ((Long) value).longValue());
            } else {
                throw new RuntimeException(
                        "Unsupported header value: " + value);
            }
        }
    }

    private void writeBootstrapPage(VaadinResponse response, String html)
            throws IOException {
        response.setContentType("text/html");
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
        writer.append(html);
        writer.close();
    }

    private void setupStandaloneDocument(BootstrapContext context,
            BootstrapPageResponse response, List<Dependency> deps) {
        setNoCacheHeaders(response);

        Document document = response.getDocument();

        DocumentType doctype = new DocumentType("html", "", "",
                document.baseUri());
        document.child(0).before(doctype);

        Element head = document.head();
        head.appendElement("meta").attr("http-equiv", "Content-Type")
                .attr("content", "text/html; charset=utf-8");

        Class<? extends UI> uiClass = context.getUIClass();

        String viewportContent = null;
        Viewport viewportAnnotation = uiClass.getAnnotation(Viewport.class);
        ViewportGeneratorClass viewportGeneratorClassAnnotation = uiClass
                .getAnnotation(ViewportGeneratorClass.class);
        if (viewportAnnotation != null
                && viewportGeneratorClassAnnotation != null) {
            throw new IllegalStateException(uiClass.getCanonicalName()
                    + " cannot be annotated with both @"
                    + Viewport.class.getSimpleName() + " and @"
                    + ViewportGeneratorClass.class.getSimpleName());
        }

        if (viewportAnnotation != null) {
            viewportContent = viewportAnnotation.value();
        } else if (viewportGeneratorClassAnnotation != null) {
            Class<? extends ViewportGenerator> viewportGeneratorClass = viewportGeneratorClassAnnotation
                    .value();
            try {
                viewportContent = viewportGeneratorClass.newInstance()
                        .getViewport(context.getRequest());
            } catch (Exception e) {
                throw new RuntimeException(
                        "Error processing viewport generator "
                                + viewportGeneratorClass.getCanonicalName(),
                        e);
            }
        }

        if (viewportContent != null) {
            head.appendElement("meta").attr("name", "viewport").attr("content",
                    viewportContent);
        }

        String title = response.getUIProvider().getPageTitle(
                new UICreateEvent(context.getRequest(), context.getUIClass()));
        if (title != null) {
            head.appendElement("title").appendText(title);
        }

        head.appendElement("style").attr("type", "text/css")
                .appendText("html, body {height:100%;margin:0;}");

        writeUsedScriptsImportsStylesheets(context.getUriResolver(), deps,
                head);

        Element body = document.body();
        body.attr("scroll", "auto");
        body.addClass(ApplicationConstants.GENERATED_BODY_CLASSNAME);
    }

    private void writeUsedScriptsImportsStylesheets(VaadinUriResolver resolver,
            List<Dependency> deps, Element head) {
        for (Dependency d : deps) {
            String resolvedUrl = resolver.resolveVaadinUri(d.getUrl());
            if (d.getType() == Type.HTML) {
                head.appendElement("link").attr("rel", "import")
                        .attr("href", resolvedUrl).attr("pending", "1")
                        .attr("onload",
                                "this.removeAttribute('pending');this.removeAttribute('onload');");
            } else if (d.getType() == Type.SCRIPT) {
                head.appendElement("script").attr("type", "text/javascript")
                        .attr("src", resolvedUrl).attr("pending", "1")
                        .attr("onload",
                                "this.removeAttribute('pending');this.removeAttribute('onload');")
                        .attr("defer", "true");
            } else if (d.getType() == Type.STYLESHEET) {
                head.appendElement("link").attr("rel", "stylesheet")
                        .attr("type", "text/css").attr("href", resolvedUrl)
                        .attr("pending", "1").attr("onload",
                                "this.removeAttribute('pending');this.removeAttribute('onload');");
            } else if (d.getType() == Type.POLYMER_STYLE) {
                head.appendElement("style").attr("is", "custom-style")
                        .attr("include", d.getUrl());
            } else {
                throw new IllegalStateException("Unknown type " + d.getType());
            }
        }
    }

    private void addComponentClasses(Component c,
            Set<Class<? extends ClientConnector>> dependencyClasses) {
        dependencyClasses.add(c.getClass());
        for (Component child : c.getChildComponents()) {
            addComponentClasses(child, dependencyClasses);
        }

    }

    private void setNoCacheHeaders(BootstrapPageResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    /**
     * @since
     * @param uiClass
     * @return
     */
    private List<Class<? extends Component>> getComponentAndParents(
            Class<?> cls) {
        List<Class<? extends Component>> result = new ArrayList<>();
        while (Component.class.isAssignableFrom(cls)) {
            result.add((Class<? extends Component>) cls);
            cls = cls.getSuperclass();
        }
        return result;
    }

    /**
     * Method to write the div element into which that actual Vaadin application
     * is rendered.
     * <p>
     * Override this method if you want to add some custom html around around
     * the div element into which the actual Vaadin application will be
     * rendered.
     *
     * @param context
     *
     * @throws IOException
     */
    private void setupMainDiv(BootstrapContext context) throws IOException {
        /*- Add classnames;
         *      .v-app
         *      .v-app-loading
         */

        List<Node> fragmentNodes = context.getBootstrapResponse()
                .getFragmentNodes();

        Element mainScriptTag = new Element(Tag.valueOf("script"), "")
                .attr("type", "text/javascript");

        StringBuilder builder = new StringBuilder();
        builder.append("//<![CDATA[\n");
        builder.append(getBootstrapJS(context));

        builder.append("//]]>");
        mainScriptTag.appendChild(
                new DataNode(builder.toString(), mainScriptTag.baseUri()));
        fragmentNodes.add(mainScriptTag);

    }

    private String getBootstrapJS(BootstrapContext context) {
        if (bootstrapJS == null) {
            try (InputStream stream = BootstrapHandler.class
                    .getResourceAsStream("BootstrapHandler.js")) {
                if (stream == null) {
                    throw new RuntimeException(
                            "Unable to load BootstrapHandler.js");
                }

                bootstrapJS = IOUtils.toString(stream);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to load BootstrapHandler.js");
            }
        }
        String result = bootstrapJS.replace("{{appId}}", context.getAppId());

        result = result.replace("{{promisePolyfill}}",
                context.getUriResolver().resolveVaadinUri(
                        "context://bower_components/promise-polyfill/Promise.min.js"));

        result = result.replace("{{collectionsPolyfill}}",
                context.getUriResolver().resolveVaadinUri(
                        "context://bower_components/es6-collections/es6-collections.js"));

        JsonObject appConfig = context.getApplicationParameters();
        boolean isDebug = !context.getSession().getConfiguration()
                .isProductionMode();
        boolean preTiming = context.getRequest()
                .getParameter("pre-timing") != null;

        int indent = 0;
        if (isDebug) {
            indent = 4;
        }
        String initialUIDL = "";
        try {
            initialUIDL = getInitialUidl(context.getRequest(), context.getUI());
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Unable to create initial UIDL", e);
        }
        String appConfigString = JsonUtil.stringify(appConfig, indent);
        if (isDebug) {
            JsonObject uidl = Json.parse(initialUIDL);
            initialUIDL = JsonUtil.stringify(uidl, indent);
            initialUIDL = initialUIDL.replace("\n", "\n       ");
            appConfigString = appConfigString.replace("\n", "\n       ");
        }
        result = result.replace("{{initialUIDL}}", initialUIDL);
        result = result.replace("{{configJSON}}", appConfigString);
        result = result.replace("{{preTiming}}", Boolean.toString(preTiming));
        return result;
    }

    protected JsonObject getApplicationParameters(BootstrapContext context) {
        VaadinRequest request = context.getRequest();
        VaadinSession session = context.getSession();
        VaadinService vaadinService = request.getService();

        JsonObject appConfig = Json.createObject();

        appConfig.put(UIConstants.UI_ID_PARAMETER, context.getUI().getUIId());

        appConfig.put("client-engine", Constants.CLIENT_ENGINE_MODULE);

        JsonObject versionInfo = Json.createObject();
        versionInfo.put("vaadinVersion", Version.getFullVersion());
        String atmosphereVersion = AtmospherePushConnection
                .getAtmosphereVersion();
        if (atmosphereVersion != null) {
            versionInfo.put("atmosphereVersion", atmosphereVersion);
        }

        appConfig.put("versionInfo", versionInfo);

        // Use locale from session if set, else from the request
        Locale locale = ServletPortletHelper.findLocale(null,
                context.getSession(), context.getRequest());
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

        // getStaticFileLocation documented to never end with a slash
        // vaadinDir should always end with a slash
        String vaadinDir = vaadinService.getStaticFileLocation(request)
                + "/VAADIN/";
        appConfig.put(ApplicationConstants.VAADIN_DIR_URL, vaadinDir);

        if (!session.getConfiguration().isProductionMode()) {
            appConfig.put("debug", true);
        }

        if (vaadinService.isStandalone(request)) {
            appConfig.put("standalone", true);
        }

        appConfig.put("heartbeatInterval", vaadinService
                .getDeploymentConfiguration().getHeartbeatInterval());

        String serviceUrl = getServiceUrl(context);
        if (serviceUrl != null) {
            appConfig.put(ApplicationConstants.SERVICE_URL, serviceUrl);
        }

        boolean sendUrlsAsParameters = vaadinService
                .getDeploymentConfiguration().isSendUrlsAsParameters();
        if (!sendUrlsAsParameters) {
            appConfig.put("sendUrlsAsParameters", false);
        }

        return appConfig;
    }

    protected abstract String getServiceUrl(BootstrapContext context);

    protected void writeError(VaadinResponse response, Throwable e)
            throws IOException {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                e.getLocalizedMessage());
    }

    private void putValueOrNull(JsonObject object, String key, String value) {
        assert object != null;
        assert key != null;
        if (value == null) {
            object.put(key, Json.createNull());
        } else {
            object.put(key, value);
        }
    }

    protected UI createAndInitUI(UIProvider provider,
            Class<? extends UI> uiClass, VaadinRequest request,
            VaadinSession session) {
        // Check for an existing UI based on embed id
        String embedId = getEmbedId(request);
        Integer uiId = Integer.valueOf(session.getNextUIid());

        // Explicit Class.cast to detect if the UIProvider does something
        // unexpected
        UICreateEvent event = new UICreateEvent(request, uiClass, uiId);
        UI ui = uiClass.cast(provider.createInstance(event));

        // Initialize some fields for a newly created UI
        if (ui.getSession() != session) {
            // Session already set
            ui.setSession(session);
        }

        PushMode pushMode = provider.getPushMode(event);
        if (pushMode == null) {
            pushMode = session.getService().getDeploymentConfiguration()
                    .getPushMode();
        }
        ui.getPushConfiguration().setPushMode(pushMode);

        Transport transport = provider.getPushTransport(event);
        if (transport != null) {
            ui.getPushConfiguration().setTransport(transport);
        }

        // Set thread local here so it is available in init
        UI.setCurrent(ui);

        ui.doInit(request, uiId.intValue(), embedId);

        session.addUI(ui);

        return ui;
    }

    /**
     * Generates the initial UIDL message that can e.g. be included in a html
     * page to avoid a separate round trip just for getting the UIDL.
     *
     * @param request
     *            the request that caused the initialization
     * @param uI
     *            the UI for which the UIDL should be generated
     * @return a string with the initial UIDL message
     * @throws IOException
     */
    protected String getInitialUidl(VaadinRequest request, UI uI)
            throws IOException {
        StringWriter writer = new StringWriter();
        try {
            writer.write("{");

            VaadinSession session = uI.getSession();
            if (session.getConfiguration().isXsrfProtectionEnabled()) {
                writer.write(getSecurityKeyUIDL(session));
            }
            new UidlWriter().write(uI, writer, false);
            writer.write("}");

            String initialUIDL = writer.toString();
            getLogger().log(Level.FINE, "Initial UIDL:" + initialUIDL);
            return initialUIDL;
        } finally {
            writer.close();
        }
    }

    /**
     * Gets the security key (and generates one if needed) as UIDL.
     *
     * @param session
     *            the vaadin session to which the security key belongs
     * @return the security key UIDL or "" if the feature is turned off
     */
    private static String getSecurityKeyUIDL(VaadinSession session) {
        String seckey = session.getCsrfToken();

        return "\"" + ApplicationConstants.UIDL_SECURITY_TOKEN_ID + "\":\""
                + seckey + "\",";
    }

    /**
     * Constructs an embed id based on information in the request.
     *
     * @since 7.2
     *
     * @param request
     *            the request to get embed information from
     * @return the embed id, or <code>null</code> if id is not available.
     *
     * @see UI#getEmbedId()
     */
    protected String getEmbedId(VaadinRequest request) {
        // Parameters sent by bootstrap.js
        String windowName = request.getParameter("v-wn");
        String appId = request.getParameter("v-appId");

        if (windowName != null && appId != null) {
            return windowName + '.' + appId;
        } else {
            return null;
        }
    }

}
