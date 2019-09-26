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
package com.vaadin.flow.server;
import java.util.stream.StreamSupport;

import org.jsoup.nodes.Element;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.JavaScriptBootstrapHandler;
import com.vaadin.flow.shared.ApplicationConstants;

import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

/**
 * Bootstrap listener that inserts the initial application configuration into
 * the client-side bootstrapping page.
 *
 * To enable this feature add tje class name to the listeners configuration
 * file:
 * `src/main/resources/META-INF/services/com.vaadin.flow.server.VaadinServiceInitListener`.
 *
 */
public class ClientIndexInitialListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addClientIndexBootstrapListener(page -> {
            JavaScriptBootstrapHandler jsHandler = (JavaScriptBootstrapHandler) StreamSupport
                    .stream(CurrentInstance.get(VaadinService.class)
                            .getRequestHandlers().spliterator(), false)
                    .filter(r -> r instanceof JavaScriptBootstrapHandler)
                    .findFirst().get();

            VaadinRequest request = page.getVaadinRequest();
            request.setAttribute(
                    ApplicationConstants.REQUEST_LOCATION_PARAMETER,
                    request.getPathInfo());

            JsonObject initial = jsHandler.getAppConfig(request,
                    page.getVaadinResponse(), page.getVaadinSession());

            Element elm = new Element("script");
            elm.attr("initial", "");
            elm.text("window.Vaadin = {Flow : {initial: "
                    + JsonUtil.stringify(initial) + "}}");
            page.getDocument().head().insertChildren(0, elm);
        });
    }

    /**
     * Decides whether the page should be modified. By default it's always
     * modified. Override this method to modify only certain cases.
     *
     * @param request
     *            the vaadin request
     * @return true in case the initial json should be added to the hosted page
     */
    public boolean isValidRoute(VaadinRequest request) {
        return true;
    }
}
