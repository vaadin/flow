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
        String color = testDiv.getCssValue("color");
        Assert.assertTrue("Color should be red, but was: " + color,
                color.equals("rgb(255, 0, 0)")
                        || color.equals("rgba(255, 0, 0, 1)"));

        // Click button to remove stylesheet
        WebElement removeButton = findElement(By.id("remove-style"));
        removeButton.click();

        // Verify that the style has been removed (back to default)
        String removedColor = testDiv.getCssValue("color");
        Assert.assertFalse(
                "Color should not be red after removal, but was: "
                        + removedColor,
                removedColor.equals("rgb(255, 0, 0)")
                        || removedColor.equals("rgba(255, 0, 0, 1)"));
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
        String color = testDiv.getCssValue("color");
        Assert.assertTrue("Color should be red, but was: " + color,
                color.equals("rgb(255, 0, 0)")
                        || color.equals("rgba(255, 0, 0, 1)"));
        String bgColor = testDiv.getCssValue("background-color");
        Assert.assertTrue("Background should be green, but was: " + bgColor,
                bgColor.equals("rgb(0, 255, 0)")
                        || bgColor.equals("rgba(0, 255, 0, 1)"));

        // Remove first stylesheet
        WebElement removeButton1 = findElement(By.id("remove-style-1"));
        removeButton1.click();

        // Verify only second style remains
        String removedColor = testDiv.getCssValue("color");
        Assert.assertFalse(
                "Color should not be red after removal, but was: "
                        + removedColor,
                removedColor.equals("rgb(255, 0, 0)")
                        || removedColor.equals("rgba(255, 0, 0, 1)"));
        String bgColorAfter = testDiv.getCssValue("background-color");
        Assert.assertTrue(
                "Background should still be green, but was: " + bgColorAfter,
                bgColorAfter.equals("rgb(0, 255, 0)")
                        || bgColorAfter.equals("rgba(0, 255, 0, 1)"));

        // Remove second stylesheet
        WebElement removeButton2 = findElement(By.id("remove-style-2"));
        removeButton2.click();

        // Verify all styles removed
        String finalBgColor = testDiv.getCssValue("background-color");
        Assert.assertFalse(
                "Background should not be green after removal, but was: "
                        + finalBgColor,
                finalBgColor.equals("rgb(0, 255, 0)")
                        || finalBgColor.equals("rgba(0, 255, 0, 1)"));
    }

    @Test
    public void duplicateStylesheet_removeWithSecondRegistration() {
        open();

        WebElement testDiv = findElement(By.id("test-div"));
        WebElement addButton = findElement(By.id("add-style"));

        // Add stylesheet first time
        addButton.click();

        // Verify style is applied
        String colorWithStyle = testDiv.getCssValue("color");
        Assert.assertTrue("Color should be red after adding style",
                colorWithStyle.equals("rgb(255, 0, 0)")
                        || colorWithStyle.equals("rgba(255, 0, 0, 1)"));

        // Add duplicate (second time) - should not change anything visually
        addButton.click();

        // Verify style is still applied
        String colorAfterDuplicate = testDiv.getCssValue("color");
        Assert.assertTrue("Color should still be red after adding duplicate",
                colorAfterDuplicate.equals("rgb(255, 0, 0)")
                        || colorAfterDuplicate.equals("rgba(255, 0, 0, 1)"));

        // Remove using the button (which uses the last registration)
        WebElement removeButton = findElement(By.id("remove-style"));
        removeButton.click();

        // Verify style is removed (should work because duplicate registrations
        // return the same dependency ID for removal)
        String removedColor = testDiv.getCssValue("color");
        Assert.assertFalse(
                "Color should not be red after removal with duplicate registration, but was: "
                        + removedColor,
                removedColor.equals("rgb(255, 0, 0)")
                        || removedColor.equals("rgba(255, 0, 0, 1)"));
    }
}