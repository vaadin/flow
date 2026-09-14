/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * A build plugin that hands back the code of a module without a sourcemap drops
 * that module from the map of the chunk it ends up in. The plugins of the dev
 * bundle build see every module of the bundle, so one of them doing that leaves
 * the emitted .map files without sources or mappings and the browser cannot map
 * the bundle back to the original files.
 *
 * @see <a href=
 *      "https://github.com/vaadin/flow/issues/16679">vaadin/flow#16679</a>
 */
public class DevBundleSourceMapsIT extends ChromeBrowserTest {

    private static final String LIT_VIEW_SOURCE = "src/main/frontend/views/lit-view.ts";

    @Override
    protected String getTestPath() {
        return "/view/com.vaadin.flow.frontend.LitView";
    }

    @Test
    public void devBundleSourceMapsPointToOriginalSources() throws IOException {
        // The dev bundle is built when the application is first opened
        open();
        waitForElementPresent(By.tagName("lit-view"));

        boolean litViewFound = false;
        List<File> sourceMaps = getDevBundleSourceMaps();
        for (File sourceMap : sourceMaps) {
            litViewFound |= assertSourceMapUsable(sourceMap);
        }

        Assert.assertTrue(
                "A sourcemap of the dev bundle should refer to "
                        + LIT_VIEW_SOURCE + ", only found " + sourceMaps,
                litViewFound);
    }

    /**
     * Asserts that the given sourcemap can be used to map the chunk back to the
     * files it was built from, and tells whether the view source is one of
     * them.
     */
    private boolean assertSourceMapUsable(File sourceMap) throws IOException {
        String name = sourceMap.getName();
        JsonNode contents = JsonMapper.shared().readTree(
                FileUtils.readFileToString(sourceMap, StandardCharsets.UTF_8));
        JsonNode sources = contents.get("sources");
        JsonNode sourcesContent = contents.get("sourcesContent");

        Assert.assertNotEquals(name + " should have a sourcemap with sources",
                0, sources.size());
        Assert.assertNotEquals(name + " should have a sourcemap with mappings",
                "", contents.get("mappings").asString());
        Assert.assertEquals(
                name + " should have the contents of every source in its "
                        + "sourcemap",
                sources.size(), sourcesContent.size());

        boolean litViewFound = false;
        for (int i = 0; i < sources.size(); i++) {
            String source = sources.get(i).asString();
            Assert.assertNotEquals(
                    name + " should have a sourcemap referring to the "
                            + "original files, was " + source,
                    "", source.trim());
            Assert.assertNotEquals(
                    name + " should have the contents of " + source
                            + " in its sourcemap",
                    "", sourcesContent.get(i).asString().trim());
            litViewFound |= source.replace('\\', '/').endsWith(LIT_VIEW_SOURCE);
        }
        return litViewFound;
    }

    private List<File> getDevBundleSourceMaps() {
        File baseDir = new File(System.getProperty("user.dir", "."));
        File buildFolder = new File(baseDir, "target/"
                + Constants.DEV_BUNDLE_LOCATION + "/webapp/VAADIN/build");
        Assert.assertTrue("The dev bundle should have been built into "
                + buildFolder.getPath(), buildFolder.isDirectory());

        List<File> sourceMaps = new ArrayList<>(List.of(buildFolder
                .listFiles((dir, name) -> name.endsWith(".js.map"))));
        Assert.assertFalse(
                "The dev bundle should have been built with sourcemaps "
                        + "enabled, see vite.config.ts",
                sourceMaps.isEmpty());
        return sourceMaps;
    }
}
