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

import java.util.logging.Logger;

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.JsonConstants;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.json.Json;
import elemental.json.JsonObject;

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

    private final static JsArray<String> MODIFIER_KEY_PROPERTIES = JsCollections
            .array("altKey", "ctrlKey", "metaKey", "shiftKey");

    protected RouterLinkHandler() {
        // Only static functionality
    }

    private static Logger getLogger() {
        return Logger.getLogger(RouterLinkHandler.class.getSimpleName());
    }

    /**
     * Adds a click event listener for the body element for handling application
     * navigation related events and sending them to server using the given
     * channel.
     *
     * @param rpcQueue
     *            the RPC queue used for sending the events
     */
    public static void bind(ServerRpcQueue rpcQueue) {
        Element body = Browser.getDocument().getBody();
        body.addEventListener("click", event -> handleClick(rpcQueue, event),
                false);
    }

    private static void handleClick(ServerRpcQueue rpcQueue, Event clickEvent) {
        if (isRouterLinkClick(clickEvent)) {
            Element target = (Element) WidgetUtil
                    .crazyJsCast(clickEvent.getTarget());
            // need to use property to get full href
            String href = (String) WidgetUtil.getJsProperty(target, "href");
            String baseURI = Browser.getWindow().getDocument().getBaseURI();

            // verify that the link is actually for this application
            if (!href.startsWith(baseURI)) {
                // ain't nobody going to see this log
                getLogger().warning("Should not use "
                        + ApplicationConstants.ROUTER_LINK_ATTRIBUTE
                        + " attribute for an external link.");
                return;
            }

            Browser.getWindow().getHistory().pushState(null, null, href);

            clickEvent.preventDefault();

            String pathname = href.replace(baseURI, "");

            sendRoutingMessageToServer(rpcQueue, pathname);
        }
    }

    private static void sendRoutingMessageToServer(ServerRpcQueue rpcQueue,
            String location) {
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_ROUTING);
        message.put(JsonConstants.RPC_POPSTATE_LOCATION, location);

        rpcQueue.add(message);
        rpcQueue.flush();
    }

    private static boolean isRouterLinkClick(Event clickEvent) {
        assert clickEvent.getType().equals("click");

        Element target = (Element) WidgetUtil
                .crazyJsCast(clickEvent.getTarget());
        if (!"A".equalsIgnoreCase(target.getTagName())) {
            return false;
        }

        if (!target.hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE)) {
            return false;
        }

        if (hasModifierKeys(clickEvent)) {
            return false;
        }

        return true;
    }

    private static boolean hasModifierKeys(Event clickEvent) {
        for (int i = 0; i < MODIFIER_KEY_PROPERTIES.length(); i++) {
            if ((boolean) WidgetUtil.getJsProperty(clickEvent,
                    MODIFIER_KEY_PROPERTIES.get(i))) {
                return true;
            }
        }
        return false;
    }

}
