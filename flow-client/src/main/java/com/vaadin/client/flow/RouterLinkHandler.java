/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.client.flow;

import java.util.Objects;

import com.vaadin.client.Registry;
import com.vaadin.client.ScrollPositionHandler;
import com.vaadin.client.URIResolver;
import com.vaadin.client.WidgetUtil;
import com.vaadin.flow.shared.ApplicationConstants;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventTarget;
import elemental.events.MouseEvent;
import elemental.html.AnchorElement;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Handler for click events originating from application navigation link
 * elements marked with {@value ApplicationConstants#ROUTER_LINK_ATTRIBUTE}.
 * <p>
 * Events are sent to server for handling.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
        if (hasModifierKeys(clickEvent)
                || !registry.getUILifecycle().isRunning()) {
            return;
        }

        AnchorElement anchor = getAnchorElement(clickEvent);
        if (anchor == null) {
            return;
        }

        final String href = anchor.getHref();
        final String baseURI = ((Element) clickEvent.getCurrentTarget())
                .getOwnerDocument().getBaseURI();

        // verify that the link is actually for this application
        if (!href.startsWith(baseURI)) {
            // scroll position is stored by a beforeunload handler in
            // ScrollPositionHandler
            return;
        }

        // special case: inside page navigation doesn't cause server side
        // round trip but need to store the scroll positions since browser adds
        // another history state
        if (isInsidePageNavigation(anchor)) {
            // there is no pop state if the hashes are exactly the same
            String currentHash = Browser.getDocument().getLocation().getHash();
            if (!currentHash.equals(anchor.getHash())) {
                registry.getScrollPositionHandler().beforeClientNavigation(href);
            }

            // the browser triggers a fragment change & pop state event
            registry.getScrollPositionHandler()
                    .setIgnoreScrollRestorationOnNextPopStateEvent(true);
            return;
        }

        if (!isRouterLinkAnchorElement(anchor)) {
            return;
        }

        handleRouterLinkClick(clickEvent, baseURI, href, registry);
    }

    private static void handleRouterLinkClick(Event clickEvent, String baseURI,
            String href, Registry registry) {
        clickEvent.preventDefault();

        String location = URIResolver.getBaseRelativeUri(baseURI, href);
        if (location.contains("#")) {
            // make sure fragment event gets fired after response
            new FragmentHandler(Browser.getWindow().getLocation().getHref(),
                    href, registry).bind();

            // don't send hash to server
            location = location.split("#", 2)[0];
        }

        JsonObject state = createNavigationEventState(href);

        sendServerNavigationEvent(registry, location, state, true);
    }

    private static JsonObject createNavigationEventState(String href) {
        double[] scrollPosition = ScrollPositionHandler.getScrollPosition();
        JsonObject state = Json.createObject();
        state.put("href", href);
        state.put("scrollPositionX", scrollPosition[0]);
        state.put("scrollPositionY", scrollPosition[1]);
        return state;
    }

    /**
     * Checks whether the given anchor links within the current page.
     *
     * @param anchor
     *            the link to check
     * @return <code>true</code> if links inside current page,
     *         <code>false</code> if not
     */
    private static boolean isInsidePageNavigation(AnchorElement anchor) {
        return isInsidePageNavigation(anchor.getPathname(),
                anchor.getHref().contains("#"));
    }

    /**
     * Checks whether the given path is for navigating inside the same page as
     * the current one.
     * <p>
     * If the paths are different, it is always outside the current page
     * navigation. If they are the same, then it depends on whether the new href
     * has any hash <code>#</code>.
     *
     * @param path
     *            the path to check against
     * @param hasFragment
     *            the <code>true</code> if the navigated url contains a
     *            fragment,<code>false</code> if not
     * @return <code>true</code> if the given location is for navigating inside
     *         the current page, <code>false</code> if not
     */
    public static boolean isInsidePageNavigation(String path,
            boolean hasFragment) {
        String currentPath = Browser.getWindow().getLocation().getPathname();
        assert currentPath != null : "window.location.path should never be null";

        if (!Objects.equals(currentPath, path)) {
            return false;
        }
        // if paths are the same, then need to check fragment.
        // if the navigated location doesn't have fragment at all, then it is
        // reload, otherwise inside page
        return hasFragment;
    }

    /**
     * Gets the anchor element, if a router link was found between the click
     * target and the event listener.
     *
     * @param clickEvent
     *            the click event
     * @return the target anchor if found, <code>null</code> otherwise
     */
    private static AnchorElement getAnchorElement(Event clickEvent) {
        assert "click".equals(clickEvent.getType());

        Element target = getTargetElement(clickEvent);
        EventTarget eventListenerElement = clickEvent.getCurrentTarget();
        // Target can become null if another click handler detaches the element
        while (target != null && target != eventListenerElement) {
            if (isAnchorElement(target)) {
                return (AnchorElement) target;
            }
            target = target.getParentElement();
        }

        return null;
    }

    private static native Element getTargetElement(Event clickEvent)
    /*-{
        if(clickEvent.composed) {
            return clickEvent.composedPath()[0];
        }
        return clickEvent.target;
    }-*/;

    /**
     * Checks if the given element is an anchor element {@code <a>}.
     *
     * @param target
     *            the element to check
     * @return <code>true</code> if the element is an anchor <code>false</code>
     *         otherwise
     */
    private static boolean isAnchorElement(Element target) {
        return "a".equalsIgnoreCase(target.getTagName());
    }

    /**
     * Checks if the given element is a router link {@code <a router-link>}.
     *
     * @param target
     *            the element to check
     * @return <code>true</code> if the element is a router link
     *         <code>false</code> otherwise
     */
    private static boolean isRouterLinkAnchorElement(AnchorElement anchor) {
        return anchor.hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE);
    }

    private static boolean hasModifierKeys(Event clickEvent) {
        assert "click".equals(clickEvent.getType());

        MouseEvent event = (MouseEvent) clickEvent;
        return event.isAltKey() || event.isCtrlKey() || event.isMetaKey()
                || event.isShiftKey();
    }

    /**
     * Notifies the server about navigation to the given location.
     * <p>
     * Ensures that navigation works even if the session has expired.
     *
     * @param registry
     *            the registry
     * @param location
     *            the location to navigate to, relative to the base URI
     * @param stateObject
     *            the state object or <code>null</code> if none applicable
     * @param routerLinkEvent
     *            <code>true</code> if this event was triggered by interaction
     *            with a router link; <code>false</code> if triggered by history
     *            navigation
     */
    public static void sendServerNavigationEvent(Registry registry,
            String location, Object stateObject, boolean routerLinkEvent) {
        assert registry != null;
        assert location != null;

        // If the server tells us the session has expired, we refresh (using the
        // new location) instead.
        registry.getMessageHandler().setNextResponseSessionExpiredHandler(
                () -> WidgetUtil.refresh());
        registry.getServerConnector().sendNavigationMessage(location,
                stateObject, routerLinkEvent);

    }
}
