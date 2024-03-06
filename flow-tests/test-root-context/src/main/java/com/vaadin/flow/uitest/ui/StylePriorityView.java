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

@Route("com.vaadin.flow.uitest.ui.StylePriorityView")
public class StylePriorityView extends Div {

    public StylePriorityView() {
        Div div = new Div();
        div.getElement().getStyle().set("display", "block !important");
        div.setText("Priority style");
        div.setId("priority-style");
        add(div);
    }

}
