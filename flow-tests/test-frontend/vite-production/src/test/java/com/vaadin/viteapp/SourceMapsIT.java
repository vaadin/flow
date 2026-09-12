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

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class SourceMapsIT {

    private static final String BUILD_PATH = "/VAADIN/build/";

    private String getRootURL() {
        return "http://localhost:8888";
    }

    /**
     * A build plugin that rewrites a chunk must chain its sourcemap onto the
     * one the bundler already has for that chunk, otherwise the emitted .map
     * file ends up without sources and mappings and the browser cannot map the
     * bundle back to the original files.
     */
    @Test
    public void bundleSourceMapsHaveSourcesAndMappings() throws Exception {
        String entryBundle = getJsBundleName();
        Set<String> bundles = new LinkedHashSet<>();
        bundles.add(entryBundle);
        bundles.addAll(getImportedBundles(entryBundle));

        Assert.assertNotNull(
                entryBundle + " should refer to an emitted sourcemap",
                getSourceMapName(download(BUILD_PATH + entryBundle)));

        for (String bundle : bundles) {
            String sourceMapName = getSourceMapName(
                    download(BUILD_PATH + bundle));
            if (sourceMapName == null) {
                // Bundler runtime helpers are emitted without a sourcemap
                continue;
            }
            JsonNode sourceMap = JsonMapper.shared()
                    .readTree(download(BUILD_PATH + sourceMapName));
            Assert.assertNotEquals(
                    bundle + " should have a sourcemap with sources", 0,
                    sourceMap.get("sources").size());
            Assert.assertNotEquals(
                    bundle + " should have a sourcemap with mappings", "",
                    sourceMap.get("mappings").asString());
        }
    }

    /**
     * Finds the bundles that the given bundle statically imports, so that the
     * chunks which are not referenced from index.html are covered as well.
     */
    private Set<String> getImportedBundles(String bundle) throws Exception {
        Set<String> bundles = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("[\"']\\./([^\"']+\\.js)[\"']")
                .matcher(download(BUILD_PATH + bundle));
        while (matcher.find()) {
            bundles.add(matcher.group(1));
        }
        return bundles;
    }

    private String getSourceMapName(String bundleContents) {
        Matcher matcher = Pattern.compile("//# sourceMappingURL=(\\S+)")
                .matcher(bundleContents);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String getJsBundleName() throws Exception {
        String indexHtml = download("/index.html");
        Matcher matcher = Pattern
                .compile(".* src=\"\\./VAADIN/build/([^\"]*)\".*",
                        Pattern.DOTALL)
                .matcher(indexHtml);
        if (!matcher.matches()) {
            throw new IllegalStateException("No script found");
        }
        return matcher.group(1);
    }

    private String download(String path) throws Exception {
        return IOUtils.toString(new URL(getRootURL() + path),
                StandardCharsets.UTF_8);
    }

}
