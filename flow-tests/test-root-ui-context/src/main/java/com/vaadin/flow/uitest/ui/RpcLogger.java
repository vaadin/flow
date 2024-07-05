/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route("rpc-logger")
public class RpcLogger extends Div {

    public RpcLogger() {
        NativeButton button = new NativeButton("Click Me");
        button.addClickListener(event -> {
        });
        Input input = new Input();
        add(button, input);
    }
}
