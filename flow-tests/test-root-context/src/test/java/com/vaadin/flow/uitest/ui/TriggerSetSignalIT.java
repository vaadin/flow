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
package com.vaadin.flow.uitest.ui;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TriggerSetSignalIT extends ChromeBrowserTest {

    @Test
    public void typingIntoInput_pushesValueIntoSignal_andEffectUpdatesStatus() {
        open();

        WebElement field = findElement(By.id("source"));
        WebElement status = findElement(By.id("status"));

        field.sendKeys("hi");

        // Each "input" event fires the trigger, which decodes the field's
        // value property and assigns it to the ValueSignal. A Signal.effect
        // on the server mirrors the signal into the status div.
        waitUntil(d -> "hi".equals(status.getText()));

        field.sendKeys(" there");
        waitUntil(d -> "hi there".equals(status.getText()));
    }
}
