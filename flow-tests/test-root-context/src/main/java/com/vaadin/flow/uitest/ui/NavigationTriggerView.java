/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.NavigationTriggerView", layout = ViewTestLayout.class)
public class NavigationTriggerView extends AbstractDivView
        implements HasUrlParameter<String>, BeforeLeaveObserver {
    private static final String CLASS_NAME = NavigationTriggerView.class
            .getName();

    public NavigationTriggerView() {
        // Cannot use the RouterLink component since these views are not
        // registered as regular views.
        Element routerLink = ElementFactory
                .createRouterLink(CLASS_NAME + "/routerlink/", "Router link");
        routerLink.setAttribute("id", "routerlink");

        Element navigateButton = ElementFactory.createButton("UI.navigate");
        navigateButton.addEventListener("click",
                e -> getUI().get().navigate(CLASS_NAME + "/navigate"));
        navigateButton.setAttribute("id", "navigate");

        Element forwardButton = ElementFactory.createButton("forward");
        forwardButton.addEventListener("click", e -> getUI().get()
                .navigate(NavigationTriggerView.class, "forward"));
        forwardButton.setAttribute("id", "forwardButton");

        Element rerouteButton = ElementFactory.createButton("reroute");
        rerouteButton.addEventListener("click", e -> getUI().get()
                .navigate(NavigationTriggerView.class, "reroute"));
        rerouteButton.setAttribute("id", "rerouteButton");

        getElement().appendChild(routerLink, navigateButton, forwardButton,
                rerouteButton);
    }

    public static String buildMessage(String path, NavigationTrigger trigger,
            String parameter) {
        return "Navigated to " + path + " with trigger " + trigger.name()
                + " and parameter " + parameter;
    }

    private void addMessage(String message) {
        Element element = ElementFactory.createDiv(message);
        element.getClassList().add("message");
        getElement().appendChild(element);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter String parameter) {
        String location = event.getLocation().getPathWithQueryParameters();
        assert location.startsWith(CLASS_NAME);

        location = location.substring(CLASS_NAME.length());
        if (location.isEmpty()) {
            // For clarity in the message
            location = "/";
        }

        addMessage(buildMessage(location, event.getTrigger(), parameter));
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (event.getLocation().getPath().endsWith("forward")) {
            event.forwardTo(CLASS_NAME, "forwarded");
        } else if (event.getLocation().getPath().endsWith("reroute")) {
            event.rerouteTo(CLASS_NAME, "rerouted");
        }
    }
}
