/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.partial;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;
import com.vaadin.flow.spring.annotation.SpringComponent;

@Route(value = "main", layout = MainLayout.class)
@RouteScope
@RouteScopeOwner(MainLayout.class)
@SpringComponent
public class MainView extends Div {

    public MainView() {

        add(new RouterLink("Navigate to second view - this works correctly",
                SecondView.class));
    }
}
