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

@Route(value = "serverview/view-with-home-button", layout = MainLayout.class)
public class ViewWithHomeButton extends Div {
    public ViewWithHomeButton() {
        setId("viewWithHomeButton");
        NativeButton homeButton = new NativeButton("Go home",
                e -> UI.getCurrent().navigate(""));
        homeButton.setId("homeButton");
        add(homeButton);
    }
}
