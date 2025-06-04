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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.webcomponent.WebComponentUI;
import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.BootstrapHandlerHelper;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.BootstrapException;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.JsonConstants;

import static com.vaadin.flow.server.frontend.FrontendUtils.EXPORT_CHUNK;
import static com.vaadin.flow.shared.ApplicationConstants.CONTENT_TYPE_TEXT_JAVASCRIPT_UTF_8;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Bootstrap handler for WebComponent requests.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public class WebComponentBootstrapHandler extends BootstrapHandler {
    private static final String REQ_PARAM_URL = "url";
    private static final String PATH_PREFIX = "/web-component/web-component";
    static final Pattern PATH_PATTERN = Pattern
            .compile(".*" + PATH_PREFIX + "-(ui|bootstrap)\\.(js|html)$");

    private static class WebComponentBootstrapContext extends BootstrapContext {

        private WebComponentBootstrapContext(VaadinRequest request,
                VaadinResponse response, UI ui,
                Function<VaadinRequest, String> callback) {
            super(request, response, ui.getInternals().getSession(), ui,
                    callback);
            setInitTheme(false);
        }

        @Override
        public <T extends Annotation> Optional<T> getPageConfigurationAnnotation(
                Class<T> annotationType) {
            WebComponentConfigurationRegistry registry = WebComponentConfigurationRegistry
                    .getInstance(getService().getContext());
            return registry.getEmbeddedApplicationAnnotation(annotationType);
        }

        @Override
        protected Optional<PwaRegistry> getPwaRegistry() {
            return Optional.empty();
        }

        @Override
        public String getAppId() {
            return "wc-" + super.getAppId();
        }
    }

    private static class WebComponentBootstrapPageBuilder
            extends BootstrapPageBuilder {
        @Override
        public Document getBootstrapPage(BootstrapContext context) {
            VaadinService service = context.getSession().getService();

            try {
                Document document = Jsoup.parse(
                        FrontendUtils.getWebComponentHtmlContent(service));
                Element head = document.head();

                DeploymentConfiguration deploymentConfiguration = service
                        .getDeploymentConfiguration();

                if (deploymentConfiguration.isProductionMode()) {
                    // The web-component.html is fetched from the bundle so it
                    // includes the entry point javascripts
                } else if (deploymentConfiguration
                        .getMode() != Mode.DEVELOPMENT_FRONTEND_LIVERELOAD) {
                    // When running without a frontend server, the
                    // web-component.html comes
                    // directly from the frontend folder and the JS
                    // entrypoint(s) need
                    // to be added
                    addGeneratedIndexContent(document,
                            getStatsJson(deploymentConfiguration));
                }

                // Specify the application ID for scripts of the
                // web-component.html
                head.select("script[src]").attr("data-app-id",
                        context.getUI().getInternals().getAppId());

                // Add `crossorigin` to fix basic auth in Safari #6560
                head.select("script[src], link[href]").attr("crossorigin",
                        "true");

                ObjectNode initialUIDL = getInitialUidl(context.getUI());

                head.prependChild(createInlineJavaScriptElement(
                        "window.JSCompiler_renameProperty = function(a) { return a; }"));

                head.prependChild(getBootstrapScript(initialUIDL, context));

                if (context.getPushMode().isEnabled()) {
                    head.prependChild(createJavaScriptModuleElement(
                            getPushScript(context), true));
                }

                setupCss(head, context);

                return document;
            } catch (IOException e) {
                throw new BootstrapException(
                        "Unable to read the web-component.html file.", e);
            }
        }

        @Override
        protected List<String> getChunkKeys(ObjectNode chunks) {
            if (chunks.has(EXPORT_CHUNK)) {
                return Collections.singletonList(EXPORT_CHUNK);
            } else {
                return super.getChunkKeys(chunks);
            }
        }
    }

    protected static void addGeneratedIndexContent(Document targetDocument,
            ObjectNode statsJson) {
        List<String> toAdd = new ArrayList<>();

        Optional<String> webComponentScript = JacksonUtils
                .stream((ArrayNode) statsJson.get("entryScripts"))
                .map(JsonNode::asText)
                .filter(script -> script.contains("webcomponenthtml"))
                .findFirst();

        if (webComponentScript.isPresent()) {
            Element elm = new Element(SCRIPT_TAG);
            elm.attr("type", "module");
            elm.attr("src", webComponentScript.get());
            toAdd.add(elm.outerHtml());
        }

        for (String row : toAdd) {
            targetDocument.head().append(row);
        }
    }

    /**
     * Creates a new bootstrap handler with default page builder.
     */
    public WebComponentBootstrapHandler() {
        super(new WebComponentBootstrapPageBuilder());
    }

    /**
     * Creates a new bootstrap handler, allowing to use custom page builder.
     *
     * @param pageBuilder
     *            Page builder to use.
     */
    protected WebComponentBootstrapHandler(PageBuilder pageBuilder) {
        super(pageBuilder);
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        if (!hasWebComponentConfigurations(request)) {
            return false;
        }
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            return false;
        }
        return (PATH_PATTERN.matcher(pathInfo).find());
    }

    /**
     * Returns the request's base url to use in constructing and initialising
     * ui.
     *
     * @param request
     *            Request to the url for.
     * @return Request's url.
     */
    protected String getRequestUrl(VaadinRequest request) {
        return ((VaadinServletRequest) request).getRequestURL().toString();
    }

    @Override
    protected BootstrapContext createAndInitUI(Class<? extends UI> uiClass,
            VaadinRequest request, VaadinResponse response,
            VaadinSession session) {

        if (!canHandleRequest(request)) {
            throw new IllegalStateException(
                    "Unexpected request URL '" + getRequestUrl(request)
                            + "' in the bootstrap handler for web "
                            + "component UI which should handle path "
                            + PATH_PATTERN.toString());
        }

        final String serviceUrl = getServiceUrl(request, response);

        BootstrapContext context = super.createAndInitUI(WebComponentUI.class,
                request, response, session);
        ObjectNode config = context.getApplicationParameters();

        PushConfiguration pushConfiguration = context.getUI()
                .getPushConfiguration();
        pushConfiguration.setPushServletMapping(
                BootstrapHandlerHelper.determinePushServletMapping(session));

        assert serviceUrl.endsWith("/");
        config.put(ApplicationConstants.SERVICE_URL, serviceUrl);
        config.put(ApplicationConstants.APP_WC_MODE, true);
        WebComponentConfigurationRegistry registry = WebComponentConfigurationRegistry
                .getInstance(request.getService().getContext());

        ArrayNode tags = registry.getConfigurations().stream()
                .map(conf -> JacksonUtils.createNode(conf.getTag()))
                .collect(JacksonUtils.asArray());
        config.put("webcomponents", tags);

        config.put(ApplicationConstants.DEV_TOOLS_ENABLED, false);

        return context;
    }

    @Override
    protected BootstrapContext createBootstrapContext(VaadinRequest request,
            VaadinResponse response, UI ui,
            Function<VaadinRequest, String> callback) {
        return new WebComponentBootstrapContext(request, response, ui,
                callback);
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        if ("HEAD".equals(request.getMethod().toUpperCase(Locale.ROOT))) {
            // Poll request to check if the dev-bundle is ready
            // Prevent the creation of a UI
            return true;
        }
        // Find UI class
        Class<? extends UI> uiClass = getUIClass(request);

        BootstrapContext context = createAndInitUI(uiClass, request, response,
                session);

        if (handleWebComponentResyncRequest(context, request, response)) {
            return true;
        }

        HandlerHelper.setResponseNoCacheHeaders(response::setHeader,
                response::setDateHeader);

        String serviceUrl = getServiceUrl(request, response);

        Document document = getPageBuilder().getBootstrapPage(context);
        writeBootstrapPage(response, document.head(), serviceUrl);
        UsageStatistics.markAsUsed(Constants.STATISTICS_EXPORTED_WC, null);
        return true;
    }

    /**
     * Copies the {@link org.jsoup.nodes.Element Elements} found in the given
     * {@code head} elements into the head of the embedding website using
     * JavaScript. Drops {@code <base>} element.
     *
     * @param response
     *            {@link com.vaadin.flow.server.VaadinResponse} into which the
     *            script is written
     * @param head
     *            head element of Vaadin Bootstrap page. The child elements are
     *            copied into the embedding page's head using JavaScript.
     * @param serviceUrl
     *            base path to use for the head elements' URLs
     * @throws IOException
     *             if writing fails
     */
    private void writeBootstrapPage(VaadinResponse response, Element head,
            String serviceUrl) throws IOException {
        writeBootstrapPage(CONTENT_TYPE_TEXT_JAVASCRIPT_UTF_8, response, head,
                serviceUrl);
    }

    /**
     * Copies the {@link org.jsoup.nodes.Element Elements} found in the given
     * {@code head} elements into the head of the embedding website using
     * JavaScript. Drops {@code <base>} element.
     *
     * @param contentType
     *            Content type of the response.
     * @param response
     *            {@link com.vaadin.flow.server.VaadinResponse} into which the
     *            script is written
     * @param head
     *            head element of Vaadin Bootstrap page. The child elements are
     *            copied into the embedding page's head using JavaScript.
     * @param serviceUrl
     *            base path to use for the head elements' URLs
     * @throws IOException
     *             if writing fails
     */
    protected void writeBootstrapPage(String contentType,
            VaadinResponse response, Element head, String serviceUrl)
            throws IOException {
        /*
         * The elements found in the head are reconstructed using JavaScript and
         * document.createElement(...). Since innerHTML and related methods do
         * not execute <script> blocks, the contents cannot be copied as pure
         * string into the head. The each element is created separately and then
         * attributes are copied and innerHTML set, if the element has
         * innerHTML. The innerHTMLs are in-lined for easier copying.
         */
        response.setContentType(contentType);
        /*
         * Collection of Elements that should be transferred to the web
         * component shadow DOMs rather than the page head
         */
        ArrayList<com.vaadin.flow.dom.Element> elementsForShadows = new ArrayList<>();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), UTF_8))) {
            writer.write("(function () {\n"
                    + "var hasScript = function(src) {\n"
                    + "  var scriptTags = Array.from(document.head.querySelectorAll('script'));\n"
                    + "  return scriptTags.some(script => script.src.endsWith(src))\n"
                    + "};\n");

            String varName = "headElem"; // generated head element
            writer.append("var ").append(varName).append("=null;");
            for (Element element : head.children()) {
                if (elementShouldNotBeTransferred(element)) {
                    getElementForShadowDom(element)
                            .ifPresent(elementsForShadows::add);
                    continue;
                }
                String conditionalFilename = getVaadinFilenameIfVaadinScript(
                        element);
                if (conditionalFilename != null) {
                    writer.append("if (!hasScript(\"" + conditionalFilename
                            + "\")) {\n");
                }
                writer.append(varName).append("=");
                writer.append("document.createElement('")
                        .append(element.tagName()).append("');");
                transferAttribute(writer, varName, element, serviceUrl);
                // set cleaned html as innerHTML for the element
                String elementHtml = element.html();
                if (elementHtml != null && elementHtml.length() > 0) {
                    writer.append(varName).append(".innerHTML=\"")
                            .append(inlineHTML(elementHtml)).append("\";");
                }
                writer.append("document.head.appendChild(").append(varName)
                        .append(");");
                if (conditionalFilename != null) {
                    writer.append("}\n");
                }
            }
            writer.append("})();");
        }

        DeploymentConfiguration config = response.getService()
                .getDeploymentConfiguration();
        VaadinContext context = response.getService().getContext();

        // stylesheet tags are only added in dev mode, because Flow always
        // rebuilds prod bundle whenever it spots embedded web components, so
        // all the styles are included into the custom bundle
        if (config.getMode() == Mode.DEVELOPMENT_BUNDLE) {
            // Add styles.css link to the web component shadow DOM
            BootstrapHandler.getStylesheetTags(context, "styles.css")
                    .forEach(element -> ElementUtil.fromJsoup(element)
                            .ifPresent(elementsForShadows::add));

            File frontendDirectory = FrontendUtils
                    .getProjectFrontendDir(config);

            // Add document.css link to the document
            BootstrapHandler
                    .getStylesheetLinks(context, "document.css",
                            frontendDirectory)
                    .forEach(link -> UI.getCurrent().getPage().executeJs(
                            BootstrapHandler.SCRIPT_TEMPLATE_FOR_STYLESHEET_LINK_TAG,
                            modifyPath(serviceUrl, link)));
        }

        WebComponentConfigurationRegistry
                .getInstance(response.getService().getContext())
                .setShadowDomElements(elementsForShadows);
    }

    private static String getVaadinFilenameIfVaadinScript(Element element) {
        if (!"script".equalsIgnoreCase(element.tagName())) {
            return null;
        }
        // Injecting a frontend bundle twice can never work.
        // The bundle contains web components that register
        // themselves and loading twice will always cause
        // custom element conflicts
        String src = element.attr("src");
        int index = src.indexOf("/VAADIN/");
        if (index != -1) {
            return src.substring(index);
        }

        return null;
    }

    private static boolean elementShouldNotBeTransferred(Element element) {
        // we skip base href adjustment, since we are in a 3rd party
        // context, also "meta" and "style" affects the page globally and should
        // be skipped
        if ("base".equals(element.tagName()) || "meta".equals(element.tagName())
                || "style".equals(element.tagName())) {
            return true;
        } else {
            // embedding context should not provide polyfill, it is left
            // to the end-user
            return "script".equals(element.tagName())
                    && element.attr("src").contains("webcomponents-loader.js");
        }
    }

    private static Optional<com.vaadin.flow.dom.Element> getElementForShadowDom(
            Element element) {
        if ("style".equals(element.tagName())) {
            return ElementUtil.fromJsoup(element);
        }
        return Optional.empty();
    }

    /**
     * Creates a javascript which copies attributes from the {@code element} to
     * the created DOM element identified by {@code elementRef}. If {@code
     * element} contains a {@code src} attribute, its path is prefixed with
     * {@code basePath}.
     *
     * @param writer
     *            response writer
     * @param elementRef
     *            variable name of the element in javascript
     * @param element
     *            jsoup element from which to copy the attributes
     * @param basePath
     *            base path of {@code src} attributes (service url's path)
     * @throws IOException
     *             if {@code writer} is unable to write
     */
    private void transferAttribute(Writer writer, String elementRef,
            Element element, String basePath) throws IOException {
        for (Attribute attribute : element.attributes()) {
            writer.append(elementRef).append(".setAttribute('")
                    .append(attribute.getKey()).append("',");
            if (attribute.getValue() == null) {
                writer.append("''");
            } else {
                String name = attribute.getKey();
                String path = attribute.getValue();
                if (name.matches("^(src|href)$")) {
                    path = modifyPath(basePath, path);
                }
                writer.append("\"").append(path).append("\"");
            }
            writer.append(");");
        }
    }

    /**
     * Create a new address for a resource which is calculated based on the
     * request base path (servlet path) and the original path for the Vaadin
     * resource.
     * <p>
     * If the resource is targeted to context root with VAADIN prefix, the path
     * part before the VAADIN is chopped of since that has been calculated to be
     * relative to our context and would target context root instead of the
     * serving servlet.
     *
     * @param basePath
     *            full servlet path, received as part of the bootstrap request.
     *            Needs to be the client-side path used, to get around proxies.
     * @param path
     *            original resource path
     * @return new resource path, relative to basePath
     */
    protected String modifyPath(String basePath, String path) {
        int vaadinIndex = path.indexOf(Constants.VAADIN_MAPPING);
        String suffix = path;
        if (vaadinIndex > 0) {
            suffix = suffix.substring(vaadinIndex);
        }
        return URI.create(checkURL(basePath + suffix)).toString();
    }

    private String checkURL(String url) {
        if (url == null) {
            return null;
        }
        if (url.contains("\"")) {
            throw new IllegalStateException(
                    "URL '" + url + "' may not contain double quotes");
        }
        return url;
    }

    private boolean hasWebComponentConfigurations(VaadinRequest request) {
        WebComponentConfigurationRegistry registry = WebComponentConfigurationRegistry
                .getInstance(request.getService().getContext());
        return registry.hasConfigurations();
    }

    private static String inlineHTML(String html) {
        // Format the received html into a form which will fit nicely as a
        // one-liner to .innerHTML="{html}", since we cannot use
        // ES6 back-ticks (``) for multi-line text
        return html
                // escape backslashes
                .replace("\\", "\\\\")
                // escape quotes
                .replace("\"", "\\\"")
                // change CDATA comment style for one-lining
                .replace("//<![CDATA[", "/*<![CDATA[*/")
                .replace("//]]>", "/*]]>*/")
                // get rid of all the unnecessary white-space
                .replace("\r", "").replaceAll("\\s{2,}", "").replace("\t", "")
                .replace("\n", "\\n");
    }

    /**
     * Returns the service url needed for initialising the UI.
     *
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @return Service url for the given request.
     */
    protected String getServiceUrl(VaadinRequest request,
            VaadinResponse response) {
        // get service url from 'url' parameter
        String url = request.getParameter(REQ_PARAM_URL);
        // if 'url' parameter was not available, use request url
        if (url == null) {
            url = getRequestUrl(request);
        }
        return url
                // +1 is to keep the trailing slash
                .substring(0, url.indexOf(PATH_PREFIX) + 1)
                // replace http:// or https:// with // to work with https://
                // proxies
                // which proxies to the same http:// url
                .replaceFirst("^" + ".*://", "//");
    }

    /**
     * Handles WebComponents resynchronization request
     *
     * @param context
     *            the bootstrap context object
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @return true if request has been handled, false otherwise
     */
    protected boolean handleWebComponentResyncRequest(BootstrapContext context,
            VaadinRequest request, VaadinResponse response) {

        if (!HandlerHelper.isRequestType(request,
                HandlerHelper.RequestType.WEBCOMPONENT_RESYNC)) {
            return false;
        }

        ObjectNode json = new UidlWriter().createUidl(context.getUI(), true,
                true);
        json.put(ApplicationConstants.UI_ID, context.getUI().getUIId());
        json.put(ApplicationConstants.UIDL_SECURITY_TOKEN_ID,
                context.getUI().getCsrfToken());
        json.put(ApplicationConstants.UIDL_PUSH_ID,
                context.getUI().getSession().getPushId());
        String responseString = "for(;;);[" + json + "]";

        try {
            VaadinService service = request.getService();
            service.writeUncachedStringResponse(response,
                    JsonConstants.JSON_CONTENT_TYPE, responseString);
        } catch (IOException e) {
            getLogger().error("Error writing JSON to response", e);
        }

        return true;
    }

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(WebComponentBootstrapHandler.class.getName());
    }
}
