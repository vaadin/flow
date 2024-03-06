/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Test;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;

public class ErrorHandlingIT extends AbstractErrorIT {

    @Test
    public void exceptionInClickHandlerDoesNotCauseInternalError() {
        open();
        $(NativeButtonElement.class).id("errorButton").click();

        assertNoSystemErrors();

        assertErrorReported(
                "An error occurred: java.lang.IllegalStateException: Intentional fail in click handler");
    }

    @Test
    public void exceptionInBeforeClientResponseDoesNotCauseInternalError() {
        open();
        $(NativeButtonElement.class).id("clientResponseButton").click();

        assertNoSystemErrors();

        assertErrorReported(
                "An error occurred: java.lang.IllegalStateException: Intentional fail in beforeClientResponse");
    }

}
