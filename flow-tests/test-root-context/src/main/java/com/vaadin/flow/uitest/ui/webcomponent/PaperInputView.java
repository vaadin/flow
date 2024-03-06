/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.webcomponent;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.webcomponent.PaperInputView", layout = ViewTestLayout.class)
public class PaperInputView extends Div {

    public PaperInputView() {
        PaperInput paperInput = new PaperInput("foo");

        add(paperInput);
        paperInput.getElement().addPropertyChangeListener("value",
                event -> showValue(paperInput.getValue()));
    }

    private void showValue(String value) {
        Div div = new Div();
        div.setText(value);
        div.setClassName("update-value");
        add(div);
    }
}
