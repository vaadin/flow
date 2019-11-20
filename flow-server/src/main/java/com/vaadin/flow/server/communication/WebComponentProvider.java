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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.BootstrapHandler.BootstrapUriResolver;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.server.webcomponent.WebComponentGenerator;

import static com.vaadin.flow.shared.ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8;
import static com.vaadin.flow.shared.ApplicationConstants.CONTENT_TYPE_TEXT_JAVASCRIPT_UTF_8;

/**
 * Request handler that supplies the script/html of the web component matching
 * the given tag.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public class WebComponentProvider extends SynchronizedRequestHandler {
    private static final String WEB_COMPONENT_PATH = "web-component/";
    private static final String PATH_PREFIX = "/" + WEB_COMPONENT_PATH;
    private static final String HTML_EXTENSION = "html";
    private static final String JS_EXTENSION = "js";
    /**
     * Matches paths ending in words separated by at least one dash, and ending
     * in either .js or .html (words cannot contain underscore)
     *
     * @see com.vaadin.flow.server.communication.WebComponentProvider.ComponentInfo
     *      for group usage
     */
    private static final Pattern TAG_PATTERN = Pattern
            .compile(".*/(([\\w&&[^_]]+-)+([\\w&&[^_]]+))\\." + "("
                    + JS_EXTENSION + "|" + HTML_EXTENSION + ")$");

    // tag name -> generated html
    private Map<String, String> cache = new HashMap<>();

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.isEmpty()) {
            return false;
        }

        if (!pathInfo.startsWith(PATH_PREFIX)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        String pathInfo = request.getPathInfo();

        final boolean compatibilityMode = session.getService()
                .getDeploymentConfiguration().isCompatibilityMode();
        final ComponentInfo componentInfo = new ComponentInfo(pathInfo);

        if (!componentInfo.hasExtension()) {
            LoggerFactory.getLogger(WebComponentProvider.class)
                    .info("Received web-component request without extension "
                            + "information (.js/.html) with request path {}",
                            pathInfo);
            return false;
        }

        if (componentInfo.getTag() == null) {
            LoggerFactory.getLogger(WebComponentProvider.class).info(
                    "Received web-component request for non-custom element with request path {}",
                    pathInfo);
            return false;
        }

        if (componentInfo.isHTML() && !compatibilityMode) {
            LoggerFactory.getLogger(WebComponentProvider.class).info(
                    "Received web-component request for html component in npm"
                            + " mode with request path {}",
                    pathInfo);
            return false;
        }

        if (componentInfo.isJS() && compatibilityMode) {
            LoggerFactory.getLogger(WebComponentProvider.class).info(
                    "Received web-component request for js component in compatibility"
                            + " mode with request path {}",
                    pathInfo);
            return false;
        }

        WebComponentConfigurationRegistry registry = WebComponentConfigurationRegistry
                .getInstance(request.getService().getContext());

        Optional<WebComponentConfiguration<? extends Component>> optionalWebComponentConfiguration = registry
                .getConfiguration(componentInfo.tag);

        if (optionalWebComponentConfiguration.isPresent()) {
            WebComponentConfiguration<? extends Component> webComponentConfiguration = optionalWebComponentConfiguration
                    .get();

            String generated;
            Supplier<String> responder;
            if (compatibilityMode) {
                responder = () ->
                        generateBowerResponse(webComponentConfiguration,
                                session, request, response);
            } else {
                response.setContentType(CONTENT_TYPE_TEXT_JAVASCRIPT_UTF_8);
                responder = () ->
                        generateNPMResponse(webComponentConfiguration.getTag(),
                                request, response);
            }
            if (cache == null) {
                generated = responder.get();
            } else {
                generated = cache.computeIfAbsent(componentInfo.tag,
                        moduleTag -> responder.get());
            }

            IOUtils.write(generated, response.getOutputStream(),
                    StandardCharsets.UTF_8);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "No web component for " + Optional
                            .ofNullable(componentInfo.tag).orElse("<null>"));
        }

        return true;
    }

    /**
     * Whether bootstrap HTML fragment are cached based on component tag.
     * Enabled by default.
     *
     * @return true iff bootstrap fragment caching is enabled
     */
    public boolean isCacheEnabled() {
        return cache != null;
    }

    /**
     * Enable / disable bootstrap HTML fragment caching based on component tag.
     * Calling this method has the side effect of always clearing the cache.
     *
     * @param cacheEnabled whether bootstrap fragments should be cached per tag
     */
    public void setCacheEnabled(boolean cacheEnabled) {
        if (cacheEnabled) {
            cache = new HashMap<>();
        } else {
            cache = null;
        }
    }

    private String generateBowerResponse(
            WebComponentConfiguration<? extends Component> configuration,
            VaadinSession session, VaadinRequest request,
            VaadinResponse response) {
        if (session.getConfiguration().useCompiledFrontendResources()) {
            response.setContentType(CONTENT_TYPE_TEXT_JAVASCRIPT_UTF_8);
            return generateCompiledUIDeclaration(session, request,
                    configuration.getTag());
        } else {
            response.setContentType(CONTENT_TYPE_TEXT_HTML_UTF_8);
            return WebComponentGenerator.generateModule(configuration,
                    getFrontendPath(request), true);
        }
    }

    private String generateCompiledUIDeclaration(VaadinSession session,
            VaadinRequest request, String tagName) {
        String contextRootRelativePath = request.getService()
                .getContextRootRelativePath(request);

        BootstrapUriResolver resolver = new BootstrapUriResolver(
                contextRootRelativePath, session);
        String polyFillsUri = resolver
                .resolveVaadinUri(BootstrapHandler.POLYFILLS_JS);

        // `thisScript` below allows to refer the currently executing script
        return getThisScript(tagName)
                + generateAddPolyfillsScript(polyFillsUri, "thisScript")
                + generateUiImport("thisScript");
    }

    private String generateAddPolyfillsScript(String polyFillsUri,
            String jsParentRef) {
        return "var scriptUri = " + jsParentRef + ".src;"
                + "var indx = scriptUri.lastIndexOf('" + WEB_COMPONENT_PATH
                + "');" + "var embeddedWebApp = scriptUri.substring(0, indx);"
                + "var js = document.createElement('script');"
                + "js.setAttribute('type','text/javascript');"
                + "js.setAttribute('src', embeddedWebApp+'" + polyFillsUri
                + "');" + "document.head.insertBefore(js, " + jsParentRef
                + ".nextSibling);";
    }

    private String generateUiImport(String jsParentRef) {
        return "var scriptUri = " + jsParentRef + ".src;"
                + "var indx = scriptUri.lastIndexOf('" + WEB_COMPONENT_PATH
                + "');" + "var uiUri = scriptUri.substring(0, indx+"
                + WEB_COMPONENT_PATH.length() + ");"
                + "var link = document.createElement('link');"
                + "link.setAttribute('rel','import');"
                + "link.setAttribute('href', uiUri+'web-component-ui.html');"
                + "document.head.insertBefore(link, " + jsParentRef
                + ".nextSibling);";
    }

    /**
     * Generate the npm response for the web component.
     *
     * @param tagName
     *         tag name of component
     * @param request
     *         current VaadinRequest
     * @param response
     *         current VaadinResponse
     * @return npm response script
     */
    protected String generateNPMResponse(String tagName, VaadinRequest request,
            VaadinResponse response) {
        // get the running script
        return getThisScript(tagName) + "var scriptUri = thisScript.src;"
                + "var index = scriptUri.lastIndexOf('" + WEB_COMPONENT_PATH
                + "');" + "var context = scriptUri.substring(0, index+"
                + WEB_COMPONENT_PATH.length() + ");"
                // figure out if we have already bootstrapped Vaadin client & ui
                + "var bootstrapAddress=context+'web-component-bootstrap.js';"
                // add the request address as a url parameter (used to get
                // service url)
                + bootstrapNpm();
    }

    protected String bootstrapNpm() {
        return "var bootstrapped = false;\n"
                + "bootstrapAddress+='?url='+bootstrapAddress;"
                // check if a script with the bootstrap source already exits
                + "var scripts = document.getElementsByTagName('script');"
                + "for (var ii = 0; ii < scripts.length; ii++){"
                + "  if (scripts[ii].src === bootstrapAddress){"
                + "    bootstrapped=true; break;" + "  }" + "}"
                // if no bootstrap -> bootstrap
                + "if (!bootstrapped){"
                + "  var uiScript = document.createElement('script');"
                + "  uiScript.setAttribute('type','text/javascript');"
                + "  uiScript.setAttribute('src', bootstrapAddress);"
                + "  document.head.appendChild(uiScript);" + "}";
    }

    private static String getFrontendPath(VaadinRequest request) {
        if (request == null) {
            return null;
        }
        String contextPath = request.getContextPath();
        if (contextPath.isEmpty()) {
            return "/frontend/";
        }
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        if (contextPath.endsWith("/")) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }
        return contextPath + "/frontend/";
    }

    private static String getThisScript(String tag) {
        return "var thisScript;" //
                + "if (document.currentScript) {" //
                + "  thisScript = document.currentScript;" //
                + "} else {" //
                + "  var elements = document.getElementsByTagName('script');" //
                + "  for (var ii = 0; ii < elements.length; ii++) {" //
                + "    var script = elements[ii];" //
                + "    if (script.src && script.src.indexOf('web-component/"
                + tag + "') != -1) {" //
                + "      thisScript = script;" //
                + "    }" //
                + "  };" //
                + "} ";
    }

    private static class ComponentInfo implements Serializable {
        final String tag;
        final String extension;

        private ComponentInfo(String pathInfo) {
            Matcher matcher = TAG_PATTERN.matcher(pathInfo);
            if (matcher.find()) {
                tag = matcher.group(1);
                // the group index of the extension is 4, since the inner
                // groups roll over with each new dash separated section;
                // group 2 will be the second last part of the component name
                // and group 3 will be the last part
                extension = matcher.group(4);
            } else {
                tag = null;
                extension = null;
            }
        }

        String getTag() {
            return tag;
        }

        boolean hasExtension() {
            return extension != null;
        }

        boolean isHTML() {
            return HTML_EXTENSION.equals(extension);
        }

        boolean isJS() {
            return JS_EXTENSION.equals(extension);
        }
    }
}
