/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.CallFunctionBeforeRemoveView", layout = ViewTestLayout.class)
public class CallFunctionBeforeRemoveView extends AbstractDivView {

    public CallFunctionBeforeRemoveView() {
        Input input = new Input();

        add(input);

        NativeButton button = new NativeButton("Call function and detach");
        add(button);
        button.addClickListener(event -> {
            if (input.getParent().isPresent()) {
                input.getElement().callJsFunction("focus");
                remove(input);
            }
        });
    }
}
