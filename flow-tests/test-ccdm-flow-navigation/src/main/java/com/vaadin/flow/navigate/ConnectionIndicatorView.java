/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
