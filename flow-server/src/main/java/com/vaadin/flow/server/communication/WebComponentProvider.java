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
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.server.webcomponent.WebComponentGenerator;

/**
 * Request handler that supplies the script/html of the web component matching
 * the given tag.
 *
 * @author Vaadin Ltd.
 */
public class WebComponentProvider extends SynchronizedRequestHandler {

    private static final String WEB_COMPONENT_PATH = "web-component/";
    private static final String PATH_PREFIX = "/" + WEB_COMPONENT_PATH;
    private static final String SUFFIX = ".html";

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

        Optional<WebComponentConfiguration<? extends Component>> optionalWebComponentConfiguration =
                WebComponentConfigurationRegistry.getInstance(
                        ((VaadinServletRequest) request).getServletContext())
                        .getConfiguration(tag.get());

        if (optionalWebComponentConfiguration.isPresent()) {
            if (cache == null) {
                cache = new HashMap<>();
            }
            WebComponentConfiguration<? extends Component> webComponentConfiguration = optionalWebComponentConfiguration
                    .get();
            String generated = cache.computeIfAbsent(tag.get(),
                    moduleTag -> generateModule(webComponentConfiguration,
                            session, servletRequest));

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
                    getFrontendPath(request));
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
        StringBuilder builder = new StringBuilder("var scriptUri = ");
        builder.append(jsParentRef);
        builder.append(".src;");
        builder.append("var indx = scriptUri.lastIndexOf('");
        builder.append(WEB_COMPONENT_PATH);
        builder.append("');");
        builder.append("var embeddedWebApp = scriptUri.substring(0, indx);");
        builder.append("var js = document.createElement('script');"
                + "js.setAttribute('type','text/javascript');"
                + "js.setAttribute('src', embeddedWebApp+'");
        builder.append(polyFillsUri);
        builder.append("'); document.head.insertBefore(js, ");
        builder.append(jsParentRef);
        builder.append(".nextSibling);");
        return builder.toString();
    }

    private String generateUiImport(String jsParentRef) {
        StringBuilder builder = new StringBuilder("var scriptUri = ");
        builder.append(jsParentRef);
        builder.append(".src;");
        builder.append("var indx = scriptUri.lastIndexOf('");
        builder.append(WEB_COMPONENT_PATH);
        builder.append("');");
        builder.append("var uiUri = scriptUri.substring(0, indx+");
        builder.append(WEB_COMPONENT_PATH.length()).append(");");
        builder.append("var link = document.createElement('link');"
                + "link.setAttribute('rel','import');"
                + "link.setAttribute('href', uiUri+'web-component-ui.html');");
        builder.append("document.head.insertBefore(link, ");
        builder.append(jsParentRef);
        builder.append(".nextSibling);");
        return builder.toString();
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
        String tag = pathInfo.substring(PATH_PREFIX.length());
        if (!tag.endsWith(SUFFIX)) {
            tag = null;
        } else {
            tag = tag.substring(0, tag.length() - SUFFIX.length());
            if (!tag.contains("-")) {
                tag = null;
            }
        }
        return Optional.ofNullable(tag);
    }
}
