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
import org.openqa.selenium.Keys;

import com.vaadin.flow.component.html.testbench.InputTextElement;

public class ExceptionDuringMapSyncIT extends AbstractErrorIT {

    @Test
    public void exceptionInMapSyncDoesNotCauseInternalError() {
        open();
        $(InputTextElement.class).first().sendKeys("foo", Keys.ENTER);

        assertNoSystemErrors();

        assertErrorReported(
                "An error occurred: java.lang.RuntimeException: Intentional exception in property sync handler");
    }

}
