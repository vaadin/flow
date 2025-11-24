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

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.ui.theme.ColorSchemeView.COLOR_SCHEME_DISPLAY_ID;
import static com.vaadin.flow.uitest.ui.theme.ColorSchemeView.SET_DARK_ID;
import static com.vaadin.flow.uitest.ui.theme.ColorSchemeView.SET_LIGHT_ID;
import static com.vaadin.flow.uitest.ui.theme.ColorSchemeView.TEST_ELEMENT_ID;
import static com.vaadin.flow.uitest.ui.theme.ColorSchemeView.THEME_NAME_DISPLAY_ID;

/**
 * Integration tests for color scheme functionality.
 */
public class ColorSchemeIT extends ChromeBrowserTest {

    @Test
    public void initialColorScheme_isEmpty() {
        open();

        DivElement colorSchemeDisplay = $(DivElement.class)
                .id(COLOR_SCHEME_DISPLAY_ID);
        // Browser may report 'light' or 'normal' as default color-scheme
        String text = colorSchemeDisplay.getText();
        Assert.assertTrue("Color scheme should be empty, 'light', or 'normal'",
                "Color Scheme: ".equals(text)
                        || "Color Scheme: light".equals(text)
                        || "Color Scheme: normal".equals(text));

        // Verify no theme attribute is set initially
        String themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme');");
        Assert.assertNull("Initial theme attribute should be null", themeAttr);
    }

    @Test
    public void setDarkTheme_colorSchemeIsSetAndStylesApplied() {
        open();

        // Click the set dark button
        $(NativeButtonElement.class).id(SET_DARK_ID).click();

        // Verify the display is updated
        DivElement colorSchemeDisplay = $(DivElement.class)
                .id(COLOR_SCHEME_DISPLAY_ID);
        Assert.assertEquals("Color Scheme: dark", colorSchemeDisplay.getText());

        // Verify the theme attribute is set
        String themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme');");
        Assert.assertEquals("dark", themeAttr);

        // Verify the inline color-scheme style is set
        String inlineStyle = (String) executeScript(
                "return document.documentElement.style.colorScheme;");
        Assert.assertEquals("dark", inlineStyle);

        // Verify the computed color-scheme property
        String colorScheme = (String) executeScript(
                "return getComputedStyle(document.documentElement).colorScheme;");
        Assert.assertEquals("dark", colorScheme);

        // Verify the CSS is applied
        TestBenchElement testElement = $(DivElement.class).id(TEST_ELEMENT_ID);
        String backgroundColor = testElement.getCssValue("background-color");
        Assert.assertEquals("Dark theme background should be rgb(30, 30, 30)",
                "rgba(30, 30, 30, 1)", backgroundColor);
    }

    @Test
    public void setLightTheme_colorSchemeIsSetAndStylesApplied() {
        open();

        // Click the set light button
        $(NativeButtonElement.class).id(SET_LIGHT_ID).click();

        // Verify the display is updated
        DivElement colorSchemeDisplay = $(DivElement.class)
                .id(COLOR_SCHEME_DISPLAY_ID);
        Assert.assertEquals("Color Scheme: light",
                colorSchemeDisplay.getText());

        // Verify the theme attribute is set
        String themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme');");
        Assert.assertEquals("light", themeAttr);

        // Verify the inline color-scheme style is set
        String inlineStyle = (String) executeScript(
                "return document.documentElement.style.colorScheme;");
        Assert.assertEquals("light", inlineStyle);

        // Verify the computed color-scheme property
        String colorScheme = (String) executeScript(
                "return getComputedStyle(document.documentElement).colorScheme;");
        Assert.assertEquals("light", colorScheme);

        // Verify the CSS is applied
        TestBenchElement testElement = $(DivElement.class).id(TEST_ELEMENT_ID);
        String backgroundColor = testElement.getCssValue("background-color");
        Assert.assertEquals(
                "Light theme background should be rgb(255, 255, 255)",
                "rgba(200, 200, 200, 1)", backgroundColor);
    }

    @Test
    public void getThemeName_returnsCorrectTheme() {
        open();

        DivElement themeNameDisplay = $(DivElement.class)
                .id(THEME_NAME_DISPLAY_ID);
        String text = themeNameDisplay.getText();

        // The theme name should be detected from the configured theme
        // In test-themes, app-theme loads fake-aura.css which has the marker
        Assert.assertTrue("Theme name text should start with 'Theme Name: '",
                text.startsWith("Theme Name: "));
    }

    @Test
    public void colorSchemePropertyReflectsServerSide() {
        open();

        // Set dark theme
        $(NativeButtonElement.class).id(SET_DARK_ID).click();
        String themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme');");
        Assert.assertEquals("dark", themeAttr);
        String inlineStyle = (String) executeScript(
                "return document.documentElement.style.colorScheme;");
        Assert.assertEquals("dark", inlineStyle);

        // Set light theme
        $(NativeButtonElement.class).id(SET_LIGHT_ID).click();
        themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme');");
        Assert.assertEquals("light", themeAttr);
        inlineStyle = (String) executeScript(
                "return document.documentElement.style.colorScheme;");
        Assert.assertEquals("light", inlineStyle);
    }

    @Test
    public void switchBetweenColorSchemes_stylesUpdateCorrectly() {
        open();

        TestBenchElement testElement = $(DivElement.class).id(TEST_ELEMENT_ID);

        // Initial state - default background
        String backgroundColor = testElement.getCssValue("background-color");
        Assert.assertEquals("rgba(200, 200, 200, 1)", backgroundColor);

        // Switch to dark
        $(NativeButtonElement.class).id(SET_DARK_ID).click();
        backgroundColor = testElement.getCssValue("background-color");
        Assert.assertEquals("rgba(30, 30, 30, 1)", backgroundColor);

        // Switch to light
        $(NativeButtonElement.class).id(SET_LIGHT_ID).click();
        backgroundColor = testElement.getCssValue("background-color");
        Assert.assertEquals("rgba(200, 200, 200, 1)", backgroundColor);

        // Switch back to dark
        $(NativeButtonElement.class).id(SET_DARK_ID).click();
        backgroundColor = testElement.getCssValue("background-color");
        Assert.assertEquals("rgba(30, 30, 30, 1)", backgroundColor);
    }
}
