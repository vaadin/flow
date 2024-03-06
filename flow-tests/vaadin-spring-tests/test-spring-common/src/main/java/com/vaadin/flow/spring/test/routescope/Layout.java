/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.routescope;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

public class Layout extends Div implements RouterLayout {

    public Layout() {
        add(createRouterLink("div", DivInLayout.class, "div-link"));
        add(createRouterLink("button", ButtonInLayout.class, "button-link"));
        add(createRouterLink("invalid", InvalidRouteScopeUsage.class,
                "invalid-route-link"));
    }

    private RouterLink createRouterLink(String text,
            Class<? extends Component> clazz, String id) {
        RouterLink link = new RouterLink(text, clazz);
        link.getStyle().set("display", "block");
        link.setId(id);
        return link;
    }
}
