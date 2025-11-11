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

import com.vaadin.flow.internal.JacksonUtils;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class DevBundleCssImportIT extends ChromeBrowserTest {

    @Test
    public void cssImportedStyles_hashCalculatedWithNotQuestionMark()
            throws IOException {
        open();
        waitForElementPresent(By.id(DevBundleCssImportView.SPAN_ID));
        WebElement span = findElement(By.id(DevBundleCssImportView.SPAN_ID));

        Assert.assertEquals("3px solid rgb(0, 0, 255)",
                span.getCssValue("border"));

        ObjectNode frontendHashes = getFrontendHashes();
        Assert.assertTrue("My-styles.css content hash is expected",
                frontendHashes.has("styles/my-styles.css"));
        Assert.assertEquals("Unexpected my-styles.css content hash",
                "c015998854f963b9169a1bca554c9ee5828feb8d1bc4a800cdf9dd5e9a0e5d87",
                frontendHashes.get("styles/my-styles.css").asText());

        ArrayNode bundleImports = getBundleImports();
        boolean found = false;
        for (int i = 0; i < bundleImports.size(); i++) {
            if (bundleImports.get(i).asText()
                    .equals("Frontend/styles/my-styles.css")) {
                found = true;
            }
        }

        Assert.assertTrue("my-sass.scss content hash is expected",
                frontendHashes.has("styles/my-sass.scss"));
        Assert.assertEquals("Unexpected my-sass.scss content hash",
                "719cbd39e90caeecd2290124044e7cefb9e6150d3c338d4df71c21bcad825ab5",
                frontendHashes.get("styles/my-sass.scss").asText());

        Assert.assertTrue("my-sass.scss import is expected", found);
        found = false;
        for (int i = 0; i < bundleImports.size(); i++) {
            if (bundleImports.get(i).asText()
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

        ObjectNode frontendHashes = getFrontendHashes();

        Assert.assertTrue("Add-on styles content hash is expected",
                frontendHashes.has("addons-styles/add-on-styles.css"));
        Assert.assertEquals("Unexpected addon styles content hash",
                "f6062ef78e2712e881faa15252bf001d737ab4f12b12e91f0d9f8030100643b6",
                frontendHashes.get("addons-styles/add-on-styles.css").asText());

        ArrayNode bundleImports = getBundleImports();
        boolean found = false;
        for (int i = 0; i < bundleImports.size(); i++) {
            if (bundleImports.get(i).asText().equals(
                    "Frontend/generated/jar-resources/addons-styles/add-on-styles.css")) {
                found = true;
            }
        }
        Assert.assertTrue("Addon import is expected", found);
    }

    public static ObjectNode getFrontendHashes() throws IOException {
        ObjectNode statsJson = getStatsJson();
        ObjectNode frontendHashes = (ObjectNode) statsJson.get("frontendHashes");
        Assert.assertNotNull("Frontend hashes are expected in the stats.json",
                frontendHashes);
        return frontendHashes;
    }

    private static ArrayNode getBundleImports() throws IOException {
        ObjectNode statsJson = getStatsJson();
        ArrayNode bundleImports = (ArrayNode) statsJson.get("bundleImports");
        Assert.assertNotNull("Bundle imports are expected in the stats.json",
                bundleImports);
        return bundleImports;
    }

    private static ObjectNode getStatsJson() throws IOException {
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
        return JacksonUtils.readTree(content);
    }
}
