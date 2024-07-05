/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.navigation;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.navigation.SecondView")
public class SecondView extends Div {

    static final String BUTTON_ID = "secondViewButton";

    public SecondView() {
        NativeButton button = new NativeButton("Change query parameter",
                e -> UI.getCurrent().navigate(
                        "com.vaadin.flow.uitest.ui.navigation.FirstView"));
        button.setId(BUTTON_ID);
        add(button);
    }

}
