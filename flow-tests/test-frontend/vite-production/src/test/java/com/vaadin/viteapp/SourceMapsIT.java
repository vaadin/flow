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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

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
                Assert.assertNotEquals(
                        entryBundle + " should refer to an emitted sourcemap",
                        entryBundle, bundle);
                // Bundler runtime helpers are emitted without a sourcemap
                continue;
            }
            assertSourceMapUsable(bundle, JsonMapper.shared()
                    .readTree(download(BUILD_PATH + matcher.group(1))));
            checkedBundles++;
        }

        Assert.assertNotEquals("No bundle with a sourcemap was found", 0,
                checkedBundles);
    }

    private void assertSourceMapUsable(String bundle, JsonNode sourceMap) {
        JsonNode sources = sourceMap.get("sources");
        JsonNode sourcesContent = sourceMap.get("sourcesContent");
        Assert.assertNotEquals(bundle + " should have a sourcemap with sources",
                0, sources.size());
        Assert.assertNotEquals(
                bundle + " should have a sourcemap with mappings", "",
                sourceMap.get("mappings").asString());
        Assert.assertEquals(
                bundle + " should have the contents of every source in its "
                        + "sourcemap",
                sources.size(), sourcesContent.size());
        for (int i = 0; i < sources.size(); i++) {
            Assert.assertNotEquals(
                    bundle + " should have a sourcemap referring to the "
                            + "original files, was " + sources.get(i),
                    "", sources.get(i).asString().trim());
            Assert.assertNotEquals(
                    bundle + " should have the contents of "
                            + sources.get(i).asString() + " in its sourcemap",
                    "", sourcesContent.get(i).asString().trim());
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
