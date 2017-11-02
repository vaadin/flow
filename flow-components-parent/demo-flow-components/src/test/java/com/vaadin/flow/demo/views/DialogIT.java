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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.demo.ComponentDemoTest;
import org.openqa.selenium.By;

/**
 * Integration tests for the {@link DialogView}.
 */
public class DialogIT extends ComponentDemoTest {

    private static final String DIALOG_OVERLAY_TAG = "vaadin-dialog-overlay";

    @Test
    public void openAndCloseBasicDialog() {
        findElement(By.id("basic-dialog-button")).click();
        assertDialogOverlayContent("Hello World!");

        closeAndVerify();
    }

    @Test
    public void openAndCloseDialogWithListener() {
        WebElement label = findElement(By.id("dialog-message-label"));
        Assert.assertTrue(label.getText().isEmpty());

        findElement(By.id("dialog-with-listener-button")).click();
        assertDialogOverlayContent("Hello World!");
        Assert.assertTrue(label.getText().contains("opened"));

        closeAndVerify();
        Assert.assertTrue(label.getText().contains("closed"));
    }

    @Test
    public void openAndCloseDialogWithHtml() {
        scrollIntoViewAndClick(findElement(By.id("dialog-with-html-button")));

        getOverlayContent().findElement(By.tagName("b"));
        getOverlayContent().findElement(By.tagName("ul"));

        closeAndVerify();
    }

    private void assertDialogOverlayContent(String expected) {
        String content = getOverlayContent().getText();
        Assert.assertTrue(content.contains(expected));
    }

    private WebElement getOverlayContent() {
        WebElement overlay = findElement(By.tagName(DIALOG_OVERLAY_TAG));
        return getInShadowRoot(overlay, By.id("content"));
    }

    private void closeAndVerify() {
        new Actions(getDriver()).sendKeys(Keys.ESCAPE).perform();
        waitForElementNotPresent(By.tagName(DIALOG_OVERLAY_TAG));
    }

    @Override
    protected String getTestPath() {
        return "/vaadin-dialog";
    }

}
