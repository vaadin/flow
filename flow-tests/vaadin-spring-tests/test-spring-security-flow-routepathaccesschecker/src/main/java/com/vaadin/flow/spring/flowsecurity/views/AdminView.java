/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.flowsecurity.views;

import jakarta.annotation.security.RolesAllowed;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.flowsecurity.SecurityUtils;

@Route(value = "admin", layout = MainView.class)
@RouteAlias(value = "alias-for-admin", layout = MainView.class)
@PageTitle("Admin View")
@Menu(order = 1.10001)
public class AdminView extends VerticalLayout {

    public AdminView(SecurityUtils securityUtils) {
        H1 welcome = new H1("Welcome to the admin page, "
                + securityUtils.getAuthenticatedUserInfo().getFullName());
        welcome.setId("welcome");
        add(welcome);
        Div div = new Div();
        div.setText(
                "This page is full of dangerous controls and secret information");
        add(div);
    }
}
