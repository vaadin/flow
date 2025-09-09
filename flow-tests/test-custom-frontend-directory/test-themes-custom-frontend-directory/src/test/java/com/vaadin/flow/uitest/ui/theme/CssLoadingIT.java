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
package com.vaadin.flow.uitest.ui.theme;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Test CSS loading order from different sources.
 *
 * The expected priority is: Lumo styles < @CssImport < page.addStylesheet
 * < @Stylehseet < parent theme < current theme (app theme)
 */
public class CssLoadingIT extends ChromeBrowserTest {

    private static final String BLUE_RGBA = "rgba(0, 0, 255, 1)";
    private static final String GREEN_RGBA = "rgba(0, 255, 0, 1)";
    private static final String YELLOW_RGBA = "rgba(255, 255, 0, 1)";
    private static final String STYLESHEET_LUMO_FONT_SIZE_M = "1.1rem";

    @Test
    public void CssImport_overrides_Lumo() {
        open();
        WebElement htmlElement = findElement(By.tagName("html"));

        Assert.assertEquals("CssImport styles should override Lumo styles.",
                STYLESHEET_LUMO_FONT_SIZE_M,
                executeScript(
                        "return getComputedStyle(arguments[0]).getPropertyValue('--lumo-font-size-m')",
                        htmlElement).toString().trim());
    }
}
