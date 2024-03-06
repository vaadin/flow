/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;

public abstract class AbstractErrorHandlerView extends AbstractDivView {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (attachEvent.isInitialAttach()) {
            attachEvent.getSession().setErrorHandler(e -> {
                Div div = new Div(
                        new Text("An error occurred: " + e.getThrowable()));
                div.addClassName("error");
                add(div);
            });
        }
    }
}
