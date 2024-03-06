/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html.testbench;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.router.Route;

@Route("InputText")
public class InputTextView extends Div {

    public InputTextView() {
        Div log = new Div();
        log.setId("log");

        Input input = new Input();
        input.setId("input");
        input.addValueChangeListener(e -> {
            log.setText("Value is '" + input.getValue() + "'");
        });
        add(log, input);
    }
}
