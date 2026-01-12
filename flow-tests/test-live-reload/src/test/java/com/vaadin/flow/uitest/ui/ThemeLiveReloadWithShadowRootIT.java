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
package com.vaadin.flow.uitest.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.testbench.TestBenchElement;

@NotThreadSafe
public class ThemeLiveReloadWithShadowRootIT extends AbstractLiveReloadIT {

    private static final String ORIGINAL_COLOR = "rgba(255, 255, 255, 1)";
    private static final String NEW_COLOR = "rgba(0, 255, 0, 1)";
    private String stylesCssLocation;

    @After
    public void resetFrontend() {
        executeScript("fetch('/context/view/reset_theme')");
    }

    @Test
    public void themeEditsShouldApplyInsideShadowRoot() throws IOException {
        open();
        stylesCssLocation = $("*").id("styles.css").getText();

        TestBenchElement inside = getInside();

        Assert.assertEquals(ORIGINAL_COLOR, inside.getCssValue("color"));

        File f = new File(stylesCssLocation);
        String stylesCss = FileUtils.readFileToString(f,
                StandardCharsets.UTF_8);
        stylesCss = stylesCss.replace(ORIGINAL_COLOR, NEW_COLOR);
        FileUtils.writeStringToFile(f, stylesCss, StandardCharsets.UTF_8);

        waitUntil(d -> {
            try {
                String color = getInside().getCssValue("color");
                return color.equals(NEW_COLOR);
            } catch (Exception e) {
                return null;
            }
        });

        // Ensure a page reload did not take place
        Assert.assertEquals(getInitialAttachId(), getAttachId());

    }

    private TestBenchElement getInside() {
        return $("component-with-theme").first().$("*").id("inside");
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
