/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.UUID;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.RefreshCurrentRouteRedirectTargetView", layout = RefreshCurrentRouteLayout.class)
public class RefreshCurrentRouteRedirectTargetView extends Div {

    static final String VIEW_ID = "forward-target-id";

    public RefreshCurrentRouteRedirectTargetView() {
        Div id = new Div(UUID.randomUUID().toString());
        id.setId(VIEW_ID);
        add(id);
    }
}
