/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.NavigationTrigger;
import com.vaadin.router.Route;
import com.vaadin.router.event.BeforeNavigationEvent;

@Route(value = "com.vaadin.flow.uitest.ui.NavigationTriggerView", layout = ViewTestLayout.class)
public class NavigationTriggerView extends AbstractDivView {
    private static final String CLASS_NAME = NavigationTriggerView.class
            .getName();

    public NavigationTriggerView() {
        // Cannot use the RouterLink component since these views are not
        // registered as regular views.
        Element routerLink = ElementFactory
                .createRouterLink(CLASS_NAME + "/routerlink", "Router link");
        routerLink.setAttribute("id", "routerlink");

        Element navigateButton = ElementFactory.createButton("UI.navigate");
        navigateButton.addEventListener("click",
                e -> getUI().get().navigateTo(CLASS_NAME + "/navigate"));
        navigateButton.setAttribute("id", "navigate");

        getElement().appendChild(routerLink, navigateButton);
    }

    @Override
    public void beforeNavigation(BeforeNavigationEvent event) {
        super.beforeNavigation(event);

        String location = event.getLocation().getPathWithQueryParameters();
        assert location.startsWith(CLASS_NAME);

        location = location.substring(CLASS_NAME.length());
        if (location.isEmpty()) {
            // For clarity in the message
            location = "/";
        }

        addMessage(buildMessage(location, event.getTrigger()));
    }

    public static String buildMessage(String path, NavigationTrigger trigger) {
        return "Navigated to " + path + " with trigger " + trigger.name();
    }

    private void addMessage(String message) {
        Element element = ElementFactory.createDiv(message);
        element.getClassList().add("message");
        getElement().appendChild(element);
    }
}
