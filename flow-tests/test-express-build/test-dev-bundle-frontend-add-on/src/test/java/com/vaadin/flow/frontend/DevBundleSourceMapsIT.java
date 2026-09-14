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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.testutil.SourceMapTestUtil;

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
    private static final String USAGE_STATISTICS_SOURCE = "src/main/frontend/vaadin-usage-statistics.js";

    @Override
    protected String getTestPath() {
        return "/view/com.vaadin.flow.frontend.LitView";
    }

    @Before
    public void init() {
        // The dev bundle is built when the application is first opened
        open();
        waitForElementPresent(By.tagName("lit-view"));
    }

    @Test
    public void devBundleSourceMaps_pointToOriginalSources()
            throws IOException {
        List<String> sources = new ArrayList<>();
        List<File> sourceMaps = getDevBundleSourceMaps();
        for (File sourceMap : sourceMaps) {
            sources.addAll(SourceMapTestUtil.assertSourceMapUsable(
                    sourceMap.getName(), read(sourceMap)));
        }

        assertHasSource(sources, LIT_VIEW_SOURCE);
        assertHasSource(sources, USAGE_STATISTICS_SOURCE);
    }

    /**
     * The plugin that keeps the usage statistics comment rewrites the module it
     * is in, which is the one case where the plugin has to produce a sourcemap
     * of its own instead of leaving the module alone.
     */
    @Test
    public void usageStatisticsComment_isRewrittenInTheBundle()
            throws IOException {
        File chunk = getDevBundleChunkOf("vaadin-dev-mode:start");
        String contents = read(chunk);

        Assert.assertTrue(
                chunk.getName() + " should have the usage statistics comment "
                        + "rewritten so that it is kept in the bundle",
                contents.contains("/*! vaadin-dev-mode:start"));
        Assert.assertFalse(
                chunk.getName() + " should no longer have the original "
                        + "usage statistics comment",
                contents.contains("/** vaadin-dev-mode:start"));
    }

    private void assertHasSource(List<String> sources, String source) {
        Assert.assertTrue(
                "A sourcemap of the dev bundle should refer to " + source
                        + ", only found " + sources,
                sources.stream().anyMatch(name -> name.endsWith(source)));
    }

    private List<File> getDevBundleSourceMaps() {
        List<File> sourceMaps = List.of(getDevBundleBuildFolder()
                .listFiles((dir, name) -> name.endsWith(".js.map")));
        Assert.assertFalse(
                "The dev bundle should have been built with sourcemaps "
                        + "enabled, see vite.config.ts",
                sourceMaps.isEmpty());
        return sourceMaps;
    }

    private File getDevBundleChunkOf(String marker) throws IOException {
        List<File> chunks = List.of(getDevBundleBuildFolder()
                .listFiles((dir, name) -> name.endsWith(".js")));
        for (File chunk : chunks) {
            if (read(chunk).contains(marker)) {
                return chunk;
            }
        }
        throw new AssertionError("No chunk of the dev bundle contains '"
                + marker + "', looked at " + chunks);
    }

    private File getDevBundleBuildFolder() {
        File baseDir = new File(System.getProperty("user.dir", "."));
        File buildFolder = new File(baseDir, "target/"
                + Constants.DEV_BUNDLE_LOCATION + "/webapp/VAADIN/build");
        Assert.assertTrue("The dev bundle should have been built into "
                + buildFolder.getPath(), buildFolder.isDirectory());
        return buildFolder;
    }

    private String read(File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }
}
