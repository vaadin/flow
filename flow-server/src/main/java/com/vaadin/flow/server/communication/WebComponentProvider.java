/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.SessionExpiredHandler;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;

import static com.vaadin.flow.shared.ApplicationConstants.CONTENT_TYPE_TEXT_JAVASCRIPT_UTF_8;

/**
 * Request handler that supplies the script/html of the web component matching
 * the given tag.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public class WebComponentProvider extends SynchronizedRequestHandler
        implements SessionExpiredHandler {
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
    private ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return isWebComponentPath(request.getPathInfo())
                && hasWebComponentConfigurations(request);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is only called when the request has a session. A web component
     * script request without a session is served through
     * {@link #handleSessionExpired(VaadinRequest, VaadinResponse)} instead.
     * Override
     * {@link #handleWebComponentRequest(VaadinRequest, VaadinResponse)} to
     * customize how the script is served in both cases.
     */
    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        return handleWebComponentRequest(request, response);
    }

    @Override
    public boolean handleSessionExpired(VaadinRequest request,
            VaadinResponse response) throws IOException {
        // The script is the same for every session. It is served without one
        // so that a host page loading the scripts of several web components in
        // parallel does not get a new session from each of them, leaving the
        // browser with a cookie for a session other than the one its embedded
        // UI was created in.
        return isWebComponentScriptRequest(request)
                && handleWebComponentRequest(request, response);
    }

    /**
     * Checks whether the request asks for the script of an exported web
     * component. Such a request does not need a session, see
     * {@link #handleSessionExpired(VaadinRequest, VaadinResponse)}.
     *
     * @param request
     *            the request to check
     * @return {@code true} if the request is for a web component script
     */
    public static boolean isWebComponentScriptRequest(VaadinRequest request) {
        String pathInfo = request.getPathInfo();
        return isWebComponentPath(pathInfo)
                && new ComponentInfo(pathInfo).isJS()
                && hasWebComponentConfigurations(request);
    }

    private static boolean isWebComponentPath(String pathInfo) {
        return pathInfo != null && pathInfo.startsWith(PATH_PREFIX)
                && !WebComponentBootstrapHandler.PATH_PATTERN.matcher(pathInfo)
                        .find();
    }

    /**
     * Handles a request for a web component script. This is called both for
     * requests that have a session and for requests that do not, so it must not
     * depend on {@link VaadinSession#getCurrent()} or on the session lock.
     *
     * @param request
     *            the request to handle
     * @param response
     *            the response to write the script to
     * @return {@code true} if the request was handled, {@code false} otherwise
     * @throws IOException
     *             if writing the response fails
     * @since 25.4
     */
    protected boolean handleWebComponentRequest(VaadinRequest request,
            VaadinResponse response) throws IOException {
        String pathInfo = request.getPathInfo();

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

        if (componentInfo.isHTML()) {
            LoggerFactory.getLogger(WebComponentProvider.class).info(
                    "Received web-component request for html component in npm"
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
            response.setContentType(CONTENT_TYPE_TEXT_JAVASCRIPT_UTF_8);
            responder = () -> generateNPMResponse(
                    webComponentConfiguration.getTag(), request, response);
            if (cache == null) {
                generated = responder.get();
            } else {
                generated = cache.computeIfAbsent(componentInfo.tag,
                        moduleTag -> responder.get());
            }

            response.getOutputStream()
                    .write(generated.getBytes(StandardCharsets.UTF_8));
        } else {
            response.sendError(HttpStatusCode.NOT_FOUND.getCode(),
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
     * @since 2.2
     */
    public boolean isCacheEnabled() {
        return cache != null;
    }

    /**
     * Enable / disable bootstrap HTML fragment caching based on component tag.
     * Calling this method has the side effect of always clearing the cache.
     *
     * @param cacheEnabled
     *            whether bootstrap fragments should be cached per tag
     * @since 2.2
     */
    public void setCacheEnabled(boolean cacheEnabled) {
        if (cacheEnabled) {
            cache = new ConcurrentHashMap<>();
        } else {
            cache = null;
        }
    }

    /**
     * Generate the npm response for the web component.
     * <p>
     * The script is served without a session when the request has none, so this
     * may run with no {@link VaadinSession} available. The result is cached per
     * tag unless caching is disabled with {@link #setCacheEnabled(boolean)}.
     *
     * @param tagName
     *            tag name of component
     * @param request
     *            current VaadinRequest
     * @param response
     *            current VaadinResponse
     * @return npm response script
     * @since 2.2
     */
    protected String generateNPMResponse(String tagName, VaadinRequest request,
            VaadinResponse response) {
        // get the running script
        boolean productionMode = request.getService()
                .getDeploymentConfiguration().isProductionMode();

        return IndexHtmlRequestHandler.featureFlagsInitializer(request)
                + getThisScript(tagName) + "var scriptUri = thisScript.src;"
                + "var index = scriptUri.lastIndexOf('" + WEB_COMPONENT_PATH
                + "');" + "var context = scriptUri.substring(0, index+"
                + WEB_COMPONENT_PATH.length() + ");"
                // figure out if we have already bootstrapped Vaadin client & ui
                + "var bootstrapAddress=context+'web-component-bootstrap.js';"
                // add the request address as a url parameter (used to get
                // service url)
                + bootstrapNpm(productionMode);
    }

    protected String bootstrapNpm(boolean productionMode) {

        String bootstrapJS = "var bootstrapped = false;\n"
                + "bootstrapAddress+='?url='+bootstrapAddress;"
                // check if a script with the bootstrap source already exits
                + "var scripts = document.getElementsByTagName('script');"
                + "for (var ii = 0; ii < scripts.length; ii++){"
                + "  if (scripts[ii].getAttribute('data-src') === bootstrapAddress){"
                + "    bootstrapped=true; break;" + "  }" + "}"
                // if no bootstrap -> bootstrap
                + "if (!bootstrapped){"
                + "  var uiScript = document.createElement('script');"
                + "  uiScript.setAttribute('type','text/javascript');"
                + "  uiScript.setAttribute('data-src', bootstrapAddress);"
                + "  document.head.appendChild(uiScript);";
        if (productionMode) {
            bootstrapJS += "  uiScript.setAttribute('src', bootstrapAddress);";
        } else {
            // With dev-bundle we need to wait the bundle is created before
            // downloading the resource by adding the script source
            bootstrapJS += """
                        const bootstrapSrc = bootstrapAddress;
                        const delay = 200;
                        const poll = async () => {
                          try {
                            const response = await fetch(bootstrapSrc, { method: 'HEAD', credentials: 'include', headers: { 'X-DevModePoll': 'true' } });
                            if (response.headers.has('X-DevModePending')) {
                              setTimeout(poll, delay);
                            } else {
                              uiScript.setAttribute('src', bootstrapSrc);
                            }
                          } catch (e) {
                            setTimeout(poll, delay);
                          }
                        };
                        poll();
                    """;
        }
        bootstrapJS += "}";
        return bootstrapJS;
    }

    private static boolean hasWebComponentConfigurations(
            VaadinRequest request) {
        WebComponentConfigurationRegistry registry = WebComponentConfigurationRegistry
                .getInstance(request.getService().getContext());
        return registry.hasConfigurations();
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
