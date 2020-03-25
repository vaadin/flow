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

package com.vaadin.client;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.MessageEvent;
import elemental.html.WebSocket;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Responsible for the client-side of the Live Reload. It refreshes the page
 * when it receives a reload command from Loive Reload server (either Spring or
 * Flow).
 * 
 * @author Vaadin Ltd
 * @since
 */
public class LiveReload {
    // The default value is true meaning if the key doesn't exist in the local
    // storage Live Reload is enabled.
    private static final String ENABLED_KEY_IN_STORAGE = "vaadin.live-reload.enabled";
    private static final int SPRING_DEV_TOOLS_PORT = 35729;
    private WebSocket webSocket;
    private Element indicator;

    /**
     * Connects to either Spring Dev Tools Live Reload server or Flow Live
     * Reload server and if the connection is successful shows an overlay on the
     * page including the status of the Live Reload.
     *
     * @param serviceUrl
     *            The root URL of the application that should be used to connect
     *            to Flow Live Reload server
     * @param uiId
     *            The UI id
     */
    public void show(String serviceUrl, int uiId) {
        if (!isEnabledOnThisBrowser()) {
            return;
        }

        String hostname = Browser.getWindow().getLocation().getHostname();
        webSocket = createWebSocket(
                "ws://" + hostname + ":" + SPRING_DEV_TOOLS_PORT);
        webSocket.setOnmessage(this::handleMessageEvent);
        webSocket.setOnerror(springWsEvent -> {
            if (!serviceUrl.startsWith("http://")) {
                Console.debug(
                        "The protocol of the url should be http for Live Reload to work.");
                return;
            }

            webSocket = createWebSocket(
                    serviceUrl.replaceFirst("http://", "ws://") + "?v-uiId="
                            + uiId + "&refresh_connection");
            webSocket.setOnmessage(this::handleMessageEvent);
            webSocket.setOnerror(flowWsEvent -> Console.debug(
                    "Live Reload server is not available, neither Spring Dev Tools nor the Flow built-in. Live Reload won't work automatically."));
        });
    }

    private native WebSocket createWebSocket(String url)
    /*-{
        return new WebSocket(url);
    }-*/;

    private void handleMessageEvent(Event evt) {
        MessageEvent messageEvent = (MessageEvent) evt;
        JsonObject data = Json.parse((String) messageEvent.getData());
        indicator = getOrCreateIndicator();
        Element indicatorMessage = Browser.getDocument()
                .getElementById("vaadin-live-reload-message");
        if ("hello".equals(data.getString("command"))) {
            indicatorMessage.setInnerHTML("Live reload: enabled");
        } else if ("reload".equals(data.getString("command"))) {
            indicatorMessage.setInnerHTML("Live reload: in progress ...");
            Browser.getWindow().getLocation().reload();
        } else {
            indicatorMessage.setHidden(true);
        }
    }

    private Element getOrCreateIndicator() {
        Element reloadIndicator = Browser.getDocument()
                .getElementById("vaadin-live-reload-indicator");
        if (reloadIndicator == null) {
            reloadIndicator = Browser.getDocument().createElement("div");
            reloadIndicator.setId("vaadin-live-reload-indicator");
            reloadIndicator.getStyle().setPosition("fixed");
            reloadIndicator.getStyle().setPadding("10px 10px 10px 10px");
            reloadIndicator.getStyle().setMargin("10px 10px 10px 10px");
            reloadIndicator.getStyle().setRight("0");
            reloadIndicator.getStyle().setTop("0");
            reloadIndicator.getStyle().setZIndex(10000);
            Element icon = Browser.getDocument().createElement("div");
            Element overlay = Browser.getDocument().createElement("div");
            overlay.setId("vaadin-live-reload-overlay");
            overlay.setHidden(true);
            icon.setId("vaadin-live-reload-icon");
            icon.getStyle().setProperty("text-align", "right");
            icon.setOnclick(evt -> overlay.setHidden(!overlay.isHidden()));
            icon.setInnerText("}>");
            reloadIndicator.appendChild(icon);
            Element message = Browser.getDocument().createElement("span");
            message.setId("vaadin-live-reload-message");
            message.setInnerText("Live reload: enabled");
            overlay.appendChild(message);
            Element disableButton = Browser.getDocument()
                    .createElement("input");
            disableButton.setAttribute("type", "button");
            disableButton.setAttribute("value", "Disable");
            disableButton.setOnclick(evt -> disableOnThisBrowser());
            overlay.appendChild(disableButton);
            reloadIndicator.appendChild(overlay);
            Browser.getDocument().getBody().appendChild(reloadIndicator);
        }
        return reloadIndicator;
    }

    private boolean isEnabledOnThisBrowser() {
        String enabled = StorageUtil.getLocalItem(ENABLED_KEY_IN_STORAGE);
        return enabled == null || Boolean.parseBoolean(enabled);
    }

    private void disableOnThisBrowser() {
        assert webSocket != null;
        assert indicator != null;

        webSocket.close();
        Browser.getDocument().getBody().removeChild(indicator);
        StorageUtil.setLocalItem(ENABLED_KEY_IN_STORAGE, "false");
    }
}
