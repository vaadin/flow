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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.BootstrapHandler.BootstrapUriResolver;
import com.vaadin.flow.server.ServletHelper;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.server.webcomponent.WebComponentGenerator;

/**
 * Request handler that supplies the script/html of the web component matching
 * the given tag.
 *
 * @author Vaadin Ltd.
 * @since
 */
public class WebComponentProvider extends SynchronizedRequestHandler {

    private static final String WEB_COMPONENT_PATH = "web-component/";
    private static final String PATH_PREFIX = "/" + WEB_COMPONENT_PATH;
    private static final String HTML_SUFFIX = ".html";
    private static final Pattern TAG_PATTERN = Pattern.compile(".*/([^/]+)(\\.js|\\.html)$");

    // tag name -> generated html
    private Map<String, String> cache;

    @Override
    public boolean synchronizedHandleRequest(
            VaadinSession session, VaadinRequest request, VaadinResponse response) throws IOException {
        VaadinServletRequest servletRequest = (VaadinServletRequest) request;
        String pathInfo = servletRequest.getPathInfo();

        if (pathInfo == null || pathInfo.isEmpty()) {
            return false;
        }

        if (!pathInfo.startsWith(PATH_PREFIX)) {
            return false;
        }

        Optional<String> tag = parseTag(pathInfo);
        if (!tag.isPresent()) {
            LoggerFactory.getLogger(WebComponentProvider.class).info(
                    "Received web-component request for non-custom element with request path {}",
                    pathInfo);
            return false;
        }
        WebComponentConfigurationRegistry registry =
                WebComponentConfigurationRegistry.getInstance(
                        ((VaadinServletRequest) request).getServletContext());

        Optional<WebComponentConfiguration<? extends Component>> optionalWebComponentConfiguration =
                registry.getConfiguration(tag.get());

        if (optionalWebComponentConfiguration.isPresent()) {
            if (cache == null) {
                cache = new HashMap<>();
            }
            WebComponentConfiguration<? extends Component> webComponentConfiguration =
                    optionalWebComponentConfiguration.get();

            String generated = "";
            if (FrontendUtils.isBowerLegacyMode()) {
                generated = cache.computeIfAbsent(tag.get(),
                        moduleTag -> generateModule(webComponentConfiguration,
                                session, servletRequest));
            } else {
                generated = cache.computeIfAbsent(tag.get(),
                        moduleTag -> generateNPMImport());
            }

            IOUtils.write(generated, response.getOutputStream(),
                    StandardCharsets.UTF_8);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "No such web component");
        }

        return true;
    }

    private String generateModule(
            WebComponentConfiguration<? extends Component> configuration,
            VaadinSession session, VaadinServletRequest request) {
        if (session.getConfiguration().useCompiledFrontendResources()) {
            return generateCompiledUIDeclaration(session, request);
        } else {
            return WebComponentGenerator.generateModule(configuration,
                    getFrontendPath(request),
                    FrontendUtils.isBowerLegacyMode());
        }
    }

    private String generateCompiledUIDeclaration(VaadinSession session,
                                                 VaadinRequest request) {
        String contextRootRelativePath = ServletHelper
                .getContextRootRelativePath(request) + "/";

        BootstrapUriResolver resolver = new BootstrapUriResolver(
                contextRootRelativePath, session);
        String polyFillsUri = resolver
                .resolveVaadinUri(BootstrapHandler.POLYFILLS_JS);
        // <code>thisScript</code> below allows to refer the currently executing
        // script
        return "var scripts = document.getElementsByTagName( 'script' );"
                + "var thisScript = scripts[ scripts.length - 1 ];"
                + generateAddPolyfillsScript(polyFillsUri, "thisScript")
                + generateUiImport("thisScript");
    }

    private String generateAddPolyfillsScript(String polyFillsUri,
                                              String jsParentRef) {
        return "var scriptUri = " + jsParentRef + ".src;"
                + "var indx = scriptUri.lastIndexOf('" + WEB_COMPONENT_PATH + "');"
                + "var embeddedWebApp = scriptUri.substring(0, indx);"
                + "var js = document.createElement('script');"
                + "js.setAttribute('type','text/javascript');"
                + "js.setAttribute('src', embeddedWebApp+'" + polyFillsUri + "');"
                + "document.head.insertBefore(js, " + jsParentRef + ".nextSibling);";
    }

    private String generateUiImport(String jsParentRef) {
        return "var scriptUri = " + jsParentRef + ".src;"
                + "var indx = scriptUri.lastIndexOf('" + WEB_COMPONENT_PATH + "');"
                + "var uiUri = scriptUri.substring(0, indx+" + WEB_COMPONENT_PATH.length() + ");"
                + "var link = document.createElement('link');"
                + "link.setAttribute('rel','import');"
                + "link.setAttribute('href', uiUri+'web-component-ui.html');"
                + "document.head.insertBefore(link, " + jsParentRef + ".nextSibling);";
    }

    private String generateNPMImport() {
        // get the running script
        return "var scripts = document.head.getElementsByTagName( 'script' );"
                + "var thisScript = scripts[ scripts.length - 1 ];"
                + "var scriptUri = thisScript.src;"
                + "var index = scriptUri.lastIndexOf('" + WEB_COMPONENT_PATH + "');"
                + "var context = scriptUri.substring(0, index+" + WEB_COMPONENT_PATH.length() + ");"
                // figure out if we have already bootstrapped Vaadin client & ui
                + "var bootstrapped = false;"
                + "var bootstrapAddress=context+'web-component-ui.html';"
                + "for (var ii = 0; ii < scripts.length; ii++){"
                + "if (scripts[ii].src === bootstrapAddress){"
                + "bootstrapped=true; break;"
                + "}}"
                // if no bootstrap -> bootstrap
                + "if (!bootstrapped){"
                + "var uiScript = document.createElement('script');"
                + "uiScript.setAttribute('type','text/javascript');"
                + "uiScript.setAttribute('src', bootstrapAddress);"
                + "document.head.appendChild(uiScript);"
                + "}";
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

    private static Optional<String> parseTag(String pathInfo) {
        Matcher matcher = TAG_PATTERN.matcher(pathInfo);
        String tag = null;
        if (matcher.find()) {
            tag = matcher.group(1);
        }
        return Optional.ofNullable(tag);

//        String tag = pathInfo.substring(PATH_PREFIX.length());
//        if (!tag.endsWith(HTML_SUFFIX)) {
//            tag = null;
//        } else {
//            tag = tag.substring(0, tag.length() - HTML_SUFFIX.length());
//            if (!tag.contains("-")) {
//                tag = null;
//            }
//        }
//        return Optional.ofNullable(tag);
    }
}
