/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
