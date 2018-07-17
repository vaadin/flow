/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.demo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.demo.DemoView.COMPONENT_WITH_VARIANTS_ID;
import static com.vaadin.flow.demo.DemoView.VARIANT_TOGGLE_BUTTONS_DIV_ID;

/**
 * Base class for the integration tests of component demos.
 *
 */
public abstract class ComponentDemoTest extends ChromeBrowserTest {
    protected WebElement layout;

    @Override
    protected int getDeploymentPort() {
        return 9998;
    }

    /**
     * Method run before each test.
     */
    @Before
    public void openDemoPageAndCheckForErrors() {
        open();
        waitForElementPresent(By.className("demo-view"));
        layout = findElement(By.className("demo-view"));
        checkLogsForErrors();
    }

    /**
     * Verifies variants functionality for the current layout.
     * 
     * The test will fail if a specific variant demo is not added first with
     * {@link DemoView#addVariantsDemo(Supplier, BiConsumer, BiConsumer, Function, Enum[])}
     * method.
     */
    protected void verifyThemeVariantsBeingToggled() {
        List<WebElement> toggleThemeButtons = layout
                .findElement(By.id(VARIANT_TOGGLE_BUTTONS_DIV_ID))
                .findElements(By.tagName("button"));
        Assert.assertFalse(
                "Expected at least one toggle theme button in 'buttonDiv', but got none",
                toggleThemeButtons.isEmpty());
        toggleThemeButtons.forEach(button -> toggleVariantAndCheck(
                layout.findElement(By.id(COMPONENT_WITH_VARIANTS_ID)), button));
    }

    private void toggleVariantAndCheck(WebElement component,
            WebElement button) {
        List<String> initialButtonThemes = getComponentThemes(component);
        String initialButtonText = button.getText();

        button.click();
        verifyThemeIsToggled(getComponentThemes(component), button.getText(),
                initialButtonThemes, initialButtonText);

        button.click();
        Assert.assertEquals(
                "After two toggle variants button clicks, button text should be the same as before testing",
                button.getText(), initialButtonText);

        List<String> currentThemes = getComponentThemes(component);
        String assertionMessage = "After two toggle variants button clicks, component 'theme' attribute should contain the same value as before testing";
        Assert.assertEquals(assertionMessage, currentThemes.size(),
                initialButtonThemes.size());
        currentThemes.forEach(currentTheme -> Assert.assertTrue(
                assertionMessage + String.format(
                        " but theme variant '%s' is missing", currentTheme),
                initialButtonThemes.contains(currentTheme)));

    }

    private void verifyThemeIsToggled(List<String> updatedThemes,
            String updatedButtonText, List<String> previousThemes,
            String previousButtonText) {
        Assert.assertNotEquals("Button should change its text after toggling",
                previousButtonText, updatedButtonText);

        boolean shouldAddTheme = previousButtonText.startsWith("Add");
        if (shouldAddTheme) {
            Assert.assertTrue(
                    "When a theme variant got added, toggle button text should start with 'Remove' word",
                    updatedButtonText.startsWith("Remove"));
            Assert.assertEquals(
                    "When a theme variant got added, component 'theme' attribute should contain one more variant that before",
                    previousThemes.size() + 1, updatedThemes.size());
            Assert.assertTrue(
                    "When a theme variant got added, component 'theme' attribute should contain all previous theme variants",
                    updatedThemes.containsAll(previousThemes));
        } else {
            Assert.assertTrue(
                    "When a theme variant got removed, toggle button text should start with 'Add' word",
                    updatedButtonText.startsWith("Add"));
            Assert.assertEquals(
                    "When a theme variant got removed, component 'theme' attribute should contain one less variant than before",
                    previousThemes.size() - 1, updatedThemes.size());
            Assert.assertTrue(
                    "When a theme variant got removed, previous theme variants should contain all theme variants from component 'theme' attribute",
                    previousThemes.containsAll(updatedThemes));
        }
    }

    private List<String> getComponentThemes(WebElement component) {
        String themeAttributeValue = component.getAttribute("theme");
        if (themeAttributeValue == null || themeAttributeValue.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(themeAttributeValue.split(" "));
    }
}
