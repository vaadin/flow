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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ElementStyleView", layout = ViewTestLayout.class)
public class ElementStyleView extends AbstractDivView {

    static final String GREEN_BORDER = "4px solid rgb(0, 255, 0)";
    static final String RED_BORDER = "10px solid rgb(255, 0, 0)";

    @Override
    protected void onShow() {
        Element mainElement = getElement();
        mainElement.getStyle().set("--foo", RED_BORDER);

        Div div = new Div();
        div.setId("red-border");
        div.getElement().getStyle().set("border", "var(--foo)");
        div.setText("Div");

        Div div2 = new Div();
        div2.setId("green-border");
        div2.setText("Div 2");
        div2.getStyle().set("--foo", GREEN_BORDER);
        div2.getElement().getStyle().set("border", "var(--foo)");
        add(div, div2);

    }

}
