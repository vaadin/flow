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
package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.BootstrapHandlerHelper;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReload.Backend;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.LocaleUtil;
import com.vaadin.flow.internal.UsageStatisticsExporter;
import com.vaadin.flow.internal.springcsrf.SpringCsrfTokenUtil;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DevToolsToken;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.ThemeUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.shared.ApplicationConstants;

import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_HOSTS_ALLOWED;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_REMOTE_ADDRESS_HEADER;
import static com.vaadin.flow.shared.ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class is responsible for serving the <code>index.html</code> according
 * to the template provided in the frontend folder. The handler will calculate
 * and inject baseHref as well as the bundle scripts into the template.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class IndexHtmlRequestHandler extends JavaScriptBootstrapHandler {

    private static final String SCRIPT = "script";
    private static final String SCRIPT_INITIAL = "initial";
    public static final String LIVE_RELOAD_PORT_ATTR = "livereload.port";

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        if (writeErrorCodeIfRequestLocationIsInvalid(request, response)) {
            return true;
        }

        DeploymentConfiguration config = session.getConfiguration();
        IndexHtmlResponse indexHtmlResponse;

        VaadinService service = request.getService();
        Document indexDocument = config.isProductionMode()
                ? getCachedIndexHtmlDocument(service)
                : getIndexHtmlDocument(service);

        prependBaseHref(request, indexDocument);

        Element htmlElement = indexDocument.getElementsByTag("html").get(0);
        if (!htmlElement.hasAttr("lang")) {
            Locale locale = LocaleUtil.getLocale(LocaleUtil::getI18NProvider);
            htmlElement.attr("lang", locale.getLanguage());
        }

        initializeFeatureFlags(indexDocument, request);

        ObjectNode initialJson = JacksonUtils.createObjectNode();

        if (service.getBootstrapInitialPredicate()
                .includeInitialUidl(request)) {
            includeInitialUidl(initialJson, session, request, response);
            UI ui = UI.getCurrent();
            var flowContainerElement = new Element(
                    ui.getInternals().getContainerTag());
            flowContainerElement.attr("id", ui.getInternals().getAppId());
            Elements outlet = indexDocument.body().select("#outlet");
            if (!outlet.isEmpty()) {
                outlet.first().appendChild(flowContainerElement);
            } else {
                indexDocument.body().appendChild(flowContainerElement);
            }
            indexHtmlResponse = new IndexHtmlResponse(request, response,
                    indexDocument, ui);
        } else {
            indexHtmlResponse = new IndexHtmlResponse(request, response,
                    indexDocument);
        }

        addInitialFlow(initialJson, indexDocument, request);

        configureErrorDialogStyles(indexDocument);

        configureHiddenElementStyles(indexDocument);

        addStyleTagReferences(indexDocument, config.isProductionMode());

        response.setContentType(CONTENT_TYPE_TEXT_HTML_UTF_8);

        VaadinContext context = session.getService().getContext();
        AppShellRegistry registry = AppShellRegistry.getInstance(context);

        if (!config.isProductionMode()) {
            UsageStatisticsExporter
                    .exportUsageStatisticsToDocument(indexDocument);
        }

        // modify the page based on the @PWA annotation
        setupPwa(indexDocument, session.getService());

        // modify the page based on the @Meta, @ViewPort, @BodySize and @Inline
        // annotations
        // and on the AppShellConfigurator
        registry.modifyIndexHtml(indexDocument, request);

        // the bootstrap page title could be used as a fallback title to
        // a server-side route that doesn't have a title
        storeAppShellTitleToUI(indexDocument);

        redirectToOldBrowserPageWhenNeeded(indexDocument);

        if (!config.isProductionMode()) {
            // Ensure no older tools incorrectly detect a bundle as production
            // mode
            addScript(indexDocument, """
                    window.Vaadin = window.Vaadin || {};
                    window.Vaadin.developmentMode = true;
                    """);
        }

        addDevBundleTheme(indexDocument, context);
        applyThemeVariant(indexDocument, context);

        if (config.isDevToolsEnabled()) {
            addDevTools(indexDocument, config, session, request);
            catchErrorsInDevMode(indexDocument);

            addLicenseChecker(indexDocument);
        } else if (!config.isProductionMode()) {
            // If a dev-tools plugin tries to register itself with disabled
            // dev-tools, the application completely breaks with a JS error
            addScript(indexDocument,
                    """
                            window.Vaadin = window.Vaadin || {};
                            window.Vaadin.devToolsPlugins = {
                                push: function(plugin) {
                                    window.console.debug("Vaadin Dev Tools disabled. Plugin cannot be registered.", plugin);
                                }
                            };
                            """);
        }

        // this invokes any custom listeners and should be run when the whole
        // page is constructed
        service.modifyIndexHtmlResponse(indexHtmlResponse);

        addCommercialBanner(service.getDeploymentConfiguration(),
                indexDocument);

        try {
            response.getOutputStream()
                    .write(indexDocument.html().getBytes(UTF_8));
        } catch (IOException e) {
            getLogger().error("Error writing 'index.html' to response", e);
            return false;
        }
        return true;
    }

    private void initializeFeatureFlags(Document indexDocument,
            VaadinRequest request) {
        String script = featureFlagsInitializer(request);
        Element scriptElement = indexDocument.head().prependElement("script");
        scriptElement.attr(SCRIPT_INITIAL, "");
        scriptElement.appendChild(new DataNode(script));
    }

    static String featureFlagsInitializer(VaadinRequest request) {
        return FeatureFlags.get(request.getService().getContext()).getFeatures()
                .stream().filter(Feature::isEnabled)
                .map(feature -> String.format("activator(\"%s\");",
                        feature.getId()))
                .collect(Collectors.joining("\n",
                        """
                                window.Vaadin = window.Vaadin || {};
                                window.Vaadin.featureFlagsUpdaters = window.Vaadin.featureFlagsUpdaters || [];
                                window.Vaadin.featureFlagsUpdaters.push((activator) => {
                                """,
                        "});"));
    }

    private static void addDevBundleTheme(Document document,
            VaadinContext context) {
        ApplicationConfiguration config = ApplicationConfiguration.get(context);
        if (config.getMode() == Mode.DEVELOPMENT_BUNDLE
                || (config.getMode() == Mode.PRODUCTION_PRECOMPILED_BUNDLE)) {
            try {
                BootstrapHandler.getStylesheetTags(context, "styles.css")
                        .forEach(link -> document.head().appendChild(link));
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Failed to create a tag for 'styles.css' in the document",
                        e);
            }
        }
    }

    private void applyThemeVariant(Document indexDocument,
            VaadinContext context) {
        ThemeUtils.getThemeAnnotation(context).ifPresent(theme -> {
            String variant = theme.variant();
            if (!variant.isEmpty()) {
                indexDocument.head().parent().attr("theme", variant);
            }
        });
    }

    private void addStyleTagReferences(Document indexDocument,
            boolean productionMode) {
        int insertLocation = -1; // At the end
        if (productionMode) {
            /*
             * In production mode, the theme css is included by Vite in
             * index.html. The CSS override order has been specified so that the
             * theme should override all other CSS, so it must come last and we
             * must insert before that.
             *
             * We don't really know which link tag it is, if there are multiple,
             * so we use the first. Then all link tags override css imports.
             */
            Elements links = indexDocument.head().getElementsByTag("link");
            if (!links.isEmpty()) {
                insertLocation = indexDocument.head().childNodes()
                        .indexOf(links.first());
            }
        }

        Comment cssImportComment = new Comment("CSSImport end");
        Comment stylesheetComment = new Comment("Stylesheet end");
        indexDocument.head().insertChildren(insertLocation, cssImportComment,
                stylesheetComment);
    }

    private void redirectToOldBrowserPageWhenNeeded(Document indexDocument) {
        addScript(indexDocument, """
                if (!('CSSLayerBlockRule' in window)) {
                    window.location.search='v-r=oldbrowser';
                }
                """);
    }

    private void catchErrorsInDevMode(Document indexDocument) {
        addScript(indexDocument, "" + //
                "window.Vaadin = window.Vaadin || {};" + //
                "window.Vaadin.ConsoleErrors = window.Vaadin.ConsoleErrors || [];"
                + //
                "const browserConsoleError = window.console.error.bind(window.console);"
                + //
                "console.error = (...args) => {" + //
                "    browserConsoleError(...args);" + //
                "    window.Vaadin.ConsoleErrors.push(args);" + //
                "};" + //
                "window.onerror = (message, source, lineno, colno, error) => {"
                + //
                "const location=source+':'+lineno+':'+colno;" + //
                "window.Vaadin.ConsoleErrors.push([message, '('+location+')']);"
                + //
                "};" + //
                "window.addEventListener('unhandledrejection', e => {" + //
                "    window.Vaadin.ConsoleErrors.push([e.reason]);" + //
                "});" //
        );
    }

    /**
     * Adds the needed overrides for the license checker to work when in
     * development mode.
     */
    public static void addLicenseChecker(Document indexDocument) {
        // maybeCheck is invoked by the WC license checker
        addScript(indexDocument, "" + //
                "window.Vaadin = window.Vaadin || {};" + //
                "window.Vaadin.VaadinLicenseChecker = {" + //
                "  maybeCheck: (productInfo) => {" + //
                // This disables the license check that the web components are
                // still using
                "  }" + //
                "};" + //
                "window.Vaadin.devTools = window.Vaadin.devTools || {};"
                + "window.Vaadin.devTools.createdCvdlElements = window.Vaadin.devTools.createdCvdlElements || [];"
                + //
                "window.Vaadin.originalCustomElementDefineFn = window.Vaadin.originalCustomElementDefineFn || window.customElements.define;"
                + //
                "window.customElements.define = function (tagName, constructor, ...args) {"
                + //
                "const { cvdlName, version } = constructor;" + //
                "if (cvdlName && version) {" + //
                "  const { connectedCallback } = constructor.prototype;" + //
                "  constructor.prototype.connectedCallback = function () {" + //
                "    window.Vaadin.devTools.createdCvdlElements.push(this);" + //
                "    if (connectedCallback) {" + //
                "      connectedCallback.call(this);" + //
                "    }" + //
                "  }" + //
                "}" + //

                "window.Vaadin.originalCustomElementDefineFn.call(this, tagName, constructor, ...args);"
                + //
                "};");

    }

    private static void addScript(Document indexDocument, String script) {
        Element elm = new Element(SCRIPT);
        elm.attr(SCRIPT_INITIAL, "");
        elm.appendChild(new DataNode(script));
        indexDocument.head().insertChildren(0, elm);
    }

    private static void addScriptSrc(Document indexDocument, String scriptUrl) {
        Element elm = new Element(SCRIPT);
        elm.attr(SCRIPT_INITIAL, "");
        elm.attr("src", scriptUrl);
        indexDocument.head().appendChild(elm);
    }

    private void storeAppShellTitleToUI(Document indexDocument) {
        if (UI.getCurrent() != null) {
            Element elm = indexDocument.head().selectFirst("title");
            if (elm != null) {
                String appShellTitle = elm.text().isEmpty() ? elm.data()
                        : elm.text();
                UI.getCurrent().getInternals().setAppShellTitle(appShellTitle);
            }
        }
    }

    private void addDevTools(Document indexDocument,
            DeploymentConfiguration config, VaadinSession session,
            VaadinRequest request) {
        VaadinService service = session.getService();

        Optional<BrowserLiveReload> liveReload = BrowserLiveReloadAccessor
                .getLiveReloadFromService(service);
        Optional<Backend> maybeBackend = liveReload
                .map(BrowserLiveReload::getBackend);

        int liveReloadPort = Constants.SPRING_BOOT_DEFAULT_LIVE_RELOAD_PORT;
        VaadinContext context = service.getContext();
        if (context instanceof VaadinServletContext vaadinServletContext) {
            String customPort = (String) vaadinServletContext.getContext()
                    .getAttribute(LIVE_RELOAD_PORT_ATTR);
            if (customPort != null) {
                liveReloadPort = Integer.parseInt(customPort);
            }
        }

        ObjectNode devToolsConf = JacksonUtils.createObjectNode();
        devToolsConf.put("enable", config.isDevModeLiveReloadEnabled());
        devToolsConf.put("url",
                BootstrapHandlerHelper.getPushURL(session, request));
        maybeBackend.ifPresent(
                backend -> devToolsConf.put("backend", backend.toString()));
        devToolsConf.put("liveReloadPort", liveReloadPort);
        if (isAllowedDevToolsHost(config, request)) {
            devToolsConf.put("token", DevToolsToken.getToken());
        }
        addScript(indexDocument, String.format("""
                window.Vaadin.devToolsPlugins = [];
                window.Vaadin.devToolsConf = %s;
                """, devToolsConf));

        indexDocument.body().appendChild(new Element("vaadin-dev-tools"));

        String pushUrl = BootstrapHandlerHelper.getServiceUrl(request) + "/"
                + ApplicationConstants.VAADIN_PUSH_DEBUG_JS;
        addScriptSrc(indexDocument, pushUrl);
    }

    static boolean isAllowedDevToolsHost(AbstractConfiguration configuration,
            VaadinRequest request) {
        String remoteAddress = request.getRemoteAddr();
        String hostsAllowedFromCfg = configuration.getStringProperty(
                SERVLET_PARAMETER_DEVMODE_HOSTS_ALLOWED, null);
        String hostsAllowed = (hostsAllowedFromCfg != null
                && !hostsAllowedFromCfg.isBlank()) ? hostsAllowedFromCfg : null;

        if (!isAllowedDevToolsHost(remoteAddress, hostsAllowed, true)) {
            return false;
        }
        String remoteHeaderIp = configuration.getStringProperty(
                SERVLET_PARAMETER_DEVMODE_REMOTE_ADDRESS_HEADER, null);
        if (remoteHeaderIp != null) {
            return isAllowedDevToolsHost(request.getHeader(remoteHeaderIp),
                    hostsAllowed, false);
        }

        Enumeration<String> allForwardedForHeaders = request
                .getHeaders("X-Forwarded-For");
        if (allForwardedForHeaders != null
                && allForwardedForHeaders.hasMoreElements()) {
            String forwardedFor = String.join(",",
                    Collections.list(allForwardedForHeaders));

            if (forwardedFor.contains(",")) {
                // X-Forwarded-For: <client>, <proxy1>, <proxy2>
                // Elements are comma-separated, with optional whitespace
                // surrounding the commas.
                // Validate all hops
                String[] hops = forwardedFor.split(",");
                if (hops.length > 0) {
                    return Stream.of(hops).map(String::trim)
                            .allMatch(ip -> isAllowedDevToolsHost(ip,
                                    hostsAllowed, false));
                } else {
                    // Potential fake header with no addresses, e.g.
                    // 'X-Forwarded-For: ,,,'
                    return false;
                }

            } else {
                return isAllowedDevToolsHost(forwardedFor.trim(), hostsAllowed,
                        false);
            }
        }

        return true;

    }

    private static boolean isAllowedDevToolsHost(String remoteAddress,
            String hostsAllowed, boolean allowLocal) {
        if (remoteAddress == null || remoteAddress.isBlank()
                || (hostsAllowed == null && !allowLocal)) {
            // No check needed if the remote address is not available
            // or if local addresses must be rejected and there are no host
            // rules defined
            return false;
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(remoteAddress);
            if (inetAddress.isLoopbackAddress()) {
                return allowLocal;
            }
        } catch (Exception e) {
            getLogger().debug(
                    "Unable to resolve remote address: '{}', so we are preventing the web socket connection",
                    remoteAddress, e);
            return false;
        }

        if (hostsAllowed != null) {
            // Allowed hosts set
            String[] allowedHosts = hostsAllowed.split(",");

            for (String allowedHost : allowedHosts) {
                if (FilenameUtils.wildcardMatch(remoteAddress,
                        allowedHost.trim())) {
                    return true;
                }
            }
        }

        return false;
    }

    private void addInitialFlow(ObjectNode initialJson, Document indexDocument,
            VaadinRequest request) {
        SpringCsrfTokenUtil.addTokenAsMetaTagsToHeadIfPresentInRequest(
                indexDocument.head(), request);
        Element elm = new Element(SCRIPT);
        elm.attr(SCRIPT_INITIAL, "");
        elm.appendChild(new DataNode("window.Vaadin = window.Vaadin || {};" + //
                "window.Vaadin.TypeScript= " + initialJson.toString() + ";"));
        indexDocument.head().insertChildren(0, elm);
    }

    private void includeInitialUidl(ObjectNode initialJson,
            VaadinSession session, VaadinRequest request,
            VaadinResponse response) {
        ObjectNode initial = getInitialJson(request, response, session);
        initialJson.set(SCRIPT_INITIAL, initial);
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return isRequestForHtml(request)
                && !BootstrapHandler.isFrameworkInternalRequest(request)
                && !BootstrapHandler.isVaadinStaticFileRequest(request)
                && request.getService().getBootstrapUrlPredicate()
                        .isValidUrl(request);
    }

    @Override
    protected void initializeUIWithRouter(BootstrapContext context, UI ui) {
        if (context.getService().getBootstrapInitialPredicate()
                .includeInitialUidl(context.getRequest())) {
            ui.getInternals().getRouter().initializeUI(ui, context.getRoute());
        }
    }

    private void configureErrorDialogStyles(Document document) {
        Element styles = document.createElement("style");
        document.head().appendChild(styles);
        setupErrorDialogs(styles);
    }

    private void configureHiddenElementStyles(Document document) {
        Element styles = document.createElement("style");
        document.head().appendChild(styles);
        setupHiddenElement(styles);
    }

    private static void prependBaseHref(VaadinRequest request,
            Document indexDocument) {
        Elements base = indexDocument.head().getElementsByTag("base");
        String baseHref = getServiceUrl(request);
        if (base.isEmpty()) {
            indexDocument.head().prependElement("base").attr("href", baseHref);
        } else {
            base.first().attr("href", baseHref);
        }
    }

    private static Document getCachedIndexHtmlDocument(VaadinService service) {
        return service.getContext().getAttribute(IndexHtmlHolder.class,
                () -> new IndexHtmlHolder(service)).getDocument();
    }

    private static Document getIndexHtmlDocument(VaadinService service)
            throws IOException {
        DeploymentConfiguration config = service.getDeploymentConfiguration();
        String index;
        try {
            index = FrontendUtils.getIndexHtmlContent(service);
        } catch (IOException e) {
            if (config.getMode() == Mode.DEVELOPMENT_FRONTEND_LIVERELOAD) {
                throw new IOException(
                        "Unable to fetch index.html from the frontend development server, check the server logs that it is running");
            } else {
                throw new IOException(
                        "Unable to find index.html. It should be available on the classpath");
            }
        }
        if (index == null) {
            if (config.isProductionMode()) {
                throw new IOException(
                        "Unable to find index.html. It should be available on the classpath when running in production mode");
            } else {
                throw new IOException(
                        "Unable to find index.html. It should be available in the frontend folder when running in development mode");
            }
        }

        Document indexHtmlDocument = Jsoup.parse(index);
        Mode mode = config.getMode();
        if (mode.isProduction()) {
            // The index.html is fetched from the bundle so it includes the
            // entry point javascripts
        } else if (mode == Mode.DEVELOPMENT_BUNDLE) {
            // When running without a frontend server, the index.html comes
            // directly from the frontend folder and the JS entrypoint(s) need
            // to be added
            addGeneratedIndexContent(indexHtmlDocument, getStatsJson(config));
        }
        modifyIndexHtmlForVite(indexHtmlDocument);
        return indexHtmlDocument;
    }

    protected static void addGeneratedIndexContent(Document targetDocument,
            ObjectNode statsJson) {

        ArrayNode indexHtmlGeneratedRows = (ArrayNode) statsJson
                .get("indexHtmlGenerated");

        List<String> toAdd = new ArrayList<>(JacksonUtils
                .stream(indexHtmlGeneratedRows).map(JsonNode::asText).toList());

        for (String row : toAdd) {
            targetDocument.head().append(row);
        }
    }

    private static void modifyIndexHtmlForVite(Document indexHtmlDocument) {
        // Workaround for https://github.com/vitejs/vite/issues/5142
        indexHtmlDocument.head().prepend(
                "<script type='text/javascript'>window.JSCompiler_renameProperty = function(a) { return a;}</script>");
    }

    static void addCommercialBanner(DeploymentConfiguration config,
            Document indexDocument) {
        System.clearProperty("vaadin." + Constants.COMMERCIAL_BANNER_TOKEN);
        if (config.isProductionMode() && config
                .getBooleanProperty(Constants.COMMERCIAL_BANNER_TOKEN, false)) {
            Element elm = new Element(SCRIPT);
            elm.attr(SCRIPT_INITIAL, "");
            elm.attr("type", "module");
            elm.appendChild(new DataNode(
                    """
                            const root = document.body.attachShadow({ mode: 'closed' })
                            root.innerHTML = '<slot></slot><vaadin-commercial-banner></vaadin-commercial-banner>'
                            """));
            indexDocument.head().insertChildren(0, elm);
        }
    }

    // Holds parsed index.html to avoid re-parsing on every request in
    // production mode
    //
    // This holder is supposed to be stored as a VaadinContext attribute
    //
    // Note: IndexHtmlHolder is not really serializable, but I can't come up
    // with
    // circumstances under which it'll break. It seems unlikely that
    // VaadinContext
    // will be serialized/deserialized.
    static final class IndexHtmlHolder implements Serializable {
        private final transient Document indexHtmlDocument;

        private IndexHtmlHolder(VaadinService service) {
            try {
                this.indexHtmlDocument = getIndexHtmlDocument(service);
                this.indexHtmlDocument.outputSettings().prettyPrint(false);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private Document getDocument() {
            return this.indexHtmlDocument.clone();
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(IndexHtmlRequestHandler.class);
    }

}
