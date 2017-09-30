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

import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.ComponentDemoTest;
import com.vaadin.testbench.By;
import com.vaadin.ui.passwordfield.PasswordField;

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
        WebElement passwordField = getPasswordField();
        Assert.assertEquals(passwordField.getAttribute("placeholder"),
                "Password");
        checkRevealMessage();

        String input = "abc";
        passwordField.sendKeys(input);
        waitUntil(driver -> input
                .equals(getPasswordField().getAttribute("value")));

        boolean isShownOriginally = isRevealButtonShown();
        layout.findElement(By.id("toggleButton")).click();
        waitUntil(driver -> isRevealButtonShown() != isShownOriginally);
        layout.findElement(By.id("toggleButton")).click();
        waitUntil(driver -> isRevealButtonShown() == isShownOriginally);
    }

    private WebElement getPasswordField() {
        return layout.findElement(By.id("passwordField"));
    }

    private boolean isRevealButtonShown() {
        return !Boolean.parseBoolean(
                getPasswordField().getAttribute("revealButtonHidden"));
    }

    private void checkRevealMessage() {
        String expectedString = Boolean.parseBoolean(
                getPasswordField().getAttribute("passwordVisible")) ? "visible"
                        : "hidden";
        assertThat(String.format(
                "Password should be %s and message label should end with '%s' string",
                expectedString, expectedString),
                layout.findElement(By.id("messageLabel")).getText(),
                Matchers.endsWith(expectedString));
    }
}
