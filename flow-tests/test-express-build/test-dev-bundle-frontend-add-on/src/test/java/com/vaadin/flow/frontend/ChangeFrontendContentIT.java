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
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import com.vaadin.flow.internal.JacksonUtils;
import tools.jackson.databind.node.ObjectNode;

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
        TestBenchElement paragraph = $("lit-view").waitForFirst().$("p")
                .waitForFirst();
        Assert.assertEquals("Greetings from test web component: Hello John Doe",
                paragraph.getText());

        File baseDir = new File(System.getProperty("user.dir", "."));

        // should create a dev-bundle
        Assert.assertTrue("New devBundle should be generated",
                new File(baseDir, "target/" + Constants.DEV_BUNDLE_LOCATION)
                        .exists());

        // should add a hash for lit-view
        File statsJson = new File(baseDir, "target/"
                + Constants.DEV_BUNDLE_LOCATION + "/config/stats.json");
        Assert.assertTrue("Stats.json should exist", statsJson.exists());

        String content = FileUtils.readFileToString(statsJson,
                StandardCharsets.UTF_8);
        ObjectNode jsonContent = JacksonUtils.readTree(content);

        ObjectNode frontendHashes = (ObjectNode) jsonContent.get("frontendHashes");

        Assert.assertNotNull("Frontend hashes are expected in the stats.json",
                frontendHashes);
        Assert.assertTrue("Lit template content hash is expected",
                frontendHashes.has("views/lit-view.ts"));
        Assert.assertTrue("Imported TS file content hash is expected",
                frontendHashes.has("views/another.ts"));
        Assert.assertEquals("Unexpected Lit template content hash",
                "c1ce265100215245a5264dd124c4d890a7f66acbb5ceddf79dcdec2d914e6c30",
                frontendHashes.get("views/lit-view.ts").asText());
        Assert.assertEquals("Unexpected imported file content hash",
                "84951a8a4f324bd4f4a4d39c9913b139af094a0b425773fb8b8d43bb7cd61f10",
                frontendHashes.get("views/another.ts").asText());
    }
}
