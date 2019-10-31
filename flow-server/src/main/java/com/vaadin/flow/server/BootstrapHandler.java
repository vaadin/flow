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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
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
import java.util.function.Function;
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
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.internal.UsageStatistics.UsageEntry;
import com.vaadin.flow.server.BootstrapUtils.ThemeSettings;
import com.vaadin.flow.server.communication.AtmospherePushConnection;
import com.vaadin.flow.server.communication.PushConnectionFactory;
import com.vaadin.flow.server.communication.UidlWriter;
import com.vaadin.flow.server.frontend.FrontendUtils;
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

import static com.vaadin.flow.server.Constants.VAADIN_MAPPING;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Request handler which handles bootstrapping of the application, i.e. the
 * initial GET request.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class BootstrapHandler extends SynchronizedRequestHandler {

    public static final String POLYFILLS_JS = "frontend://bower_components/webcomponentsjs/webcomponents-loader.js";

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
                VaadinResponse response, VaadinSession session, UI ui,
                Function<VaadinRequest, String> contextCallback) {
            this.request = request;
            this.response = response;
            this.session = session;
            this.ui = ui;
            parameterBuilder = new ApplicationParameterBuilder(contextCallback);

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
            DeploymentConfiguration config = session.getConfiguration();
            if (config.isCompatibilityMode()) {
                if (session.getBrowser().isEs6Supported()) {
                    frontendRootUrl = config.getEs6FrontendPrefix();
                } else {
                    frontendRootUrl = config.getEs5FrontendPrefix();
                }
            } else {
                frontendRootUrl = config.getNpmFrontendPrefix();
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

        Document document = pageBuilder.getBootstrapPage(context);
        writeBootstrapPage(response, document.outerHtml());

        return true;
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
    protected static final class BootstrapPageBuilder
            implements PageBuilder, Serializable {

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

            BootstrapUtils.getInitialPageSettings(context).ifPresent(
                    initialPageSettings -> handleInitialPageSettings(context,
                            head, initialPageSettings));

            if (config.isCompatibilityMode()) {
                /* Append any theme elements to initial page. */
                handleThemeContents(context, document);
            }

            if (!config.isProductionMode()) {
                if (config.isBowerMode()) {
                    exportBowerUsageStatistics(document);
                } else {
                    exportNpmUsageStatistics(document);
                }
            }

            setupPwa(document, context);

            if (!config.isCompatibilityMode() && !config.isProductionMode()) {
                checkWebpackStatus(document);
            }

            BootstrapPageResponse response = new BootstrapPageResponse(
                    context.getRequest(), context.getSession(),
                    context.getResponse(), document, context.getUI(),
                    context.getUriResolver());
            context.getSession().getService().modifyBootstrapPage(response);

            return document;
        }

        private String getClientEngine() {
            return clientEngineFile.get();
        }

        private void checkWebpackStatus(Document document) {
            DevModeHandler devMode = DevModeHandler.getDevModeHandler();
            if (devMode != null) {
                String errorMsg = devMode.getFailedOutput();
                if (errorMsg != null) {
                    document.body()
                            .appendChild(new Element(Tag.valueOf("div"), "")
                                    .attr("class", "v-system-error")
                                    .html("<h3>Webpack Error</h3><pre>"
                                            + errorMsg + "</pre>"));
                }
            }
        }

        private void exportBowerUsageStatistics(Document document) {
            String registerScript = UsageStatistics.getEntries().map(entry -> {
                String json = createUsageStatisticsJson(entry);

                String escapedName = Json.create(entry.getName()).toJson();

                // Registers the entry in a way that is picked up as a Vaadin
                // WebComponent by the usage stats gatherer
                return String.format("window.Vaadin[%s]=%s;", escapedName,
                        json);
            }).collect(Collectors.joining("\n"));

            if (!registerScript.isEmpty()) {
                document.body().appendElement(SCRIPT_TAG).text(registerScript);
            }
        }

        private void exportNpmUsageStatistics(Document document) {
            String entries = UsageStatistics.getEntries()
                    .map(BootstrapPageBuilder::createUsageStatisticsJson)
                    .collect(Collectors.joining(","));

            if (!entries.isEmpty()) {
                // Registers the entries in a way that is picked up as a Vaadin
                // WebComponent by the usage stats gatherer
                document.body().appendElement(SCRIPT_TAG)
                        .text("window.Vaadin.registrations = window.Vaadin.registrations || [];\n"
                                + "window.Vaadin.registrations.push(" + entries
                                + ");");
            }
        }

        private static String createUsageStatisticsJson(UsageEntry entry) {
            JsonObject json = Json.createObject();

            json.put("is", entry.getName());
            json.put("version", entry.getVersion());

            return json.toJson();
        }

        private void handleThemeContents(BootstrapContext context,
                Document document) {
            ThemeSettings themeSettings = BootstrapUtils
                    .getThemeSettings(context);

            if (themeSettings == null) {
                // no theme configured for the application
                return;
            }

            List<JsonObject> themeContents = themeSettings.getHeadContents();
            if (themeContents != null) {
                themeContents.stream()
                        .map(dependency -> createDependencyElement(context,
                                dependency))
                        .forEach(element -> insertElements(element,
                                document.head()::appendChild));
            }

            JsonObject themeContent = themeSettings.getHeadInjectedContent();
            if (themeContent != null) {
                Element dependency = createDependencyElement(context,
                        themeContent);
                insertElements(dependency, document.head()::appendChild);
            }

            if (themeSettings.getHtmlAttributes() != null) {
                Element html = document.body().parent();
                assert "html".equalsIgnoreCase(html.tagName());
                themeSettings.getHtmlAttributes().forEach(html::attr);
            }
        }

        private Element createDependencyElement(BootstrapContext context,
                JsonObject dependencyJson) {
            String type = dependencyJson.getString(Dependency.KEY_TYPE);
            if (Dependency.Type.contains(type)) {
                Dependency.Type dependencyType = Dependency.Type.valueOf(type);
                return createDependencyElement(context.getUriResolver(),
                        LoadMode.INLINE, dependencyJson, dependencyType);
            }
            return Jsoup.parse(
                    dependencyJson.getString(Dependency.KEY_CONTENTS), "",
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

        private void handleInitialPageSettings(BootstrapContext context,
                Element head, InitialPageSettings initialPageSettings) {
            if (initialPageSettings.getViewport() != null) {
                Elements viewport = head.getElementsByAttributeValue("name",
                        VIEWPORT);
                if (!viewport.isEmpty() && viewport.size() == 1) {
                    viewport.get(0).attr(CONTENT_ATTRIBUTE,
                            initialPageSettings.getViewport());
                } else {
                    head.appendElement(META_TAG).attr("name", VIEWPORT).attr(
                            CONTENT_ATTRIBUTE,
                            initialPageSettings.getViewport());
                }
            }

            initialPageSettings.getInline(InitialPageSettings.Position.PREPEND)
                    .stream()
                    .map(dependency -> createDependencyElement(context,
                            dependency))
                    .forEach(element -> insertElements(element,
                            head::prependChild));
            initialPageSettings.getInline(InitialPageSettings.Position.APPEND)
                    .stream()
                    .map(dependency -> createDependencyElement(context,
                            dependency))
                    .forEach(element -> insertElements(element,
                            head::appendChild));

            initialPageSettings.getElement(InitialPageSettings.Position.PREPEND)
                    .forEach(element -> insertElements(element,
                            head::prependChild));
            initialPageSettings.getElement(InitialPageSettings.Position.APPEND)
                    .forEach(element -> insertElements(element,
                            head::appendChild));
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

            JsonObject initialUIDL = getInitialUidl(context.getUI());
            Map<LoadMode, JsonArray> dependenciesToProcessOnServer = popDependenciesToProcessOnServer(
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
        protected JsonObject getInitialUidl(UI ui) {
            JsonObject json = new UidlWriter().createUidl(ui, false);

            VaadinSession session = ui.getSession();
            if (session.getConfiguration().isXsrfProtectionEnabled()) {
                writeSecurityKeyUIDL(json, ui);
            }
            writePushIdUIDL(json, session);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Initial UIDL: {}", json.asString());
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
        private void writePushIdUIDL(JsonObject response,
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
        private void writeSecurityKeyUIDL(JsonObject response, UI ui) {
            String seckey = ui.getCsrfToken();
            response.put(ApplicationConstants.UIDL_SECURITY_TOKEN_ID, seckey);
        }

        private List<Element> applyUserDependencies(Element head,
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

        private List<Element> inlineDependenciesInHead(Element head,
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

        private Map<LoadMode, JsonArray> popDependenciesToProcessOnServer(
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

        private void setupFrameworkLibraries(Element head,
                JsonObject initialUIDL, BootstrapContext context) {

            VaadinService service = context.getSession().getService();
            DeploymentConfiguration conf = service.getDeploymentConfiguration();

            if (conf.isCompatibilityMode()) {
                inlineEs6Collections(head, context);
                appendWebComponentsPolyfills(head, context);
            } else {
                conf.getPolyfills().forEach(
                        polyfill -> head.appendChild(createJavaScriptElement(
                                "./" + VAADIN_MAPPING + polyfill, false)));
                try {
                    appendNpmBundle(head, service, context);
                } catch (IOException e) {
                    throw new BootstrapException(
                            "Unable to read webpack stats file.", e);
                }
            }

            if (context.getPushMode().isEnabled()) {
                head.appendChild(getPushScript(context));
            }

            head.appendChild(getBootstrapScript(initialUIDL, context));
            head.appendChild(
                    createJavaScriptElement(getClientEngineUrl(context)));
        }

        private void appendNpmBundle(Element head, VaadinService service,
                BootstrapContext context) throws IOException {
            String content = FrontendUtils.getStatsAssetsByChunkName(service);
            if (content == null) {
                throw new IOException(
                        "The stats file from webpack (stats.json) was not found.\n"
                                + "This typically mean that you have started the application without executing the 'prepare-frontend' Maven target.\n"
                                + "If you are using Spring Boot and are launching the Application class directly, "
                                + "you need to run \"mvn install\" once first or launch the application using \"mvn spring-boot:run\"");
            }
            JsonObject chunks = Json.parse(content);

            for (String key : chunks.keys()) {
                Element script = createJavaScriptElement(
                        "./" + VAADIN_MAPPING + chunks.getString(key));
                if (key.endsWith(".es5")) {
                    head.appendChild(
                            script.attr("nomodule", true).attr("data-app-id",
                                    context.getUI().getInternals().getAppId()));
                } else {
                    head.appendChild(
                            script.attr("type", "module").attr("data-app-id",
                                    context.getUI().getInternals().getAppId()));
                }
            }
        }

        private String getClientEngineUrl(BootstrapContext context) {
            // use nocache version of client engine if it
            // has been compiled by SDM or eclipse
            // In production mode, this should really be loaded by the static
            // block
            // so emit a warning if we get here (tests will always get here)
            final boolean productionMode = context.getSession()
                    .getConfiguration().isProductionMode();

            boolean resolveNow = !productionMode || getClientEngine() == null;
            if (resolveNow
                    && ClientResourcesUtils.getResource("/META-INF/resources/"
                            + CLIENT_ENGINE_NOCACHE_FILE) != null) {
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

        private void inlineEs6Collections(Element head,
                BootstrapContext context) {
            if (!context.getSession().getBrowser().isEs6Supported()) {
                head.appendChild(
                        createInlineJavaScriptElement(ES6_COLLECTIONS));
            }
        }

        private void setupCss(Element head, BootstrapContext context) {
            Element styles = head.appendElement("style").attr("type",
                    CSS_TYPE_ATTRIBUTE_VALUE);
            // Add any body style that is defined for the application using
            // @BodySize
            String bodySizeContent = BootstrapUtils.getBodySizeContent(context);
            styles.appendText(bodySizeContent);

            // Basic reconnect and system error dialog styles just to make them
            // visible and outside of normal flow
            styles.appendText(".v-reconnect-dialog, .v-system-error {" // @formatter:off
                    +   "position: absolute;"
                    +   "color: black;"
                    +   "background: white;"
                    +   "top: 1em;"
                    +   "right: 1em;"
                    +   "border: 1px solid black;"
                    +   "padding: 1em;"
                    +   "z-index: 10000;"
                    +   "max-width: calc(100vw - 4em);"
                    +   "max-height: calc(100vh - 4em);"
                    +   "overflow: auto;"
                    + "} .v-system-error {"
                    +   "color: red;"
                    +   "pointer-events: auto;"
                    + "}"); // @formatter:on
        }

        private void setupMetaAndTitle(Element head, BootstrapContext context) {
            head.appendElement(META_TAG).attr("http-equiv", "Content-Type")
                    .attr(CONTENT_ATTRIBUTE,
                            ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);

            head.appendElement(META_TAG).attr("http-equiv", "X-UA-Compatible")
                    .attr(CONTENT_ATTRIBUTE, "IE=edge");

            head.appendElement("base").attr("href", getServiceUrl(context));

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

        private void appendWebComponentsPolyfills(Element head,
                BootstrapContext context) {
            VaadinSession session = context.getSession();
            DeploymentConfiguration config = session.getConfiguration();

            String es5AdapterUrl = "frontend://bower_components/webcomponentsjs/custom-elements-es5-adapter.js";
            VaadinService service = session.getService();
            if (!service.isResourceAvailable(POLYFILLS_JS, session.getBrowser(),
                    null)) {
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
                head.appendChild(
                        createInlineJavaScriptElement(BABEL_HELPERS_JS));

                if (session.getBrowser().isEs5AdapterNeeded()) {
                    head.appendChild(
                            createJavaScriptElement(context.getUriResolver()
                                    .resolveVaadinUri(es5AdapterUrl), false));
                }
            }

            String resolvedUrl = context.getUriResolver()
                    .resolveVaadinUri(POLYFILLS_JS);
            head.appendChild(createJavaScriptElement(resolvedUrl, false));

        }

        private Element createInlineJavaScriptElement(
                String javaScriptContents) {
            // defer makes no sense without src:
            // https://developer.mozilla.org/en/docs/Web/HTML/Element/script
            Element wrapper = createJavaScriptElement(null, false);
            wrapper.appendChild(
                    new DataNode(javaScriptContents, wrapper.baseUri()));
            return wrapper;
        }

        private Element createJavaScriptElement(String sourceUrl,
                boolean defer) {
            return createJavaScriptElement(sourceUrl, defer, "text/javascript");
        }

        private Element createJavaScriptElement(String sourceUrl, boolean defer,
                String type) {
            Element jsElement = new Element(Tag.valueOf(SCRIPT_TAG), "")
                    .attr("type", type).attr(DEFER_ATTRIBUTE, defer);
            if (sourceUrl != null) {
                jsElement = jsElement.attr("src", sourceUrl);
            }
            return jsElement;
        }

        private Element createJavaScriptElement(String sourceUrl) {
            return createJavaScriptElement(sourceUrl, true);
        }

        private Element createDependencyElement(BootstrapUriResolver resolver,
                LoadMode loadMode, JsonObject dependency,
                Dependency.Type type) {
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
                dependencyElement = createJavaScriptElement(url,
                        !inlineElement);
                break;
            case JS_MODULE:
                if (url != null && UrlUtil.isExternal(url)) {
                    dependencyElement = createJavaScriptElement(url,
                            !inlineElement, "module");
                } else {
                    dependencyElement = null;
                }
                break;
            case HTML_IMPORT:
                dependencyElement = createHtmlImportElement(url);
                break;
            default:
                throw new IllegalStateException(
                        "Unsupported dependency type: " + type);
            }

            if (inlineElement && dependencyElement != null) {
                dependencyElement.appendChild(new DataNode(
                        dependency.getString(Dependency.KEY_CONTENTS),
                        dependencyElement.baseUri()));
            }

            return dependencyElement;
        }

        private Element createHtmlImportElement(String url) {
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

        private Element getPushScript(BootstrapContext context) {
            VaadinRequest request = context.getRequest();

            // Parameter appended to JS to bypass caches after version upgrade.
            String versionQueryParam = "?v=" + Version.getFullVersion();

            // Load client-side dependencies for push support
            String pushJSPath = context.getRequest().getService()
                    .getContextRootRelativePath(request);

            if (request.getService().getDeploymentConfiguration()
                    .isProductionMode()) {
                pushJSPath += ApplicationConstants.VAADIN_PUSH_JS;
            } else {
                pushJSPath += ApplicationConstants.VAADIN_PUSH_DEBUG_JS;
            }

            pushJSPath += versionQueryParam;

            return createJavaScriptElement(pushJSPath);
        }

        private Element getBootstrapScript(JsonValue initialUIDL,
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

        private String getBootstrapJS(JsonValue initialUIDL,
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
         * @return A non-null {@link JsonObject} with application parameters.
         */
        public JsonObject getApplicationParameters(BootstrapContext context) {
            JsonObject appConfig = getApplicationParameters(
                    context.getRequest(), context.getSession());

            appConfig.put(ApplicationConstants.UI_ID_PARAMETER,
                    context.getUI().getUIId());
            return appConfig;
        }

        private JsonObject getApplicationParameters(VaadinRequest request,
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

            boolean sendUrlsAsParameters = deploymentConfiguration
                    .isSendUrlsAsParameters();
            if (!sendUrlsAsParameters) {
                appConfig.put("sendUrlsAsParameters", false);
            }

            return appConfig;
        }

        private void putValueOrNull(JsonObject object, String key,
                String value) {
            assert object != null;
            assert key != null;
            if (value == null) {
                object.put(key, Json.createNull());
            } else {
                object.put(key, value);
            }
        }

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

    private static class LazyClientEngineInit {
        private static final String CLIENT_ENGINE_FILE = readClientEngine();

        private LazyClientEngineInit() {
            // this is a utility class, instances should not be created
        }

        private static String readClientEngine() {
            // read client engine file name
            try (InputStream prop = ClientResourcesUtils
                    .getResource("/META-INF/resources/"
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

    }
}
