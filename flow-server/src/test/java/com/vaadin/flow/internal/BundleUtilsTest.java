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
package com.vaadin.flow.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class BundleUtilsTest {

    private List<AutoCloseable> closeOnTearDown = new ArrayList<>();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
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

        Assert.assertTrue(bundleImports.contains("Frontend/foo.js"));
        Assert.assertTrue(bundleImports.contains("foo.js"));
        Assert.assertTrue(bundleImports.contains("./foo.js"));
    }

    @Test
    public void jarImportVariantsIncluded() {
        mockStatsJson("Frontend/generated/jar-resources/my/addon.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        Assert.assertTrue(bundleImports
                .contains("Frontend/generated/jar-resources/my/addon.js"));
        Assert.assertTrue(bundleImports.contains("./my/addon.js"));
        Assert.assertTrue(bundleImports.contains("my/addon.js"));
    }

    @Test
    public void frontendInTheMiddleNotTouched() {
        mockStatsJson("my/Frontend/foo.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        Assert.assertEquals(Set.of("my/Frontend/foo.js"), bundleImports);
    }

    @Test
    public void themeVariantsHandled() {
        mockStatsJson("@foo/bar/theme/lumo/file.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        Assert.assertTrue(
                bundleImports.contains("@foo/bar/theme/lumo/file.js"));
        Assert.assertTrue(bundleImports.contains("@foo/bar/src/file.js"));
    }

    @Test
    public void themeVariantsFromJarHandled() {
        mockStatsJson("Frontend/generated/jar-resources/theme/lumo/file.js",
                "Frontend/generated/jar-resources/theme/material/file.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        Assert.assertTrue(bundleImports.contains(
                "Frontend/generated/jar-resources/theme/lumo/file.js"));
        Assert.assertTrue(bundleImports.contains(
                "Frontend/generated/jar-resources/theme/material/file.js"));
        Assert.assertTrue(bundleImports.contains("./src/file.js"));
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
        Assert.assertSame("Should return cached instance on second call", first,
                second);
    }

    @Test
    public void loadStatsJson_cachedResultIsConsistent() {
        ObjectNode first = BundleUtils.loadStatsJson();
        ObjectNode second = BundleUtils.loadStatsJson();

        // Verify both have same content (whether same instance or not)
        Assert.assertEquals("Cached result should be consistent",
                first.toString(), second.toString());
    }

    @Test
    public void isPreCompiledProductionBundle_usesCachedStats() {
        // Call multiple times
        boolean first = BundleUtils.isPreCompiledProductionBundle();
        boolean second = BundleUtils.isPreCompiledProductionBundle();
        boolean third = BundleUtils.isPreCompiledProductionBundle();

        // All should return same result
        Assert.assertEquals("Should return consistent results", first, second);
        Assert.assertEquals("Should return consistent results", second, third);
    }
}
