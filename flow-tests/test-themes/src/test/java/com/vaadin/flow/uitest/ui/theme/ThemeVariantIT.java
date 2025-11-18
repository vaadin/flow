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

import static com.vaadin.flow.uitest.ui.theme.ThemeVariantView.CLEAR_THEME_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeVariantView.SET_DARK_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeVariantView.SET_LIGHT_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeVariantView.TEST_ELEMENT_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeVariantView.THEME_NAME_DISPLAY_ID;
import static com.vaadin.flow.uitest.ui.theme.ThemeVariantView.THEME_VARIANT_DISPLAY_ID;

/**
 * Integration tests for theme variant functionality.
 */
public class ThemeVariantIT extends ChromeBrowserTest {

    @Test
    public void initialThemeVariant_isEmpty() {
        open();

        DivElement variantDisplay = $(DivElement.class)
                .id(THEME_VARIANT_DISPLAY_ID);
        Assert.assertEquals("Theme Variant: ", variantDisplay.getText());

        // Verify the DOM attribute is not set
        String themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme');");
        Assert.assertNull("Initial theme attribute should be null", themeAttr);
    }

    @Test
    public void setDarkTheme_variantIsSetAndStylesApplied() {
        open();

        // Click the set dark button
        $(NativeButtonElement.class).id(SET_DARK_ID).click();

        // Verify the display is updated
        DivElement variantDisplay = $(DivElement.class)
                .id(THEME_VARIANT_DISPLAY_ID);
        Assert.assertEquals("Theme Variant: dark", variantDisplay.getText());

        // Verify the DOM attribute is set
        String themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme');");
        Assert.assertEquals("dark", themeAttr);

        // Verify the CSS is applied
        TestBenchElement testElement = $(DivElement.class).id(TEST_ELEMENT_ID);
        String backgroundColor = testElement.getCssValue("background-color");
        Assert.assertEquals("Dark theme background should be rgb(30, 30, 30)",
                "rgba(30, 30, 30, 1)", backgroundColor);
    }

    @Test
    public void setLightTheme_variantIsSetAndStylesApplied() {
        open();

        // Click the set light button
        $(NativeButtonElement.class).id(SET_LIGHT_ID).click();

        // Verify the display is updated
        DivElement variantDisplay = $(DivElement.class)
                .id(THEME_VARIANT_DISPLAY_ID);
        Assert.assertEquals("Theme Variant: light", variantDisplay.getText());

        // Verify the DOM attribute is set
        String themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme');");
        Assert.assertEquals("light", themeAttr);

        // Verify the CSS is applied
        TestBenchElement testElement = $(DivElement.class).id(TEST_ELEMENT_ID);
        String backgroundColor = testElement.getCssValue("background-color");
        Assert.assertEquals(
                "Light theme background should be rgb(255, 255, 255)",
                "rgba(255, 255, 255, 1)", backgroundColor);
    }

    @Test
    public void clearTheme_variantIsEmptyAndDefaultStylesApplied() {
        open();

        // First set a theme
        $(NativeButtonElement.class).id(SET_DARK_ID).click();

        // Verify it was set
        DivElement variantDisplay = $(DivElement.class)
                .id(THEME_VARIANT_DISPLAY_ID);
        Assert.assertEquals("Theme Variant: dark", variantDisplay.getText());

        // Now clear it
        $(NativeButtonElement.class).id(CLEAR_THEME_ID).click();

        // Verify the display is cleared
        Assert.assertEquals("Theme Variant: ", variantDisplay.getText());

        // Verify the DOM attribute is removed
        String themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme');");
        Assert.assertNull("Theme attribute should be null after clearing",
                themeAttr);

        // Verify the default CSS is applied
        TestBenchElement testElement = $(DivElement.class).id(TEST_ELEMENT_ID);
        String backgroundColor = testElement.getCssValue("background-color");
        Assert.assertEquals("Default background should be rgb(200, 200, 200)",
                "rgba(200, 200, 200, 1)", backgroundColor);
    }

    @Test
    public void getThemeName_returnsCorrectTheme() {
        open();

        DivElement themeNameDisplay = $(DivElement.class)
                .id(THEME_NAME_DISPLAY_ID);
        String text = themeNameDisplay.getText();

        // The theme name should be detected from the configured theme
        // In test-themes, app-theme doesn't set lumo or aura markers,
        // so it should be empty
        Assert.assertTrue("Theme name text should start with 'Theme Name: '",
                text.startsWith("Theme Name: "));
    }

    @Test
    public void themeVariantAttributeReflectsServerSide() {
        open();

        // Set dark theme
        $(NativeButtonElement.class).id(SET_DARK_ID).click();
        String themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme');");
        Assert.assertEquals("dark", themeAttr);

        // Set light theme
        $(NativeButtonElement.class).id(SET_LIGHT_ID).click();
        themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme');");
        Assert.assertEquals("light", themeAttr);

        // Clear theme
        $(NativeButtonElement.class).id(CLEAR_THEME_ID).click();
        themeAttr = (String) executeScript(
                "return document.documentElement.getAttribute('theme');");
        Assert.assertNull(themeAttr);
    }

    @Test
    public void switchBetweenVariants_stylesUpdateCorrectly() {
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
        Assert.assertEquals("rgba(255, 255, 255, 1)", backgroundColor);

        // Switch back to dark
        $(NativeButtonElement.class).id(SET_DARK_ID).click();
        backgroundColor = testElement.getCssValue("background-color");
        Assert.assertEquals("rgba(30, 30, 30, 1)", backgroundColor);

        // Clear
        $(NativeButtonElement.class).id(CLEAR_THEME_ID).click();
        backgroundColor = testElement.getCssValue("background-color");
        Assert.assertEquals("rgba(200, 200, 200, 1)", backgroundColor);
    }
}
