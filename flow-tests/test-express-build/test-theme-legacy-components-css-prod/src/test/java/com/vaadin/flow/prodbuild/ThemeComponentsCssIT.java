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
import org.openqa.selenium.StaleElementReferenceException;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ThemeComponentsCssIT extends ChromeBrowserTest {

    private static final String WHITE_COLOR = "rgba(255, 255, 255, 1)";
    private static final String RED_COLOR = "rgba(255, 0, 0, 1)";
    private static final String GREEN_COLOR = "rgba(0, 128, 0, 1)";
    private File nodeModules;

    @Before
    public void init() {
        File baseDir = new File(System.getProperty("user.dir", "."));
        nodeModules = new File(baseDir, "node_modules");
    }

    @Test
    public void themeComponentsCSS_stylesApplied() {
        open();

        // no node_modules are expected
        Assert.assertFalse(nodeModules.exists());
        waitUntil(driver -> {
            try {
                TestBenchElement component = $("vaadin-horizontal-layout")
                        .first();
                return GREEN_COLOR.equals(getPartBackgroundColor(component, 0))
                        && WHITE_COLOR
                                .equals(getPartBackgroundColor(component, 1))
                        && RED_COLOR
                                .equals(getPartBackgroundColor(component, 2));
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    private static String getPartBackgroundColor(TestBenchElement component,
            int index) {
        return component.$("div").get(index).getCssValue("background-color");
    }

}
