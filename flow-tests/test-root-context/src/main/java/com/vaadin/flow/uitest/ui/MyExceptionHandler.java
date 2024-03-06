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
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.uitest.MyException;

/**
 * The exception handler for the
 *
 * @since 1.0
 */
public class MyExceptionHandler extends Div
        implements HasErrorParameter<MyException> {

    public MyExceptionHandler() {
        Label label = new Label("My exception handler.");
        label.setId("custom-exception");
        add(label);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<MyException> parameter) {
        return 404;
    }
}
