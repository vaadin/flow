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
package com.vaadin.flow;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route("com.vaadin.flow.PopStateHandlerView")
public class PopStateHandlerView extends RouterLinkView {

    @Override
    protected void addLinks() {
        getElement().appendChild(
                createPushStateButtons(
                        "com.vaadin.flow.PopStateHandlerView/another/"),
                ElementFactory.createParagraph(),
                createPushStateButtons(
                        "com.vaadin.flow.PopStateHandlerView/forum/"),
                ElementFactory.createParagraph(),
                createPushStateButtons(
                        "com.vaadin.flow.PopStateHandlerView/forum/#!/category/1"),
                ElementFactory.createParagraph(),
                createPushStateButtons(
                        "com.vaadin.flow.PopStateHandlerView/forum/#!/category/2"),
                ElementFactory.createParagraph(), createPushStateButtons(
                        "com.vaadin.flow.PopStateHandlerView/forum/#"));
    }

    protected Element createPushStateButtons(String target) {
        Element button = ElementFactory.createButton(target).setAttribute("id",
                target);
        String historyPush = "window.history.pushState(null, null, event.target.textContent)";
        if (VaadinSession.getCurrent().getService().getDeploymentConfiguration()
                .isReactEnabled()) {
            historyPush = "window.dispatchEvent(new CustomEvent('vaadin-navigate', { detail: {  url: event.target.textContent, replace: false } }))";
        }
        button.addEventListener("click", e -> {
        }).addEventData(historyPush);
        return button;
    }
}
