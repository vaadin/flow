/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.AnchorView")
public class AnchorView extends Div {

    public AnchorView() {
        Anchor navigation = new Anchor("com.vaadin.flow.NavigationView",
                "Navigation");
        navigation.setId(NavigationView.ANCHOR_ID);
        add(new Span("AnchorView"), new Div(), navigation);
    }
}
