/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Optional;

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
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.UsageStatisticsExporter;
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
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.component.internal.JavaScriptBootstrapUI.SERVER_ROUTING;
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

    private static final String SCRIPT_INITIAL = "initial";
    private static final String CONTENT_ATTRIBUTE = "content";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String SPRING_CSRF_TOKEN_ATTRIBUTE_IN_SESSION = "org.springframework.security.web.csrf.CsrfToken";
    private static final String SPRING_CSRF_HEADER_PROPERTY = "headerName";
    private static final String SPRING_CSRF_PARAMETER_PROPERTY = "parameterName";
    private static final String SPRING_CSRF_TOKEN_PROPERTY = "token";
    private static final String SPRING_CSRF_PARAMETER_NAME_ATTRIBUTE = "_csrf_parameter";
    private static final String SPRING_CSRF_HEADER_NAME_ATTRIBUTE = "_csrf_header";
    private static final String SPRING_CSRF_TOKEN_ATTRIBUTE = "_csrf";
    private static final String META_TAG = "meta";

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        if (writeErrorCodeIfRequestLocationIsInvalid(request, response)) {
            return true;
        }

        DeploymentConfiguration config = session.getConfiguration();
        IndexHtmlResponse indexHtmlResponse;

        Document indexDocument = config.isProductionMode()
                ? getCachedIndexHtmlDocument(request.getService())
                : getIndexHtmlDocument(request.getService());

        prependBaseHref(request, indexDocument);

        JsonObject initialJson = Json.createObject();

        if (request.getService().getBootstrapInitialPredicate()
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

        showWebpackErrors(session.getService(), indexDocument);
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
        request.getService().modifyIndexHtmlResponse(indexHtmlResponse);

        if (!config.isProductionMode()) {
            addDevmodeGizmo(indexDocument, config, session, request);
            catchErrorsInDevMode(indexDocument);
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

    private void catchErrorsInDevMode(Document indexDocument) {
        Element elm = new Element("script");
        elm.attr(SCRIPT_INITIAL, "");
        elm.appendChild(new DataNode("" + //
                "window.Vaadin = window.Vaadin || {};" + //
                "window.Vaadin.ConsoleErrors = window.Vaadin.ConsoleErrors || [];"
                + //
                "const browserConsoleError = window.console.error.bind(window.console);"
                + //
                "console.error = (...args) => {" + //
                "    browserConsoleError(...args);" + //
                "    window.Vaadin.ConsoleErrors.push(args);" + //
                "};" + //
                "" //
        ));
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

    private void addDevmodeGizmo(Document indexDocument,
            DeploymentConfiguration config, VaadinSession session,
            VaadinRequest request) {
        VaadinService service = session.getService();
        Optional<BrowserLiveReload> liveReload = BrowserLiveReloadAccessor
                .getLiveReloadFromService(service);

        if (liveReload.isPresent()) {
            Element devmodeGizmo = new Element("vaadin-devmode-gizmo");
            if (!config.isDevModeLiveReloadEnabled()) {
                devmodeGizmo.attr("liveReloadDisabled", "");
            }
            devmodeGizmo.attr("url",
                    BootstrapHandlerHelper.getPushURL(session, request));
            BrowserLiveReload.Backend backend = liveReload.get().getBackend();
            if (backend != null) {
                devmodeGizmo.attr("backend", backend.toString());
            }
            devmodeGizmo.attr("springbootlivereloadport", Integer
                    .toString(Constants.SPRING_BOOT_DEFAULT_LIVE_RELOAD_PORT));
            indexDocument.body().appendChild(devmodeGizmo);
        }
    }

    private void addInitialFlow(JsonObject initialJson, Document indexDocument,
            VaadinRequest request) {
        Object springCsrfToken = request
                .getAttribute(SPRING_CSRF_TOKEN_ATTRIBUTE_IN_SESSION);
        if (springCsrfToken != null) {
            JsonObject springCsrfTokenJson = JsonUtils
                    .beanToJson(springCsrfToken);
            if (springCsrfTokenJson != null
                    && springCsrfTokenJson.hasKey(SPRING_CSRF_TOKEN_PROPERTY)
                    && springCsrfTokenJson
                            .hasKey(SPRING_CSRF_HEADER_PROPERTY)) {
                String springCsrfTokenString = springCsrfTokenJson
                        .getString(SPRING_CSRF_TOKEN_PROPERTY);
                String springCsrfTokenHeaderName = springCsrfTokenJson
                        .getString(SPRING_CSRF_HEADER_PROPERTY);
                String springCsrfTokenParameterName = springCsrfTokenJson
                        .getString(SPRING_CSRF_PARAMETER_PROPERTY);

                addMetaTagToHead(indexDocument.head(),
                        SPRING_CSRF_TOKEN_ATTRIBUTE, springCsrfTokenString);
                addMetaTagToHead(indexDocument.head(),
                        SPRING_CSRF_HEADER_NAME_ATTRIBUTE,
                        springCsrfTokenHeaderName);
                addMetaTagToHead(indexDocument.head(),
                        SPRING_CSRF_PARAMETER_NAME_ATTRIBUTE,
                        springCsrfTokenParameterName);
            }
        }

        Element elm = new Element("script");
        elm.attr(SCRIPT_INITIAL, "");
        elm.appendChild(new DataNode("window.Vaadin = {TypeScript: "
                + JsonUtil.stringify(initialJson) + "};"));
        indexDocument.head().insertChildren(0, elm);
    }

    private void addMetaTagToHead(Element head, String name, String value) {
        Element meta = new Element(META_TAG);
        meta.attr(NAME_ATTRIBUTE, name);
        meta.attr(CONTENT_ATTRIBUTE, value);
        head.insertChildren(0, meta);
    }

    private void includeInitialUidl(JsonObject initialJson,
            VaadinSession session, VaadinRequest request,
            VaadinResponse response) {
        JsonObject initial = getInitialJson(request, response, session);
        initialJson.put(SCRIPT_INITIAL, initial);
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return !BootstrapHandler.isFrameworkInternalRequest(request) && request
                .getService().getBootstrapUrlPredicate().isValidUrl(request);
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
        String index = FrontendUtils.getIndexHtmlContent(service);
        if (index != null) {
            return Jsoup.parse(index);
        }
        String frontendDir = FrontendUtils
                .getProjectFrontendDir(service.getDeploymentConfiguration());
        String indexHtmlFilePath;
        if (frontendDir.endsWith("/") || frontendDir.endsWith(File.separator)) {
            indexHtmlFilePath = frontendDir + "index.html";
        } else if (frontendDir.contains(File.separator)) {
            indexHtmlFilePath = frontendDir + File.separatorChar + "index.html";
        } else {
            indexHtmlFilePath = frontendDir + "/index.html";
        }
        String message = String.format(
                "Failed to load content of '%1$s'. "
                        + "It is required to have '%1$s' file when "
                        + "using client side bootstrapping.",
                indexHtmlFilePath);
        throw new IOException(message);
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
    private static final class IndexHtmlHolder implements Serializable {
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
