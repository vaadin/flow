/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.navigation.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(value = "second")
public class SecondView extends Div {

    static final String BUTTON_ID = "secondViewButton";

    public SecondView() {
        NativeButton button = new NativeButton("Change query parameter",
                e -> UI.getCurrent().navigate("first"));
        button.setId(BUTTON_ID);
        add(button);
    }

}
