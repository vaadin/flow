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
package com.vaadin.flow.demo.views;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.ComponentDemoTest;
import org.openqa.selenium.By;

/**
 * Integration tests for the {@link PaperButtonView}.
 *
 */
public class PaperButtonIT extends ComponentDemoTest {
    @Test
    public void clickOnRaisedButton_textIsDisaplayed() {
        WebElement button = layout.findElement(By.id("raised-button"));
        WebElement message = layout.findElement(By.id("raised-button-message"));

        Assert.assertTrue("Button should be raised",
                isAttributeTrue(button, "raised"));
        scrollIntoViewAndClick(button);

        waitUntil(driver -> message.getText()
                .equals("Button " + button.getText() + " was clicked."));
    }

    @Test
    public void clickOnLinkButton_textIsDisaplayed() {
        WebElement button = layout.findElement(By.id("link-button"));
        WebElement message = layout.findElement(By.id("link-button-message"));

        Assert.assertTrue("Button should have noink",
                isAttributeTrue(button, "noink"));
        scrollIntoViewAndClick(button);

        waitUntil(driver -> message.getText()
                .equals("Button " + button.getText() + " was clicked."));
    }

    @Test
    public void clickOnToggleButton_textIsDisaplayed() {
        WebElement button = layout.findElement(By.id("toggle-button"));
        WebElement message = layout.findElement(By.id("toggle-button-message"));

        Assert.assertTrue("Button should have toggles",
                isAttributeTrue(button, "toggles"));
        Assert.assertTrue("Button should be raised",
                isAttributeTrue(button, "raised"));
        scrollIntoViewAndClick(button);

        waitUntil(driver -> message.getText()
                .equals("Button " + button.getText() + " was clicked."));
    }

    @Test
    public void disableButtonIsShown() {
        WebElement button = layout.findElement(By.id("disabled-button"));

        Assert.assertTrue("Button should be disabled",
                isAttributeTrue(button, "disabled"));

    }

    private boolean isAttributeTrue(WebElement element, String attribute) {
        return element.getAttribute(attribute) != null
                && !"false".equals(element.getAttribute(attribute));
    }

    @Override
    protected String getTestPath() {
        return "/paper-button";
    }

}
