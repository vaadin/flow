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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BundleUtilsTest {

    private List<AutoCloseable> closeOnTearDown = new ArrayList<>();
    @TempDir
    Path temporaryFolder;

    @AfterEach
    void tearDown() {
        for (AutoCloseable closeable : closeOnTearDown) {
            try {
                closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    void frontendImportVariantsIncluded() {
        mockStatsJson("Frontend/foo.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        assertTrue(bundleImports.contains("Frontend/foo.js"));
        assertTrue(bundleImports.contains("foo.js"));
        assertTrue(bundleImports.contains("./foo.js"));
    }

    @Test
    void jarImportVariantsIncluded() {
        mockStatsJson("Frontend/generated/jar-resources/my/addon.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        assertTrue(bundleImports
                .contains("Frontend/generated/jar-resources/my/addon.js"));
        assertTrue(bundleImports.contains("./my/addon.js"));
        assertTrue(bundleImports.contains("my/addon.js"));
    }

    @Test
    void frontendInTheMiddleNotTouched() {
        mockStatsJson("my/Frontend/foo.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        assertEquals(Set.of("my/Frontend/foo.js"), bundleImports);
    }

    @Test
    void themeVariantsHandled() {
        mockStatsJson("@foo/bar/theme/lumo/file.js");
        Set<String> bundleImports = BundleUtils.loadBundleImports();

        assertTrue(bundleImports.contains("@foo/bar/theme/lumo/file.js"));
        assertTrue(bundleImports.contains("@foo/bar/src/file.js"));
    }

    @Test
    void themeVariantsFromJarHandled() {
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
        MockedStatic<BundleUtils> mock = mockStatic(BundleUtils.class);
        mock.when(() -> BundleUtils.loadStatsJson()).thenReturn(statsJson);
        mock.when(() -> BundleUtils.loadBundleImports()).thenCallRealMethod();
        closeOnTearDown.add(mock);

    }

    @Test
    void loadStatsJson_cachesResult_returnsSameInstance() {
        // First call loads and caches
        ObjectNode first = BundleUtils.loadStatsJson();
        // Second call returns cached instance
        ObjectNode second = BundleUtils.loadStatsJson();
        assertSame(first, second,
                "Should return cached instance on second call");
    }

    @Test
    void loadStatsJson_cachedResultIsConsistent() {
        ObjectNode first = BundleUtils.loadStatsJson();
        ObjectNode second = BundleUtils.loadStatsJson();

        // Verify both have same content (whether same instance or not)
        assertEquals(first.toString(), second.toString(),
                "Cached result should be consistent");
    }

    @Test
    void isPreCompiledProductionBundle_usesCachedStats() {
        // Call multiple times
        boolean first = BundleUtils.isPreCompiledProductionBundle();
        boolean second = BundleUtils.isPreCompiledProductionBundle();
        boolean third = BundleUtils.isPreCompiledProductionBundle();

        // All should return same result
        assertEquals(first, second, "Should return consistent results");
        assertEquals(second, third, "Should return consistent results");
    }

    @JsDefinition
    interface CarriedJs {
        @JsExpression("this.focus()")
        void focus();
    }

    @JsDefinition
    interface MissingJs {
        @JsExpression("this.blur()")
        void blur();
    }

    @Test
    void checkJsFunctionsBundled_productionMode_logsErrorOnlyForMissingFunction() {
        ObjectNode statsJson = JacksonUtils.createObjectNode();
        statsJson.putArray(BundleUtils.JS_DEFINITION_FUNCTION_IDS_STATS_KEY)
                .add(JsCall.functionId("this.focus()", 0, false))
                .add("another function");
        MockedStatic<BundleUtils> bundleUtils = mockStatic(BundleUtils.class,
                CALLS_REAL_METHODS);
        bundleUtils.when(BundleUtils::loadStatsJson).thenReturn(statsJson);
        closeOnTearDown.add(bundleUtils);
        Logger logger = mock(Logger.class);
        MockedStatic<LoggerFactory> loggerFactory = mockStatic(
                LoggerFactory.class);
        loggerFactory.when(() -> LoggerFactory.getLogger(BundleUtils.class))
                .thenReturn(logger);
        closeOnTearDown.add(loggerFactory);
        DeploymentConfiguration configuration = mock(
                DeploymentConfiguration.class);
        when(configuration.isProductionMode()).thenReturn(true);
        VaadinService service = mock(VaadinService.class);
        when(service.getDeploymentConfiguration()).thenReturn(configuration);
        VaadinService.setCurrent(service);
        closeOnTearDown.add(CurrentInstance::clearAll);

        BundleUtils.checkJsFunctionsBundled(CarriedJs.class,
                () -> List.of(JsCall.functionId("this.focus()", 0, false)));
        verify(logger, never()).error(anyString(), any(Object[].class));

        BundleUtils.checkJsFunctionsBundled(MissingJs.class,
                () -> List.of(JsCall.functionId("this.blur()", 0, false)));
        verify(logger).error(anyString(), eq(MissingJs.class.getName()),
                eq("MissingJs"), eq("MissingJs"));
    }
}
