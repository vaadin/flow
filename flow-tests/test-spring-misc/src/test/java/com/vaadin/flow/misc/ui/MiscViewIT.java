/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.misc.ui;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * Class for testing issues in a spring-boot container.
 */
public class MiscViewIT extends ChromeBrowserTest {
    @Override
    protected String getTestPath() {
        return "/";
    }

    private TestBenchElement testComponent;

    @Before
    public void setup() throws Exception {
        super.setup();

        // Open browser
        open();
        
        // Wait for the test WC tag in the page
        String tagName = "test-component";
        waitUntilWithMessage(
                ExpectedConditions
                        .presenceOfElementLocated(By.tagName(tagName)),
                "Failed to load " + tagName, 25);
        testComponent = $(tagName).first();
        // Wait for the test WC to be initialised as a  Polymer element
        waitUntillWithMessage(
                driver -> getCommandExecutor().executeScript(
                        "return arguments[0].$ !== undefined", testComponent),
                "Failed to load $ for " + tagName);
    }

    /**
     * Just a control test that assures that webcomponents is working.
     */
    @Test
    public void testWebComponentBehavior() {
        WebElement content = testComponent.$(TestBenchElement.class).id("content");
        Assert.assertEquals("", content.getText());

        WebElement button = testComponent.$(TestBenchElement.class).id("button");
        button.click();
        Assert.assertEquals("Hello World", content.getText());
    }

    /**
     * Check for #5994 issue.
     * 
     * vaadin-development-mode-detection sets 'Vaadin.developmentMode' based
     * on the host-name, so 'http://localhost' means developmentMode=true
     * unless it's a flow app, and server forces productionMode for the
     * client. 
     */
    @Test
    public void testDevelopmentModeFlag() {
        String flowProdMode = String.valueOf(
                executeScript("return window.Vaadin.Flow.clients.ROOT.productionMode"));
        Assert.assertEquals("true", flowProdMode);

        String detectDevMode = String.valueOf(
                executeScript("return window.Vaadin.developmentMode"));
        Assert.assertEquals("false", detectDevMode);
    }

    private void waitUntillWithMessage(ExpectedCondition<?> condition,
            String message) {
        waitUntilWithMessage(condition, message, 10);
    }

    private void waitUntilWithMessage(ExpectedCondition<?> condition,
            String message, long time) {
        try {
            waitUntil(condition, time);
        } catch (TimeoutException te) {
            Assert.fail(message);
        }
    }
}
