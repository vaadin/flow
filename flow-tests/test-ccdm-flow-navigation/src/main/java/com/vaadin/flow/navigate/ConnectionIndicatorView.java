/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.navigate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "connection-indicator")
@PageTitle("Connection Indicator Tests")
public class ConnectionIndicatorView extends Div {

    public static final String CONNECT_SERVER = "connect-server";
    public static final String SET_CUSTOM_MESSAGES = "set-custom-message";
    public static final String CUSTOM_RECONNECTING_MESSAGE = "custom reconnecting from Java";
    public static final String CUSTOM_OFFLINE_MESSAGE = "custom offline from Java";

    public ConnectionIndicatorView() {
        NativeButton ping = new NativeButton("Ping server",
                e -> add(new Span("Server reached")));
        ping.setId(CONNECT_SERVER);
        add(ping);

        NativeButton setCustomReconnecting = new NativeButton(
                "Set custom reconnecting message", e -> {
                    UI ui = getUI().get();
                    ui.getReconnectDialogConfiguration()
                            .setDialogText(CUSTOM_RECONNECTING_MESSAGE);
                    ui.getReconnectDialogConfiguration()
                            .setDialogTextGaveUp(CUSTOM_OFFLINE_MESSAGE);
                });
        setCustomReconnecting.setId(SET_CUSTOM_MESSAGES);
        add(setCustomReconnecting);
    }

}
