/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import static com.vaadin.flow.component.UI.SERVER_ROUTING;
import static com.vaadin.flow.shared.ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.BootstrapHandlerHelper;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.internal.UsageStatisticsExporter;
import com.vaadin.flow.internal.springcsrf.SpringCsrfTokenUtil;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FrontendUtils;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

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

    private static final Pattern THEME_GENERATED_FILE_PATTERN = Pattern
            .compile("theme-([\\s\\S]+?)\\.generated\\.js");

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

        JsonObject initialJson = Json.createObject();

        if (service.getBootstrapInitialPredicate()
                .includeInitialUidl(request)) {
            includeInitialUidl(initialJson, session, request, response);

            indexHtmlResponse = new IndexHtmlResponse(request, response,
                    indexDocument, UI.getCurrent());

            // App might be using classic server-routing, which is true
            // unless we detect a call to JavaScriptBootstrapUI.connectClient
            session.setAttribute(SERVER_ROUTING, Boolean.TRUE);
        } else {
            indexHtmlResponse = new IndexHtmlResponse(request, response,
                    indexDocument);
        }

        addInitialFlow(initialJson, indexDocument, request);

        configureErrorDialogStyles(indexDocument);

        configureHiddenElementStyles(indexDocument);

        if (!config.enableDevServer()) {
            addStylesCssLink(config, indexDocument);
        }

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

        // modify the page based on registered IndexHtmlRequestListener:s
        service.modifyIndexHtmlResponse(indexHtmlResponse);

        if (config.isDevToolsEnabled()) {
            addDevTools(indexDocument, config, session, request);
            catchErrorsInDevMode(indexDocument);

            addLicenseChecker(indexDocument);
        }

        try {
            response.getOutputStream()
                    .write(indexDocument.html().getBytes(UTF_8));
        } catch (IOException e) {
            getLogger().error("Error writing 'index.html' to response", e);
            return false;
        }
        return true;
    }

    /**
     * Adds a link tag to the page head for the themes/my-theme/styles.css,
     * which is served in express build mode by static file server directly from
     * frontend/themes folder.
     * </p>
     * Example: <link rel="stylesheet" href="themes/hello-speed-foo/styles.css">
     *
     * @param config
     *            deployment configuration
     * @param indexDocument
     *            the page document to add the tag to
     * @throws IOException
     *             if theme name cannot be extracted from file
     */
    private void addStylesCssLink(DeploymentConfiguration config,
            Document indexDocument) throws IOException {
        String themeName = getThemeName(config.getProjectFolder());
        if (themeName != null) {
            Element element = new Element("link");
            element.attr("rel", "stylesheet");
            element.attr("href", "themes/" + themeName + "/styles.css");
            indexDocument.head().appendChild(element);
        }
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

        if (liveReload.isPresent()) {
            Element devTools = new Element("vaadin-dev-tools");
            if (!config.isDevModeLiveReloadEnabled()) {
                devTools.attr("liveReloadDisabled", "");
            }
            devTools.attr("url",
                    BootstrapHandlerHelper.getPushURL(session, request));
            BrowserLiveReload.Backend backend = liveReload.get().getBackend();
            if (backend != null) {
                devTools.attr("backend", backend.toString());
            }
            devTools.attr("springbootlivereloadport", Integer
                    .toString(Constants.SPRING_BOOT_DEFAULT_LIVE_RELOAD_PORT));
            indexDocument.body().appendChild(devTools);
        }
    }

    private void addInitialFlow(JsonObject initialJson, Document indexDocument,
            VaadinRequest request) {
        SpringCsrfTokenUtil.addTokenAsMetaTagsToHeadIfPresentInRequest(
                indexDocument.head(), request);
        Element elm = new Element(SCRIPT);
        elm.attr(SCRIPT_INITIAL, "");
        elm.appendChild(new DataNode("window.Vaadin = window.Vaadin || {};" + //
                "window.Vaadin.TypeScript= " + JsonUtil.stringify(initialJson)
                + ";"));
        indexDocument.head().insertChildren(0, elm);
    }

    private void includeInitialUidl(JsonObject initialJson,
            VaadinSession session, VaadinRequest request,
            VaadinResponse response) {
        JsonObject initial = getInitialJson(request, response, session);
        initialJson.put(SCRIPT_INITIAL, initial);
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
        String index = FrontendUtils.getIndexHtmlContent(service);
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
        if (config.isProductionMode()) {
            // The index.html is fetched from the bundle so it includes the
            // entry point javascripts
        } else if (!service.getDeploymentConfiguration().enableDevServer()) {
            // When running without a frontend server, the index.html comes
            // directly from the frontend folder and the JS entrypoint(s) need
            // to be added
            URL statsJsonUrl = FrontendUtils.findBundleFile(
                    config.getProjectFolder(), "config/stats.json");
            if (statsJsonUrl == null) {
                throw new IllegalStateException(
                        "The application is running in express mode but there is no bundle found. There is no dev-bundle in the project or on the classpath nor is there a default bundle included");
            }
            String statsJson = IOUtils.toString(statsJsonUrl,
                    StandardCharsets.UTF_8);
            addBundleEntryPoints(indexHtmlDocument, config,
                    Json.parse(statsJson));
        }
        modifyIndexHtmlForVite(indexHtmlDocument);
        return indexHtmlDocument;
    }

    private static void addBundleEntryPoints(Document indexHtmlDocument,
            DeploymentConfiguration config, JsonObject statsJson) {
        JsonArray entryScripts = statsJson.getArray("entryScripts");
        for (int i = 0; i < entryScripts.length(); i++) {
            String entryScript = entryScripts.getString(i);
            Element elm = new Element(SCRIPT);
            elm.attr("type", "module");
            elm.attr("src", "VAADIN/dev-bundle/" + entryScript);
            indexHtmlDocument.head().appendChild(elm);
        }
    }

    private static void modifyIndexHtmlForVite(Document indexHtmlDocument) {
        // Workaround for https://github.com/vitejs/vite/issues/5142
        indexHtmlDocument.head().prepend(
                "<script type='text/javascript'>window.JSCompiler_renameProperty = function(a) { return a;}</script>");
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

    private static String getThemeName(File projectFolder) throws IOException {
        File themeJs = new File(projectFolder, FrontendUtils.FRONTEND
                + FrontendUtils.GENERATED + FrontendUtils.THEME_IMPORTS_NAME);

        if (!themeJs.exists()) {
            getLogger().debug(
                    "Couldn't find file 'theme.js'. A link tag for styles.css won't be added");
            return null;
        }

        String themeJsContent = FileUtils.readFileToString(themeJs,
                StandardCharsets.UTF_8);
        Matcher matcher = THEME_GENERATED_FILE_PATTERN.matcher(themeJsContent);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IllegalStateException(
                    "Couldn't extract theme name from theme imports file 'theme.js'");
        }
    }

}
