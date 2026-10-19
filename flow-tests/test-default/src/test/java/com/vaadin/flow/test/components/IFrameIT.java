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
package com.vaadin.flow.test.components;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(IFrameView.class)
public class IFrameIT extends AbstractDefaultIT {

    @BrowserTest
    public void testIFrameReload() {
        open();

        waitForElementPresent(By.id("frame1"));
        getDriver().switchTo().frame("frame1");
        Assertions.assertEquals("A", findElement(By.id("Friday")).getText());

        getDriver().switchTo().parentFrame();
        findElement(By.id("Reload")).click();

        getDriver().switchTo().frame("frame1");
        // The reload replaces the document, so the span is looked up anew on
        // every attempt instead of reusing a reference that turns stale
        waitUntil(ExpectedConditions.textToBe(By.id("Friday"), "B"));
    }

    @BrowserTest
    public void testIFrameWithDynamicResource() {
        open();

        waitForElementPresent(By.id("frame2"));
        getDriver().switchTo().frame("frame2");
        Assertions.assertEquals("Dynamic",
                findElement(By.id("content")).getText());
    }
}
