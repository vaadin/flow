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
package com.vaadin.flow.tests.components.textfield;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.tests.components.AbstractComponentIT;
import com.vaadin.testbench.By;

/**
 * Integration tests for {@link com.vaadin.ui.PasswordField}.
 */
public class PasswordFieldViewIT extends AbstractComponentIT {

    @Before
    public void init() {
        open();
    }

    @Test
    public void assertReadOnly() {
        WebElement webComponent = findElement(By.tagName("vaadin-password-field"));

        WebElement passwordField = getInShadowRoot(webComponent, By.id("input"));
        Assert.assertNull(passwordField.getAttribute("readonly"));

        WebElement button = findElement(By.id("read-only"));
        button.click();

        waitUntil(driver -> passwordField.getAttribute("readonly"));

        button.click();

        waitUntil(driver -> passwordField.getAttribute("readonly") == null);
    }

    @Test
    public void assertRequired() {
        WebElement webComponent = findElement(By.tagName("vaadin-password-field"));

        WebElement passwordField = getInShadowRoot(webComponent, By.id("input"));
        Assert.assertNull(passwordField.getAttribute("required"));

        WebElement button = findElement(By.id("required"));
        button.click();
        waitUntil(driver -> passwordField.getAttribute("required"));

        button.click();
        waitUntil(driver -> passwordField.getAttribute("required") == null);
    }
}
