package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
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

    @Test
    public void packageLockExists_nothingIsCopied() throws IOException {
        Options options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withBuildDirectory("target");

        File packageLockFile = temporaryFolder
                .newFile(Constants.PACKAGE_LOCK_JSON);
        File devBundleFolder = new File(options.getNpmFolder(),
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
        final Lookup lookup = Mockito.mock(Lookup.class);
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(lookup.lookup(ClassFinder.class)).thenReturn(finder);

        Options options = new Options(lookup, temporaryFolder.getRoot())
                .withBuildDirectory("target");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        Mockito.when(finder
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());

        File devBundleFolder = new File(options.getNpmFolder(),
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
            throws IOException {
        final Lookup lookup = Mockito.mock(Lookup.class);
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(lookup.lookup(ClassFinder.class)).thenReturn(finder);

        Options options = new Options(lookup, temporaryFolder.getRoot())
                .withBuildDirectory("target");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        Mockito.when(finder
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());

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
        final Lookup lookup = Mockito.mock(Lookup.class);
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(lookup.lookup(ClassFinder.class)).thenReturn(finder);

        Options options = new Options(lookup, temporaryFolder.getRoot())
                .withBuildDirectory("target").withEnablePnpm(true);

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        Mockito.when(finder
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_YAML))
                .thenReturn(jarPackageLock.toURI().toURL());

        File devBundleFolder = new File(options.getNpmFolder(),
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
        File devBundleFolder = new File(options.getNpmFolder(),
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
