/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.routescope;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

@Route(value = "div-in-layout", layout = Layout.class)
@RouteScope
@RouteScopeOwner(Layout.class)
@Component
public class DivInLayout extends Div {

    @Autowired
    private ApplicationContext context;

    private Div serviceInfo;

    public DivInLayout() {
        // the component is in the route scope so the text should be the same
        // until route scope is active.
        Div div = createInfo("div-id");
        div.setText(UUID.randomUUID().toString());
        add(div);

        serviceInfo = createInfo("service-info");
        add(serviceInfo);
    }

    private Div createInfo(String id) {
        Div div = new Div();
        div.setId(id);
        div.getElement().getStyle().set("display", "block");
        return div;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        serviceInfo.setText(context.getBean(MyService.class).getValue());
    }

}
