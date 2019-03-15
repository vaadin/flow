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
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentGenerator;
import com.vaadin.flow.server.webcomponent.WebComponentBuilderRegistry;

/**
 * Request handler that supplies the script/html of the WebComponent matching
 * the given tag.
 */
public class WebComponentProvider extends SynchronizedRequestHandler {

    private static final String PATH_PREFIX = "/web-component/";
    private static final String SUFFIX = ".html";

    // exporter class to script/html
    private Map<Class<?>, String> cache;

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
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
                WebComponentBuilderRegistry.getInstance(
                        ((VaadinServletRequest) request).getServletContext())
                .getConfiguration(tag.get());

        if (optionalWebComponentConfiguration.isPresent()) {
            if (cache == null) {
                cache = new HashMap<>();
            }
            WebComponentConfiguration<? extends Component> webComponentConfiguration =
                    optionalWebComponentConfiguration.get();
            String generated;
            if (cache.containsKey(webComponentConfiguration.getClass())) {
                generated = cache.get(webComponentConfiguration.getClass());
            } else {
                String uiElement;
                if (session.getConfiguration().getRootElementId().isEmpty()) {
                    uiElement = "document.body";
                } else {
                    uiElement = "document.getElementById('"
                            + session.getConfiguration().getRootElementId()
                            + "')";
                }

                generated = WebComponentGenerator.generateModule(uiElement,
                        tag.get(), webComponentConfiguration, request);
                cache.put(webComponentConfiguration.getClass(), generated);
            }

            IOUtils.write(generated, response.getOutputStream(),
                    StandardCharsets.UTF_8);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "No such web component");
        }

        return true;
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
