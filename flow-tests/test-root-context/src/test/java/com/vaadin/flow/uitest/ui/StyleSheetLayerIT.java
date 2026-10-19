/*
 * Copyright 2000-2026 Vaadin Ltd.
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

public class StyleSheetLayerIT extends ChromeBrowserTest {

    private static final String BLUE = "rgba(0, 0, 255, 1)";
    private static final String GREEN = "rgba(0, 128, 0, 1)";
    private static final String YELLOW = "rgba(255, 255, 0, 1)";

    @Test
    public void styleSheetAnnotationWithLayer_appliedBelowUnlayeredStyles() {
        open();

        WebElement annotatedDiv = findElement(By.id("annotated-div"));
        // The layered sheet applies...
        waitUntil(driver -> YELLOW
                .equals(annotatedDiv.getCssValue("background-color")));
        // ...but loses to the unlayered rule despite its higher specificity
        Assert.assertEquals(GREEN, annotatedDiv.getCssValue("color"));
    }

    @Test
    public void addLayeredStylesheet_appliedBelowUnlayeredStyles_andRemoved() {
        open();

        WebElement testDiv = findElement(By.id("test-div"));
        waitUntil(driver -> GREEN.equals(testDiv.getCssValue("color")));

        findElement(By.id("add-layered-style")).click();

        // The layered sheet applies...
        waitUntil(
                driver -> BLUE.equals(testDiv.getCssValue("background-color")));
        // ...but loses to the unlayered rule despite its higher specificity
        Assert.assertEquals(GREEN, testDiv.getCssValue("color"));

        findElement(By.id("remove-layered-style")).click();

        waitUntil(driver -> !BLUE
                .equals(testDiv.getCssValue("background-color")));
        Assert.assertEquals(GREEN, testDiv.getCssValue("color"));
    }
}
