/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("com.vaadin.flow.uitest.ui.BrokenRouterLinkView")
public class BrokenRouterLinkView extends AbstractDivView {

    public final static String LINK_ID = "broken-link";

    public BrokenRouterLinkView() {
        final RouterLink routerLink = new RouterLink("Broken",
                BrokenRouterLinkView.class);
        Div spacer = new Div();
        spacer.setHeight("5000px");
        add(spacer);

        routerLink.getElement().setAttribute("href", "somewhere_non_existent");
        routerLink.setId(LINK_ID);
        add(routerLink);
    }
}
