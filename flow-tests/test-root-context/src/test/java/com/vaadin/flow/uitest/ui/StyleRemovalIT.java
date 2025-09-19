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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class StyleRemovalIT extends ChromeBrowserTest {

    @Test
    public void addAndRemoveStylesheet_stylesheetIsAddedAndRemoved() {
        open();

        // Click button to add stylesheet
        WebElement addButton = findElement(By.id("add-style"));
        addButton.click();

        // Verify that the style has been applied
        WebElement testDiv = findElement(By.id("test-div"));
        Assert.assertEquals("rgb(255, 0, 0)", testDiv.getCssValue("color"));

        // Click button to remove stylesheet
        WebElement removeButton = findElement(By.id("remove-style"));
        removeButton.click();

        // Verify that the style has been removed (back to default)
        Assert.assertNotEquals("rgb(255, 0, 0)", testDiv.getCssValue("color"));
    }

    @Test
    public void multipleStylesheets_canBeRemovedIndependently() {
        open();

        // Add first stylesheet
        WebElement addButton1 = findElement(By.id("add-style-1"));
        addButton1.click();

        // Add second stylesheet
        WebElement addButton2 = findElement(By.id("add-style-2"));
        addButton2.click();

        // Verify both styles are applied
        WebElement testDiv = findElement(By.id("test-div"));
        Assert.assertEquals("rgb(255, 0, 0)", testDiv.getCssValue("color"));
        Assert.assertEquals("rgb(0, 255, 0)",
                testDiv.getCssValue("background-color"));

        // Remove first stylesheet
        WebElement removeButton1 = findElement(By.id("remove-style-1"));
        removeButton1.click();

        // Verify only second style remains
        Assert.assertNotEquals("rgb(255, 0, 0)", testDiv.getCssValue("color"));
        Assert.assertEquals("rgb(0, 255, 0)",
                testDiv.getCssValue("background-color"));

        // Remove second stylesheet
        WebElement removeButton2 = findElement(By.id("remove-style-2"));
        removeButton2.click();

        // Verify all styles removed
        Assert.assertNotEquals("rgb(0, 255, 0)",
                testDiv.getCssValue("background-color"));
    }
}