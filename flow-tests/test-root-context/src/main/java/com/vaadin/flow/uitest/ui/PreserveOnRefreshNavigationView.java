/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

@Route(value = PreserveOnRefreshNavigationView.VIEW_PATH)
@PreserveOnRefresh
public class PreserveOnRefreshNavigationView extends Div {

    static final String VIEW_PATH = "com.vaadin.flow.uitest.ui.PreserveOnRefreshNavigationView";

    public PreserveOnRefreshNavigationView() {
        add(createNavigationButton("one"));
        add(createNavigationButton("two"));
        add(createNavigationButton("three"));

        getElement().appendChild(createRouterLink("one"),
                createRouterLink("two"), createRouterLink("three"));
    }

    private NativeButton createNavigationButton(String param) {
        NativeButton button = new NativeButton("navigate to " + param,
                ev -> selfNavigate(param));
        button.setId("button-" + param);
        return button;
    }

    private void selfNavigate(String param) {
        UI.getCurrent().navigate(PreserveOnRefreshNavigationView.class,
                QueryParameters.of("param", param));
    }

    private Element createRouterLink(String param) {
        Element routerLink = ElementFactory.createRouterLink(
                VIEW_PATH + "?param=" + param, "link to " + param);
        routerLink.setAttribute("id", "link-" + param);
        return routerLink;
    }
}
