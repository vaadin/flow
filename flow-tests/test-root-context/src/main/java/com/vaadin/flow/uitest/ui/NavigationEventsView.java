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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.NavigationEventsView", layout = ViewTestLayout.class)
public class NavigationEventsView extends Div
        implements BeforeEnterObserver, AfterNavigationObserver {

    public static final String BEFORE_ENTER = "BeforeEnterEvent";
    public static final String AFTER_NAVIGATION = "AfterNavigationEvent";

    private final Element messages = new Element("div");

    public NavigationEventsView() {
        messages.setAttribute("id", "messages");
        getElement().appendChild(messages);

        RouterLink routerLink = new RouterLink("RouterLink to self",
                NavigationEventsView.class);
        routerLink.setId("router-link");
        Anchor anchor = new Anchor(
                "/view/com.vaadin.flow.uitest.ui.NavigationEventsView",
                "Anchor to self");
        anchor.setId("anchor");

        add(routerLink, anchor);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        addMessage(BEFORE_ENTER);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        addMessage(AFTER_NAVIGATION);
    }

    private void addMessage(String message) {
        Element element = new Element("div");
        element.setText(message);
        messages.appendChild(element);
    }
}
