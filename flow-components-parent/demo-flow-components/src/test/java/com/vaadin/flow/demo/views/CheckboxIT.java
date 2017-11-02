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
 * Integration tests for the {@link CheckboxView}.
 */
public class CheckboxIT extends ComponentDemoTest {

    @Override
    protected String getTestPath() {
        return "/vaadin-checkbox";
    }

    @Test
    public void defaultCheckbox() {
        WebElement checkbox = layout.findElement(By.id("default-checkbox"));
        Assert.assertTrue("Default checkbox should be present",
                "vaadin-checkbox".equals(checkbox.getTagName()));
        Assert.assertEquals(
                "Default checkbox label should have text 'Default Checkbox'",
                "Default Checkbox", checkbox.getText());
    }

    @Test
    public void disabledCheckbox() {
        WebElement checkbox = layout.findElement(By.id("disabled-checkbox"));
        Assert.assertFalse("Disabled checkbox should be disabled",
                getInShadowRoot(checkbox, By.id("nativeCheckbox")).isEnabled());
    }

    @Test
    public void indeterminateCheckbox() {
        WebElement checkbox = layout
                .findElement(By.id("indeterminate-checkbox"));
        Assert.assertNotNull(
                "Indeterminate checkbox should have property 'indeterminate'",
                checkbox.getAttribute("indeterminate"));

        scrollIntoViewAndClick(checkbox);
        waitUntil(driver -> checkbox.getAttribute("checked") != null);

        WebElement reset = layout.findElement(By.id("reset-indeterminate"));
        scrollIntoViewAndClick(reset);
        waitUntil(driver -> checkbox.getAttribute("indeterminate") != null);
    }

    @Test
    public void valueChangeCheckbox() {
        WebElement checkbox = layout
                .findElement(By.id("value-change-checkbox"));
        WebElement message = layout
                .findElement(By.id("value-change-checkbox-message"));
        scrollIntoViewAndClick(
                getInShadowRoot(checkbox, By.id("nativeCheckbox")));
        Assert.assertEquals("Clicking checkbox should update message div",
                "Checkbox value changed from 'false' to 'true'",
                message.getText());
    }

    @Test
    public void accessibleCheckbox() {
        WebElement checkbox = layout.findElement(By.id("accessible-checkbox"));
        Assert.assertEquals(
                "Accessible checkbox should have the aria-label attribute",
                "Click me", checkbox.getAttribute("aria-label"));
    }
}
