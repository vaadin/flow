/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEV_DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.OVERRIDES;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.VAADIN_CORE_NPM_PACKAGE;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class TaskUpdatePackagesNpmTest {

    private static final String PLATFORM_DIALOG_VERSION = "2.5.2";
    private static final String USER_SPECIFIED_MIXIN_VERSION = "2.4.1";
    private static final String VAADIN_ELEMENT_MIXIN = "@vaadin/vaadin-element-mixin";
    private static final String VAADIN_DIALOG = "@vaadin/vaadin-dialog";
    private static final String VAADIN_OVERLAY = "@vaadin/vaadin-overlay";
    private static final String PLATFORM_ELEMENT_MIXIN_VERSION = "2.4.2";
    private static final String PLATFORM_OVERLAY_VERSION = "3.5.1";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File npmFolder;

    private ClassFinder finder;

    private Logger logger = Mockito
            .spy(LoggerFactory.getLogger(NodeUpdater.class));
    private File generatedPath;

    private File versionJsonFile;

    private File packageJson;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();
        generatedPath = new File(npmFolder, "generated");
        generatedPath.mkdir();
        versionJsonFile = new File(npmFolder, "versions.json");
        finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());

        packageJson = new File(npmFolder, PACKAGE_JSON);
    }

    @Test
    public void npmIsInUse_platformVersionsJsonHasPinnedVersions_versionsArePinned()
            throws IOException {
        runTestWithoutPreexistingPackageJson();
    }

    private void runTestWithoutPreexistingPackageJson() throws IOException {
        createBasicVaadinVersionsJson();
        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        Assert.assertTrue("Updates we're not written", task.modified);
        verifyVersions(PLATFORM_DIALOG_VERSION, PLATFORM_ELEMENT_MIXIN_VERSION,
                PLATFORM_OVERLAY_VERSION);
        verifyVersionLockingWithNpmOverrides(true, true, true);
    }

    @Test
    public void npmIsInUse_userHasPinnedPlatformProvidedVersionInPackageJson_userPinnedVersionUsed()
            throws IOException {
        // run the basic test to produce an existing package.json
        runTestWithoutPreexistingPackageJson();

        // user pins a transitive dependency in package.json
        final JsonObject packageJsonJson = getOrCreatePackageJson();
        JsonObject dependencies = packageJsonJson.getObject(DEPENDENCIES);
        dependencies.put(VAADIN_ELEMENT_MIXIN, USER_SPECIFIED_MIXIN_VERSION);
        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJsonJson.toJson(), StandardCharsets.UTF_8);

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        Assert.assertTrue(
                "User's updates in package.json should have been noticed.",
                task.modified);

        // versions should be the same, except overridden mixin
        verifyVersions(PLATFORM_DIALOG_VERSION, USER_SPECIFIED_MIXIN_VERSION,
                PLATFORM_OVERLAY_VERSION);
    }

    @Test
    public void npmIsInUse_applicationHasPinnedPlatformProvidedVersionInAddon_applicationPinnedVersionIsUsed()
            throws IOException {
        // run the basic test to produce an existing package.json
        runTestWithoutPreexistingPackageJson();

        // user adds an application/add-on specific version,
        // in practice excludes the vaadin brought in version
        final Map<String, String> applicationDependencies = createApplicationDependencies();
        applicationDependencies.put(VAADIN_ELEMENT_MIXIN,
                USER_SPECIFIED_MIXIN_VERSION);
        final TaskUpdatePackages task = createTask(applicationDependencies);
        task.execute();
        Assert.assertTrue(
                "User's added application dependency updates should have been noticed",
                task.modified);

        // versions should be the same, except overridden mixin
        verifyVersions(PLATFORM_DIALOG_VERSION, USER_SPECIFIED_MIXIN_VERSION,
                PLATFORM_OVERLAY_VERSION);
    }

    @Test
    public void npmIsInUse_platformVersionIsBumped_versionsAreUpdated()
            throws IOException {
        // run the basic test to produce an existing package.json
        runTestWithoutPreexistingPackageJson();
        // write new versions json and scanned deps
        final String newVersion = "20.0.0";
        createVaadinVersionsJson(newVersion, newVersion, newVersion);

        final Map<String, String> applicationDependencies = createApplicationDependencies();
        applicationDependencies.put(VAADIN_DIALOG, newVersion);
        final TaskUpdatePackages task = createTask(applicationDependencies);
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersions(newVersion, newVersion, newVersion);
        verifyVersionLockingWithNpmOverrides(true, true, true);
    }

    @Test
    public void npmIsInUse_noPlatformVersionJsonPresent_noFailure()
            throws IOException {
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(null);
        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());
        JsonObject dependencies = getOrCreatePackageJson()
                .getObject(DEPENDENCIES);
        Assert.assertEquals(PLATFORM_DIALOG_VERSION,
                dependencies.get(VAADIN_DIALOG).asString());
    }

    @Test
    public void npmIsInUse_platformVersionsJsonAdded_versionsPinned()
            throws IOException {
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(null);
        createTask(createApplicationDependencies()).execute();

        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());
        final String newVersion = "20.0.0";
        createVaadinVersionsJson(newVersion, newVersion, newVersion);

        final Map<String, String> applicationDependencies = createApplicationDependencies();
        applicationDependencies.put(VAADIN_DIALOG, newVersion);
        final TaskUpdatePackages task = createTask(applicationDependencies);
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersions(newVersion, newVersion, newVersion);
        verifyVersionLockingWithNpmOverrides(true, true, true);
    }

    @Test
    public void pnpmIsInUse_platformVersionsJsonAdded_dependenciesAdded()
            throws IOException {
        verifyPlatformDependenciesAreAdded(true);
    }

    @Test
    public void npmIsInUse_platformVersionsJsonAdded_dependenciesAdded()
            throws IOException {
        verifyPlatformDependenciesAreAdded(false);
    }

    @Test
    public void npmIsInUse_versionJsonHasBadVersion_noFailureNothingAdded()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, "{{{foobar}}");

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersions(PLATFORM_DIALOG_VERSION, PLATFORM_ELEMENT_MIXIN_VERSION,
                null);
        verifyVersionLockingWithNpmOverrides(true, true, false);
    }

    @Test
    public void npmIsInUse_executionAfterDependencyRemoved_overlayIsCleanedOfDependency()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION);

        final Map<String, String> applicationDependencies = createApplicationDependencies();
        applicationDependencies.put(VAADIN_ELEMENT_MIXIN,
                PLATFORM_ELEMENT_MIXIN_VERSION);
        applicationDependencies.put(VAADIN_OVERLAY, PLATFORM_OVERLAY_VERSION);
        TaskUpdatePackages task = createTask(applicationDependencies);
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersionLockingWithNpmOverrides(true, true, true);

        // Remove platform lock for vaadin-element-mixin
        final JsonObject versions = Json.parse(FileUtils
                .readFileToString(versionJsonFile, StandardCharsets.UTF_8));
        versions.getObject("core").remove("vaadin-element-mixin");
        FileUtils.writeStringToFile(versionJsonFile, versions.toJson(),
                StandardCharsets.UTF_8);

        // Remove VAADIN_ELEMENT_MIXIN from the application dependencies
        applicationDependencies.remove(VAADIN_ELEMENT_MIXIN);
        task = createTask(applicationDependencies);

        task.execute();

        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersionLockingWithNpmOverrides(true, false, true);
    }

    @Test
    public void npmIsInUse_dependencyMovedToDevDependencies_overrideNotRemoved()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION);

        final Map<String, String> applicationDependencies = createApplicationDependencies();
        applicationDependencies.put(VAADIN_ELEMENT_MIXIN,
                PLATFORM_ELEMENT_MIXIN_VERSION);
        applicationDependencies.put(VAADIN_OVERLAY, PLATFORM_OVERLAY_VERSION);
        TaskUpdatePackages task = createTask(applicationDependencies);
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersionLockingWithNpmOverrides(true, true, true);

        // Remove platform lock for vaadin-element-mixin
        final JsonObject versions = Json.parse(FileUtils
                .readFileToString(versionJsonFile, StandardCharsets.UTF_8));
        versions.getObject("core").remove("vaadin-element-mixin");
        FileUtils.writeStringToFile(versionJsonFile, versions.toJson(),
                StandardCharsets.UTF_8);

        // Move element-mixin to devDependencies
        JsonObject packageJson = getOrCreatePackageJson();
        packageJson.getObject(DEV_DEPENDENCIES).put(VAADIN_ELEMENT_MIXIN,
                PLATFORM_ELEMENT_MIXIN_VERSION);
        FileUtils.writeStringToFile(this.packageJson, packageJson.toJson(),
                StandardCharsets.UTF_8);

        // Remove VAADIN_ELEMENT_MIXIN from the application dependencies
        applicationDependencies.remove(VAADIN_ELEMENT_MIXIN);
        task = createTask(applicationDependencies);

        task.execute();

        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersionLockingWithNpmOverrides(true, true, true);
    }

    @Test
    public void npmIsInUse_versionsJsonHasSnapshotVersions_notAddedToPackageJson()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, "20.0-SNAPSHOT");

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersions(PLATFORM_DIALOG_VERSION, PLATFORM_ELEMENT_MIXIN_VERSION,
                null);
        verifyVersionLockingWithNpmOverrides(true, true, false);
    }

    @Test
    public void npmIsInUse_packageJsonHasNonNumericVersion_versionNotOverridden()
            throws IOException {
        final JsonObject packageJson = getOrCreatePackageJson();
        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        dependencies.put(VAADIN_ELEMENT_MIXIN, "file:../foobar");
        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJson.toJson(), StandardCharsets.UTF_8);

        createBasicVaadinVersionsJson();

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();

        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersions(PLATFORM_DIALOG_VERSION, "file:../foobar",
                PLATFORM_OVERLAY_VERSION);
        verifyVersionLockingWithNpmOverrides(true, false, true);
    }

    @Test
    public void npmIsInUse_packageJsonVersionIsUpdated_vaadinSectionIsNotChanged()
            throws IOException {
        final JsonObject packageJson = getOrCreatePackageJson();
        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        dependencies.put(VAADIN_ELEMENT_MIXIN, "1.2.3");
        JsonObject vaadinSection = Json.createObject();
        JsonObject vaadinDependencies = Json.createObject();
        packageJson.put(VAADIN_DEP_KEY, vaadinSection);
        vaadinSection.put(DEPENDENCIES, vaadinDependencies);
        vaadinDependencies.put(VAADIN_ELEMENT_MIXIN,
                PLATFORM_ELEMENT_MIXIN_VERSION);
        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJson.toJson(), StandardCharsets.UTF_8);

        createBasicVaadinVersionsJson();

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();

        JsonObject newVaadinDeps = getOrCreatePackageJson()
                .getObject(VAADIN_DEP_KEY).getObject(DEPENDENCIES);

        Assert.assertEquals(PLATFORM_ELEMENT_MIXIN_VERSION,
                newVaadinDeps.getString(VAADIN_ELEMENT_MIXIN));
    }

    // #11025
    @Test
    public void npmIsInUse_versionsJsonHasVaadinCoreVersionPinned_vaadinCoreVersionIgnored()
            throws IOException {
        final String expectedElementMixinVersion = "21.0.0-alpha2";
        String versionJsonString = //@formatter:off
                "{ \"core\": {" + "\"vaadin-element-mixin\": {\n"
                        + "    \"jsVersion\": \"" + expectedElementMixinVersion
                        + "\",\n" + "    \"npmName\": \"" + VAADIN_ELEMENT_MIXIN
                        + "\"\n" + "},\n" + "\"vaadin-core\": {\n"
                        + "    \"jsVersion\": \"21.0.0.alpha1\",\n"
                        // broken for npm
                        + "    \"npmName\": \"" + VAADIN_CORE_NPM_PACKAGE
                        + "\"\n" + "},\n" + "}}},\n";//@formatter:on
        FileUtils.write(versionJsonFile, versionJsonString,
                StandardCharsets.UTF_8);

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersions(PLATFORM_DIALOG_VERSION, expectedElementMixinVersion,
                null);
        verifyVersionLockingWithNpmOverrides(true, true, false);
        final JsonObject packageJson = getOrCreatePackageJson();
        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);

        Assert.assertFalse(
                VAADIN_CORE_NPM_PACKAGE
                        + " version should not be written to package.json",
                dependencies.hasKey(VAADIN_CORE_NPM_PACKAGE));
        final JsonObject vaadinDependencies = packageJson
                .getObject(VAADIN_DEP_KEY).getObject(DEPENDENCIES);
        Assert.assertFalse(VAADIN_CORE_NPM_PACKAGE
                + " version should not be written to vaadin dependencies in package.json",
                vaadinDependencies.hasKey(VAADIN_CORE_NPM_PACKAGE));
    }

    @Test
    public void passUnorderedApplicationDependenciesAndReadUnorderedPackageJson_resultingPackageJsonIsOrdered()
            throws IOException {
        createBasicVaadinVersionsJson();

        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("foo", "bar");
        // "bar" is lexicographically before the "foo" but in the linked hash
        // map it's set after
        map.put("baz", "foobar");

        JsonObject packageJson = getOrCreatePackageJson();
        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);

        packageJson.remove(DEPENDENCIES);

        packageJson.put("name", "a");
        packageJson.put("license", "b");
        packageJson.put("version", "c");

        LinkedHashSet<String> mainKeys = new LinkedHashSet<>(
                Arrays.asList(packageJson.keys()));

        packageJson.put(DEPENDENCIES, dependencies);

        // Json object preserve the order of keys
        dependencies.put("foo-pack", "bar");
        dependencies.put("baz-pack", "foobar");
        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJson.toJson(), StandardCharsets.UTF_8);

        TaskUpdatePackages task = createTask(map);

        task.execute();

        // now read the package json file
        packageJson = getOrCreatePackageJson();

        List<String> list = Arrays.asList(packageJson.keys());
        int indexOfOverrides = list.indexOf(OVERRIDES);
        if (indexOfOverrides == -1) {
            // the "vaadin" key is the last one if no overrides
            Assert.assertEquals(list.size() - 1, list.indexOf(VAADIN_DEP_KEY));
        } else {
            // the "vaadin" key is the second to the last one with overrides
            Assert.assertEquals(list.size() - 2, list.indexOf(VAADIN_DEP_KEY));
        }

        List<String> keysBeforeDeps = new ArrayList<>();

        for (String key : packageJson.keys()) {
            if (key.equals(DEV_DEPENDENCIES) || key.equals(DEPENDENCIES)) {
                break;
            }
            if (mainKeys.contains(key)) {
                keysBeforeDeps.add(key);
            }
        }

        // the order of the main keys is the same
        Assert.assertArrayEquals(mainKeys.toArray(), keysBeforeDeps.toArray());

        checkOrder(DEPENDENCIES, packageJson.getObject(DEPENDENCIES));
        checkOrder(DEV_DEPENDENCIES, packageJson.getObject(DEV_DEPENDENCIES));
        checkOrder(VAADIN_DEP_KEY, packageJson.getObject(VAADIN_DEP_KEY));
    }

    private void checkOrder(String path, JsonObject object) {
        String[] keys = object.keys();
        if (path.isEmpty()) {
            Assert.assertTrue("Keys in the package Json are not sorted",
                    isSorted(keys));
        } else {
            Assert.assertTrue(
                    "Keys for the object " + path
                            + " in the package Json are not sorted",
                    isSorted(keys));
        }
        for (String key : keys) {
            JsonValue value = object.get(key);
            if (value instanceof JsonObject) {
                checkOrder(path + "/" + key, (JsonObject) value);
            }
        }
    }

    private boolean isSorted(String[] array) {
        if (array.length < 2) {
            return true;
        }
        for (int i = 0; i < array.length - 1; i++) {
            if (array[i].compareTo(array[i + 1]) > 0) {
                return false;
            }
        }
        return true;
    }

    // #11888
    @Test
    public void npmIsInUse_versionsJsonContainsSameVersions_nothingIsModified()
            throws IOException {
        String versionJsonString = //@formatter:off
                "{ \"core\": {" + "\"vaadin-element-mixin\": {\n"
                        + "    \"jsVersion\": \"" + PLATFORM_DIALOG_VERSION
                        + "\",\n" + "    \"npmName\": \"" + VAADIN_DIALOG
                        + "\"\n" + "},\n" + "}}},\n";//@formatter:on
        FileUtils.write(versionJsonFile, versionJsonString,
                StandardCharsets.UTF_8);

        TaskUpdatePackages task = createTask(createApplicationDependencies());
        task.execute();
        Assert.assertTrue(
                "Creation of package.json should be marked with modified",
                task.modified);

        // Rewriting with the same packages should not mark as modified
        task = createTask(createApplicationDependencies());
        task.execute();
        Assert.assertFalse("PackageJson modified without changes.",
                task.modified);
    }

    @Test
    public void nonNumericVersionsNotPinned() throws IOException {
        final JsonObject packageJson = getOrCreatePackageJson();
        createBasicVaadinVersionsJson();
        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        dependencies.put("localdep", "./localdeps/localdep");
        File file = new File(npmFolder, PACKAGE_JSON);
        FileUtils.writeStringToFile(file, packageJson.toJson(),
                StandardCharsets.UTF_8);

        Assert.assertFalse(packageJson.hasKey("overrides")
                && packageJson.getObject("overrides").hasKey("localdep"));

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();

        final JsonObject newPackageJson = getOrCreatePackageJson();

        Assert.assertFalse(newPackageJson.hasKey("overrides")
                && newPackageJson.getObject("overrides").hasKey("localdep"));
    }

    @Test
    public void platformVersion_returnsExpectedVersion() throws IOException {
        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());

        //@formatter:off
        String versionJsonString = "{"
                        + "  \"platform\": \"21.0.0\"\n"
                        + "}\n";
        //@formatter:on
        FileUtils.write(versionJsonFile, versionJsonString,
                StandardCharsets.UTF_8);

        Optional<String> vaadinVersion = task.getVaadinVersion(finder);

        Assert.assertTrue("versions.json should have had the platform field",
                vaadinVersion.isPresent());
        Assert.assertEquals("Received faulty version", "21.0.0",
                vaadinVersion.get());

        //@formatter:off
        versionJsonString = "{"
                + "}\n";
        //@formatter:on
        FileUtils.write(versionJsonFile, versionJsonString,
                StandardCharsets.UTF_8);
        vaadinVersion = task.getVaadinVersion(finder);

        Assert.assertFalse("versions.json should not contain platform version",
                vaadinVersion.isPresent());
    }

    @Test
    public void noVersionsJson_getVersionsDoesntThrow() {
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(null);
        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());

        Optional<String> vaadinVersion = task.getVaadinVersion(finder);

        Assert.assertFalse("versions.json should not contain platform version",
                vaadinVersion.isPresent());
    }

    @Test
    public void oldVersionsJson_shouldDowngrade() throws IOException {
        // run the basic test to produce an existing package.json
        runTestWithoutPreexistingPackageJson();
        // write new versions json and scanned deps
        final String oldPlatformVersion = "1.0.0";
        createVaadinVersionsJson(oldPlatformVersion, oldPlatformVersion,
                oldPlatformVersion);

        final Map<String, String> applicationDependencies = createApplicationDependencies();
        final String appDependencyVersion = "1.5.0";
        applicationDependencies.put(VAADIN_DIALOG, appDependencyVersion);
        final TaskUpdatePackages task = createTask(applicationDependencies);
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersions(appDependencyVersion, oldPlatformVersion,
                oldPlatformVersion);
        verifyVersionLockingWithNpmOverrides(true, true, true);
    }

    private void createBasicVaadinVersionsJson() {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION);
    }

    private void createVaadinVersionsJson(String dialogVersion,
            String elementMixinVersion, String overlayVersion) {
        // testing with exact versions json content instead of mocking parsing
        String versionJsonString = //@formatter:off
                "{ \"core\": {"
                        + "\"vaadin-dialog\": {\n"
                        + "   \"component\": true,\n"
                        + "   \"javaVersion\": \"{{version}}\",\n"
                        + "    \"jsVersion\": \"" + dialogVersion + "\",\n"
                        + "    \"npmName\": \"" + VAADIN_DIALOG + "\"\n"
                        + "},\n"
                        + "\"vaadin-element-mixin\": {\n"
                        + "    \"jsVersion\": \"" + elementMixinVersion
                        + "\",\n" + "    \"npmName\": \"" + VAADIN_ELEMENT_MIXIN
                        + "\"\n" + "},\n"
                        + "\"vaadin-overlay\": {\n"
                        + "    \"jsVersion\": \"" + overlayVersion + "\",\n"
                        + "    \"npmName\": \"" + VAADIN_OVERLAY + "\",\n"
                        + "    \"releasenotes\": true\n"
                        + "}}},\n";//@formatter:on
        try {
            FileUtils.write(versionJsonFile, versionJsonString,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> createApplicationDependencies() {
        Map<String, String> applicationScannedDependencies = new HashMap<>();
        applicationScannedDependencies.put(VAADIN_DIALOG,
                PLATFORM_DIALOG_VERSION);
        return applicationScannedDependencies;
    }

    private TaskUpdatePackages createTask(
            Map<String, String> applicationDependencies) {
        return createTask(applicationDependencies, false);
    }

    private TaskUpdatePackages createTask(
            Map<String, String> applicationDependencies, boolean enablePnpm) {
        final FrontendDependencies frontendDependenciesScanner = Mockito
                .mock(FrontendDependencies.class);
        Mockito.when(frontendDependenciesScanner.getPackages())
                .thenReturn(applicationDependencies);
        Options options = new Options(Mockito.mock(Lookup.class), npmFolder)
                .withBuildDirectory(TARGET).withEnablePnpm(enablePnpm)
                .withBundleBuild(true);

        return new TaskUpdatePackages(finder, frontendDependenciesScanner,
                options) {
        };
    }

    private JsonObject getOrCreatePackageJson() throws IOException {
        if (packageJson.exists()) {
            return Json.parse(FileUtils.readFileToString(packageJson,
                    StandardCharsets.UTF_8));
        } else {
            final JsonObject packageJsonJson = Json.createObject();
            packageJsonJson.put(DEPENDENCIES, Json.createObject());
            FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                    packageJsonJson.toJson(), StandardCharsets.UTF_8);
            return packageJsonJson;
        }
    }

    private void verifyVersions(String expectedDialogVersion,
            String expectedElementMixinVersion, String expectedOverlayVersion)
            throws IOException {
        JsonObject dependencies = getOrCreatePackageJson()
                .getObject(DEPENDENCIES);
        if (expectedDialogVersion == null) {
            Assert.assertNull("Dependency added when it should not have been",
                    dependencies.get(VAADIN_DIALOG));
        } else {
            Assert.assertEquals(expectedDialogVersion,
                    dependencies.getString(VAADIN_DIALOG));
        }
        if (expectedElementMixinVersion == null) {
            Assert.assertNull("Dependency added when it should not have been",
                    dependencies.get(VAADIN_ELEMENT_MIXIN));
        } else {
            Assert.assertEquals(expectedElementMixinVersion,
                    dependencies.getString(VAADIN_ELEMENT_MIXIN));
        }
        if (expectedOverlayVersion == null) {
            Assert.assertNull("Dependency added when it should not have been",
                    dependencies.get(VAADIN_OVERLAY));
        } else {
            Assert.assertEquals(expectedOverlayVersion,
                    dependencies.getString(VAADIN_OVERLAY));
        }
    }

    private void verifyVersionLockingWithNpmOverrides(boolean hasDialogLocking,
            boolean hasElementMixinLocking, boolean hasOverlayLocking)
            throws IOException {
        JsonObject overrides = getOrCreatePackageJson().getObject(OVERRIDES);

        if (hasDialogLocking) {
            Assert.assertTrue("Dialog override was not present",
                    overrides.hasKey(VAADIN_DIALOG));
            Assert.assertEquals("$" + VAADIN_DIALOG,
                    overrides.getString(VAADIN_DIALOG));
        } else {
            Assert.assertNull("vaadin-dialog dependency should not be present",
                    overrides.get(VAADIN_DIALOG));
        }
        if (hasElementMixinLocking) {
            Assert.assertTrue("Element-Mixin override was not present",
                    overrides.hasKey(VAADIN_ELEMENT_MIXIN));
            Assert.assertEquals("$" + VAADIN_ELEMENT_MIXIN,
                    overrides.getString(VAADIN_ELEMENT_MIXIN));
        } else {
            Assert.assertNull(
                    "vaadin-element-mixin dependency should not be present",
                    overrides.get(VAADIN_ELEMENT_MIXIN));
        }
        if (hasOverlayLocking) {
            Assert.assertTrue("Overlay override was not present",
                    overrides.hasKey(VAADIN_OVERLAY));
            Assert.assertEquals("$" + VAADIN_OVERLAY,
                    overrides.getString(VAADIN_OVERLAY));
        } else {
            Assert.assertNull("vaadin-overlay dependency should not be present",
                    overrides.get(VAADIN_OVERLAY));
        }
    }

    private void verifyPlatformDependenciesAreAdded(boolean enablePnpm)
            throws IOException {
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());
        final String newVersion = "20.0.0";
        createVaadinVersionsJson(newVersion, newVersion, newVersion);

        final Map<String, String> applicationDependencies = Collections
                .emptyMap();
        final TaskUpdatePackages task = createTask(applicationDependencies,
                enablePnpm);

        verifyVersions(null, null, null);

        task.execute();

        verifyVersions(newVersion, newVersion, newVersion);
    }

}
