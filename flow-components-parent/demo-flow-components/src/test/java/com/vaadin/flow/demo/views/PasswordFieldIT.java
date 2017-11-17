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

import com.vaadin.flow.demo.ComponentDemoTest;
import com.vaadin.testbench.By;
import com.vaadin.ui.textfield.PasswordField;

/**
 * Integration tests for the {@link PasswordField}.
 */
public class PasswordFieldIT extends ComponentDemoTest {
    @Override
    protected String getTestPath() {
        return "/vaadin-password-field";
    }

    @Test
    public void valueChangeListenerReportsCorrectValues() {
        WebElement passwordFieldValueDiv = layout
                .findElement(By.id("password-field-value"));
        WebElement passwordField = layout
                .findElement(By.id("password-field-with-value-change-listener"));

        passwordField.sendKeys("a");
        waitUntilTextsEqual("Password field value changed from '' to 'a'",
                passwordFieldValueDiv.getText());

        passwordField.sendKeys(Keys.BACK_SPACE);
        waitUntilTextsEqual("Password field value changed from 'a' to ''",
                passwordFieldValueDiv.getText());
    }

    @Test
    public void passwordFieldHasPlaceholder() {
        WebElement passwordField = layout
                .findElement(By.id("password-field-with-value-change-listener"));
        Assert.assertEquals(passwordField.getAttribute("placeholder"),
                "placeholder text");
    }

    private void waitUntilTextsEqual(String expected, String actual) {
        waitUntil(driver -> expected.equals(actual));
    }
}
