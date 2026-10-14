/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
