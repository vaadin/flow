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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Verifies that the dev bundle can be mapped back to the original sources. This
 * module builds it with {@code build.sourcemap} enabled, see
 * {@code vite.config.ts}.
 */
public class DevBundleSourceMapsIT extends ChromeBrowserTest {

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
        List<File> sourceMaps = getBundleFiles(".map");
        Assert.assertNotEquals("Dev bundle should contain sourcemaps", 0,
                sourceMaps.size());

        boolean litViewFound = false;
        for (File sourceMap : sourceMaps) {
            litViewFound |= assertSourceMapUsable(sourceMap);
        }
        Assert.assertTrue(
                "The sourcemaps should refer to views/lit-view.ts, so that the "
                        + "browser can show the original source of the view",
                litViewFound);
    }

    /**
     * The plugin that rewrites the dev mode comment of the usage statistics
     * module is the one that has to return a sourcemap of its own. The
     * rewritten comment starts with {@code /*!}, which tells a minifier to keep
     * it, and the rewritten module has to stay in the sourcemap of its chunk.
     */
    @Test
    public void usageStatisticsCommentIsRewrittenAndStaysInSourceMap()
            throws IOException {
        File chunk = null;
        for (File bundle : getBundleFiles(".js")) {
            String contents = FileUtils.readFileToString(bundle,
                    StandardCharsets.UTF_8);
            Assert.assertFalse(
                    bundle.getName() + " should have the dev mode comment "
                            + "rewritten into the form a minifier keeps",
                    contents.contains("/** vaadin-dev-mode:start"));
            if (contents.contains("/*! vaadin-dev-mode:start")) {
                chunk = bundle;
            }
        }
        Assert.assertNotNull(
                "The dev bundle should contain the dev mode comment of the "
                        + "usage statistics module",
                chunk);

        File sourceMap = new File(chunk.getParentFile(),
                chunk.getName() + ".map");
        Assert.assertTrue(chunk.getName() + " should have a sourcemap",
                sourceMap.isFile());

        boolean stubFound = false;
        for (JsonNode source : readSourceMap(sourceMap).get("sources")) {
            stubFound |= source.asString()
                    .endsWith("vaadin-usage-statistics-stub.ts");
        }
        Assert.assertTrue("The rewritten module should be in the sourcemap of "
                + chunk.getName(), stubFound);
    }

    /**
     * Asserts that the given sourcemap has the original sources with their
     * contents, and tells whether one of them is the Lit view of this
     * application.
     */
    private boolean assertSourceMapUsable(File sourceMap) throws IOException {
        String name = sourceMap.getName();
        JsonNode json = readSourceMap(sourceMap);
        JsonNode sources = json.get("sources");
        JsonNode sourcesContent = json.get("sourcesContent");

        Assert.assertNotEquals(name + " should have sources", 0,
                sources.size());
        Assert.assertNotEquals(name + " should have mappings", "",
                json.get("mappings").asString());
        Assert.assertEquals(name + " should have the contents of every source",
                sources.size(), sourcesContent.size());

        boolean litViewFound = false;
        for (int i = 0; i < sources.size(); i++) {
            String source = sources.get(i).asString();
            Assert.assertNotEquals(
                    name + " should refer to the original files, was " + source,
                    "", source.trim());
            Assert.assertNotEquals(
                    name + " should have the contents of " + source, "",
                    sourcesContent.get(i).asString().trim());
            litViewFound |= source.endsWith("views/lit-view.ts");
        }
        return litViewFound;
    }

    private JsonNode readSourceMap(File sourceMap) throws IOException {
        return JsonMapper.shared().readTree(
                FileUtils.readFileToString(sourceMap, StandardCharsets.UTF_8));
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
        Assert.assertTrue("Dev bundle should have been built",
                buildFolder.isDirectory());
        return List.of(
                buildFolder.listFiles((dir, name) -> name.endsWith(suffix)));
    }

}
