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
package com.vaadin.flow.internal;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleUtilsTest {

    private List<AutoCloseable> closeOnTearDown = new ArrayList<>();
    @TempDir
    Path temporaryFolder;

    @AfterEach
    public void tearDown() {
        for (AutoCloseable closeable : closeOnTearDown) {
            try {
                closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void frontendImportVariantsIncluded() {
        mockStatsJson("Frontend/foo.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        assertTrue(bundleImports.contains("Frontend/foo.js"));
        assertTrue(bundleImports.contains("foo.js"));
        assertTrue(bundleImports.contains("./foo.js"));
    }

    @Test
    public void jarImportVariantsIncluded() {
        mockStatsJson("Frontend/generated/jar-resources/my/addon.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        assertTrue(bundleImports
                .contains("Frontend/generated/jar-resources/my/addon.js"));
        assertTrue(bundleImports.contains("./my/addon.js"));
        assertTrue(bundleImports.contains("my/addon.js"));
    }

    @Test
    public void frontendInTheMiddleNotTouched() {
        mockStatsJson("my/Frontend/foo.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        assertEquals(Set.of("my/Frontend/foo.js"), bundleImports);
    }

    @Test
    public void themeVariantsHandled() {
        mockStatsJson("@foo/bar/theme/lumo/file.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        assertTrue(bundleImports.contains("@foo/bar/theme/lumo/file.js"));
        assertTrue(bundleImports.contains("@foo/bar/src/file.js"));
    }

    @Test
    public void themeVariantsFromJarHandled() {
        mockStatsJson("Frontend/generated/jar-resources/theme/lumo/file.js",
                "Frontend/generated/jar-resources/theme/material/file.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        assertTrue(bundleImports.contains(
                "Frontend/generated/jar-resources/theme/lumo/file.js"));
        assertTrue(bundleImports.contains(
                "Frontend/generated/jar-resources/theme/material/file.js"));
        assertTrue(bundleImports.contains("./src/file.js"));
    }

    private void mockStatsJson(String... imports) {
        ObjectNode statsJson = JacksonUtils.createObjectNode();
        ArrayNode importsArray = JacksonUtils.createArrayNode();
        for (String anImport : imports) {
            importsArray.add(anImport);
        }

        statsJson.set("bundleImports", importsArray);

        mockStatsJsonLoading(statsJson);
    }

    private void mockStatsJsonLoading(JsonNode statsJson) {
        MockedStatic<BundleUtils> mock = Mockito.mockStatic(BundleUtils.class);
        mock.when(() -> BundleUtils.loadStatsJson()).thenReturn(statsJson);
        mock.when(() -> BundleUtils.loadBundleImports()).thenCallRealMethod();
        closeOnTearDown.add(mock);

    }

    @Test
    public void loadStatsJson_cachesResult_returnsSameInstance() {
        // First call loads and caches
        ObjectNode first = BundleUtils.loadStatsJson();
        // Second call returns cached instance
        ObjectNode second = BundleUtils.loadStatsJson();
        assertSame(first, second,
                "Should return cached instance on second call");
    }

    @Test
    public void loadStatsJson_cachedResultIsConsistent() {
        ObjectNode first = BundleUtils.loadStatsJson();
        ObjectNode second = BundleUtils.loadStatsJson();

        // Verify both have same content (whether same instance or not)
        assertEquals(first.toString(), second.toString(),
                "Cached result should be consistent");
    }

    @Test
    public void isPreCompiledProductionBundle_usesCachedStats() {
        // Call multiple times
        boolean first = BundleUtils.isPreCompiledProductionBundle();
        boolean second = BundleUtils.isPreCompiledProductionBundle();
        boolean third = BundleUtils.isPreCompiledProductionBundle();

        // All should return same result
        assertEquals(first, second, "Should return consistent results");
        assertEquals(second, third, "Should return consistent results");
    }
}
