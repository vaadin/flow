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
package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.util.regex.Pattern;

import com.vaadin.flow.server.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.VaadinAppShellRegistry;

import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.component.internal.JavaScriptBootstrapUI.SERVER_ROUTING;
import static com.vaadin.flow.shared.ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * In clientSideMode, this class is responsible for serving
 * <code>index.html</code> according to the template provided in frontend
 * folder. The handler will calculate and inject baseHref as well as the bundle
 * scripts into the template.
 */
public class IndexHtmlRequestHandler extends JavaScriptBootstrapHandler {

    private static final Pattern PATH_WITH_EXTENSION = Pattern
            .compile("\\.[A-z][A-z\\d]+$");

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        Document indexDocument = getIndexHtmlDocument(request);

        prependBaseHref(request, indexDocument);
        IndexHtmlResponse indexHtmlResponse;

        if (request.getService().getBootstrapInitialPredicate()
                .includeInitialUidl(request)) {
            includeInitialUidl(session, request, response, indexDocument);

            indexHtmlResponse = new IndexHtmlResponse(request, response, indexDocument, UI.getCurrent());

            // App might be using classic server-routing, which is true
            // unless we detect a call to JavaScriptBootstrapUI.connectClient
            session.setAttribute(SERVER_ROUTING, Boolean.TRUE);
        } else {
            indexHtmlResponse = new IndexHtmlResponse(request, response, indexDocument);
        }

        configureErrorDialogStyles(indexDocument);

        showWebpackErrors(indexDocument);

        response.setContentType(CONTENT_TYPE_TEXT_HTML_UTF_8);

        // modify the page based on the page config annotations (@Meta, etc)
        VaadinContext context = session.getService().getContext();
        VaadinAppShellRegistry.getInstance(context)
                .modifyIndexHtmlResponse(indexDocument);

        // modify the page based on the @PWA annotation
        setupPwa(indexDocument, session.getService());

        // modify the page based on the @Inline annotation
        VaadinAppShellRegistry.getInstance(context)
                .modifyIndexHtmlResponeWithInline(indexDocument, session, request);

        // modify the page based on registered IndexHtmlRequestListener:s
        request.getService().modifyIndexHtmlResponse(indexHtmlResponse);

        try {
            response.getOutputStream()
                    .write(indexDocument.html().getBytes(UTF_8));
        } catch (IOException e) {
            getLogger().error("Error writing 'index.html' to response", e);
            return false;
        }
        return true;
    }

    private void includeInitialUidl(VaadinSession session,
            VaadinRequest request, VaadinResponse response,
            Document indexDocument) {
        JsonObject initial = getInitialJson(request, response, session);


        Element elm = new Element("script");
        elm.attr("initial", "");
        elm.appendChild(new DataNode("window.Vaadin = {Flow : {initial: "
                + JsonUtil.stringify(initial) + "}}"));
        indexDocument.head().insertChildren(0, elm);
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null
                || !PATH_WITH_EXTENSION.matcher(pathInfo).find();
    }

    @Override
    protected void initializeUIWithRouter(VaadinRequest request, UI ui) {
        if (request.getService().getBootstrapInitialPredicate()
                .includeInitialUidl(request)) {
            ui.getRouter().initializeUI(ui, request);
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

    private static Document getIndexHtmlDocument(VaadinRequest request)
            throws IOException {
        String index = FrontendUtils.getIndexHtmlContent(request.getService());
        if (index != null) {
            return Jsoup.parse(index);
        }
        String frontendDir = FrontendUtils.getProjectFrontendDir(
                request.getService().getDeploymentConfiguration());
        String message = String
                .format("Failed to load content of '%1$sindex.html'."
                        + "It is required to have '%1$sindex.html' file in "
                        + "clientSideMode.", frontendDir);
        throw new IOException(message);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(IndexHtmlRequestHandler.class);
    }
}
