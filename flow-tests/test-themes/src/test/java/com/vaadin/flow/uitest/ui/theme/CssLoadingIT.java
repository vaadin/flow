/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
public class CssLoadingIT extends AbstractBaseIT {

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

    @Test
    public void multipleDefinitions_correctOverrides() {
        open();
        assertStylesOverride("p1", GREEN_RGBA, "16px", "1px");

        // @Stylesheet should override color and font-size but not margin
        assertStylesOverride("p2", BLUE_RGBA, "18px", "1px");

        assertStylesOverride("p3", YELLOW_RGBA, "20px", "2px");
    }

    private void assertStylesOverride(String elementId, String expectedColor,
            String expectedFontSize, String expectedMargin) {
        Assert.assertEquals(expectedColor,
                $(ParagraphElement.class).id(elementId).getCssValue("color"));
        Assert.assertEquals(expectedFontSize, $(ParagraphElement.class)
                .id(elementId).getCssValue("font-size"));
        Assert.assertEquals(expectedMargin,
                $(ParagraphElement.class).id(elementId).getCssValue("margin"));
    }

}
