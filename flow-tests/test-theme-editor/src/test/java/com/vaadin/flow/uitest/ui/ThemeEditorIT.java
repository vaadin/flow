/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import java.util.List;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.testutil.DevToolsElement;
import com.vaadin.testbench.TestBenchElement;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class ThemeEditorIT extends AbstractThemeEditorIT {
    @Override
    protected String getTestPath() {
        return "/context/view/com.vaadin.flow.uitest.ui.ThemeEditorView";
    }

    @Test
    public void testButton() {
        open();

        DevToolsElement devTools = $(DevToolsElement.class).waitForFirst();
        devTools.expand();

        devTools.showThemeEditor();

        TestBenchElement themeEditor =
                devTools.$("vaadin-dev-tools-theme-editor").first();
        themeEditor.$("button").first().click();

        new Actions(getDriver()).click(findElement(By.id("button"))).perform();

        TestBenchElement propertiesList = themeEditor.$("vaadin-dev-tools" +
                "-theme-property-list").waitForFirst();
        List<TestBenchElement> propertyEditors = propertiesList.$("*").hasAttribute(
                "data-testid").all();
        for (TestBenchElement testBenchElement : propertyEditors) {
            WebElement webElement = testBenchElement.getWrappedElement();
            String dataTestIdAttribute = webElement.getAttribute("data-testid");
            System.out.println(dataTestIdAttribute);
        }
    }
}
