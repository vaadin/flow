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
package com.vaadin.flow.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class DevBundleCssImportIT extends ChromeBrowserTest {

    @Test
    public void cssImportedStyles_hashCalculatedWithNotQuestionMark()
            throws IOException {
        open();
        waitForElementPresent(By.id(DevBundleCssImportView.SPAN_ID));
        WebElement span = findElement(By.id(DevBundleCssImportView.SPAN_ID));

        Assert.assertEquals("3px solid rgb(0, 0, 255)",
                span.getCssValue("border"));

        JsonObject frontendHashes = getFrontendHashes();
        Assert.assertTrue("My-styles.css content hash is expected",
                frontendHashes.hasKey("styles/my-styles.css"));
        Assert.assertEquals("Unexpected my-styles.css content hash",
                "c015998854f963b9169a1bca554c9ee5828feb8d1bc4a800cdf9dd5e9a0e5d87",
                frontendHashes.getString("styles/my-styles.css"));

        JsonArray bundleImports = getBundleImports();
        boolean found = false;
        for (int i = 0; i < bundleImports.length(); i++) {
            if (bundleImports.get(i).asString()
                    .equals("Frontend/styles/my-styles.css")) {
                found = true;
            }
        }

        Assert.assertTrue("my-sass.scss content hash is expected",
                frontendHashes.hasKey("styles/my-sass.scss"));
        Assert.assertEquals("Unexpected my-sass.scss content hash",
                "719cbd39e90caeecd2290124044e7cefb9e6150d3c338d4df71c21bcad825ab5",
                frontendHashes.getString("styles/my-sass.scss"));

        Assert.assertTrue("my-sass.scss import is expected", found);
        found = false;
        for (int i = 0; i < bundleImports.length(); i++) {
            if (bundleImports.get(i).asString()
                    .equals("Frontend/styles/my-sass.scss")) {
                found = true;
            }
        }
        Assert.assertTrue("my-sass.scss import is expected", found);
    }

    @Test
    public void cssImportedStyles_stylesInMetaInfResources_stylesApplied_hashCalculated()
            throws IOException {
        open();

        waitForElementPresent(By.id(DevBundleCssImportView.MY_COMPONENT_ID));
        WebElement myComponent = findElement(
                By.id(DevBundleCssImportView.MY_COMPONENT_ID));

        Assert.assertEquals("1px solid rgb(0, 128, 0)",
                myComponent.getCssValue("border"));

        JsonObject frontendHashes = getFrontendHashes();

        Assert.assertTrue("Add-on styles content hash is expected",
                frontendHashes.hasKey("addons-styles/add-on-styles.css"));
        Assert.assertEquals("Unexpected addon styles content hash",
                "f6062ef78e2712e881faa15252bf001d737ab4f12b12e91f0d9f8030100643b6",
                frontendHashes.getString("addons-styles/add-on-styles.css"));

        JsonArray bundleImports = getBundleImports();
        boolean found = false;
        for (int i = 0; i < bundleImports.length(); i++) {
            if (bundleImports.get(i).asString().equals(
                    "Frontend/generated/jar-resources/addons-styles/add-on-styles.css")) {
                found = true;
            }
        }
        Assert.assertTrue("Addon import is expected", found);
    }

    public static JsonObject getFrontendHashes() throws IOException {
        JsonObject statsJson = getStatsJson();
        JsonObject frontendHashes = statsJson.getObject("frontendHashes");
        Assert.assertNotNull("Frontend hashes are expected in the stats.json",
                frontendHashes);
        return frontendHashes;
    }

    private static JsonArray getBundleImports() throws IOException {
        JsonObject statsJson = getStatsJson();
        JsonArray bundleImports = statsJson.getArray("bundleImports");
        Assert.assertNotNull("Bundle imports are expected in the stats.json",
                bundleImports);
        return bundleImports;
    }

    private static JsonObject getStatsJson() throws IOException {
        File baseDir = new File(System.getProperty("user.dir", "."));

        // should create a dev-bundle
        Assert.assertTrue("New devBundle should be generated",
                new File(baseDir, "target/" + Constants.DEV_BUNDLE_LOCATION)
                        .exists());

        File statsJson = new File(baseDir, "target/"
                + Constants.DEV_BUNDLE_LOCATION + "/config/stats.json");
        Assert.assertTrue("Stats.json should exist", statsJson.exists());

        String content = FileUtils.readFileToString(statsJson,
                StandardCharsets.UTF_8);
        return Json.parse(content);
    }
}
