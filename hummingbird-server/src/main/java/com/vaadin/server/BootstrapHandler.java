/*
 * Copyright 2000-2016 Vaadin Ltd.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import com.google.gwt.thirdparty.guava.common.net.UrlEscapers;
import com.vaadin.annotations.AnnotationReader;
import com.vaadin.annotations.Viewport;
import com.vaadin.annotations.ViewportGeneratorClass;
import com.vaadin.server.communication.AtmospherePushConnection;
import com.vaadin.server.communication.UidlWriter;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.VaadinUriResolver;
import com.vaadin.shared.Version;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.UI;

import elemental.json.Json;
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
public abstract class BootstrapHandler extends SynchronizedRequestHandler {

    private static final CharSequence GWT_STAT_EVENTS_JS = "if (typeof window.__gwtStatsEvent != 'function') {"
            + "vaadin.gwtStatsEvents = [];"
            + "window.__gwtStatsEvent = function(event) {"
            + "vaadin.gwtStatsEvents.push(event); " + "return true;};};";

    private static final String TYPE_TEXT_JAVASCRIPT = "text/javascript";
    private static final String CONTENT_ATTRIBUTE = "content";
    private static final String META_TAG = "meta";

    private static String bootstrapJS;

    private static Logger getLogger() {
        return Logger.getLogger(BootstrapHandler.class.getName());
    }

    static {
        try (InputStream stream = BootstrapHandler.class
                .getResourceAsStream("BootstrapHandler.js");
                BufferedReader bf = new BufferedReader(new InputStreamReader(
                        stream, StandardCharsets.UTF_8));) {
            StringBuilder sb = new StringBuilder();
            bf.lines().forEach(sb::append);
            bootstrapJS = sb.toString();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected class BootstrapContext {

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

    private static class BootstrapUriResolver extends VaadinUriResolver {
        private final BootstrapContext context;

        protected BootstrapUriResolver(BootstrapContext bootstrapContext) {
            context = bootstrapContext;
        }

        @Override
        protected String getVaadinDirUrl() {
            return context.getApplicationParameters()
                    .getString(ApplicationConstants.VAADIN_DIR_URL);
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
            return UrlEscapers.urlFormParameterEscaper().escape(queryString);
        }
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        // We do not want to handle /APP requests here, instead let it fall
        // through and produce a 404
        return !ServletHelper.isAppRequest(request);
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        // Find UI class
        Class<? extends UI> uiClass = getUIClass(request);

        UI ui = createAndInitUI(uiClass, request, session);

        BootstrapContext context = new BootstrapContext(request, response,
                session, ui);

        String html = getBootstrapPageHtml(context);

        ServletHelper.setResponseNoCacheHeaders(response::setHeader,
                response::setDateHeader);

        writeBootstrapPage(response, html);

        return true;
    }

    private static String getBootstrapPageHtml(BootstrapContext context)
            throws IOException {
        VaadinRequest request = context.getRequest();

        Document document = Document.createShell("");
        BootstrapPageResponse pageResponse = new BootstrapPageResponse(request,
                context.getSession(), context.getUI(), document);

        setupDocumentHead(context, pageResponse);
        setupDocumentBody(pageResponse);

        return document.outerHtml();
    }

    private static void writeBootstrapPage(VaadinResponse response, String html)
            throws IOException {
        response.setContentType("text/html");
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
        writer.append(html);
        writer.close();
    }

    private static void setupDocumentHead(BootstrapContext context,
            BootstrapPageResponse response) {
        Document document = response.getDocument();

        DocumentType doctype = new DocumentType("html", "", "",
                document.baseUri());
        document.child(0).before(doctype);

        Element head = document.head();
        head.appendElement(META_TAG).attr("http-equiv", "Content-Type")
                .attr(CONTENT_ATTRIBUTE, "text/html; charset=utf-8");

        /*
         * Enable Chrome Frame in all versions of IE if installed.
         */
        head.appendElement(META_TAG).attr("http-equiv", "X-UA-Compatible")
                .attr(CONTENT_ATTRIBUTE, "IE=11;chrome=1");

        Class<? extends UI> uiClass = context.getUI().getClass();

        String viewportContent = getViewportContent(uiClass,
                context.getRequest());
        if (viewportContent != null) {
            head.appendElement(META_TAG).attr("name", "viewport")
                    .attr(CONTENT_ATTRIBUTE, viewportContent);
        }
        String title = AnnotationReader.getPageTitle(uiClass);
        if (title != null) {
            head.appendElement("title").appendText(title);
        }
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
                + "background: white;" //
                + "position: absolute;" //
                + "top: 1em;" //
                + "right: 1em;" //
                + "border: 1px solid black;" //
                + "padding: 1em;" //
                + "}");

        if (context.getSession().getBrowser().isPhantomJS()) {
            // Collections polyfill needed only for PhantomJS

            head.appendElement("script").attr("type", "text/javascript")
                    .attr("src", context.getUriResolver()
                            .resolveVaadinUri("vaadin://es6-collections.js"));
        }

        if (context.getPushMode().isEnabled()) {
            head.appendChild(getPushScript(context));
        }

        head.appendChild(getBootstrapScript(context));
    }

    private static void setupDocumentBody(BootstrapPageResponse response) {
        Element body = response.getDocument().body();
        body.attr("scroll", "auto");
        body.addClass(ApplicationConstants.GENERATED_BODY_CLASSNAME);

        body.appendElement("noscript").append(
                "You have to enable javascript in your browser to use this web site.");
    }

    private static Element getPushScript(BootstrapContext context) {
        VaadinRequest request = context.getRequest();

        VaadinService vaadinService = request.getService();
        String vaadinLocation = vaadinService.getStaticFileLocation(request)
                + "/VAADIN/";

        // Parameter appended to JS to bypass caches after version upgrade.
        String versionQueryParam = "?v=" + Version.getFullVersion();

        // Load client-side dependencies for push support
        String pushJS = vaadinLocation + "push/";
        if (context.getRequest().getService().getDeploymentConfiguration()
                .isProductionMode()) {
            pushJS += ApplicationConstants.VAADIN_PUSH_JS;
        } else {
            pushJS += ApplicationConstants.VAADIN_PUSH_DEBUG_JS;
        }

        pushJS += versionQueryParam;

        return new Element(Tag.valueOf("script"), "")
                .attr("type", TYPE_TEXT_JAVASCRIPT).attr("src", pushJS);
    }

    private static Element getBootstrapScript(BootstrapContext context) {
        Element mainScript = new Element(Tag.valueOf("script"), "").attr("type",
                TYPE_TEXT_JAVASCRIPT);

        StringBuilder builder = new StringBuilder();
        builder.append("//<![CDATA[\n");
        builder.append(getBootstrapJS(context));

        builder.append("//]]>");
        mainScript.appendChild(
                new DataNode(builder.toString(), mainScript.baseUri()));
        return mainScript;
    }

    private static String getBootstrapJS(BootstrapContext context) {
        boolean isDebug = !context.getSession().getConfiguration()
                .isProductionMode();
        String result = getBootstrapJS();
        JsonObject appConfig = context.getApplicationParameters();

        int indent = 0;
        if (isDebug) {
            indent = 4;
        }
        JsonValue initialUIDL = getInitialUidl(context.getUI());

        String appConfigString = JsonUtil.stringify(appConfig, indent);
        String initialUIDLString = JsonUtil.stringify(initialUIDL, indent);
        if (isDebug) {
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

    protected JsonObject getApplicationParameters(BootstrapContext context) {
        VaadinRequest request = context.getRequest();
        VaadinSession session = context.getSession();
        VaadinService vaadinService = request.getService();

        JsonObject appConfig = Json.createObject();
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

        // getStaticFileLocation documented to never end with a slash
        // vaadinDir should always end with a slash
        String vaadinDir = vaadinService.getStaticFileLocation(request)
                + "/VAADIN/";
        appConfig.put(ApplicationConstants.VAADIN_DIR_URL, vaadinDir);

        if (!session.getConfiguration().isProductionMode()) {
            appConfig.put("debug", true);
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

    protected UI createAndInitUI(Class<? extends UI> uiClass,
            VaadinRequest request, VaadinSession session) {
        Integer uiId = Integer.valueOf(session.getNextUIid());

        UI ui = createInstance(uiClass);

        // Initialize some fields for a newly created UI
        ui.setSession(session);

        PushMode pushMode = AnnotationReader.getPushMode(uiClass);
        if (pushMode == null) {
            pushMode = session.getService().getDeploymentConfiguration()
                    .getPushMode();
        }
        ui.getPushConfiguration().setPushMode(pushMode);

        Transport transport = AnnotationReader.getPushTransport(uiClass);
        if (transport != null) {
            ui.getPushConfiguration().setTransport(transport);
        }

        // Set thread local here so it is available in init
        UI.setCurrent(ui);

        ui.doInit(request, uiId.intValue(), null);

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
        if (bootstrapJS == null) {
            throw new BootstrapException(
                    "BootstrapHandler.js has not been loaded during initilization");
        }
        return bootstrapJS;
    }

    private static UI createInstance(Class<? extends UI> uiClass) {
        try {
            return uiClass.newInstance();
        } catch (InstantiationException e) {
            throw new BootstrapException(
                    "Could not create an instance of the UI class "
                            + uiClass.getName(),
                    e);
        } catch (IllegalAccessException e) {
            throw new BootstrapException(
                    "No public no-args constructor available for the UI "
                            + uiClass.getName(),
                    e);
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
            try {
                viewportContent = viewportGeneratorClass.newInstance()
                        .getViewport(request);
            } catch (InstantiationException e) {
                throw new BootstrapException(
                        "Bootstrap failed: Could not create an instance of viewport generator class "
                                + viewportGeneratorClass.getName() + " for UI "
                                + uiClass.getName(),
                        e);
            } catch (IllegalAccessException e) {
                throw new BootstrapException(
                        "Bootstrap failed: No public no-args constructor available for viewport generator class "
                                + viewportGeneratorClass.getName()
                                + " was available for UI " + uiClass.getName(),
                        e);
            }
        }
        return viewportContent;
    }
}
