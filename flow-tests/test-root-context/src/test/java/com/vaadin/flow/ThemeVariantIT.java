/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ThemeVariantIT extends ChromeBrowserTest {

    @Test
    public void themeVariant_setAndGet() {
        open();

        // Initially, theme should be empty (not set)
        Assert.assertEquals("Initial theme: (empty)",
                findElement(By.id("initial-theme")).getText());
        Assert.assertEquals("Current theme: (empty)",
                findElement(By.id("current-theme")).getText());

        // Set theme to dark
        findElement(By.id("set-dark")).click();
        Assert.assertEquals("Current theme: dark",
                findElement(By.id("current-theme")).getText());

        // Verify the attribute is actually set in the browser
        String themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme')");
        Assert.assertEquals("dark", themeAttr);

        // Set theme to light
        findElement(By.id("set-light")).click();
        Assert.assertEquals("Current theme: light",
                findElement(By.id("current-theme")).getText());

        // Verify the attribute changed
        themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme')");
        Assert.assertEquals("light", themeAttr);

        // Clear theme
        findElement(By.id("clear-theme")).click();
        Assert.assertEquals("Current theme: (empty)",
                findElement(By.id("current-theme")).getText());

        // Verify the attribute is removed
        themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme')");
        Assert.assertNull(themeAttr);
    }

    @Test
    public void themeVariant_persistsAcrossRequests() {
        open();

        // Set theme to dark
        findElement(By.id("set-dark")).click();
        Assert.assertEquals("Current theme: dark",
                findElement(By.id("current-theme")).getText());

        // Click "Get Theme" button which causes a server roundtrip
        findElement(By.id("get-theme")).click();

        // Theme should still be dark
        Assert.assertEquals("Current theme: dark",
                findElement(By.id("current-theme")).getText());
    }
}
