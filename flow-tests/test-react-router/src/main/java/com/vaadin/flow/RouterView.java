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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("com.vaadin.flow.RouterView")
public class RouterView extends Div {

    public RouterView() {
        RouterLink link = new RouterLink("RouterLink", NavigationView.class);
        link.setId(NavigationView.ROUTER_LINK_ID);

        add(new Span("RouterView"), new Div(), link);
    }
}
