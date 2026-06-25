/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.contexttest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route(":tenant/plus-test")
public class PlusInRouteParameterView extends Div
        implements BeforeEnterObserver {

    public static final String TENANT_ID = "tenant_content";

    private final Div tenantDiv = new Div();

    public PlusInRouteParameterView() {
        tenantDiv.setId(TENANT_ID);
        add(tenantDiv);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String tenant = event.getRouteParameters().get("tenant").orElse("");
        tenantDiv.setText(tenant);
    }
}
