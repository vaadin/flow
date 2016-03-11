/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.hummingbird;

import com.vaadin.client.Console;
import com.vaadin.client.Registry;
import com.vaadin.client.URIResolver;
import com.vaadin.shared.ApplicationConstants;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.MouseEvent;
import elemental.html.AnchorElement;

/**
 * Handler for click events originating from application navigation link
 * elements marked with {@value ApplicationConstants#ROUTER_LINK_ATTRIBUTE}.
 * <p>
 * Events are sent to server for handling.
 *
 * @since
 * @author Vaadin Ltd
 */
public class RouterLinkHandler {

    private RouterLinkHandler() {
        // Only static functionality
    }

    /**
     * Adds a click event listener for the given element for intercepting
     * application navigation related click events and sending them to server.
     *
     * @param registry
     *            the registry
     * @param element
     *            the element to listen to click events in
     */
    public static void bind(Registry registry, Element element) {
        element.addEventListener("click", event -> handleClick(registry, event),
                false);
    }

    private static void handleClick(Registry registry, Event clickEvent) {
        if (isRouterLinkClick(clickEvent) && !hasModifierKeys(clickEvent)
                && registry.getUILifecycle().isRunning()) {
            AnchorElement target = (AnchorElement) clickEvent.getTarget();

            String href = target.getHref();
            String baseURI = target.getOwnerDocument().getBaseURI();

            // verify that the link is actually for this application
            if (!href.startsWith(baseURI)) {
                // ain't nobody going to see this log
                Console.warn("Should not use "
                        + ApplicationConstants.ROUTER_LINK_ATTRIBUTE
                        + " attribute for an external link.");
                return;
            }

            Browser.getWindow().getHistory().pushState(null, null, href);

            clickEvent.preventDefault();

            String location = URIResolver.getBaseRelativeUri(baseURI, href);

            registry.getServerConnector().sendNavigationMessage(location, null);
        }
    }

    private static boolean isRouterLinkClick(Event clickEvent) {
        assert "click".equals(clickEvent.getType());

        Element target = (Element) clickEvent.getTarget();
        if (!"A".equalsIgnoreCase(target.getTagName())) {
            return false;
        }

        if (!target.hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE)) {
            return false;
        }

        return true;
    }

    private static boolean hasModifierKeys(Event clickEvent) {
        assert "click".equals(clickEvent.getType());

        MouseEvent event = (MouseEvent) clickEvent;
        return event.isAltKey() || event.isCtrlKey() || event.isMetaKey()
                || event.isShiftKey();
    }

}
