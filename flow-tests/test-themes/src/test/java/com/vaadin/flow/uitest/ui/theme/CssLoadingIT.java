/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import static com.vaadin.flow.uitest.ui.theme.CssLoadingView.CSS_IMPORT__PAGE_ADD_STYLESHEET;
import static com.vaadin.flow.uitest.ui.theme.CssLoadingView.CSS_IMPORT__STYLESHEET;
import static com.vaadin.flow.uitest.ui.theme.CssLoadingView.PAGE_ADD_STYLESHEET__STYLESHEET;

public class CssLoadingIT extends ChromeBrowserTest {

    private static final String BLUE_RGBA = "rgba(0, 0, 255, 1)";
    private static final String GREEN_RGBA = "rgba(0, 255, 0, 1)";
    private static final String STYLESHEET_LUMO_FONT_SIZE_M = " 30px";

    @Test
    public void StyleSheet_overrides_CssImport() {
        open();
        // @StyleSheet [blue] > page.addStyleSheet() [green] < @CssImport [red]
        Assert.assertEquals(
                "Styles from @StyleSheet should have a higher priority than from @CssImport.",
                BLUE_RGBA, $(ParagraphElement.class).id(CSS_IMPORT__STYLESHEET)
                        .getCssValue("color"));
    }

    @Test
    public void addStyleSheet_overrides_StyleSheet() {
        open();
        Assert.assertEquals(
                "Styles from addStyleSheet() should have a higher priority than from @StyleSheet.",
                BLUE_RGBA,
                $(ParagraphElement.class).id(PAGE_ADD_STYLESHEET__STYLESHEET)
                        .getCssValue("color"));
    }

    @Test
    public void addStyleSheet_overrides_CssImport() {
        open();
        Assert.assertEquals(
                "Styles from addStyleSheet() should have a higher priority than from @CssImport.",
                GREEN_RGBA,
                $(ParagraphElement.class).id(CSS_IMPORT__PAGE_ADD_STYLESHEET)
                        .getCssValue("color"));
    }

    @Test
    public void lumoStyleIsOverridden() {
        open();
        WebElement htmlElement = findElement(By.tagName("html"));

        Assert.assertEquals(STYLESHEET_LUMO_FONT_SIZE_M, executeScript(
                "return getComputedStyle(arguments[0]).getPropertyValue('--lumo-font-size-m')",
                htmlElement));
    }
}
