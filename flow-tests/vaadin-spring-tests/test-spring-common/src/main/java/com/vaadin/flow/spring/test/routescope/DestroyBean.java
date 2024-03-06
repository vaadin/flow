/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.routescope;

import javax.annotation.PreDestroy;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;

@RouteScope
@SpringComponent
public class DestroyBean {

    @PreDestroy
    public void preDestroy() {
        UI ui = UI.getCurrent();
        Div info = new Div();
        info.setText("Bean is destroyed");
        info.addClassName("info");
        ui.add(info);
    }
}
