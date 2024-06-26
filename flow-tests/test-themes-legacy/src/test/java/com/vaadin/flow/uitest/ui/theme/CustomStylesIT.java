/**
 * Copyright (C) 2024 Vaadin Ltd
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

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class CustomStylesIT extends ChromeBrowserTest {

    @Test
    public void importedStyleOverridesTheme() {
        open();

        WebElement text = findElement(By.id("custom-style"));
        String fontSize = text.getCssValue("font-size");
        // check that custom style is applied overriding the theme
        Assert.assertEquals("12px", fontSize);

        // self check that theme styles are also applied
        Assert.assertEquals("rgba(255, 0, 0, 1)", text.getCssValue("color"));
    }
}
