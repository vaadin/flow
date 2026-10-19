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
package com.vaadin.flow.test.dependencies;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(DependencyView.class)
public class DependencyIT extends AbstractDefaultIT {

    private static final String RED = "rgba(255, 0, 0, 1)";
    private static final String BLUE = "rgba(0, 0, 255, 1)";

    @BrowserTest
    public void styleInjection() {
        open();
        // Initial stylesheet makes all text red
        Assertions.assertEquals(RED,
                findElement(By.id("hello")).getCssValue("color"));

        // Inject stylesheet which makes text blue
        findElement(By.id("loadBlue")).click();
        Assertions.assertEquals(BLUE,
                findElement(By.id("hello")).getCssValue("color"));
    }

    @BrowserTest
    public void scriptInjection() {
        open();
        // Initial JS registers a body click handler
        clickElementWithJs(findElement(By.tagName("body")));
        String addedBodyText = findElement(By.cssSelector(".body-click-added"))
                .getText();
        Assertions.assertEquals(
                "Click on body, reported by JavaScript click handler",
                addedBodyText);

        // Inject scripts
        findElement(By.id("loadJs")).click();
        waitForElementPresent(By.id("read-global-var-text"));
        String addedJsText = findElement(By.id("read-global-var-text"))
                .getText();
        Assertions.assertEquals(
                "Second script loaded. Global variable (window.globalVar) is: 'Set by set-global-var.js'",
                addedJsText);
    }
}
