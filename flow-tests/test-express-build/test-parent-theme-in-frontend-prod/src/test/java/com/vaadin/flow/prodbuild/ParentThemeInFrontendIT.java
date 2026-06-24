/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.prodbuild;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ParentThemeInFrontendIT extends ChromeBrowserTest {

    private static final String BLUE_COLOR = "rgba(0, 0, 255, 1)";
    private static final String RED_COLOR = "rgba(255, 0, 0, 1)";
    private static final String GREEN_COLOR = "rgba(0, 128, 0, 1)";
    private File nodeModules;

    @Before
    public void init() {
        File baseDir = new File(System.getProperty("user.dir", "."));
        nodeModules = new File(baseDir, "node_modules");
    }

    @Test
    public void parentThemeInFrontendFolder_stylesAppliedFromParentTheme() {
        open();

        // no node_modules are expected
        Assert.assertFalse(nodeModules.exists());

        // check that the background colour is overridden by the child theme
        waitUntilChildThemeBackgroundColor();
        waitUntilParentThemeStyles();
        waitUntilImportedStyles();
        checkLogsForErrors();
    }

    @Test
    public void imagesLocatedInThemeAndWebapp_shouldResolveUrlsAndRenderImages() {
        open();

        waitForElementPresent(By.id("vaadin-logo"));

        String staticResourceUrl = $(DivElement.class).id("vaadin-logo")
                .getCssValue("background-image");
        Assert.assertTrue(
                "Should render the background image of element with static resource image",
                staticResourceUrl.contains("/images/vaadin-logo.png"));

        String themeResourceUrl = $(DivElement.class).id("hilla-logo")
                .getCssValue("background-image");
        Assert.assertTrue(
                "Should render the background image of element with theme resource image",
                themeResourceUrl.contains(
                        "VAADIN/themes/specific-theme/images/hilla-logo.png"));

        // no 404 errors
        checkLogsForErrors();
    }

    private void waitUntilParentThemeStyles() {
        waitUntil(driver -> {
            try {
                final WebElement p = findElement(By.tagName("p"));
                final WebElement span = findElement(By.tagName("span"));
                return RED_COLOR.equals(p.getCssValue("color"))
                        && GREEN_COLOR.equals(span.getCssValue("color"));
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    private void waitUntilImportedStyles() {
        waitUntil(driver -> {
            try {
                final WebElement span = findElement(By.tagName("span"));
                String border = span.getCssValue("border");
                System.out.println(border);
                return "3px dashed rgb(255, 255, 255)".equals(border);
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
