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
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

public class RouterLinksIT extends ChromeBrowserTest {

    public static final String TEXT_INPUT = "abc";

    @Test
    public void handleEventOnServer() {
        open();

        String originalUrl = getDriver().getCurrentUrl();
        WebElement textInput = getInShadowRoot(findElement(By.id("template")),
                By.id("input"));

        Assert.assertTrue("Input was not empty",
                textInput.getAttribute("value").isEmpty());
        textInput.sendKeys(TEXT_INPUT);
        Assert.assertEquals("Input was missing contents", TEXT_INPUT,
                textInput.getAttribute("value"));
        WebElement link = getInShadowRoot(findElement(By.id("template")),
                By.linkText("Navigate"));

        Assert.assertEquals("Navigate", link.getText());

        getCommandExecutor().executeScript("return arguments[0].click()",
                link);

        // Original url should end with UI and the navigation link Template
        Assert.assertNotEquals(originalUrl, getDriver().getCurrentUrl());

        textInput = getInShadowRoot(findElement(By.id("template")), By.id("input"));

        Assert.assertEquals("Input didn't keep content through navigation",
                TEXT_INPUT, textInput.getAttribute("value"));
    }
}
