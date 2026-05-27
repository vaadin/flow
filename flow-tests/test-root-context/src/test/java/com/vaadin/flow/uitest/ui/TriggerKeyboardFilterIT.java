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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TriggerKeyboardFilterIT extends ChromeBrowserTest {

    @Test
    public void onlyFilteredKeys_reachTheServer() {
        open();

        WebElement field = findElement(By.id("source"));
        WebElement status = findElement(By.id("status"));

        // The action appends event.key for every fire, so the final status
        // text is the concatenation of every key that passed the filter, in
        // order. Interleaving filtered and non-filtered keys makes a leak
        // observable: had any letter reached the server, it would appear
        // between the filtered names below.
        field.sendKeys("a");
        field.sendKeys(Keys.ENTER);
        waitUntil(d -> "Enter".equals(status.getText()));

        field.sendKeys("b");
        field.sendKeys(Keys.ESCAPE);
        waitUntil(d -> "EnterEscape".equals(status.getText()));

        field.sendKeys("c");
        field.sendKeys(Keys.ENTER);
        waitUntil(d -> "EnterEscapeEnter".equals(status.getText()));
    }
}
