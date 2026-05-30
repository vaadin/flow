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

public class TriggerSequenceIT extends ChromeBrowserTest {

    @Test
    public void completedSequence_fires_partialSequence_doesNotFire() {
        open();

        WebElement field = findElement(By.id("source"));
        WebElement status = findElement(By.id("status"));

        // "hi" completes the sequence — one fire, status becomes "!".
        field.sendKeys("hi");
        waitUntil(d -> "!".equals(status.getText()));

        // "hx" pushes the position back (h matches slot 0, x resets), then
        // "hi" completes again. The status should have advanced by exactly
        // one fire, ending at "!!", confirming the interrupted attempt was
        // not counted.
        field.sendKeys("hx");
        field.sendKeys("hi");
        waitUntil(d -> "!!".equals(status.getText()));

        // Lone "i" without preceding "h" doesn't reach slot 1 from a fresh
        // i=0 (slot 0 is h). Adding the leading h before the next "hi" is
        // also required.
        field.sendKeys("i");
        field.sendKeys("hi");
        waitUntil(d -> "!!!".equals(status.getText()));
    }
}
