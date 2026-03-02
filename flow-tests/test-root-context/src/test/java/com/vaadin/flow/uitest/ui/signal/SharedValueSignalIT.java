/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.signal;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration test for SharedValueSignal behavior with repeatable reads.
 */
public class SharedValueSignalIT extends ChromeBrowserTest {

    // verifies that a shared value signal with repeatable reads behaves as
    // expected when the value is updated between reads
    @Test
    public void repeatableReads_updateSignalBetweenReads_valueStaysSame() {
        open();

        NativeButtonElement button = $(NativeButtonElement.class)
                .id("repeatable-read-button");
        NativeButtonElement printSignalButton = $(NativeButtonElement.class)
                .id("print-signal-button");

        button.click();
        printSignalButton.click();

        Assert.assertEquals("initial", getFirstReadValue());
        Assert.assertEquals("initial", getSecondReadValue());
        Assert.assertEquals("updated #1", getSecondPeekConfirmedValue());
        Assert.assertEquals("updated #1", getSignalValue());

        button.click();
        printSignalButton.click();

        Assert.assertEquals("updated #1", getFirstReadValue());
        Assert.assertEquals("updated #1", getSecondReadValue());
        Assert.assertEquals("updated #2", getSecondPeekConfirmedValue());
        Assert.assertEquals("updated #2", getSignalValue());

        button.click();
        printSignalButton.click();

        Assert.assertEquals("updated #2", getFirstReadValue());
        Assert.assertEquals("updated #2", getSecondReadValue());
        Assert.assertEquals("updated #3", getSecondPeekConfirmedValue());
        Assert.assertEquals("updated #3", getSignalValue());
    }

    private String getFirstReadValue() {
        return findElement(By.id("first-read-value")).getText();
    }

    private String getSecondReadValue() {
        return findElement(By.id("second-read-value")).getText();
    }

    private String getSecondPeekConfirmedValue() {
        return findElement(By.id("second-peek-confirmed-value")).getText();
    }

    private String getSignalValue() {
        return findElement(By.id("signal-value")).getText()
                .replace("Current signal value: ", "");
    }
}
