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
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.WaitForVaadinView", layout = ViewTestLayout.class)
public class WaitForVaadinView extends AbstractDivView {
    private final Div message;
    private final NativeButton button;

    public WaitForVaadinView() {
        message = new Div();
        message.setText("Not updated");
        message.setId("message");

        button = new NativeButton("Click to update", e -> waitAndUpdate());

        add(message, button);
    }

    private void waitAndUpdate() {
        try {
            message.setText("Updated");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
