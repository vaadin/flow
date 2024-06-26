/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.mixedtest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route("navigate-to-styled")
public class NavigateToStyled extends Div {

    public NavigateToStyled() {
        NativeButton button = new NativeButton("Navigate", event -> event
                .getSource().getUI().get().navigate(StyledDiv.class));
        button.setId("navigate");
        add(button);
    }

}
