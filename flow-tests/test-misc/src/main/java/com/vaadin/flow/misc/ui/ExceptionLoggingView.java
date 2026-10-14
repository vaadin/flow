/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(value = "exception-logging")
@JavaScript("./exception-logging.js")
@JavaScript("./consoleLoggingProxy.js")
public class ExceptionLoggingView extends Div {

    public ExceptionLoggingView() {
        NativeButton causeException = new NativeButton(
                "Cause client side exception", e -> {
                    getUI().get().getPage().executeJs("null.foo");
                });
        causeException.setId("exception");
        add(causeException);

        /*
         * Used for manually testing that the name of an offending external
         * function is actually reported in the browser.
         */
        NativeButton causeExternalException = new NativeButton(
                "Cause external client side exception", e -> {
                    getUI().get().getPage().executeJs("externalErrorTrigger()");
                });
        causeExternalException.setId("externalException");

        add(causeException, causeExternalException);
    }
}