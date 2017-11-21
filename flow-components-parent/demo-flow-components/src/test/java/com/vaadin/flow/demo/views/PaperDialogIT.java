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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.demo.ComponentDemoTest;
import com.vaadin.testbench.By;

public class PaperDialogIT extends ComponentDemoTest {
    @Before
    public void init() {
        assertFalse(isElementPresent(By.tagName("paper-dialog")));
    }

    @Test
    public void openAndClosePlainDialog() {
        // click on the button to open the dialog
        getButtonWithText("Plain dialog").click();
        waitForElementPresent(By.tagName("paper-dialog"));
        assertMessageIsEqualsTo("Plain dialog was opened");

        WebElement dialog = findElement(By.tagName("paper-dialog"));
        assertTrue(dialog.getText().contains("Plain dialog"));

        // click on the page, should close the dialog
        clickOutsideTheDialog(dialog);
        waitUntil(driver -> dialog.getCssValue("display").equals("none"));
        assertMessageIsEqualsTo("Plain dialog was closed");
    }

    @Test
    public void openAndCloseModalDialog() {
        // click on the button to open the dialog
        getButtonWithText("Modal dialog").click();
        waitForElementPresent(By.tagName("paper-dialog"));
        assertMessageIsEqualsTo("Modal dialog was opened");

        WebElement dialog = findElement(By.tagName("paper-dialog"));
        assertTrue(dialog.getText().contains("Modal dialog"));

        // click on the page, should not close the dialog
        clickOutsideTheDialog(dialog);
        assertFalse(dialog.getCssValue("display").equals("none"));
        assertMessageIsEqualsTo("Modal dialog was opened");

        // click on the close button, should close the dialog
        WebElement close = dialog.findElement(By.tagName("paper-button"));
        close.click();
        waitUntil(driver -> dialog.getCssValue("display").equals("none"));
        assertMessageIsEqualsTo("Modal dialog was closed");
    }

    @Test
    public void openAndCloseNestedDialogs() {
        // click on the button to open the dialog
        scrollIntoViewAndClick(getButtonWithText("Nested dialogs"));
        waitForElementPresent(By.tagName("paper-dialog"));
        assertMessageIsEqualsTo("Nested dialogs was opened");

        WebElement dialog = findElement(By.tagName("paper-dialog"));
        assertTrue(dialog.getText().contains("Nested dialogs"));

        // click on the button to open the second dialog
        dialog.findElement(By.tagName("paper-button")).click();

        waitForElementPresent(By.id("second-dialog"));
        assertMessageIsEqualsTo("Second dialog was opened");

        WebElement secondDialog = findElement(By.id("second-dialog"));
        assertTrue(secondDialog.getText().contains("Second dialog"));

        // click on the page, should close the second dialog and not the first
        clickOutsideTheDialog(secondDialog);
        waitUntil(driver -> secondDialog.getCssValue("display").equals("none"));
        assertFalse(dialog.getCssValue("display").equals("none"));
        assertMessageIsEqualsTo("Second dialog was closed");

        // click on the page again, should close the first dialog
        clickOutsideTheDialog(dialog);
        waitUntil(driver -> dialog.getCssValue("display").equals("none"));
        assertMessageIsEqualsTo("Nested dialogs was closed");
    }

    @Test
    public void openAndCloseDialogWithActions() {
        // click on the button to open the dialog
        WebElement openButton = getButtonWithText("Dialog with actions");
        scrollIntoViewAndClick(openButton);
        waitForElementPresent(By.tagName("paper-dialog"));
        assertMessageIsEqualsTo("Dialog with actions was opened");

        WebElement dialog = findElement(By.tagName("paper-dialog"));
        assertTrue(dialog.getText().contains("Dialog with actions"));

        // click on the Do nothing button, should not close the dialog
        getButtonWithText(dialog, "Do nothing").click();
        assertFalse(dialog.getCssValue("display").equals("none"));
        assertMessageIsEqualsTo("Dialog with actions was opened");

        // click on the Decline button, should close the dialog
        getButtonWithText(dialog, "Decline").click();
        waitUntil(driver -> dialog.getCssValue("display").equals("none"));
        assertMessageIsEqualsTo("Dialog with actions was closed");

        // reopen the dialog
        openButton.click();
        waitUntilNot(driver -> dialog.getCssValue("display").equals("none"));
        assertMessageIsEqualsTo("Dialog with actions was opened");

        // click on the Accept button, should close the dialog
        getButtonWithText(dialog, "Accept").click();
        waitUntil(driver -> dialog.getCssValue("display").equals("none"));
        assertMessageIsEqualsTo("Dialog with actions was closed");
    }

    @Test
    public void openAndCloseAnimatedDialog() {
        // click on the button to open the dialog
        scrollIntoViewAndClick(getButtonWithText("Animated dialog"));
        waitForElementPresent(By.tagName("paper-dialog"));
        assertMessageIsEqualsTo("Animated dialog was opened");

        WebElement dialog = findElement(By.tagName("paper-dialog"));
        assertTrue(dialog.getText().contains("Animated dialog"));

        // click on the page, should close the dialog
        clickOutsideTheDialog(dialog);
        waitUntil(driver -> dialog.getCssValue("display").equals("none"));
        assertMessageIsEqualsTo("Animated dialog was closed");
    }

    private void assertMessageIsEqualsTo(String message) {
        waitForElementPresent(By.id("dialogsMessage"));
        WebElement messageDiv = findElement(By.id("dialogsMessage"));
        assertTrue(messageDiv.getText().equalsIgnoreCase(message));
    }

    private void clickOutsideTheDialog(WebElement dialog) {
        Dimension size = dialog.getSize();
        new Actions(getDriver()).moveToElement(dialog, size.getWidth() + 50,
                size.getHeight() + 50).click().build().perform();
    }

    private WebElement getButtonWithText(String text) {
        return this.getButtonWithText(findElement(By.tagName("main-layout")),
                text);
    }

    private WebElement getButtonWithText(WebElement root, String text) {
        List<WebElement> buttons = root
                .findElements(By.tagName("paper-button"));
        return buttons.stream()
                .filter(button -> button.getText().equalsIgnoreCase(text))
                .findAny().get();
    }

    @Override
    protected String getTestPath() {
        return "/paper-dialog";
    }

}
