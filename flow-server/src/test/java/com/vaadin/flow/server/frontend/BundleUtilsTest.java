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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.DEV_BUNDLE_JAR_PATH;

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
    public void packageLockExists_nothingIsCopied() throws IOException {
        Options options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withBuildDirectory("target");

        File packageLockFile = temporaryFolder
                .newFile(Constants.PACKAGE_LOCK_JSON);
        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        devBundleFolder.mkdirs();
        File devPackageLockJson = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_JSON);

        final String existingLockFile = "{ \"existing\" }";
        FileUtils.write(packageLockFile, existingLockFile);

        FileUtils.write(devPackageLockJson, "{ \"bundleFile\"}");

        BundleUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils
                .readFileToString(packageLockFile, StandardCharsets.UTF_8);

        Assert.assertEquals("Existing file should not be overwritten",
                existingLockFile, packageLockContents);
    }

    @Test
    public void noPackageLockExists_devBundleLockIsCopied_notJarLock()
            throws IOException {
        Options options = new MockOptions(temporaryFolder.getRoot())
                .withBuildDirectory("target");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());

        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        devBundleFolder.mkdirs();
        File devPackageLockJson = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_JSON);

        final String packageLockContent = "{ \"bundleFile\"}";
        FileUtils.write(devPackageLockJson, packageLockContent);

        BundleUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        Assert.assertEquals("dev-bundle file should be used",
                packageLockContent, packageLockContents);
    }

    @Test
    public void noPackageLockExists_jarDevBundleLockIsCopied()
            throws IOException, ClassNotFoundException {
        Options options = new MockOptions(temporaryFolder.getRoot())
                .withBuildDirectory("target");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        File jarHybridPackageLock = new File(options.getNpmFolder(),
                "hybrid-temp.json");
        final String jarHybridPackageLockContent = "{ \"hybridJarData\"}";
        FileUtils.write(jarHybridPackageLock, jarHybridPackageLockContent);

        Mockito.doThrow(new ClassNotFoundException("No Hilla"))
                .when(options.getClassFinder())
                .loadClass("com.vaadin.hilla.EndpointController");
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());
        Mockito.when(options.getClassFinder().getResource(
                DEV_BUNDLE_JAR_PATH + "hybrid-" + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarHybridPackageLock.toURI().toURL());

        BundleUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        Assert.assertEquals("File should be gotten from jar on classpath",
                jarPackageLockContent, packageLockContents);
    }

    @Test
    public void noPackageLockExists_hillaUsed_jarHybridDevBundleLockIsCopied()
            throws IOException, ClassNotFoundException {
        Options options = new MockOptions(temporaryFolder.getRoot())
                .withBuildDirectory("target");

        Path dummyView = options.getFrontendDirectory().toPath()
                .resolve(Path.of("views", "dummy.tsx"));
        Files.createDirectories(dummyView.getParent());
        Files.writeString(dummyView, "const x = 1;");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        File jarHybridPackageLock = new File(options.getNpmFolder(),
                "hybrid-temp.json");
        final String jarHybridPackageLockContent = "{ \"hybridJarData\"}";
        FileUtils.write(jarHybridPackageLock, jarHybridPackageLockContent);

        Mockito.when(options.getClassFinder()
                .loadClass("com.vaadin.hilla.EndpointController"))
                .thenReturn(Object.class);
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());
        Mockito.when(options.getClassFinder().getResource(
                DEV_BUNDLE_JAR_PATH + "hybrid-" + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarHybridPackageLock.toURI().toURL());

        BundleUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        Assert.assertEquals("File should be gotten from jar on classpath",
                jarHybridPackageLockContent, packageLockContents);
    }

    @Test
    public void noPackageLockExists_hillaUsed_hybridPackageLockNotPresentInJar_jarDevBundleIsCopied()
            throws IOException, ClassNotFoundException {
        Options options = new MockOptions(temporaryFolder.getRoot())
                .withBuildDirectory("target");

        Path dummyView = options.getFrontendDirectory().toPath()
                .resolve(Path.of("views", "dummy.tsx"));
        Files.createDirectories(dummyView.getParent());
        Files.writeString(dummyView, "const x = 1;");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        Mockito.when(options.getClassFinder()
                .loadClass("com.vaadin.hilla.EndpointController"))
                .thenReturn(Object.class);
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());
        Mockito.when(options.getClassFinder().getResource(
                DEV_BUNDLE_JAR_PATH + "hybrid-" + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(null);

        BundleUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        Assert.assertEquals("File should be gotten from jar on classpath",
                jarPackageLockContent, packageLockContents);
    }

    @Test
    public void pnpm_noPackageLockExists_devBundleLockYamlIsCopied_notJarLockOrJson()
            throws IOException {
        Options options = new MockOptions(temporaryFolder.getRoot())
                .withBuildDirectory("target").withEnablePnpm(true);

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_YAML))
                .thenReturn(jarPackageLock.toURI().toURL());

        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        devBundleFolder.mkdirs();
        File devPackageLockJson = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_JSON);
        File devPackageLock = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_YAML);

        final String packageLockContent = "{ \"bundleFile\"}";
        FileUtils.write(devPackageLock, packageLockContent);
        FileUtils.write(devPackageLockJson, "{ \"json\"}");

        BundleUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_YAML),
                StandardCharsets.UTF_8);

        Assert.assertEquals("dev-bundle file should be used",
                packageLockContent, packageLockContents);
    }

    @Test
    public void pnpm_packageLockExists_nothingIsCopied() throws IOException {
        Options options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withBuildDirectory("target")
                .withEnablePnpm(true);

        File packageLockFile = temporaryFolder
                .newFile(Constants.PACKAGE_LOCK_YAML);
        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        devBundleFolder.mkdirs();
        File devPackageLockJson = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_YAML);

        final String existingLockFile = "{ \"existing\" }";
        FileUtils.write(packageLockFile, existingLockFile);

        FileUtils.write(devPackageLockJson, "{ \"bundleFile\"}");

        BundleUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils
                .readFileToString(packageLockFile, StandardCharsets.UTF_8);

        Assert.assertEquals("Existing file should not be overwritten",
                existingLockFile, packageLockContents);
    }
}
