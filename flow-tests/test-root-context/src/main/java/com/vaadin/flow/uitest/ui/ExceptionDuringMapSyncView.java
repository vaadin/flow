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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ExceptionDuringMapSyncView", layout = ViewTestLayout.class)
public class ExceptionDuringMapSyncView extends AbstractErrorHandlerView {

    public ExceptionDuringMapSyncView() {
        Input input = new Input();
        input.addValueChangeListener(event -> {
            throw new RuntimeException(
                    "Intentional exception in property sync handler");
        });
        add(input);
    }

}
