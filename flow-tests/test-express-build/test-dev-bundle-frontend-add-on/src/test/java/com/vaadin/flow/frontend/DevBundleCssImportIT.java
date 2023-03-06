/*
 * Copyright 2000-2023 Vaadin Ltd.
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
    public void cssImportedStyles_stylesInMetaInfResources_stylesApplied_hashCalculated() throws IOException {
        open();

        WebElement myComponent =
                findElement(By.id(DevBundleCssImportView.MY_COMPONENT_ID));

        Assert.assertEquals("1px solid rgb(0, 128, 0)", myComponent.getCssValue(
                "border"));

        JsonObject frontendHashes = getFrontendHashes();

        // TODO: remove '?inline' after https://github.com/vaadin/flow/pull/16125
        Assert.assertTrue("Add-on styles content hash is expected",
                frontendHashes.hasKey("addons-styles/add-on-styles.css?inline"));
        Assert.assertEquals("Unexpected addon styles content hash",
                "5a7bc75b3b5edc5051ed36f75f625e204260459b4f1872dfc03a255b944fc89e",
                frontendHashes.getString("addons-styles/add-on-styles.css?inline"));

        JsonArray bundleImports = getBundleImports();
        boolean found = false;
        for (int i = 0; i < bundleImports.length(); i++) {
            if (bundleImports.get(i).asString()
                    .equals("Frontend/generated/jar-resources/addons-styles/add-on-styles.css?inline")) {
                found = true;
            }
        }
        Assert.assertTrue("Addon import is expected", found);
    }

    private static JsonObject getFrontendHashes() throws IOException {
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
                new File(baseDir, Constants.DEV_BUNDLE_LOCATION).exists());

        File statsJson = new File(baseDir,
                Constants.DEV_BUNDLE_LOCATION + "/config/stats.json");
        Assert.assertTrue("Stats.json should exist", statsJson.exists());

        String content = FileUtils.readFileToString(statsJson,
                StandardCharsets.UTF_8);
        return Json.parse(content);
    }
}
