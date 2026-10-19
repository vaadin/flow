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
package com.vaadin.flow.testutil;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Helpers for the tests that check the sourcemaps a frontend build emits.
 * <p>
 * A build plugin that rewrites a module or a chunk has to chain its sourcemap
 * onto the one the bundler already has, otherwise the emitted map ends up
 * without the original sources and the browser cannot map the bundle back to
 * them.
 */
public final class SourceMapTestUtil {

    private SourceMapTestUtil() {
    }

    /**
     * Asserts that the given sourcemap can be used to map the bundle it belongs
     * to back to the files it was built from.
     *
     * @param name
     *            the name of the sourcemap, used in the assertion messages
     * @param sourceMap
     *            the contents of the sourcemap file
     * @return the original files the sourcemap refers to
     */
    public static List<String> assertSourceMapUsable(String name,
            String sourceMap) {
        JsonNode contents = JsonMapper.shared().readTree(sourceMap);
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

        List<String> sourceNames = new ArrayList<>();
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
            sourceNames.add(source.replace('\\', '/'));
        }
        return sourceNames;
    }
}
