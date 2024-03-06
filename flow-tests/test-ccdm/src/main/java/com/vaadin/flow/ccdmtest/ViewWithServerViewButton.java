/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(value = "view-with-server-view-button", layout = MainLayout.class)
public class ViewWithServerViewButton extends Div {
    public ViewWithServerViewButton() {
        setId("viewWithServerViewButton");
        NativeButton serverViewButton = new NativeButton("Server view",
                e -> UI.getCurrent().navigate("serverview"));
        serverViewButton.setId("serverViewButton");

        NativeButton serverViewThrowsExcpetionButton = new NativeButton(
                "Go to a server view that thorws exception",
                e -> UI.getCurrent().navigate(ViewThrowsException.class));
        serverViewThrowsExcpetionButton
                .setId("serverViewThrowsExcpetionButton");

        add(serverViewButton, serverViewThrowsExcpetionButton);
    }
}
