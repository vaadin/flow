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
import elemental.events.MessageEvent;
import elemental.html.WebSocket;
import elemental.json.Json;
import elemental.json.JsonObject;

public class LiveReload {
    private static final int SPRING_DEV_TOOLS_PORT = 35729;

    public void show() {
        String hostname = Browser.getWindow().getLocation().getHostname();
        WebSocket webSocket = createWebSocket(
                "ws://" + hostname + ":" + SPRING_DEV_TOOLS_PORT);
        webSocket.setOnmessage(evt -> {
            MessageEvent messageEvent = (MessageEvent) evt;
            JsonObject data = Json.parse((String) messageEvent.getData());
            if ("hello".equals(data.getString("command"))) {
                getOrCreateLiveRefreshIndicator().setInnerHTML("}> Live Reload active");
            }else if ("reload".equals(data.getString("command"))) {
                getOrCreateLiveRefreshIndicator().setInnerHTML("}> Live Reload in progress...");
                Browser.getWindow().getLocation().reload();
            }
        });
    }

    private Element getOrCreateLiveRefreshIndicator() {
        Element indicator = Browser.getDocument().getElementById("vaadin-live-reload-indicator");
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
            Element message = Browser.getDocument().createElement("div");
            icon.setId("vaadin-live-reload-icon");
            icon.setOnclick(evt -> message.setHidden(!message.isHidden()));
            icon.setInnerText("}>");
            message.setId("vaadin-live-reload-message");
            message.setInnerText("Live Reload is active");
            indicator.appendChild(icon);
            indicator.appendChild(message);
            Browser.getDocument().getBody().appendChild(indicator);
        }
        return indicator;
    }

    private native WebSocket createWebSocket(String url)
    /*-{
        return new WebSocket(url);
    }-*/;
}
