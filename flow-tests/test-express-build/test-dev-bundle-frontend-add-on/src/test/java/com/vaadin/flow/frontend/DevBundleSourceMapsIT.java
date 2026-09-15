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

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.testutil.SourceMapTestUtil;

/**
 * Verifies that the dev bundle can be mapped back to the original sources. This
 * module builds it with {@code build.sourcemap} enabled, see
 * {@code vite.config.ts}.
 */
public class DevBundleSourceMapsIT extends ChromeBrowserTest {

    private static final String LIT_VIEW_SOURCE = "src/main/frontend/views/lit-view.ts";
    private static final String USAGE_STATISTICS_SOURCE = "src/main/frontend/vaadin-usage-statistics-stub.ts";

    @Before
    public void init() {
        // Opening a view makes the application build the dev bundle
        open();
        $("lit-view").waitForFirst();
    }

    @Override
    protected String getTestPath() {
        return "/view/com.vaadin.flow.frontend.LitView";
    }

    /**
     * A build plugin that returns the code of a module without a sourcemap
     * makes the bundler leave that module out of the sourcemap of the chunk it
     * ends up in. A plugin that sees every module then leaves the emitted .map
     * files without any of the original sources.
     */
    @Test
    public void devBundleSourceMapsPointToOriginalSources() throws IOException {
        List<File> sourceMaps = getBundleFiles(".js.map");
        Assert.assertFalse(
                "The dev bundle should have been built with sourcemaps "
                        + "enabled, see vite.config.ts",
                sourceMaps.isEmpty());

        List<String> sources = new ArrayList<>();
        for (File sourceMap : sourceMaps) {
            sources.addAll(SourceMapTestUtil.assertSourceMapUsable(
                    sourceMap.getName(), read(sourceMap)));
        }

        assertHasSource(sources, LIT_VIEW_SOURCE);
        assertHasSource(sources, USAGE_STATISTICS_SOURCE);
    }

    /**
     * The plugin that rewrites the dev mode comment of the usage statistics
     * module is the one that has to return a sourcemap of its own. The
     * rewritten comment starts with {@code /*!}, which tells a minifier to keep
     * it.
     */
    @Test
    public void usageStatisticsCommentIsRewrittenInTheBundle()
            throws IOException {
        File chunk = getBundleChunkOf("vaadin-dev-mode:start");
        String contents = read(chunk);

        Assert.assertTrue(
                chunk.getName() + " should have the usage statistics comment "
                        + "rewritten so that a minifier keeps it",
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

    /**
     * Returns the chunk of the dev bundle that contains the given marker.
     */
    private File getBundleChunkOf(String marker) throws IOException {
        List<File> chunks = getBundleFiles(".js");
        for (File chunk : chunks) {
            if (read(chunk).contains(marker)) {
                return chunk;
            }
        }
        throw new AssertionError("No chunk of the dev bundle contains '"
                + marker + "', looked at " + chunks);
    }

    /**
     * Returns the files of the built dev bundle whose name ends with the given
     * suffix.
     */
    private List<File> getBundleFiles(String suffix) {
        File buildFolder = new File(
                new File(System.getProperty("user.dir", "."),
                        "target/" + Constants.DEV_BUNDLE_LOCATION),
                "webapp/VAADIN/build");
        Assert.assertTrue("The dev bundle should have been built into "
                + buildFolder.getPath(), buildFolder.isDirectory());
        return List.of(
                buildFolder.listFiles((dir, name) -> name.endsWith(suffix)));
    }

    private String read(File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

}
