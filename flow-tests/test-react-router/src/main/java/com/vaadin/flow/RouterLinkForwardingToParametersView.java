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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("com.vaadin.flow.RouterLinkForwardingToParametersView")
public class RouterLinkForwardingToParametersView extends Div {

    public RouterLinkForwardingToParametersView() {
        RouterLink link = new RouterLink("Forwarding view",
                ForwardingToParametersView.class);
        link.setId("forwardViewLink");
        add(link);
    }
}
