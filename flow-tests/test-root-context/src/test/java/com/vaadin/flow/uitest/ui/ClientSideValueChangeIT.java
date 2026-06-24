/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ClientSideValueChangeIT extends ChromeBrowserTest {

    @Test
    public void clientSideValueEntryDuringRoundTrip_enteredValueShouldNotBeOverridden() {
        open();

        getCommandExecutor().disableWaitForVaadin();

        InputTextElement input = $(InputTextElement.class).id("inputfield");
        input.setValue("abc");
        input.sendKeys("123");

        waitUntil(ExpectedConditions.presenceOfElementLocated(By.id("status")));

        Assert.assertEquals(
                "User input during round-trip was unexpectedly overridden",
                "abc123",
                $(InputTextElement.class).id("inputfield").getValue());
    }

    @Test
    public void clientSideValueEntryDuringRoundTrip_serverChangesValue_serverValueShouldBeUsed() {
        open();

        getCommandExecutor().disableWaitForVaadin();

        InputTextElement input = $(InputTextElement.class)
                .id("inputfieldserversetsvalue");
        input.setValue("abc");
        input.sendKeys("123");

        waitUntil(ExpectedConditions
                .presenceOfElementLocated(By.id("statusserversetsvalue")));

        Assert.assertEquals(
                "Value set by server during round-trip was unexpectedly overridden",
                "fromserver", $(InputTextElement.class)
                        .id("inputfieldserversetsvalue").getValue());
    }
}
