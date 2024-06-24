/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ClientSideExceptionHandlingView", layout = ViewTestLayout.class)
@JavaScript("externalErrorTrigger.js")
public class ClientSideExceptionHandlingView extends Div {

    static final String CAUSE_EXCEPTION_ID = "causeException";
    private NativeButton causeException;

    public ClientSideExceptionHandlingView() {
        causeException = new NativeButton("Cause client side exception", e -> {
            getUI().get().getPage().executeJavaScript("null.foo");
        });
        causeException.setId(CAUSE_EXCEPTION_ID);

        /*
         * Used for manually testing that the name of an offending external
         * function is actually reported in the browser.
         */
        NativeButton causeExternalException = new NativeButton(
                "Cause external client side exception", e -> {
                    getUI().get().getPage()
                            .executeJavaScript("externalErrorTrigger()");
                });

        add(causeException, causeExternalException);
    }
}
