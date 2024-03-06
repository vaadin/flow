/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;

@JsModule("./divConnector.js")
public class JsInjectedDiv extends Div {

    public JsInjectedDiv() {
        getElement().executeJs("window.divConnector.jsFunction(this)");
    }

    @ClientCallable
    private void handleClientCall(String value) {
        setText(value);
    }
}
