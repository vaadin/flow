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

import org.jsoup.nodes.Element;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.shared.ApplicationConstants;

import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

/**
 * A bootstrap listener that inserts the initial UIDL fragment in the
 * <code>index.html</code> file.
 */
class BootstrapInitialInserter
        implements ClientIndexBootstrapListener {

    @Override
    public void modifyBootstrapPage(ClientIndexBootstrapPage page) {
        VaadinService vaadinService = CurrentInstance
                .get(VaadinService.class);

        if (!vaadinService.getBootstrapInitialPredicate()
                .includeInitial(page.getVaadinRequest())) {
            return;
        }

        VaadinRequest request = page.getVaadinRequest();
        request.setAttribute(
                ApplicationConstants.REQUEST_LOCATION_PARAMETER,
                request.getPathInfo());

        BootstrapHandler handler = page.getHandler();

        JsonObject initial = handler.getInitialJson(request,
                page.getVaadinResponse(), page.getVaadinSession());

        Element elm = new Element("script");
        elm.attr("initial", "");
        elm.text("window.Vaadin = {Flow : {initial: "
                + JsonUtil.stringify(initial) + "}}");
        page.getDocument().head().insertChildren(0, elm);

    }
}
