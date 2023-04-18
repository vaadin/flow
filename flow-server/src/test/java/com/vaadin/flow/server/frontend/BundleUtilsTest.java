package com.vaadin.flow.server.frontend;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class BundleUtilsTest {

    @Mock
    ClassLoader classLoader;
    private List<AutoCloseable> closeOnTearDown = new ArrayList<>();

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

        Assert.assertTrue(bundleImports.contains("Frontend/generated/jar-resources/my/addon.js"));
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

        Assert.assertTrue(bundleImports.contains("@foo/bar/theme/lumo/file.js"));
        Assert.assertTrue(bundleImports.contains("@foo/bar/src/file.js"));
    }

    private void mockStatsJson(String... imports) {
        JsonObject statsJson = Json.createObject();
        JsonArray importsArray = Json.createArray();
        for (int i = 0; i < imports.length; i++) {
            importsArray.set(i, imports[i]);
        }

        statsJson.put("bundleImports", importsArray);

        mockStatsJsonLoading(statsJson);
    }

    private void mockStatsJsonLoading(JsonObject statsJson) {
        MockedStatic<BundleUtils> mock = Mockito.mockStatic(BundleUtils.class);
        mock.when(() -> BundleUtils.loadStatsJson()).thenReturn(statsJson);
        mock.when(() -> BundleUtils.loadBundleImports()).thenCallRealMethod();
        closeOnTearDown.add(mock);

    }

}
