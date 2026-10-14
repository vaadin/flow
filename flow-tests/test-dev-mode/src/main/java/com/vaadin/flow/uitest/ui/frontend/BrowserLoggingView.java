/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.frontend;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.frontend.BrowserLoggingView", layout = ViewTestLayout.class)
@Tag("div")
@JavaScript(value = "./consoleLoggingProxy.js", loadMode = LoadMode.INLINE)
public class BrowserLoggingView extends Div {
    public BrowserLoggingView() {
        NativeLabel label = new NativeLabel("Just a label");
        label.setId("elementId");
        add(label);

        NativeButton causeException = new NativeButton(
                "Cause client side exception", e -> {
                    getUI().get().getPage().executeJs("null.foo");
                });
        causeException.setId("exception");
        add(causeException);
    }
}
