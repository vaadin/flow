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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.AbstractChromeTest;
import com.vaadin.testbench.By;

/**
 * Integration tests for the {@link VaadinButtonView}.
 */
public class VaadinButtonIT extends AbstractChromeTest {

    private WebElement layout;

    @Before
    public void init() {
        open();
        driver.manage().window().setSize(new Dimension(1500, 1000));

        waitForElementPresent(By.tagName("main-layout"));
        layout = findElement(By.tagName("main-layout"));
    }

    @Test
    public void clickOnDefaultButton_textIsDisplayed() {
        WebElement button = layout.findElement(By.id("default-button"));

        button.click();
        waitUntilMessageIsChangedForClickedButton("Vaadin button");
    }

    @Test
    public void clickOnImageButton_textIsDisplayed() {
        WebElement button = layout.findElement(By.id("image-button"));

        WebElement img = button.findElement(By.tagName("img"));
        img.getAttribute("href");

        Assert.assertEquals(getRootURL() + "/img/vaadin-logo.svg",
                img.getAttribute("src"));
        Assert.assertEquals("Vaadin logo", img.getAttribute("alt"));

        button.click();
        waitUntilMessageIsChangedForClickedButton("with image");
    }

    @Test
    public void clickOnAccessibleButton_textIsDisplayed() {
        WebElement button = layout.findElement(By.id("accessible-button"));
        Assert.assertEquals("Click me", button.getAttribute("aria-label"));

        button.click();
        waitUntilMessageIsChangedForClickedButton("Accessible");
    }

    @Test
    public void clickOnTabIndexButtons_textIsDisplayed() {
        WebElement button1 = layout.findElement(By.id("button-tabindex-1"));
        Assert.assertEquals("1", button1.getAttribute("tabindex"));
        WebElement button2 = layout.findElement(By.id("button-tabindex-2"));
        Assert.assertEquals("2", button2.getAttribute("tabindex"));
        WebElement button3 = layout.findElement(By.id("button-tabindex-3"));
        Assert.assertEquals("3", button3.getAttribute("tabindex"));

        button1.click();
        waitUntilMessageIsChangedForClickedButton("1");

        button2.click();
        waitUntilMessageIsChangedForClickedButton("2");

        button3.click();
        waitUntilMessageIsChangedForClickedButton("3");
    }

    @Test
    public void clickOnDisabledButton_nothingIsDisplayed() {
        WebElement button = layout.findElement(By.id("disabled-button"));
        Assert.assertTrue("The button should contain the 'disabled' attribute",
                button.getAttribute("disabled").equals("")
                        || button.getAttribute("disabled").equals("true"));

        button.click();
        WebElement message = layout.findElement(By.id("buttonMessage"));
        Assert.assertEquals("", message.getText());
    }

    private void waitUntilMessageIsChangedForClickedButton(
            String messageString) {
        WebElement message = layout.findElement(By.id("buttonMessage"));
        waitUntil(driver -> message.getText()
                .equals("Button " + messageString + " was clicked."));
    }

    @Override
    protected String getTestPath() {
        return "/vaadin-button";
    }

}
