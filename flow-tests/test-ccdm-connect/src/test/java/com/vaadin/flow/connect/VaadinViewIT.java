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
package com.vaadin.flow.connect;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * Class for testing issues in a spring-boot container.
 */
public class VaadinViewIT extends ChromeBrowserTest {
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

        String tagName = "test-component";

        waitForElementPresent(By.tagName(tagName));

        testComponent = $(tagName).first();
        waitUntil(
                drv -> getCommandExecutor().executeScript(
                        "return arguments[0].$ !== undefined", testComponent),
                25);
    }

    /**
     * Just a control test that assures that webcomponents is working.
     */
    @Test
    public void should_load_web_component() {
        WebElement content = testComponent.$(TestBenchElement.class).id("content");
        WebElement button = testComponent.$(TestBenchElement.class).id("button");
        button.click();
        Assert.assertEquals("Hello World", content.getText());
    }

    /**
     * Just a control test that assures that webcomponents is working.
     * @throws Exception
     */
    @Test
    public void should_request_connect_service() throws Exception {
        WebElement button = testComponent.$(TestBenchElement.class).id("connect");
        button.click();

        WebElement content = testComponent.$(TestBenchElement.class).id("content");
        // Wait for the server connect response
        waitUntil(ExpectedConditions.textToBePresentInElement(content,
                "Hello, Friend!"), 25);
    }
}
