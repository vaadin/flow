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
package com.vaadin.flow.mixedtest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class IdTestIT extends ChromeBrowserTest {
    @Override
    protected String getTestPath() {
        return "/";
    }

    @Test
    public void testIds() {
        open();
        waitUntilWithMessage(ExpectedConditions
                        .presenceOfElementLocated(By.tagName("my-component")),
                "Failed to load my-component", 25);

        WebElement myComponent = findElement(By.tagName("my-component"));

        // wait for polymer initalisation
        waitUntillWithMessage(driver -> getCommandExecutor().executeScript(
                "return arguments[0].constructor.polymerElementVersion !== undefined",
                myComponent),
                "Failed to load constructor.polymerElementVersion for 'my-component'");

        waitUntillWithMessage(driver -> getCommandExecutor()
                .executeScript("return arguments[0].$ !== undefined",
                        myComponent), "Failed to load $ for 'my-component'");

        waitUntillWithMessage(webDriver -> ((WebElement) getCommandExecutor()
                .executeScript("return arguments[0].shadowRoot", myComponent))
                .findElements(By.id("content")).stream().findFirst()
                .isPresent(), "Failed to load content element from shadowroot");

        WebElement content = getInShadowRoot(myComponent, By.id("content"));
        Assert.assertEquals("", content.getText());

        WebElement button = getInShadowRoot(myComponent, By.id("button"));
        button.click();
        Assert.assertEquals("1", content.getText());

        button.click();
        Assert.assertEquals("2", content.getText());
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
