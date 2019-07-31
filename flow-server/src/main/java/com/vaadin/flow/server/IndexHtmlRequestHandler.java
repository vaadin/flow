package com.vaadin.flow.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.FrontendUtils;

import elemental.json.Json;

import static com.vaadin.flow.server.Constants.VAADIN_MAPPING;
import static com.vaadin.flow.shared.ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * In client-side bootstrapping mode, this class is responsible for serving
 * <code>index.html</code> according to the template provided in frontend
 * folder. The handler will calculate and inject baseHref as well as the bundle
 * scripts into the template.
 */
public class IndexHtmlRequestHandler extends BootstrapHandler {

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        Document indexDocument = getIndexHtmlDocument(request);
        if (indexDocument == null) {
            return false;
        }
        prependBaseHref(request, indexDocument);
        appendNpmBundle(indexDocument.head(), request.getService());
        response.setContentType(CONTENT_TYPE_TEXT_HTML_UTF_8);
        try {
            writeStream(response.getOutputStream(), indexDocument.html());
        } catch (IOException e) {
            getLogger().error("Error happens while writing 'index.html' file",
                    e);
            return false;
        }
        return true;
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo != null && !pathInfo.matches(".+\\..*$");
    }

    private static void prependBaseHref(VaadinRequest request,
            Document indexDocument) {
        Elements base = indexDocument.head().getElementsByTag("base");
        if (base.isEmpty()) {
            indexDocument.head().prependElement("base").attr("href",
                    getServiceUrl(request));
        }
    }

    private static Document getIndexHtmlDocument(VaadinRequest request) {
        try {
            String index = FrontendUtils
                    .getIndexHtmlContent(request.getService());
            return index != null ? Jsoup.parse(index) : null;
        } catch (IOException e) {
            getLogger().error("Can't read 'index.html'", e);
        }
        return null;
    }

    private static void appendNpmBundle(Element head, VaadinService service)
            throws IOException {
        String content = FrontendUtils.getStatsContent(service);
        if (content == null) {
            throw new IOException(
                    "The stats file from webpack (stats.json) was not found.\n"
                            + "This typically mean that you have started the application without executing the 'prepare-frontend' Maven target.\n"
                            + "If you are using Spring Boot and are launching the Application class directly, "
                            + "you need to run \"mvn install\" once first or launch the application using \"mvn spring-boot:run\"");
        }
        elemental.json.JsonObject chunks = Json.parse(content)
                .getObject("assetsByChunkName");
        for (String key : chunks.keys()) {
            Element script = FrontendUtils.createJavaScriptElement(
                    "./" + VAADIN_MAPPING + chunks.getString(key));
            if (key.endsWith(".es5")) {
                head.appendChild(script.attr("nomodule", true));
            } else {
                head.appendChild(script.attr("type", "module"));
            }
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(IndexHtmlRequestHandler.class);
    }

    private static void writeStream(OutputStream outputStream, String indexHtml)
            throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(outputStream, UTF_8))) {
            writer.append(indexHtml);
        }
    }

    /**
     * Gets the service URL as a URL relative to the request URI.
     *
     * @param vaadinRequest
     *            the request
     * @return the relative service URL
     */
    private static String getServiceUrl(VaadinRequest vaadinRequest) {
        String pathInfo = vaadinRequest.getPathInfo();
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
}
