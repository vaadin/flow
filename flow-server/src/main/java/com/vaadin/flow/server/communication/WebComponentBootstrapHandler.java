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

import javax.servlet.ServletContext;
import java.lang.annotation.Annotation;
import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.webcomponent.WebComponentUI;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.shared.ApplicationConstants;

import elemental.json.JsonObject;

/**
 * Bootstrap handler for WebComponent requests.
 *
 * @author Vaadin Ltd.
 */
public class WebComponentBootstrapHandler extends BootstrapHandler {

    private static final String PATH_PREFIX = "/web-component/web-component-ui.html";

    private static class WebComponentBootstrapContext extends BootstrapContext {

        private WebComponentBootstrapContext(VaadinRequest request,
                                             VaadinResponse response, UI ui) {
            super(request, response, ui.getInternals().getSession(), ui);
        }

        @Override
        public <T extends Annotation> Optional<T> getPageConfigurationAnnotation(
                Class<T> annotationType) {
            ServletContext servletContext = ((VaadinServletService) getRequest()
                    .getService()).getServlet().getServletContext();
            WebComponentConfigurationRegistry registry = WebComponentConfigurationRegistry
                    .getInstance(servletContext);
            return registry.getEmbeddedApplicationAnnotation(annotationType);
        }
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        VaadinServletRequest servletRequest = (VaadinServletRequest) request;
        String pathInfo = servletRequest.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            return false;
        }

        return (pathInfo.equals(PATH_PREFIX));
    }

    @Override
    protected BootstrapContext createAndInitUI(Class<? extends UI> uiClass,
                                               VaadinRequest request, VaadinResponse response,
                                               VaadinSession session) {
        BootstrapContext context = super.createAndInitUI(WebComponentUI.class,
                request, response, session);
        JsonObject config = context.getApplicationParameters();

        VaadinServletRequest servletRequest = (VaadinServletRequest) request;
        String requestURL = servletRequest.getRequestURL().toString();

        if (!requestURL.endsWith(PATH_PREFIX)) {
            throw new IllegalStateException("Unexpected request URL '"
                    + requestURL
                    + "' in the bootstrap handler for web component UI which should handle path "
                    + PATH_PREFIX);
        }
        // remove path prefix but keep the trailing slash
        String serviceUrl = requestURL.substring(0,
                requestURL.length() - PATH_PREFIX.length() + 1);
        // replace http:// or https:// with // to work with https:// proxies which proxies to the same http:// url
        serviceUrl = serviceUrl.replaceFirst("^.*://", "//");

        assert serviceUrl.endsWith("/");
        config.put(ApplicationConstants.SERVICE_URL, serviceUrl);
        return context;
    }

    @Override
    protected BootstrapContext createBootstrapContext(VaadinRequest request,
                                                      VaadinResponse response, UI ui) {
        return new WebComponentBootstrapContext(request, response, ui);
    }
}
