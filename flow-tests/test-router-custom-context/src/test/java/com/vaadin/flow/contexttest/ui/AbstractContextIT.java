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
package com.vaadin.flow.contexttest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public abstract class AbstractContextIT extends ChromeBrowserTest {

    static final String JETTY_CONTEXT = System.getProperty(
            "vaadin.test.jettyContextPath", "/custom-context-router");

    private static final String RED = "rgba(255, 0, 0, 1)";
    private static final String BLUE = "rgba(0, 0, 255, 1)";

    protected abstract String getAppContext();

    protected abstract void verifyCorrectUI();

    @Override
    protected String getTestPath() {
        return JETTY_CONTEXT + getAppContext();
    }

    @Test
    public void testStyleInjection() {
        open();
        verifyCorrectUI();
        styleInjection();
    }

    @Test
    public void testScriptInjection() {
        open();
        verifyCorrectUI();
        scriptInjection();
    }

    private void styleInjection() {
        // Initial stylesheet makes all text red
        Assert.assertEquals(RED, findElementById("hello").getCssValue("color"));

        // Inject stylesheet which makes text blue
        findElementById("loadBlue").click();

        // Wait as the framework will not stop until the stylesheet is loaded
        waitUntil(input -> findElementById("hello").getCssValue("color")
                .equals(BLUE));
    }

    private void scriptInjection() {
        // Initial JS registers a body click handler
        findElement(By.cssSelector("body")).click();
        String addedBodyText = findElement(By.cssSelector(".body-click-added"))
                .getText();
        Assert.assertEquals(
                "Click on body, reported by JavaScript click handler",
                addedBodyText);

        // Inject scripts
        findElementById("loadJs").click();
        String addedJsText = findElementById("appended-element").getText();
        Assert.assertEquals("Added by script", addedJsText);
    }

    protected WebElement findElementById(String id) {
        return findElement(By.id(id));
    }

}
