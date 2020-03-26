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

import java.util.Date;

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
    private static final String ACTIVE_KEY_IN_SESSION_STORAGE = "vaadin.live-reload.active";
    private static final String LAST_RELOAD_KEY_IN_STORAGE = "vaadin.live-reload.last-reload";
    private static final String LAST_RELOAD_TEXT = "Last automatic reload happened at ";
    private static final int SPRING_DEV_TOOLS_PORT = 35729;
    private String serviceUrl;
    private int uiId;
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
        this.serviceUrl = serviceUrl;
        this.uiId = uiId;
        if (isEnabled()) {
            indicator = getOrCreateIndicator();
            if (getLastReloadInStorage() != null) {
                getOrCreateReloadNotification();
            }
            openWebSocketConnection();
        }
    }

    private void openWebSocketConnection() {
        closeWebSocketConnection();
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
            webSocket.setOnerror(this::handleErrorEvent);
            webSocket.setOnclose(e -> {
                if (indicator != null) {
                    updateActiveIndicator();
                }
            });
        });
    }

    private void closeWebSocketConnection() {
        if (webSocket != null) {
            webSocket.close();
            webSocket = null;
        }
    }

    private native WebSocket createWebSocket(String url)
    /*-{
        return new WebSocket(url);
    }-*/;

    private void handleMessageEvent(Event evt) {
        MessageEvent messageEvent = (MessageEvent) evt;
        JsonObject data = Json.parse((String) messageEvent.getData());
        if ("hello".equals(data.getString("command"))) {
            updateActiveIndicator();
            showMessage("Live reload: available");
        } else if ("reload".equals(data.getString("command"))) {
            if (isActive()) {
                showMessage("Live reload: in progress ...");
                saveLastReloadInStorage(new Date());
                Browser.getWindow().getLocation().reload();
            }
        } else {
            showMessage(null);
        }
    }

    private void handleErrorEvent(Event ev) {
        Console.debug(
                "Live Reload server is not available, neither Spring Dev Tools nor the Flow built-in. Live Reload won't work automatically.");
        showMessage(
                "Live reload: error; check browser console for more details");
    }

    private void showMessage(String msg) {
        indicator = getOrCreateIndicator();
        Element message = Browser.getDocument()
                .getElementById("vaadin-live-reload-message");
        if (msg == null) {
            message.setHidden(true);
        } else {
            message.setHidden(false);
            message.setInnerHTML(msg);
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

            Element overlay = Browser.getDocument().createElement("div");
            overlay.setId("vaadin-live-reload-overlay");
            overlay.setHidden(true);

            Element icon = Browser.getDocument().createElement("div");
            icon.setId("vaadin-live-reload-icon");
            icon.getStyle().setProperty("text-align", "right");
            icon.setOnclick(evt -> overlay.setHidden(!overlay.isHidden()));
            icon.setInnerText("}>");
            reloadIndicator.appendChild(icon);

            Element message = Browser.getDocument().createElement("span");
            message.setId("vaadin-live-reload-message");
            message.setInnerText("Live reload: enabled");
            overlay.appendChild(message);

            Element activeButton = Browser.getDocument().createElement("div");
            activeButton.setId("vaadin-live-reload-active");
            activeButton.setOnclick(e -> setActive(!isActive()));
            activeButton.getStyle().setVisibility("hidden");
            activeButton.getStyle().setTextDecoration("underline");
            activeButton.getStyle().setCursor("pointer");
            overlay.appendChild(activeButton);

            Element disableButton = Browser.getDocument().createElement("div");
            disableButton.setId("vaadin-live-reload-disable");
            disableButton.setInnerText("Disable");
            overlay.appendChild(disableButton);
            disableButton.getStyle().setTextDecoration("underline");
            disableButton.getStyle().setCursor("pointer");
            disableButton.setOnclick(e -> disable());

            reloadIndicator.appendChild(overlay);
            Browser.getDocument().getBody().appendChild(reloadIndicator);
        }
        return reloadIndicator;
    }

    private Element getOrCreateReloadNotification() {
        Element reloadNotificationElement = Browser.getDocument()
                .getElementById("vaadin-live-reload-notification");
        if (reloadNotificationElement == null) {
            reloadNotificationElement = Browser.getDocument()
                    .createElement("div");
            reloadNotificationElement.setId("vaadin-live-reload-notification");
            Element message = Browser.getDocument().createElement("span");
            message.setId("vaadin-live-reload-timestamp");
            message.setInnerText(LAST_RELOAD_TEXT + getLastReloadInStorage());
            reloadNotificationElement.appendChild(message);
            Element liveReloadOverlay = Browser.getDocument()
                    .getElementById("vaadin-live-reload-overlay");
            liveReloadOverlay.appendChild(reloadNotificationElement);
        } else {
            Element message = Browser.getDocument()
                    .getElementById("vaadin-live-reload-timestamp");
            message.setInnerText(LAST_RELOAD_TEXT + getLastReloadInStorage());
        }
        return reloadNotificationElement;
    }

    private void saveLastReloadInStorage(Date date) {

        StorageUtil.setSessionItem(LAST_RELOAD_KEY_IN_STORAGE,
                buildStringWith2LeadingZeroes(date.getHours()) + ":"
                        + buildStringWith2LeadingZeroes(date.getMinutes()) + ":"
                        + buildStringWith2LeadingZeroes(date.getSeconds()));
    }

    private String getLastReloadInStorage(){
        return StorageUtil.getSessionItem(LAST_RELOAD_KEY_IN_STORAGE);
    }

    private boolean isActive() {
        String active = StorageUtil
                .getSessionItem(ACTIVE_KEY_IN_SESSION_STORAGE);
        return active == null || Boolean.parseBoolean(active);
    }

    private void setActive(boolean active) {
        StorageUtil.setSessionItem(ACTIVE_KEY_IN_SESSION_STORAGE,
                Boolean.toString(active));
        if (active && (webSocket == null
                || webSocket.getReadyState() != WebSocket.OPEN)) {
            openWebSocketConnection();
        }
        updateActiveIndicator();
    }

    private void updateActiveIndicator() {
        Element icon = Browser.getDocument()
                .getElementById("vaadin-live-reload-icon");
        Element toggle = Browser.getDocument()
                .getElementById("vaadin-live-reload-active");
        if (icon != null && toggle !=null) {
            toggle.getStyle().setVisibility("visible");
            if (isActive()) {
                if (webSocket != null
                        && webSocket.getReadyState() == WebSocket.OPEN) {
                    icon.setInnerHTML("}&gt;<sup style='color: green'>●</sup>");
                } else {
                    // Live-reload is active, but websocket connection is not
                    // established. Show a red indicator.
                    icon.setInnerHTML("}&gt;<sup style='color: red'>●</sup>");
                }
                toggle.setInnerText("Deactivate in this window");
            } else {
                icon.setInnerHTML("}&gt;<sup style='color: yellow'>●</sup>");
                toggle.setInnerText("Activate in this window");
            }
        }
    }

    private boolean isEnabled() {
        String enabled = StorageUtil.getLocalItem(ENABLED_KEY_IN_STORAGE);
        return enabled == null || Boolean.parseBoolean(enabled);
    }

    private void disable() {
        assert indicator != null;

        closeWebSocketConnection();
        Browser.getDocument().getBody().removeChild(indicator);
        StorageUtil.setLocalItem(ENABLED_KEY_IN_STORAGE, "false");
    }

    private String buildStringWith2LeadingZeroes(int number) {
        if (number < 10) {
            return "0" + number;
        } else {
            return number + "";
        }
    }
}
