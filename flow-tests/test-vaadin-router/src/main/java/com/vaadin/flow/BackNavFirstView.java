/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("com.vaadin.flow.BackNavFirstView")
public class BackNavFirstView extends Div {

    public BackNavFirstView() {
        add(new NativeButton("Server side navigation", event -> getUI()
                .ifPresent(ui -> ui.navigate(BackNavSecondView.class))));
        add(new RouterLink("Client side navigation", BackNavSecondView.class));
    }
}
