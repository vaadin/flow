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
package com.vaadin.viteapp;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Test;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.testutil.SourceMapTestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SourceMapsIT extends BundleAccess {

    private static final Pattern IMPORTED_BUNDLE = Pattern
            .compile("[\"']\\./([^\"']+\\.js)[\"']");
    private static final Pattern SOURCE_MAPPING_URL = Pattern
            .compile("//# sourceMappingURL=(\\S+)");

    /**
     * A build plugin that rewrites a chunk must chain its sourcemap onto the
     * one the bundler already has for that chunk, otherwise the emitted .map
     * file ends up without the original sources and the browser cannot map the
     * bundle back to them.
     */
    @Test
    public void bundleSourceMapsPointToOriginalSources() throws Exception {
        String entryBundle = getJsBundleName();
        int checkedBundles = 0;

        for (String bundle : getBundles(entryBundle)) {
            String contents = download(BUILD_PATH + bundle);
            Matcher matcher = SOURCE_MAPPING_URL.matcher(contents);
            if (!matcher.find()) {
                assertNotEquals(
                        entryBundle + " should refer to an emitted sourcemap",
                        entryBundle, bundle);
                // Bundler runtime helpers are emitted without a sourcemap
                continue;
            }
            SourceMapTestUtil.assertSourceMapUsable(bundle,
                    download(BUILD_PATH + matcher.group(1)));
            checkedBundles++;
        }

        assertNotEquals("No bundle with a sourcemap was found", 0,
                checkedBundles);
    }

    /**
     * The sourcemaps of the Flow client are published as their own artifact
     * that only the dev server brings in, so a production build copies the
     * client out of the jars without them.
     */
    @Test
    public void clientIsCopiedFromJarsWithoutSourceMaps() throws Exception {
        File jarResources = new File(System.getProperty("user.dir", "."),
                FrontendUtils.DEFAULT_FRONTEND_DIR + FrontendUtils.GENERATED
                        + FrontendUtils.JAR_RESOURCES_FOLDER);

        try (Stream<Path> files = Files.walk(jarResources.toPath())) {
            List<Path> copied = files.filter(Files::isRegularFile).toList();

            assertTrue(
                    "The build should have copied the client from the jars "
                            + "into " + jarResources,
                    copied.stream().anyMatch(file -> file.endsWith(
                            Path.of("bootstrap", "Bootstrapper.js"))));
            assertEquals(
                    "A production build should not copy any sourcemap into "
                            + jarResources,
                    List.of(),
                    copied.stream()
                            .filter(file -> file.toString().endsWith(".js.map"))
                            .toList());
        }
    }

    /**
     * Collects the given bundle and the bundles it imports, directly or through
     * another bundle. Names that are not served are left out, as not every file
     * name in a bundle is an emitted chunk.
     */
    private Set<String> getBundles(String entryBundle) throws Exception {
        Set<String> bundles = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.add(entryBundle);
        while (!pending.isEmpty()) {
            String bundle = pending.remove();
            String contents = downloadIfAvailable(BUILD_PATH + bundle);
            if (contents == null || !bundles.add(bundle)) {
                continue;
            }
            Matcher matcher = IMPORTED_BUNDLE.matcher(contents);
            while (matcher.find()) {
                pending.add(matcher.group(1));
            }
        }
        return bundles;
    }

}
