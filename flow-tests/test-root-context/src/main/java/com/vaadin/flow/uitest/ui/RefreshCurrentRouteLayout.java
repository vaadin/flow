package com.vaadin.flow.uitest.ui;

import java.util.UUID;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.RouterLayout;

public class RefreshCurrentRouteLayout implements RouterLayout {

    final static String ROUTER_LAYOUT_ID = "routerlayoutid";

    private Div layout = new Div();

    public RefreshCurrentRouteLayout() {
        final String uniqueId = UUID.randomUUID().toString();
        Div routerLayoutId = new Div(uniqueId);
        routerLayoutId.setId(ROUTER_LAYOUT_ID);
        layout.add(routerLayoutId);
    }

    @Override
    public Element getElement() {
        return layout.getElement();
    }
}
