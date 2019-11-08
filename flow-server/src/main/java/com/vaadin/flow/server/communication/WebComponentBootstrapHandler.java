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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.webcomponent.WebComponentUI;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ServletHelper;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import static com.vaadin.flow.shared.ApplicationConstants.CONTENT_TYPE_TEXT_JAVASCRIPT_UTF_8;
import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Bootstrap handler for WebComponent requests.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public class WebComponentBootstrapHandler extends BootstrapHandler {
    private static final String REQ_PARAM_URL = "url";
    private static final String PATH_PREFIX = "/web-component/web-component";
    private static final Pattern PATH_PATTERN =
            Pattern.compile(".*" + PATH_PREFIX + "-(ui|bootstrap)\\.(js|html)$");

    private static class WebComponentBootstrapContext extends BootstrapContext {

        private WebComponentBootstrapContext(VaadinRequest request,
                                             VaadinResponse response, UI ui,
                                             Function<VaadinRequest, String> callback) {
            super(request, response, ui.getInternals().getSession(), ui, callback);
        }

        @Override
        public <T extends Annotation> Optional<T> getPageConfigurationAnnotation(
                Class<T> annotationType) {
            WebComponentConfigurationRegistry registry = WebComponentConfigurationRegistry
                    .getInstance(getRequest().getService().getContext());
            return registry.getEmbeddedApplicationAnnotation(annotationType);
        }

        @Override
        protected Optional<ThemeDefinition> getTheme() {
            Optional<Theme> optionalTheme = getPageConfigurationAnnotation(Theme.class);
            return optionalTheme.map(ThemeDefinition::new);
        }
    }

    /**
     * Creates a new bootstrap handler with default page builder.
     */
    public WebComponentBootstrapHandler() {
        super();
    }

    /**
     * Creates a new bootstrap handler, allowing to use custom page builder.
     * @param pageBuilder Page builder to use.
     */
    protected WebComponentBootstrapHandler(PageBuilder pageBuilder) {
        super(pageBuilder);
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            return false;
        }
        return (PATH_PATTERN.matcher(pathInfo).find());
    }

    /**
     * Returns the request's base url to use in constructing and initialising ui.
     * @param request Request to the url for.
     * @return Request's url.
     */
    protected String getRequestUrl(VaadinRequest request) {
        return ((VaadinServletRequest)request).getRequestURL().toString();
    }

    @Override
    protected BootstrapContext createAndInitUI(
            Class<? extends UI> uiClass, VaadinRequest request,
            VaadinResponse response, VaadinSession session) {

        if(!canHandleRequest(request)) {
            throw new IllegalStateException("Unexpected request URL '"
                    + getRequestUrl(request) + "' in the bootstrap handler for web "
                    + "component UI which should handle path "
                    + PATH_PATTERN.toString());
        }

        final String serviceUrl = getServiceUrl(request, response);

        BootstrapContext context = super.createAndInitUI(WebComponentUI.class,
                request, response, session);
        JsonObject config = context.getApplicationParameters();

        String pushURL = context.getSession().getConfiguration().getPushURL();
        if (pushURL == null) {
            pushURL = serviceUrl;
        } else {
            try {
                URI uri = new URI(serviceUrl);
                pushURL = uri.resolve(new URI(pushURL)).toASCIIString();
            } catch (URISyntaxException exception) {
                throw new IllegalStateException(String.format(
                        "Can't resolve pushURL '%s' based on the service URL '%s'",
                        pushURL, serviceUrl), exception);
            }
        }
        PushConfiguration pushConfiguration = context.getUI()
                .getPushConfiguration();
        pushConfiguration.setPushUrl(pushURL);

        assert serviceUrl.endsWith("/");
        config.put(ApplicationConstants.SERVICE_URL, serviceUrl);
        config.put(ApplicationConstants.APP_WC_MODE, true);
        WebComponentConfigurationRegistry registry = WebComponentConfigurationRegistry
                .getInstance(request.getService().getContext());

        JsonArray tags = registry.getConfigurations().stream()
                .map(conf -> Json.create(conf.getTag()))
                .collect(JsonUtils.asArray());
        config.put("webcomponents", tags);
        return context;
    }

    @Override
    protected BootstrapContext createBootstrapContext(VaadinRequest request,
                                                      VaadinResponse response, UI ui, Function<VaadinRequest, String> callback) {
        return new WebComponentBootstrapContext(request, response, ui, callback);
    }


    @Override
    public boolean synchronizedHandleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response) throws IOException {
        if (session.getService().getDeploymentConfiguration().isCompatibilityMode()) {
            return super.synchronizedHandleRequest(session, request, response);
        } else {
            // Find UI class
            Class<? extends UI> uiClass = getUIClass(request);

            BootstrapContext context = createAndInitUI(uiClass, request, response,
                    session);

            ServletHelper.setResponseNoCacheHeaders(response::setHeader,
                    response::setDateHeader);

            String serviceUrl = getServiceUrl(request, response);

            Document document = getPageBuilder().getBootstrapPage(context);
            writeBootstrapPage(response, document.head(), serviceUrl);
            return true;
        }
    }

    /**
     * Copies the {@link org.jsoup.nodes.Element Elements} found in the given
     * {@code head} elements into the head of the embedding website using
     * JavaScript. Drops {@code <base>} element.
     *
     * @param response
     *         {@link com.vaadin.flow.server.VaadinResponse} into which the
     *         script is written
     * @param head
     *         head element of Vaadin Bootstrap page. The child elements are
     *         copied into the embedding page's head using JavaScript.
     * @param serviceUrl
     *         base path to use for the head elements' URLs
     * @throws IOException
     *         if writing fails
     */
    private void writeBootstrapPage(
            VaadinResponse response, Element head, String serviceUrl) throws IOException {
        writeBootstrapPage(CONTENT_TYPE_TEXT_JAVASCRIPT_UTF_8, response, head, serviceUrl);
    }

    /**
     * Copies the {@link org.jsoup.nodes.Element Elements} found in the given
     * {@code head} elements into the head of the embedding website using
     * JavaScript. Drops {@code <base>} element.
     *
     * @param contentType
     *          Content type of the response.
     * @param response
     *         {@link com.vaadin.flow.server.VaadinResponse} into which the
     *         script is written
     * @param head
     *         head element of Vaadin Bootstrap page. The child elements are
     *         copied into the embedding page's head using JavaScript.
     * @param serviceUrl
     *         base path to use for the head elements' URLs
     * @throws IOException
     *         if writing fails
     */
    protected void writeBootstrapPage(String contentType, VaadinResponse response, Element head, String serviceUrl) throws IOException {
        /*
            The elements found in the head are reconstructed using JavaScript and
            document.createElement(...). Since innerHTML and related methods
            do not execute <script> blocks, the contents cannot be copied as
            pure string into the head. The each element is created separately
            and then attributes are copied and innerHTML set, if the element
            has innerHTML. The innerHTMLs are in-lined for easier copying.
        */
        response.setContentType(contentType);
        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(response.getOutputStream(), UTF_8))) {
            String varName = "headElem"; // generated head element
            writer.append("var ").append(varName).append("=null;");
            for (Element element : head.children()) {
                if (elementShouldNotBeTransferred(element)) {
                    continue;
                }
                writer.append(varName).append("=");
                writer.append("document.createElement('").append(element.tagName()).append("');");
                transferAttribute(writer, varName, element, serviceUrl);
                // set cleaned html as innerHTML for the element
                String elementHtml = element.html();
                if (elementHtml != null && elementHtml.length() > 0) {
                    writer.append(varName).append(".innerHTML=\"")
                        .append(inlineHTML(elementHtml)).append("\";");
                }
                writer.append("document.head.appendChild(").append(varName).append(");");
            }
        }
    }

    private static boolean elementShouldNotBeTransferred(Element element) {
        // we skip base href adjustment, since we are in a 3rd party
        // context
        if ("base".equals(element.tagName())) {
            return true;
        } else {
            // embedding context should not provide polyfill, it is left to the end-user
            return "script".equals(element.tagName())
                    && element.attr("src").contains("webcomponents-loader.js");
        }
    }

    /**
     * Creates a javascript which copies attributes from the {@code element} to
     * the created DOM element identified by {@code elementRef}. If {@code
     * element} contains a {@code src} attribute, its path is prefixed with
     * {@code basePath}.
     *
     * @param writer
     *         response writer
     * @param elementRef
     *         variable name of the element in javascript
     * @param element
     *         jsoup element from which to copy the attributes
     * @param basePath
     *         base path of {@code src} attributes (service url's path)
     * @throws IOException
     *         if {@code writer} is unable to write
     */
    private void transferAttribute(
            Writer writer, String elementRef, Element element,
            String basePath) throws IOException {
        for (Attribute attribute : element.attributes()) {
            writer.append(elementRef).append(".setAttribute('").append(attribute.getKey()).append("',");
            if (attribute.getValue() == null) {
                writer.append("''");
            } else {
                String path = attribute.getValue();
                if ("src".equals(attribute.getKey())) {
                    path = modifyPath(basePath, path);
                }
                writer.append("'").append(path).append("'");
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
        if (vaadinIndex > 0) {
            String subPath = path.substring(vaadinIndex);
            return URI.create(basePath + subPath).toString();
        } else {
            return URI.create(basePath + path).toString();

        }
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
                .replaceAll("\\s{2,}", "")
                .replace("\t", "")
                .replace("\n", "")
                .replace("\r", "");
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
            url = ((VaadinServletRequest) request).getRequestURL().toString();
        }
        return url
                // +1 is to keep the trailing slash
                .substring(0, url.indexOf(PATH_PREFIX) + 1)
                // replace http:// or https:// with // to work with https:// proxies
                // which proxies to the same http:// url
                .replaceFirst("^" + ".*://", "//");
    }
}
