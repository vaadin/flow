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
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.ComponentDemoTest;
import org.openqa.selenium.By;

/**
 * Integration tests for the {@link ComboBoxView}.
 */
public class ComboBoxIT extends ComponentDemoTest {
    @Before
    public void init() {
        waitForElementPresent(By.tagName("vaadin-combo-box"));
    }

    @Test
    public void openStringBoxAndSelectAnItem() {
        WebElement comboBox = layout.findElement(By.id("string-selection-box"));
        WebElement message = layout
                .findElement(By.id("string-selection-message"));

        executeScript("arguments[0].selectedItem = arguments[0].items[2]",
                comboBox);

        Assert.assertEquals("Selected browser: Opera", message.getText());
    }

    @Test
    public void openObjectBoxAndSelectAnItem() {
        WebElement comboBox = layout.findElement(By.id("object-selection-box"));
        WebElement message = layout
                .findElement(By.id("object-selection-message"));

        executeScript("arguments[0].selectedItem = arguments[0].items[1]",
                comboBox);

        waitUntil(driver -> message.getText().equals(
                "Selected song: Sculpted\nFrom album: Two Fold Pt.1\nBy artist: Haywyre"));
    }

    @Test
    public void openValueBoxSelectTwoItems() {
        WebElement comboBox = layout.findElement(By.id("value-selection-box"));
        WebElement message = layout
                .findElement(By.id("value-selection-message"));

        executeScript("arguments[0].selectedItem = arguments[0].items[1]",
                comboBox);

        waitUntil(
                driver -> message.getText().equals("Selected artist: Haywyre"));

        executeScript("arguments[0].selectedItem = arguments[0].items[0]",
                comboBox);

        waitUntil(driver -> message.getText().equals(
                "Selected artist: Haircuts for Men\nThe old selection was: Haywyre"));
    }

    @Override
    protected String getTestPath() {
        return "/vaadin-combo-box";
    }

}
