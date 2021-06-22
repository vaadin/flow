/*
 * Copyright 2000-2021 Vaadin Ltd.
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.jcip.annotations.NotThreadSafe;
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

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.VAADIN_CORE_NPM_PACKAGE;

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

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();
        generatedPath = new File(npmFolder, "generated");
        generatedPath.mkdir();
        versionJsonFile = new File(npmFolder, "versions.json");
        finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());
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
    }

    @Test
    public void npmIsInUse_noPlatformVersionJsonPresent_noFailure()
            throws IOException {
        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(null);
        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());
        JsonObject dependencies = getOrCreatePackageJson()
                .getObject(DEPENDENCIES);
        Assert.assertEquals(PLATFORM_DIALOG_VERSION,
                dependencies.get(VAADIN_DIALOG).asString());
    }

    @Test
    public void npmIsInUse_platformVersionsJsonAdded_versionsPinned()
            throws IOException {
        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(null);
        createTask(createApplicationDependencies()).execute();

        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());
        final String newVersion = "20.0.0";
        createVaadinVersionsJson(newVersion, newVersion, newVersion);

        final Map<String, String> applicationDependencies = createApplicationDependencies();
        applicationDependencies.put(VAADIN_DIALOG, newVersion);
        final TaskUpdatePackages task = createTask(applicationDependencies);
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersions(newVersion, newVersion, newVersion);
    }

    @Test
    public void npmIsInUse_switchToPnpm_pinnedVersionsDeleted()
            throws IOException {
        runTestWithoutPreexistingPackageJson();
        final TaskUpdatePackages task = createTask(
                createApplicationDependencies(), true);
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        // only the application dependency should stay
        verifyVersions(PLATFORM_DIALOG_VERSION, null, null);
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
    }

    @Test
    public void npmIsInUse_packageJsonHasBadVersion_pinnedVersionUsed()
            throws IOException {
        final JsonObject packageJson = getOrCreatePackageJson();
        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        dependencies.put(VAADIN_ELEMENT_MIXIN, "asdfasagqae4rat");
        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJson.toJson(), StandardCharsets.UTF_8);

        createBasicVaadinVersionsJson();

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();

        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersions(PLATFORM_DIALOG_VERSION, PLATFORM_ELEMENT_MIXIN_VERSION,
                PLATFORM_OVERLAY_VERSION);
    }

    // #11025
    @Test
    public void npmIsInUse_versionsJsonHasVaadinCoreVersionPinned_vaadinCoreVersionIgnored()
            throws IOException {
        final String expectedElementMixinVersion = "21.0.0-alpha2";
        String versionJsonString = //@formatter:off
                "{ \"core\": {"
                        + "\"vaadin-element-mixin\": {\n"
                        + "    \"jsVersion\": \""+expectedElementMixinVersion+"\",\n"
                        + "    \"npmName\": \""+VAADIN_ELEMENT_MIXIN+"\"\n"
                        + "},\n"
                        + "\"vaadin-core\": {\n"
                        + "    \"jsVersion\": \"21.0.0.alpha1\",\n" // broken for npm
                        + "    \"npmName\": \""+VAADIN_CORE_NPM_PACKAGE+"\"\n"
                        + "},\n"
                        +"}}},\n";//@formatter:on
        FileUtils.write(versionJsonFile, versionJsonString,
                StandardCharsets.UTF_8);

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        Assert.assertTrue("Updates not picked", task.modified);

        verifyVersions(PLATFORM_DIALOG_VERSION, expectedElementMixinVersion,
                null);
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
        // Json object preserve the order of keys
        dependencies.put("foo-pack", "bar");
        dependencies.put("baz-pack", "foobar");
        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJson.toJson(), StandardCharsets.UTF_8);

        TaskUpdatePackages task = createTask(map);

        task.execute();

        // now read the package json file
        packageJson = getOrCreatePackageJson();

        checkOrder("", packageJson);
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
                        + "    \"jsVersion\": \""+dialogVersion+"\",\n"
                        + "    \"npmName\": \""+VAADIN_DIALOG+"\"\n"
                        + "},\n"
                        + "\"vaadin-element-mixin\": {\n"
                        + "    \"jsVersion\": \""+elementMixinVersion+"\",\n"
                        + "    \"npmName\": \""+VAADIN_ELEMENT_MIXIN+"\"\n"
                        + "},\n"
                        + "\"vaadin-overlay\": {\n"
                        + "    \"jsVersion\": \""+overlayVersion+"\",\n"
                        + "    \"npmName\": \""+VAADIN_OVERLAY+"\",\n"
                        + "    \"releasenotes\": true\n"
                        +"}}},\n";//@formatter:on
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
        return new TaskUpdatePackages(finder, frontendDependenciesScanner,
                npmFolder, generatedPath, null, false, enablePnpm, TARGET) {
        };
    }

    private JsonObject getOrCreatePackageJson() throws IOException {
        File packageJson = new File(npmFolder, PACKAGE_JSON);
        if (packageJson.exists())
            return Json.parse(FileUtils.readFileToString(packageJson,
                    StandardCharsets.UTF_8));
        else {
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

}
