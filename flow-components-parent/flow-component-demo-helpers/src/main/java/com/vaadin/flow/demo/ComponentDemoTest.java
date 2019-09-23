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

import static com.vaadin.flow.demo.DemoView.COMPONENT_WITH_VARIANTS_ID;
import static com.vaadin.flow.demo.DemoView.VARIANT_TOGGLE_BUTTONS_DIV_ID;

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

/**
 * Base class for the integration tests of component demos.
 *
 * @since 1.0
 */
public abstract class ComponentDemoTest extends ChromeBrowserTest {
    protected WebElement layout;

    /**
     * Default variant producer
     * <p>
     * With current design, the theme variant can be obtained from the button
     * attached to the demo
     */
    public static class DefaultProducer
            implements Function<WebElement, String> {

        @Override
        public String apply(WebElement button) {
            String[] variant = button.getText().split("'");
            return variant[1];
        }
    }

    private Function<WebElement, String> DEFAULT_VARIANT_PRODUCER = new DefaultProducer();

    @Override
    protected int getDeploymentPort() {
        return 9998;
    }

    /**
     * Runs before each test.
     */
    @Before
    public void openDemoPageAndCheckForErrors() {
        open();
        waitForElementPresent(By.className("demo-view"));
        layout = findElement(By.className("demo-view"));
        checkLogsForErrors();
    }

    /**
     * Verifies variants functionality for the current layout with using the
     * {@link DefaultProducer}.
     * <p>
     * The test will fail if a specific variant demo is not added first with
     * {@link DemoView#addVariantsDemo(Supplier, BiConsumer, BiConsumer, Function, Enum[])}
     * method.
     */
    protected void verifyThemeVariantsBeingToggled() {
        verifyThemeVariantsBeingToggled(DEFAULT_VARIANT_PRODUCER);
    }

    /**
     * Verifies variants functionality for the current layout with customized
     * variant producer implementation.
     */
    protected void verifyThemeVariantsBeingToggled(
            Function<WebElement, String> variantProducer) {
        List<WebElement> toggleThemeButtons = layout
                .findElement(By.id(VARIANT_TOGGLE_BUTTONS_DIV_ID))
                .findElements(By.tagName("button"));
        Assert.assertFalse(
                "Expected at least one toggle theme button in 'buttonDiv', but got none",
                toggleThemeButtons.isEmpty());
        toggleThemeButtons.forEach(button -> toggleVariantAndCheck(
                layout.findElement(By.id(COMPONENT_WITH_VARIANTS_ID)), button,
                variantProducer));
    }

    private void toggleVariantAndCheck(WebElement component, WebElement button,
            Function<WebElement, String> variantProducer) {
        List<String> initialButtonThemes = getComponentThemes(component);
        String initialButtonText = button.getText();

        button.click();
        verifyThemeIsToggled(getComponentThemes(component), button.getText(),
                initialButtonThemes, initialButtonText,
                variantProducer.apply(button));

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
            String previousButtonText, String variantName) {
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

            Assert.assertTrue("The selected theme variant:" + variantName
                    + " should be added to the component 'theme' attribute.",
                    updatedThemes.contains(variantName));
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
            Assert.assertFalse("The selected theme variant:" + variantName
                    + " should be removed from the component 'theme' attribute.",
                    updatedThemes.contains(variantName));
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
