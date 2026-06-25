/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.testbench.TestBenchElement;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class ThemeLiveReloadIT extends AbstractLiveReloadIT {

    private static final String ORIGINAL_COLOR = "rgba(144, 238, 144, 1)";
    private static final String NEW_COLOR = "rgba(144, 0, 144, 1)";
    private String stylesCssLocation;

    @After
    public void resetFrontend() {
        executeScript("fetch('/context/view/reset_theme')");
    }

    @Test
    public void themeEditsShouldNotReloadPage() throws IOException {
        open();
        stylesCssLocation = $("*").id("styles.css").getText();

        TestBenchElement div1 = $("*").id("div1");

        Assert.assertEquals(ORIGINAL_COLOR,
                div1.getCssValue("backgroundColor"));

        // Modify CSS
        File f = new File(stylesCssLocation);
        String stylesCss = FileUtils.readFileToString(f,
                StandardCharsets.UTF_8);
        stylesCss = stylesCss.replace(ORIGINAL_COLOR, NEW_COLOR);
        FileUtils.writeStringToFile(f, stylesCss, StandardCharsets.UTF_8);

        waitUntil(d -> {
            String color = div1.getCssValue("backgroundColor");
            return color.equals(NEW_COLOR);
        });

        // Ensure a page reload did not take place
        Assert.assertEquals(getInitialAttachId(), getAttachId());
    }

    @After
    public void resetCss() throws IOException {
        if (stylesCssLocation != null) {
            File originalStylesCss = new File(stylesCssLocation
                    .replace("styles.css", "styles.css.original"));
            File stylesCss = new File(stylesCssLocation);
            FileUtils.copyFile(originalStylesCss, stylesCss);
        }
    }
}
