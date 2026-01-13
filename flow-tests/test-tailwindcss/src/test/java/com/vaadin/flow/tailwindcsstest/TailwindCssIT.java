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
package com.vaadin.flow.tailwindcsstest;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TailwindCssIT extends ChromeBrowserTest {

    @Test
    public void tailwindCssWorks_builtin() {
        var view = openView();
        String viewBackground = view.getCssValue("backgroundColor");
        Assert.assertEquals("oklch(0.967 0.003 264.542)", viewBackground);

        var h1 = view.findElement(By.tagName("h1"));
        Assert.assertEquals("Tailwind CSS does work!", h1.getText());
    }

    @Test
    public void tailwindCssWorks_customThemeDirective() {
        openView();

        var customRedElement = findElement(By.id("custom-theme-red"));
        String redColor = customRedElement.getCssValue("color");
        // red should be rgba(255, 0, 0, 1)
        Assert.assertTrue("Expected red color, got: " + redColor,
                redColor.startsWith("rgba(255, 0, 0, 1"));

        var customBlueElement = findElement(By.id("custom-theme-blue"));
        String blueColor = customBlueElement.getCssValue("color");
        // #1e40af is rgba(30, 64, 175, 1)
        Assert.assertTrue("Expected custom blue color, got: " + blueColor,
                blueColor.startsWith("rgba(30, 64, 175, 1"));
    }

    @Test
    public void generatedTailwindCssContainsCustomImport() {
        open();
        waitForDevServer();

        // Verify that the generated tailwind.css file contains the custom
        // import
        var tailwindCssContent = (String) executeScript(
                """
                        return Array.from(document.styleSheets)
                            .map(s => Array.from(s.cssRules).map(r => r.cssText))
                            .find(rules => rules.find(rule => rule.startsWith('@layer properties')))
                            .join('\\n');
                        """);
        Assert.assertNotNull("Tailwind CSS should be loaded",
                tailwindCssContent);
        Assert.assertFalse("Tailwind CSS content should not be empty",
                tailwindCssContent.isEmpty());
    }

    private WebElement openView() {
        open();
        waitForDevServer();
        return findElement(By.cssSelector(".bg-gray-100"));
    }
}
