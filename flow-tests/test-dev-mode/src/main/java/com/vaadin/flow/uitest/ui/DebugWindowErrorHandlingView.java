/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DebugWindowErrorHandlingView", layout = ViewTestLayout.class)
@JavaScript("/externalErrorTrigger.js")
public class DebugWindowErrorHandlingView extends Div {

    static final String EXEC_JS_EXCEPTION_ID = "execJsException";
    static final String CLIENT_SIDE_EXCEPTION_ID = "clientSideException";
    static final String CLIENT_SIDE_ERROR_ID = "clientSideError";
    static final String CLIENT_SIDE_PROMISE_REJECTION_ID = "clientSidePromiseRejection";
    static final String NUMBER_OF_ERRORS_ID = "numberOfErrors";
    static final String CAUSE_ERRORS_ID = "causeErrors";

    public DebugWindowErrorHandlingView() {
        NativeButton execJsException = new NativeButton("Exception from execJS",
                e -> {
                    getUI().get().getPage().executeJs("null.foo");
                });
        execJsException.setId(EXEC_JS_EXCEPTION_ID);

        NativeButton clientSideException = new NativeButton(
                "Exception from client side", e -> {
                    getUI().get().getPage()
                            .executeJs("setTimeout(() => {null.foo;}, 1);");
                });
        clientSideException.setId(CLIENT_SIDE_EXCEPTION_ID);

        NativeButton clientSideErrorLog = new NativeButton(
                "Client side console.error", e -> {
                    getUI().get().getPage().executeJs(
                            "setTimeout(() => {console.error('Client side error');}, 1);");
                });
        clientSideErrorLog.setId(CLIENT_SIDE_ERROR_ID);

        NativeButton clientSidePromiseRejection = new NativeButton(
                "Client side promise rejection", e -> {
                    getUI().get().getPage().executeJs(
                            "import('./this-file-does-not-exist.js')");
                });
        clientSidePromiseRejection.setId(CLIENT_SIDE_PROMISE_REJECTION_ID);

        /*
         * Used for manually testing that the name of an offending external
         * function is actually reported in the browser.
         */
        NativeButton causeExternalException = new NativeButton(
                "Cause external client side exception", e -> {
                    getUI().get().getPage().executeJs("externalErrorTrigger()");
                });

        Input numberOfErrors = new Input();
        numberOfErrors.setPlaceholder("Number of errors");
        numberOfErrors.setId(NUMBER_OF_ERRORS_ID);
        NativeButton causeErrors = new NativeButton("Cause errors", e -> {
            getUI().get().getPage().executeJs(
                    "setTimeout(() => {for (i=1; i <= $0; i++) {console.error(`Error ${i}`);}}, 0);",
                    Integer.parseInt(numberOfErrors.getValue()));
        });
        causeErrors.setId(CAUSE_ERRORS_ID);
        add(execJsException, clientSideException, clientSideErrorLog,
                clientSidePromiseRejection, causeExternalException,
                numberOfErrors, causeErrors);
    }
}
