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
package com.vaadin.flow.devbuild;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import elemental.json.Json;
import elemental.json.JsonObject;

public class ParentThemeIT extends ChromeBrowserTest {

    private static final String BLUE_COLOR = "rgba(0, 0, 255, 1)";
    private static final String RED_COLOR = "rgba(255, 0, 0, 1)";

    private File statsJson;
    private File themeAssetsInBundle;

    @Before
    public void init() {
        File baseDir = new File(System.getProperty("user.dir", "."));
        statsJson = new File(baseDir, "target/" + Constants.DEV_BUNDLE_LOCATION
                + "/config/stats.json");
        themeAssetsInBundle = new File(baseDir,
                "target/" + Constants.DEV_BUNDLE_LOCATION
                        + "/assets/themes/reusable-theme");
    }

    @Test
    public void parentTheme_stylesAppliedFromParentTheme() {
        open();

        // check that the background colour is overriden by the child theme
        waitUntilChildThemeBackgroundColor();
        waitUntilParentThemeStyles();
        checkLogsForErrors();

        // app dev bundle has reusable theme in stats.json
        verifyThemeJsonHashReusableThemeRecord();
        // check that the bundle has the expected asset for dev mode
        verifyExternalAssetInBundle();
        waitUntilExternalAsset();
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

    private void waitUntilExternalAsset() {
        waitUntil(driver -> {
            WebElement image = findElement(By.tagName("img"));
            if (image != null) {
                String id = image.getAttribute("id");
                Assert.assertEquals("Fortawesome asset not found, but expected",
                        "snowflake", id);
                return true;
            }
            return false;
        });
    }

    private void verifyExternalAssetInBundle() {
        File lineAwesome = new File(themeAssetsInBundle,
                "fortawesome/icons/snowflake.svg");
        Assert.assertTrue("External asset file is not found in the bundle",
                lineAwesome.exists());
    }

    private void verifyThemeJsonHashReusableThemeRecord() {
        try {
            String themeJsonContent = FileUtils.readFileToString(statsJson,
                    StandardCharsets.UTF_8);
            JsonObject json = Json.parse(themeJsonContent);
            Assert.assertTrue(json.hasKey("themeJsonContents"));
            Assert.assertTrue(json.getObject("themeJsonContents")
                    .hasKey("reusable-theme"));
            Assert.assertFalse(json.getObject("themeJsonContents")
                    .getString("reusable-theme").isBlank());
        } catch (IOException e) {
            throw new RuntimeException("Failed to verify theme.json hash", e);
        }
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
