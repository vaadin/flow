/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.ReactAdapterView")
public class ReactAdapterView extends Div {

    public ReactAdapterView() {
        var input = new ReactInput("initialValue");

        var listenerOutput = new Span();
        listenerOutput.setId("listenerOutput");

        input.addValueChangeListener(listenerOutput::setText);

        var setValueButton = new NativeButton("Set value",
                (event) -> input.setValue("set value"));
        setValueButton.setId("setValueButton");

        var getOutput = new Span();
        getOutput.setId("getOutput");

        var getValueButton = new NativeButton("Get value",
                (event) -> getOutput.setText(input.getValue()));
        getValueButton.setId("getValueButton");

        add(new Div(input, listenerOutput), new Div(setValueButton),
                new Div(getValueButton, getOutput));
    }

}
