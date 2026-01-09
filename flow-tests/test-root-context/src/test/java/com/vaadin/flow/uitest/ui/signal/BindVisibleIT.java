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
package com.vaadin.flow.uitest.ui.signal;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration tests for binding element visibility to signals.
 */
public class BindVisibleIT extends ChromeBrowserTest {

    @Test
    public void toggleSignal_updatesVisibility() {
        open();

        waitForElementVisible(By.id("target-visible-initially"));
        waitForElementNotPresent(By.id("target-hidden-initially"));

        NativeButtonElement toggle1 = $(NativeButtonElement.class)
                .id("toggle-button-1");
        NativeButtonElement toggle2 = $(NativeButtonElement.class)
                .id("toggle-button-2");

        toggle1.click();
        // present in DOM but invisible
        waitForElementPresent(By.id("target-visible-initially"));
        waitUntil(ExpectedConditions.invisibilityOfElementLocated(
                By.id("target-visible-initially")));

        toggle2.click();
        waitForElementVisible(By.id("target-hidden-initially"));
    }
}
