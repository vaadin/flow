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
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ReusingThemeIT extends ChromeBrowserTest {

    private static final String GREEN_COLOR = "rgba(0, 255, 0, 1)";

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
    public void reusableTheme_stylesAppliedFromReusableTheme_devBundleCreated() {
        open();

        // check that the background colour from reused theme applied
        waitUntilCustomBackgroundColor();
        checkLogsForErrors();

        // check that the bundle has the expected asset
        verifyThemeJsonHash();
        verifyExternalAssetInBundle();
        waitUntilExternalAsset();
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

    private void verifyThemeJsonHash() {
        try {
            String themeJsonContent = FileUtils.readFileToString(statsJson,
                    StandardCharsets.UTF_8);
            ObjectNode json = JacksonUtils.readTree(themeJsonContent);
            Assert.assertTrue(json.has("themeJsonContents"));
            Assert.assertTrue(((ObjectNode) json.get("themeJsonContents"))
                    .has("reusable-theme"));
            Assert.assertFalse(((ObjectNode) json.get("themeJsonContents"))
                    .get("reusable-theme").asText().isBlank());

            Assert.assertTrue(json.has("frontendHashes"));
            ObjectNode frontendHashes = (ObjectNode) json.get("frontendHashes");

            Assert.assertTrue(frontendHashes.has(
                    "generated/jar-resources/themes/reusable-theme/components/my-reusable-component.css"));
            Assert.assertFalse(frontendHashes.get(
                    "generated/jar-resources/themes/reusable-theme/components/my-reusable-component.css")
                    .asText().isBlank());

        } catch (IOException e) {
            throw new RuntimeException("Failed to verify theme.json hash", e);
        }
    }

    private void waitUntilCustomBackgroundColor() {
        waitUntil(driver -> isCustomBackGroundColor());
    }

    private boolean isCustomBackGroundColor() {
        try {
            final WebElement body = findElement(By.tagName("body"));
            return GREEN_COLOR.equals(body.getCssValue("background-color"));
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

}
