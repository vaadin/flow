/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.prodbuild;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ParentThemeIT extends ChromeBrowserTest {

    private static final String BLUE_COLOR = "rgba(0, 0, 255, 1)";
    private static final String RED_COLOR = "rgba(255, 0, 0, 1)";

    @Test
    public void parentTheme_stylesAppliedFromParentTheme() {
        open();

        // check that the background colour is overridden by the child theme
        waitUntilChildThemeBackgroundColor();
        waitUntilParentThemeStyles();
        checkLogsForErrors();
    }

    private void waitUntilParentThemeStyles() {
        waitUntil(driver -> {
            try {
                final WebElement p = findElement(By.tagName("p"));
                return RED_COLOR.equals(p.getCssValue("color"));
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    private void waitUntilChildThemeBackgroundColor() {
        waitUntil(driver -> isChildThemeBackGroundColor());
    }

    private boolean isChildThemeBackGroundColor() {
        try {
            final WebElement body = findElement(By.tagName("body"));
            return BLUE_COLOR.equals(body.getCssValue("background-color"));
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

}
