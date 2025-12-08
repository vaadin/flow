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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * Test CSS loading order from different sources.
 *
 * The expected priority is: Lumo styles < @CssImport < page.addStylesheet
 * < @Stylehseet < parent theme < current theme (app theme)
 */
public class CssLoadingIT extends ChromeBrowserTest {

    @Test
    public void overridesCorrect() {
        open();
        for (String id : CssLoadingView.idToExpectedColor.keySet()) {
            if (id.equals("laterAddStylesheetVsCssImport")) {
                continue;
            }
            assertColor(id);
        }
        $("*").id("load").click();
        for (String id : CssLoadingView.idToExpectedColor.keySet()) {
            // Loading the stylesheet should not affect other styles but make
            // the "laterAddStylesheetVsCssImport" one correct also
            assertColor(id);
        }
    }

    @Test
    public void fakeAuraStylesApplied() {
        open();

        // Verify that the Aura theme stylesheet from @StyleSheet has been
        // applied
        // by checking that the CSS custom property is present on :root
        String auraLoaded = (String) executeScript(
                "return getComputedStyle(document.documentElement).getPropertyValue('--fake-aura-theme-loaded').trim() ");
        Assert.assertEquals(
                "Expected :root --fake-aura-theme-loaded custom property to be set by aura/fake-aura.css",
                "1", auraLoaded);
    }

    private void assertColor(String id) {
        TestBenchElement element = $("*").id(id);
        String elementBackground = (String) executeScript(
                "return getComputedStyle(arguments[0]).backgroundColor",
                element);
        String expected = CssLoadingView.idToExpectedColor.get(id);
        String expectedBrowserColorName = getBrowserColorName(expected);
        if (!expectedBrowserColorName.equals(elementBackground)) {
            Assert.fail(element.getText() + ". Was " + elementBackground);
        }

    }

    private String getBrowserColorName(String color) {
        return (String) executeScript(
                "e = document.createElement('div'); e.style.backgroundColor=arguments[0];document.body.append(e);ret = getComputedStyle(e).backgroundColor;e.remove(); return ret",
                color);
    }

}
