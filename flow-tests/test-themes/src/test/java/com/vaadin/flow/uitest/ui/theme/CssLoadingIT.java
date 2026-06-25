/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
