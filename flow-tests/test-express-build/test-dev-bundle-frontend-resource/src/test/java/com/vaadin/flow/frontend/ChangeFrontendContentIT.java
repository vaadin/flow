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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import elemental.json.Json;
import elemental.json.JsonObject;

public class ChangeFrontendContentIT extends ChromeBrowserTest {

    @Before
    public void init() {
        open();
    }

    @Override
    protected String getTestPath() {
        return "/view/com.vaadin.flow.frontend.LitView";
    }

    @Test
    public void litTemplateWebComponentAdded_newBundleCreated_hashCalculated()
            throws IOException {
        waitForElementPresent(By.tagName("lit-view"));

        File baseDir = new File(System.getProperty("user.dir", "."));

        // should create a dev-bundle
        Assert.assertTrue("New devBundle should be generated",
                new File(baseDir, Constants.DEV_BUNDLE_LOCATION).exists());

        // should add a hash for lit-view
        File statsJson = new File(baseDir,
                Constants.DEV_BUNDLE_LOCATION + "/config/stats.json");
        Assert.assertTrue("Stats.json should exist", statsJson.exists());

        String content = FileUtils.readFileToString(statsJson,
                StandardCharsets.UTF_8);
        JsonObject jsonContent = Json.parse(content);

        JsonObject frontendHashes = jsonContent.getObject("frontendHashes");

        Assert.assertNotNull("Frontend hashes are expected in the stats.json",
                frontendHashes);
        Assert.assertTrue("Lit template content hash is expected",
                frontendHashes.hasKey("views/lit-view.ts"));
        Assert.assertEquals("Unexpected Lit template content hash",
                "b1773c54fd31c71d2ae454262c900a224e9ee72090d1f582d981b5727a43a391",
                frontendHashes.getString("views/lit-view.ts"));
    }
}
