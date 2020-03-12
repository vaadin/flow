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

import com.google.gwt.storage.client.Storage;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.MessageEvent;
import elemental.html.WebSocket;
import elemental.json.Json;
import elemental.json.JsonObject;

public class LiveReload {
    // The default value is true meaning if the key doesn't exist in the local
    // storage Live Reload is enabled.
    private static final String ENABLED_KEY_IN_STORAGE = "vaadin.live-reload.enabled";
    private static final int SPRING_DEV_TOOLS_PORT = 35729;
    private WebSocket webSocket;
    private Element indicator;

    public void show() {
        if (!isEnabled())
            return;

        String hostname = Browser.getWindow().getLocation().getHostname();
        webSocket = createWebSocket(
                "ws://" + hostname + ":" + SPRING_DEV_TOOLS_PORT);
        webSocket.setOnmessage(evt -> {
            MessageEvent messageEvent = (MessageEvent) evt;
            JsonObject data = Json.parse((String) messageEvent.getData());
            indicator = getOrCreateIndicator();
            Element indicatorMessage = Browser.getDocument()
                    .getElementById("vaadin-live-reload-message");
            if ("hello".equals(data.getString("command"))) {
                indicatorMessage.setInnerHTML("Live Reload active");
            } else if ("reload".equals(data.getString("command"))) {
                indicatorMessage.setInnerHTML("Live Reload in progress...");
                Browser.getWindow().getLocation().reload();
            } else {
                indicatorMessage.setHidden(true);
            }
        });
    }

    private Element getOrCreateIndicator() {
        Element indicator = Browser.getDocument()
                .getElementById("vaadin-live-reload-indicator");
        if (indicator == null) {
            indicator = Browser.getDocument().createElement("div");
            indicator.setId("vaadin-live-reload-indicator");
            indicator.getStyle().setPosition("fixed");
            indicator.getStyle().setPadding("10px 10px 10px 10px");
            indicator.getStyle().setMargin("10px 10px 10px 10px");
            indicator.getStyle().setRight("0");
            indicator.getStyle().setTop("0");
            indicator.getStyle().setZIndex(10000);
            Element icon = Browser.getDocument().createElement("div");
            Element overlay = Browser.getDocument().createElement("div");
            overlay.setId("vaadin-live-reload-overlay");
            icon.setId("vaadin-live-reload-icon");
            icon.getStyle().setProperty("text-align", "right");
            // icon.getStyle().setProperty("transform", "rotate(90deg)");
            icon.setOnclick(evt -> overlay.setHidden(!overlay.isHidden()));
            icon.setInnerText("}>");
            indicator.appendChild(icon);
            Element message = Browser.getDocument().createElement("span");
            message.setId("vaadin-live-reload-message");
            message.setInnerText("Live Reload is active");
            overlay.appendChild(message);
            Element disableButton = Browser.getDocument()
                    .createElement("input");
            disableButton.setAttribute("type", "button");
            disableButton.setAttribute("value", "Disable");
            disableButton.setOnclick(evt -> disable());
            overlay.appendChild(disableButton);
            indicator.appendChild(overlay);
            Browser.getDocument().getBody().appendChild(indicator);
        }
        return indicator;
    }

    private boolean isEnabled() {
        Storage storage = Storage.getLocalStorageIfSupported();
        if (storage == null) {
            return true;
        }

        String enabled = storage.getItem(ENABLED_KEY_IN_STORAGE);
        return enabled == null || Boolean.parseBoolean(enabled);
    }

    private void disable() {
        assert webSocket != null;
        assert indicator != null;

        webSocket.close();
        Browser.getDocument().getBody().removeChild(indicator);
        Storage storage = Storage.getLocalStorageIfSupported();
        if (storage == null) {
            Console.warn(
                    "Your browser does not support local storage. Live Reload can't be disabled permanently.");
            return;
        }

        storage.setItem(ENABLED_KEY_IN_STORAGE, "false");
    }

    private native WebSocket createWebSocket(String url)
    /*-{
        return new WebSocket(url);
    }-*/;
}
