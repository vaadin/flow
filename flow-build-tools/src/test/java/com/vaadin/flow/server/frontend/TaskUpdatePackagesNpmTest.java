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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEV_DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.OVERRIDES;
import static com.vaadin.flow.server.frontend.NodeUpdater.PNPM;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.VAADIN_CORE_NPM_PACKAGE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NotThreadSafe
class TaskUpdatePackagesNpmTest {

    private static final String PLATFORM_DIALOG_VERSION = "2.5.2";
    private static final String USER_SPECIFIED_MIXIN_VERSION = "2.4.1";
    private static final String VAADIN_ELEMENT_MIXIN = "@vaadin/vaadin-element-mixin";
    private static final String VAADIN_DIALOG = "@vaadin/vaadin-dialog";
    private static final String VAADIN_OVERLAY = "@vaadin/vaadin-overlay";

    private static final String REACT_COMPONENTS = "@vaadin/react-components";

    private static final String PLATFORM_ELEMENT_MIXIN_VERSION = "2.4.2";
    private static final String PLATFORM_OVERLAY_VERSION = "3.5.1";

    @TempDir
    File temporaryFolder;

    private File npmFolder;

    private ClassFinder finder;

    private Logger logger = Mockito
            .spy(LoggerFactory.getLogger(NodeUpdater.class));
    private File generatedPath;

    private File versionJsonFile;

    private File packageJson;

    @BeforeEach
    void setUp() throws IOException {
        npmFolder = Files.createTempDirectory(temporaryFolder.toPath(), "tmp")
                .toFile();
        generatedPath = new File(npmFolder, "generated");
        generatedPath.mkdir();
        versionJsonFile = new File(npmFolder, "versions.json");
        finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());

        packageJson = new File(npmFolder, PACKAGE_JSON);
    }

    @Test
    void npmIsInUse_platformVersionsJsonHasPinnedVersions_versionsArePinned()
            throws IOException {
        runTestWithoutPreexistingPackageJson();
    }

    private void runTestWithoutPreexistingPackageJson() throws IOException {
        createBasicVaadinVersionsJson();
        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        assertTrue(task.modified, "Updates we're not written");
        verifyVersions(PLATFORM_DIALOG_VERSION, PLATFORM_ELEMENT_MIXIN_VERSION,
                PLATFORM_OVERLAY_VERSION);
        verifyVersionLockingWithNpmOverrides(true, true, true);
    }

    @Test
    void npmIsInUse_userHasPinnedPlatformProvidedVersionInPackageJson_userPinnedVersionUsed()
            throws IOException {
        // run the basic test to produce an existing package.json
        runTestWithoutPreexistingPackageJson();

        // user pins a transitive dependency in package.json
        final ObjectNode packageJsonJson = getOrCreatePackageJson();
        ObjectNode dependencies = (ObjectNode) packageJsonJson
                .get(DEPENDENCIES);
        dependencies.put(VAADIN_ELEMENT_MIXIN, USER_SPECIFIED_MIXIN_VERSION);
        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJsonJson.toPrettyString(), StandardCharsets.UTF_8);

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        assertTrue(task.modified,
                "User's updates in package.json should have been noticed.");

        // versions should be the same, except overridden mixin
        verifyVersions(PLATFORM_DIALOG_VERSION, USER_SPECIFIED_MIXIN_VERSION,
                PLATFORM_OVERLAY_VERSION);
    }

    @Test
    void npmIsInUse_applicationHasPinnedPlatformProvidedVersionInAddon_applicationPinnedVersionIsUsed()
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
        assertTrue(task.modified,
                "User's added application dependency updates should have been noticed");

        // versions should be the same, except overridden mixin
        verifyVersions(PLATFORM_DIALOG_VERSION, USER_SPECIFIED_MIXIN_VERSION,
                PLATFORM_OVERLAY_VERSION);
    }

    @Test
    void npmIsInUse_platformVersionIsBumped_versionsAreUpdated()
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
        assertTrue(task.modified, "Updates not picked");

        verifyVersions(newVersion, newVersion, newVersion);
        verifyVersionLockingWithNpmOverrides(true, true, true);
    }

    @Test
    void npmIsInUse_noPlatformVersionJsonPresent_noFailure()
            throws IOException {
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(null);
        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        assertTrue(task.modified, "Updates not picked");

        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());
        JsonNode dependencies = getOrCreatePackageJson().get(DEPENDENCIES);
        assertEquals(PLATFORM_DIALOG_VERSION,
                dependencies.get(VAADIN_DIALOG).asString());
    }

    @Test
    void npmIsInUse_platformVersionsJsonAdded_versionsPinned()
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
        assertTrue(task.modified, "Updates not picked");

        verifyVersions(newVersion, newVersion, newVersion);
        verifyVersionLockingWithNpmOverrides(true, true, true);
    }

    @Test
    void overridesContainPinnedVersion_platformVersionUpdatedToNewerWhileDependencyAdded_versionGetsReference()
            throws IOException {

        ObjectNode packageJson = getOrCreatePackageJson();
        packageJson.set(OVERRIDES, JacksonUtils.createObjectNode());
        ((ObjectNode) packageJson.get(OVERRIDES)).put("@vaadin/aura", "1.0");

        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJson.toPrettyString(), StandardCharsets.UTF_8);

        JsonNode overrides = getOrCreatePackageJson().get(OVERRIDES);
        assertEquals("1.0", overrides.get("@vaadin/aura").asString());

        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());
        String versionJsonString = """
                {
                  "core": {
                    "vaadin-aura": {
                      "jsVersion": "2.0",
                      "npmName": "@vaadin/aura"
                    }
                  }
                }
                """;
        try {
            FileUtils.write(versionJsonFile, versionJsonString,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final Map<String, String> applicationDependencies = Collections
                .singletonMap("@vaadin/aura", "2.0");
        final TaskUpdatePackages task = createTask(applicationDependencies);
        task.execute();
        assertTrue(task.modified, "Updates not picked");

        overrides = getOrCreatePackageJson().get(OVERRIDES);

        assertEquals("$@vaadin/aura", overrides.get("@vaadin/aura").asString());
    }

    @Test
    void pnpmIsInUse_platformVersionsJsonAdded_dependenciesAdded()
            throws IOException {
        verifyPlatformDependenciesAreAdded(true);
    }

    @Test
    void npmIsInUse_platformVersionsJsonAdded_dependenciesAdded()
            throws IOException {
        verifyPlatformDependenciesAreAdded(false);
    }

    @Test
    void npmIsInUse_versionJsonHasBadVersion_noFailureNothingAdded()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, "{{{foobar}}");

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        assertTrue(task.modified, "Updates not picked");

        verifyVersions(PLATFORM_DIALOG_VERSION, PLATFORM_ELEMENT_MIXIN_VERSION,
                null);
        verifyVersionLockingWithNpmOverrides(true, true, false);
    }

    @Test
    void npmIsInUse_executionAfterDependencyRemoved_overlayIsCleanedOfDependency()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION);

        final Map<String, String> applicationDependencies = createApplicationDependencies();
        applicationDependencies.put(VAADIN_ELEMENT_MIXIN,
                PLATFORM_ELEMENT_MIXIN_VERSION);
        applicationDependencies.put(VAADIN_OVERLAY, PLATFORM_OVERLAY_VERSION);
        TaskUpdatePackages task = createTask(applicationDependencies);
        task.execute();
        assertTrue(task.modified, "Updates not picked");

        verifyVersionLockingWithNpmOverrides(true, true, true);

        // Remove platform lock for vaadin-element-mixin
        final ObjectNode versions = JacksonUtils.readTree(FileUtils
                .readFileToString(versionJsonFile, StandardCharsets.UTF_8));
        ((ObjectNode) versions.get("core")).remove("vaadin-element-mixin");
        FileUtils.writeStringToFile(versionJsonFile, versions.toString(),
                StandardCharsets.UTF_8);

        // Remove VAADIN_ELEMENT_MIXIN from the application dependencies
        applicationDependencies.remove(VAADIN_ELEMENT_MIXIN);
        task = createTask(applicationDependencies);

        task.execute();

        assertTrue(task.modified, "Updates not picked");

        verifyVersionLockingWithNpmOverrides(true, false, true);
    }

    @Test
    void npmIsInUse_dependencyMovedToDevDependencies_overrideNotRemoved()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION);

        final Map<String, String> applicationDependencies = createApplicationDependencies();
        applicationDependencies.put(VAADIN_ELEMENT_MIXIN,
                PLATFORM_ELEMENT_MIXIN_VERSION);
        applicationDependencies.put(VAADIN_OVERLAY, PLATFORM_OVERLAY_VERSION);
        TaskUpdatePackages task = createTask(applicationDependencies);
        task.execute();
        assertTrue(task.modified, "Updates not picked");

        verifyVersionLockingWithNpmOverrides(true, true, true);

        // Remove platform lock for vaadin-element-mixin
        final ObjectNode versions = JacksonUtils.readTree(FileUtils
                .readFileToString(versionJsonFile, StandardCharsets.UTF_8));
        ((ObjectNode) versions.get("core")).remove("vaadin-element-mixin");
        FileUtils.writeStringToFile(versionJsonFile, versions.toString(),
                StandardCharsets.UTF_8);

        // Move element-mixin to devDependencies
        ObjectNode packageJson = getOrCreatePackageJson();
        ((ObjectNode) packageJson.get(DEV_DEPENDENCIES))
                .put(VAADIN_ELEMENT_MIXIN, PLATFORM_ELEMENT_MIXIN_VERSION);
        // Remove VAADIN_ELEMENT_MIXIN override from Vaadin overrides
        JacksonUtils.removeNestedKey(packageJson,
                List.of(VAADIN_DEP_KEY, OVERRIDES, VAADIN_ELEMENT_MIXIN));
        // Save modified package.json
        FileUtils.writeStringToFile(this.packageJson,
                packageJson.toPrettyString(), StandardCharsets.UTF_8);

        // Remove VAADIN_ELEMENT_MIXIN from the application dependencies
        applicationDependencies.remove(VAADIN_ELEMENT_MIXIN);
        task = createTask(applicationDependencies);

        task.execute();

        assertTrue(task.modified, "Updates not picked");

        verifyVersionLockingWithNpmOverrides(true, true, true);
    }

    @Test
    void npmIsInUse_emptyVaadinOverrides_obsoleteOverride_overrideRemoved()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION);

        final Map<String, String> applicationDependencies = createApplicationDependencies();
        applicationDependencies.put(VAADIN_ELEMENT_MIXIN,
                PLATFORM_ELEMENT_MIXIN_VERSION);
        applicationDependencies.put(VAADIN_OVERLAY, PLATFORM_OVERLAY_VERSION);
        TaskUpdatePackages task = createTask(applicationDependencies);
        task.execute();

        // Remove platform lock for vaadin-element-mixin
        final ObjectNode versions = JacksonUtils.readTree(FileUtils
                .readFileToString(versionJsonFile, StandardCharsets.UTF_8));
        ((ObjectNode) versions.get("core")).remove("vaadin-element-mixin");
        FileUtils.writeStringToFile(versionJsonFile, versions.toString(),
                StandardCharsets.UTF_8);

        // Remove Vaadin overrides from package.json (simulate old style
        // overrides not tracked using Vaadin overrides section introduced in
        // PR #24008)
        ObjectNode packageJson = getOrCreatePackageJson();
        JacksonUtils.removeNestedKey(packageJson,
                List.of(VAADIN_DEP_KEY, OVERRIDES));
        FileUtils.writeStringToFile(this.packageJson,
                packageJson.toPrettyString(), StandardCharsets.UTF_8);

        // Remove VAADIN_ELEMENT_MIXIN from the application dependencies
        applicationDependencies.remove(VAADIN_ELEMENT_MIXIN);
        task = createTask(applicationDependencies);

        task.execute();

        assertTrue(task.modified, "Updates not picked");

        verifyVersionLockingWithNpmOverrides(true, false, true);
    }

    @Test
    void npmIsInUse_versionsJsonHasSnapshotVersions_notAddedToPackageJson()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, "20.0-SNAPSHOT");

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        assertTrue(task.modified, "Updates not picked");

        verifyVersions(PLATFORM_DIALOG_VERSION, PLATFORM_ELEMENT_MIXIN_VERSION,
                null);
        verifyVersionLockingWithNpmOverrides(true, true, false);
    }

    @Test
    void npmIsInUse_packageJsonHasNonNumericVersion_versionNotOverridden()
            throws IOException {
        final ObjectNode packageJson = getOrCreatePackageJson();
        ObjectNode dependencies = (ObjectNode) packageJson.get(DEPENDENCIES);
        dependencies.put(VAADIN_ELEMENT_MIXIN, "file:../foobar");
        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJson.toPrettyString(), StandardCharsets.UTF_8);

        createBasicVaadinVersionsJson();

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();

        assertTrue(task.modified, "Updates not picked");

        verifyVersions(PLATFORM_DIALOG_VERSION, "file:../foobar",
                PLATFORM_OVERLAY_VERSION);
        verifyVersionLockingWithNpmOverrides(true, false, true);
    }

    @Test
    void missingTypeInPackageJson_typeModuleIsAdded() throws IOException {
        ObjectNode packageJson = getOrCreatePackageJson();
        assertFalse(packageJson.has("type"), "No type should be available");

        createBasicVaadinVersionsJson();

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();

        // get latest package.json file
        packageJson = getOrCreatePackageJson();

        assertTrue(packageJson.has("type"), "Type should have been addded.");
        assertEquals("module", packageJson.get("type").asString(),
                "Type should be module");
    }

    @Test
    void faultyTypeInPackageJson_typeModuleIsAdded() throws IOException {
        ObjectNode packageJson = getOrCreatePackageJson();
        packageJson.put("type", "commonjs");

        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJson.toPrettyString(), StandardCharsets.UTF_8);

        createBasicVaadinVersionsJson();

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();

        // get latest package.json file
        packageJson = getOrCreatePackageJson();

        assertTrue(packageJson.has("type"),
                "Type should not have been removed");
        assertEquals("module", packageJson.get("type").asString(),
                "Type should have been updated to 'module'");
    }

    @Test
    void npmIsInUse_packageJsonVersionIsUpdated_vaadinSectionIsNotChanged()
            throws IOException {
        final ObjectNode packageJson = (ObjectNode) getOrCreatePackageJson();
        ObjectNode dependencies = (ObjectNode) packageJson.get(DEPENDENCIES);
        dependencies.put(VAADIN_ELEMENT_MIXIN, "1.2.3");
        ObjectNode vaadinSection = JacksonUtils.createObjectNode();
        ObjectNode vaadinDependencies = JacksonUtils.createObjectNode();
        packageJson.set(VAADIN_DEP_KEY, vaadinSection);
        vaadinSection.set(DEPENDENCIES, vaadinDependencies);
        vaadinDependencies.put(VAADIN_ELEMENT_MIXIN,
                PLATFORM_ELEMENT_MIXIN_VERSION);
        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJson.toPrettyString(), StandardCharsets.UTF_8);

        createBasicVaadinVersionsJson();

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();

        JsonNode newVaadinDeps = getOrCreatePackageJson().get(VAADIN_DEP_KEY)
                .get(DEPENDENCIES);

        assertEquals(PLATFORM_ELEMENT_MIXIN_VERSION,
                newVaadinDeps.get(VAADIN_ELEMENT_MIXIN).asString());
    }

    // #11025
    @Test
    void npmIsInUse_versionsJsonHasVaadinCoreVersionPinned_vaadinCoreVersionIgnored()
            throws IOException {
        final String expectedElementMixinVersion = "21.0.0-alpha2";
        String versionJsonString =
        //@formatter:off
                "{ \"core\": {"
                        + "\"vaadin-element-mixin\": {\n"
                        + "    \"jsVersion\": \"" + expectedElementMixinVersion + "\",\n"
                        + "    \"npmName\": \"" + VAADIN_ELEMENT_MIXIN + "\"\n"
                        + "},\n"
                        + "\"vaadin-core\": {\n"
                        + "    \"jsVersion\": \"21.0.0.alpha1\",\n"
                        // broken for npm
                        + "    \"npmName\": \"" + VAADIN_CORE_NPM_PACKAGE + "\"\n"
                        + "}\n"
                        + "}}\n";//@formatter:on
        FileUtils.write(versionJsonFile, versionJsonString,
                StandardCharsets.UTF_8);

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();
        assertTrue(task.modified, "Updates not picked");

        verifyVersions(PLATFORM_DIALOG_VERSION, expectedElementMixinVersion,
                null);
        verifyVersionLockingWithNpmOverrides(true, true, false);
        final ObjectNode packageJson = getOrCreatePackageJson();
        JsonNode dependencies = packageJson.get(DEPENDENCIES);

        assertFalse(dependencies.has(VAADIN_CORE_NPM_PACKAGE),
                VAADIN_CORE_NPM_PACKAGE
                        + " version should not be written to package.json");
        final JsonNode vaadinDependencies = packageJson.get(VAADIN_DEP_KEY)
                .get(DEPENDENCIES);
        assertFalse(vaadinDependencies.has(VAADIN_CORE_NPM_PACKAGE),
                VAADIN_CORE_NPM_PACKAGE
                        + " version should not be written to vaadin dependencies in package.json");
    }

    @Test
    void passUnorderedApplicationDependenciesAndReadUnorderedPackageJson_resultingPackageJsonIsOrdered()
            throws IOException {
        createBasicVaadinVersionsJson();

        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("foo", "bar");
        // "bar" is lexicographically before the "foo" but in the linked hash
        // map it's set after
        map.put("baz", "foobar");

        ObjectNode packageJson = getOrCreatePackageJson();
        ObjectNode dependencies = (ObjectNode) packageJson.get(DEPENDENCIES);

        packageJson.remove(DEPENDENCIES);

        packageJson.put("name", "a");
        packageJson.put("license", "b");
        packageJson.put("version", "c");
        packageJson.put("type", "module");

        LinkedHashSet<String> mainKeys = new LinkedHashSet<>(
                JacksonUtils.getKeys(packageJson));

        packageJson.set(DEPENDENCIES, dependencies);

        // Json object preserve the order of keys
        dependencies.put("foo-pack", "bar");
        dependencies.put("baz-pack", "foobar");
        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJson.toPrettyString(), StandardCharsets.UTF_8);

        TaskUpdatePackages task = createTask(map);

        task.execute();

        // now read the package json file
        packageJson = getOrCreatePackageJson();

        List<String> list = JacksonUtils.getKeys(packageJson);
        int indexOfOverrides = list.indexOf(OVERRIDES);
        if (indexOfOverrides == -1) {
            // the "vaadin" key is the last one if no overrides
            assertEquals(list.size() - 1, list.indexOf(VAADIN_DEP_KEY));
        } else {
            // the "vaadin" key is the second to the last one with overrides
            assertEquals(list.size() - 2, list.indexOf(VAADIN_DEP_KEY));
        }

        List<String> keysBeforeDeps = new ArrayList<>();

        for (String key : JacksonUtils.getKeys(packageJson)) {
            if (key.equals(DEV_DEPENDENCIES) || key.equals(DEPENDENCIES)) {
                break;
            }
            if (mainKeys.contains(key)) {
                keysBeforeDeps.add(key);
            }
        }

        // the order of the main keys is the same
        assertArrayEquals(mainKeys.toArray(), keysBeforeDeps.toArray());

        checkOrder(DEPENDENCIES, packageJson.get(DEPENDENCIES));
        checkOrder(DEV_DEPENDENCIES, packageJson.get(DEV_DEPENDENCIES));
        checkOrder(VAADIN_DEP_KEY, packageJson.get(VAADIN_DEP_KEY));
    }

    private void checkOrder(String path, JsonNode object) {
        List<String> keys = JacksonUtils.getKeys(object);
        if (path.isEmpty()) {
            assertTrue(isSorted(keys),
                    "Keys in the package Json are not sorted");
        } else {
            assertTrue(isSorted(keys), "Keys for the object " + path
                    + " in the package Json are not sorted");
        }
        for (String key : keys) {
            JsonNode value = object.get(key);
            if (value instanceof ObjectNode) {
                checkOrder(path + "/" + key, value);
            }
        }
    }

    private boolean isSorted(List<String> array) {
        if (array.size() < 2) {
            return true;
        }
        for (int i = 0; i < array.size() - 1; i++) {
            if (array.get(i).compareTo(array.get(i + 1)) > 0) {
                return false;
            }
        }
        return true;
    }

    // #11888
    @Test
    void npmIsInUse_versionsJsonContainsSameVersions_nothingIsModified()
            throws IOException {
        String versionJsonString =
        //@formatter:off
                          "{ \"core\": {"
                        + "\"vaadin-element-mixin\": {\n"
                        + "    \"jsVersion\": \"" + PLATFORM_DIALOG_VERSION + "\",\n"
                        + "    \"npmName\": \"" + VAADIN_DIALOG + "\"\n"
                        + "}\n"
                        + "}}\n";
                //@formatter:on
        FileUtils.write(versionJsonFile, versionJsonString,
                StandardCharsets.UTF_8);

        TaskUpdatePackages task = createTask(createApplicationDependencies());
        task.execute();
        assertTrue(task.modified,
                "Creation of package.json should be marked with modified");

        // Rewriting with the same packages should not mark as modified
        task = createTask(createApplicationDependencies());
        task.execute();
        assertFalse(task.modified, "PackageJson modified without changes.");
    }

    @Test
    void nonNumericVersionsNotPinned() throws IOException {
        final JsonNode packageJson = getOrCreatePackageJson();
        createBasicVaadinVersionsJson();
        ObjectNode dependencies = (ObjectNode) packageJson.get(DEPENDENCIES);
        dependencies.put("localdep", "./localdeps/localdep");
        File file = new File(npmFolder, PACKAGE_JSON);
        FileUtils.writeStringToFile(file, packageJson.toPrettyString(),
                StandardCharsets.UTF_8);

        assertFalse(packageJson.has("overrides")
                && packageJson.get("overrides").has("localdep"));

        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();

        final JsonNode newPackageJson = getOrCreatePackageJson();

        assertFalse(newPackageJson.has("overrides")
                && newPackageJson.get("overrides").has("localdep"));
    }

    @Test
    void oldVersionsJson_shouldDowngrade() throws IOException {
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
        assertTrue(task.modified, "Updates not picked");

        verifyVersions(appDependencyVersion, oldPlatformVersion,
                oldPlatformVersion);
        verifyVersionLockingWithNpmOverrides(true, true, true);
    }

    @Test
    void oldVersionsJson_shouldDowngrade_verifyPnpmOverrides()
            throws IOException {
        // run the basic test to produce an existing package.json
        runTestWithoutPreexistingPackageJson();
        // write new versions json and scanned deps
        final String oldPlatformVersion = "1.0.0";
        createVaadinVersionsJson(oldPlatformVersion, oldPlatformVersion,
                oldPlatformVersion);

        final Map<String, String> applicationDependencies = createApplicationDependencies();
        final String appDependencyVersion = "1.5.0";
        applicationDependencies.put(VAADIN_DIALOG, appDependencyVersion);
        final TaskUpdatePackages task = createTask(applicationDependencies,
                true);
        task.execute();
        assertTrue(task.modified, "Updates not picked");

        verifyVersions(appDependencyVersion, oldPlatformVersion,
                oldPlatformVersion);
        verifyVersionLockingWithPnpmOverrides(true, true, true);
    }

    @Test
    void npmOverridesExist_customOverridesCopiedOver_verifyPnpmOverrides()
            throws IOException {
        // run the basic test to produce an existing package.json
        runTestWithoutPreexistingPackageJson();
        // write new versions json and scanned deps
        final String oldPlatformVersion = "1.0.0";
        createVaadinVersionsJson(oldPlatformVersion, oldPlatformVersion,
                oldPlatformVersion);

        String CUSTOM_COMPONENT = "@custom/component";

        try {
            ObjectNode versionJson = getOrCreatePackageJson();
            if (versionJson.has(OVERRIDES)) {
                ((ObjectNode) versionJson.get(OVERRIDES)).set(CUSTOM_COMPONENT,
                        JacksonUtils.createNode("1.2.1"));
            } else {
                ObjectNode npmOverrides = JacksonUtils.createObjectNode();
                npmOverrides.set(CUSTOM_COMPONENT,
                        JacksonUtils.createNode("1.2.1"));
                versionJson.set(OVERRIDES, npmOverrides);
            }
            FileUtils.write(packageJson, versionJson.toPrettyString(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final Map<String, String> applicationDependencies = createApplicationDependencies();
        final String appDependencyVersion = "1.5.0";
        applicationDependencies.put(VAADIN_DIALOG, appDependencyVersion);
        final TaskUpdatePackages task = createTask(applicationDependencies,
                true);
        task.execute();
        assertTrue(task.modified, "Updates not picked");

        verifyVersions(appDependencyVersion, oldPlatformVersion,
                oldPlatformVersion);
        verifyVersionLockingWithPnpmOverrides(true, true, true);

        JsonNode pnpm = getOrCreatePackageJson().get(PNPM);
        assertNotNull(pnpm, "Object for 'pnpm' should exist");
        JsonNode overrides = pnpm.get(OVERRIDES);
        assertNotNull(overrides, "Object for 'overrides' should exist");

        assertTrue(overrides.has(CUSTOM_COMPONENT),
                "Custom component override was not present");
        assertEquals("1.2.1", overrides.get(CUSTOM_COMPONENT).asString());
    }

    @Test
    void reactEnabled_scannerDependencies_coreDependenciesNotAdded()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION);
        final FrontendDependencies frontendDependenciesScanner = Mockito
                .mock(FrontendDependencies.class);
        Mockito.when(frontendDependenciesScanner.getPackages())
                .thenReturn(createApplicationDependencies());
        Options options = new MockOptions(finder, npmFolder)
                .withBuildDirectory(TARGET).withEnablePnpm(false)
                .withBundleBuild(true).withReact(true)
                .withFrontendDependenciesScanner(frontendDependenciesScanner);
        final TaskUpdatePackages task = new TaskUpdatePackages(options) {
        };
        task.execute();
        final ObjectNode newPackageJson = getOrCreatePackageJson();

        assertTrue(newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(VAADIN_DIALOG));
        assertTrue(newPackageJson.has("vaadin") && newPackageJson.get("vaadin")
                .get("dependencies").has(VAADIN_DIALOG));
        assertTrue(newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(VAADIN_OVERLAY));
        assertTrue(newPackageJson.has("vaadin") && newPackageJson.get("vaadin")
                .get("dependencies").has(VAADIN_OVERLAY));
        assertTrue(newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(REACT_COMPONENTS));
        assertTrue(newPackageJson.has("vaadin") && newPackageJson.get("vaadin")
                .get("dependencies").has(REACT_COMPONENTS));
    }

    @Test
    void reactEnabled_scannerDependenciesAndExclusions_excludedDependenciesNotAdded()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION,
                Set.of(VAADIN_DIALOG, VAADIN_OVERLAY));
        final FrontendDependencies frontendDependenciesScanner = Mockito
                .mock(FrontendDependencies.class);
        Mockito.when(frontendDependenciesScanner.getPackages())
                .thenReturn(createApplicationDependencies());
        Options options = new MockOptions(finder, npmFolder)
                .withBuildDirectory(TARGET).withEnablePnpm(false)
                .withBundleBuild(true).withReact(true)
                .withFrontendDependenciesScanner(frontendDependenciesScanner);
        final TaskUpdatePackages task = new TaskUpdatePackages(options) {
        };
        task.execute();
        final JsonNode newPackageJson = getOrCreatePackageJson();

        assertFalse(newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(VAADIN_DIALOG));
        assertFalse(newPackageJson.has("vaadin") && newPackageJson.get("vaadin")
                .get("dependencies").has(VAADIN_DIALOG));
        assertFalse(newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(VAADIN_OVERLAY));
        assertFalse(newPackageJson.has("vaadin") && newPackageJson.get("vaadin")
                .get("dependencies").has(VAADIN_OVERLAY));
        assertTrue(newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(REACT_COMPONENTS));
        assertTrue(newPackageJson.has("vaadin") && newPackageJson.get("vaadin")
                .get("dependencies").has(REACT_COMPONENTS));
    }

    @Test
    void reactEnabled_noScannerDependencies_coreDependenciesNotAdded()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION);
        final FrontendDependencies frontendDependenciesScanner = Mockito
                .mock(FrontendDependencies.class);
        Mockito.when(frontendDependenciesScanner.getPackages())
                .thenReturn(new HashMap<>());
        Options options = new MockOptions(finder, npmFolder)
                .withBuildDirectory(TARGET).withEnablePnpm(false)
                .withBundleBuild(true).withReact(true)
                .withFrontendDependenciesScanner(frontendDependenciesScanner);
        final TaskUpdatePackages task = new TaskUpdatePackages(options) {
        };
        task.execute();
        final JsonNode newPackageJson = getOrCreatePackageJson();

        assertFalse(newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(VAADIN_DIALOG));
        assertFalse(newPackageJson.has("vaadin") && newPackageJson.get("vaadin")
                .get("dependencies").has(VAADIN_DIALOG));
        assertTrue(newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(VAADIN_OVERLAY));
        assertTrue(newPackageJson.has("vaadin") && newPackageJson.get("vaadin")
                .get("dependencies").has(VAADIN_OVERLAY));
        assertTrue(newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(REACT_COMPONENTS));
        assertTrue(newPackageJson.has("vaadin") && newPackageJson.get("vaadin")
                .get("dependencies").has(REACT_COMPONENTS));
    }

    @Test
    void reactDisabled_coreDependenciesAdded() throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION);
        final FrontendDependencies frontendDependenciesScanner = Mockito
                .mock(FrontendDependencies.class);
        Mockito.when(frontendDependenciesScanner.getPackages())
                .thenReturn(createApplicationDependencies());
        Options options = new MockOptions(finder, npmFolder)
                .withBuildDirectory(TARGET).withEnablePnpm(false)
                .withBundleBuild(true).withReact(false)
                .withFrontendDependenciesScanner(frontendDependenciesScanner);
        final TaskUpdatePackages task = new TaskUpdatePackages(options) {
        };
        task.execute();
        final JsonNode newPackageJson = getOrCreatePackageJson();

        assertTrue(newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(VAADIN_DIALOG));
        assertTrue(newPackageJson.has("vaadin") && newPackageJson.get("vaadin")
                .get("dependencies").has(VAADIN_DIALOG));
        assertTrue(newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(VAADIN_OVERLAY));
        assertTrue(newPackageJson.has("vaadin") && newPackageJson.get("vaadin")
                .get("dependencies").has(VAADIN_OVERLAY));
        assertFalse(newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(REACT_COMPONENTS));
        assertFalse(newPackageJson.has("vaadin") && newPackageJson.get("vaadin")
                .get("dependencies").has(REACT_COMPONENTS));

    }

    @Test
    void webComponentsExcluded_reactDisabled_noExclusionsInVersions()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION);
        Options options = new MockOptions(finder, npmFolder)
                .withBuildDirectory(TARGET).withEnablePnpm(false)
                .withBundleBuild(true).withReact(false)
                .withNpmExcludeWebComponents(true);
        // with scanned application dependencies
        execTaskUpdatePackages(createApplicationDependencies(), options);
        JsonNode pkgJson = getOrCreatePackageJson();

        assertTrue(hasInDependencies(pkgJson, VAADIN_DIALOG));
        assertTrue(hasInVaadinDependencies(pkgJson, VAADIN_DIALOG));
        assertTrue(hasInDependencies(pkgJson, VAADIN_OVERLAY));
        assertTrue(hasInVaadinDependencies(pkgJson, VAADIN_OVERLAY));
        assertFalse(hasInDependencies(pkgJson, REACT_COMPONENTS));
        assertFalse(hasInVaadinDependencies(pkgJson, REACT_COMPONENTS));

        // without scanned application dependencies
        execTaskUpdatePackages(new HashMap<>(), options);
        pkgJson = getOrCreatePackageJson();

        assertFalse(hasInDependencies(pkgJson, VAADIN_DIALOG));
        assertFalse(hasInVaadinDependencies(pkgJson, VAADIN_DIALOG));
        assertTrue(hasInDependencies(pkgJson, VAADIN_OVERLAY));
        assertTrue(hasInVaadinDependencies(pkgJson, VAADIN_OVERLAY));
        assertFalse(hasInDependencies(pkgJson, REACT_COMPONENTS));
        assertFalse(hasInVaadinDependencies(pkgJson, REACT_COMPONENTS));
    }

    @Test
    void webComponentsExcluded_reactDisabled_exclusionsInVersions_noWebComponentsIncluded()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION,
                Set.of(VAADIN_DIALOG));
        Options options = new MockOptions(finder, npmFolder)
                .withBuildDirectory(TARGET).withEnablePnpm(false)
                .withBundleBuild(true).withReact(false)
                .withNpmExcludeWebComponents(true);

        // with scanned application dependencies
        execTaskUpdatePackages(createApplicationDependencies(), options);
        JsonNode pkgJson = getOrCreatePackageJson();

        assertFalse(hasInDependencies(pkgJson, VAADIN_DIALOG));
        assertFalse(hasInVaadinDependencies(pkgJson, VAADIN_DIALOG));
        assertTrue(hasInDependencies(pkgJson, VAADIN_OVERLAY));
        assertTrue(hasInVaadinDependencies(pkgJson, VAADIN_OVERLAY));
        assertFalse(hasInDependencies(pkgJson, REACT_COMPONENTS));
        assertFalse(hasInVaadinDependencies(pkgJson, REACT_COMPONENTS));

        // without scanned application dependencies
        execTaskUpdatePackages(new HashMap<>(), options);
        pkgJson = getOrCreatePackageJson();

        assertFalse(hasInDependencies(pkgJson, VAADIN_DIALOG));
        assertFalse(hasInVaadinDependencies(pkgJson, VAADIN_DIALOG));
        assertTrue(hasInDependencies(pkgJson, VAADIN_OVERLAY));
        assertTrue(hasInVaadinDependencies(pkgJson, VAADIN_OVERLAY));
        assertFalse(hasInDependencies(pkgJson, REACT_COMPONENTS));
        assertFalse(hasInVaadinDependencies(pkgJson, REACT_COMPONENTS));
    }

    @Test
    void webComponentsExcluded_reactEnabled_noExclusionsInVersions()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION);
        Options options = new MockOptions(finder, npmFolder)
                .withBuildDirectory(TARGET).withEnablePnpm(false)
                .withBundleBuild(true).withReact(true)
                .withNpmExcludeWebComponents(true);

        // with scanned application dependencies
        execTaskUpdatePackages(createApplicationDependencies(), options);
        JsonNode pkgJson = getOrCreatePackageJson();

        assertTrue(hasInDependencies(pkgJson, VAADIN_DIALOG));
        assertTrue(hasInVaadinDependencies(pkgJson, VAADIN_DIALOG));
        assertTrue(hasInDependencies(pkgJson, VAADIN_OVERLAY));
        assertTrue(hasInVaadinDependencies(pkgJson, VAADIN_OVERLAY));
        assertFalse(hasInDependencies(pkgJson, REACT_COMPONENTS));
        assertFalse(hasInVaadinDependencies(pkgJson, REACT_COMPONENTS));

        // without scanned application dependencies
        execTaskUpdatePackages(new HashMap<>(), options);
        pkgJson = getOrCreatePackageJson();

        assertFalse(hasInDependencies(pkgJson, VAADIN_DIALOG));
        assertFalse(hasInVaadinDependencies(pkgJson, VAADIN_DIALOG));
        assertTrue(hasInDependencies(pkgJson, VAADIN_OVERLAY));
        assertTrue(hasInVaadinDependencies(pkgJson, VAADIN_OVERLAY));
        assertFalse(hasInDependencies(pkgJson, REACT_COMPONENTS));
        assertFalse(hasInVaadinDependencies(pkgJson, REACT_COMPONENTS));
    }

    @Test
    void webComponentsExcluded_reactEnabled_exclusionsInVersions_noWebComponentsIncluded()
            throws IOException {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION,
                Set.of(VAADIN_DIALOG));
        Options options = new MockOptions(finder, npmFolder)
                .withBuildDirectory(TARGET).withEnablePnpm(false)
                .withBundleBuild(true).withReact(true)
                .withNpmExcludeWebComponents(true);

        // with scanned application dependencies
        execTaskUpdatePackages(createApplicationDependencies(), options);
        JsonNode pkgJson = getOrCreatePackageJson();

        assertFalse(hasInDependencies(pkgJson, VAADIN_DIALOG));
        assertFalse(hasInVaadinDependencies(pkgJson, VAADIN_DIALOG));
        assertTrue(hasInDependencies(pkgJson, VAADIN_OVERLAY));
        assertTrue(hasInVaadinDependencies(pkgJson, VAADIN_OVERLAY));
        assertFalse(hasInDependencies(pkgJson, REACT_COMPONENTS));
        assertFalse(hasInVaadinDependencies(pkgJson, REACT_COMPONENTS));

        // without scanned application dependencies
        execTaskUpdatePackages(new HashMap<>(), options);
        pkgJson = getOrCreatePackageJson();

        assertFalse(hasInDependencies(pkgJson, VAADIN_DIALOG));
        assertFalse(hasInVaadinDependencies(pkgJson, VAADIN_DIALOG));
        assertTrue(hasInDependencies(pkgJson, VAADIN_OVERLAY));
        assertTrue(hasInVaadinDependencies(pkgJson, VAADIN_OVERLAY));
        assertFalse(hasInDependencies(pkgJson, REACT_COMPONENTS));
        assertFalse(hasInVaadinDependencies(pkgJson, REACT_COMPONENTS));
    }

    private void execTaskUpdatePackages(
            Map<String, String> scannedApplicationDependencies,
            Options options) {
        final FrontendDependencies frontendDependenciesScanner = Mockito
                .mock(FrontendDependencies.class);
        Mockito.when(frontendDependenciesScanner.getPackages())
                .thenReturn(scannedApplicationDependencies);
        options = options
                .withFrontendDependenciesScanner(frontendDependenciesScanner);
        final TaskUpdatePackages task = new TaskUpdatePackages(options) {
        };
        task.execute();
    }

    private boolean hasInDependencies(JsonNode newPackageJson, String key) {
        return newPackageJson.has("dependencies")
                && newPackageJson.get("dependencies").has(key);
    }

    private boolean hasInVaadinDependencies(JsonNode newPackageJson,
            String key) {
        return newPackageJson.has("vaadin")
                && newPackageJson.get("vaadin").get("dependencies").has(key);
    }

    private void createBasicVaadinVersionsJson() {
        createVaadinVersionsJson(PLATFORM_DIALOG_VERSION,
                PLATFORM_ELEMENT_MIXIN_VERSION, PLATFORM_OVERLAY_VERSION);
    }

    private void createVaadinVersionsJson(String dialogVersion,
            String elementMixinVersion, String overlayVersion) {
        createVaadinVersionsJson(dialogVersion, elementMixinVersion,
                overlayVersion, null);
    }

    private void createVaadinVersionsJson(String dialogVersion,
            String elementMixinVersion, String overlayVersion,
            Set<String> exclusions) {
        String exclusionsString = exclusions != null
                ? ",\"exclusions\": ["
                        + exclusions.stream().map(str -> "\"" + str + "\"")
                                .collect(Collectors.joining(","))
                        + "]\n"
                : "";
        // testing with exact versions json content instead of mocking parsing
        String versionJsonString = //@formatter:off
                "{ \"core\": {"
                        + "\"vaadin-dialog\": {\n"
                        + "   \"component\": true,\n"
                        + "   \"javaVersion\": \"{{version}}\",\n"
                        + "    \"jsVersion\": \"" + dialogVersion + "\",\n"
                        + "    \"npmName\": \"" + VAADIN_DIALOG + "\",\n"
                        + "    \"mode\": \"lit\"\n"
                        + "},\n"
                        + "\"vaadin-element-mixin\": {\n"
                        + "    \"jsVersion\": \"" + elementMixinVersion
                        + "\",\n" + "    \"npmName\": \"" + VAADIN_ELEMENT_MIXIN
                        + "\"\n" + "},\n"
                        + "\"vaadin-overlay\": {\n"
                        + "    \"jsVersion\": \"" + overlayVersion + "\",\n"
                        + "    \"npmName\": \"" + VAADIN_OVERLAY + "\",\n"
                        + "    \"releasenotes\": true\n"
                        + "}},\n"
                        + "\"react\": {\n" +
                        "        \"react-components\": {\n" +
                        "            \"jsVersion\": \"24.4.0-alpha13\",\n" +
                        "            \"npmName\": \"@vaadin/react-components\",\n" +
                        "            \"mode\": \"react\"\n" + exclusionsString +
                        "        }\n" +
                        "    }}\n";//@formatter:on
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
        Options options = new MockOptions(finder, npmFolder)
                .withBuildDirectory(TARGET).withEnablePnpm(enablePnpm)
                .withBundleBuild(true).withReact(false)
                .withFrontendDependenciesScanner(frontendDependenciesScanner);
        return new TaskUpdatePackages(options) {
        };
    }

    private ObjectNode getOrCreatePackageJson() throws IOException {
        if (packageJson.exists()) {
            return JacksonUtils.readTree(FileUtils.readFileToString(packageJson,
                    StandardCharsets.UTF_8));
        } else {
            final ObjectNode packageJsonJson = JacksonUtils.createObjectNode();
            packageJsonJson.set(DEPENDENCIES, JacksonUtils.createObjectNode());
            FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                    packageJsonJson.toPrettyString(), StandardCharsets.UTF_8);
            return packageJsonJson;
        }
    }

    private void verifyVersions(String expectedDialogVersion,
            String expectedElementMixinVersion, String expectedOverlayVersion)
            throws IOException {
        JsonNode dependencies = getOrCreatePackageJson().get(DEPENDENCIES);
        if (expectedDialogVersion == null) {
            assertNull(dependencies.get(VAADIN_DIALOG),
                    "Dependency added when it should not have been");
        } else {
            assertEquals(expectedDialogVersion,
                    dependencies.get(VAADIN_DIALOG).asString());
        }
        if (expectedElementMixinVersion == null) {
            assertNull(dependencies.get(VAADIN_ELEMENT_MIXIN),
                    "Dependency added when it should not have been");
        } else {
            assertEquals(expectedElementMixinVersion,
                    dependencies.get(VAADIN_ELEMENT_MIXIN).asString());
        }
        if (expectedOverlayVersion == null) {
            assertNull(dependencies.get(VAADIN_OVERLAY),
                    "Dependency added when it should not have been");
        } else {
            assertEquals(expectedOverlayVersion,
                    dependencies.get(VAADIN_OVERLAY).asString());
        }
    }

    private void verifyVersionLockingWithNpmOverrides(boolean hasDialogLocking,
            boolean hasElementMixinLocking, boolean hasOverlayLocking)
            throws IOException {
        JsonNode overrides = getOrCreatePackageJson().get(OVERRIDES);
        assertNotNull(overrides, "Object for 'overrides' should exist");

        if (hasDialogLocking) {
            assertTrue(overrides.has(VAADIN_DIALOG),
                    "Dialog override was not present");
            assertEquals("$" + VAADIN_DIALOG,
                    overrides.get(VAADIN_DIALOG).asString());
        } else {
            assertNull(overrides.get(VAADIN_DIALOG),
                    "vaadin-dialog dependency should not be present");
        }
        if (hasElementMixinLocking) {
            assertTrue(overrides.has(VAADIN_ELEMENT_MIXIN),
                    "Element-Mixin override was not present");
            assertEquals("$" + VAADIN_ELEMENT_MIXIN,
                    overrides.get(VAADIN_ELEMENT_MIXIN).asString());
        } else {
            assertNull(overrides.get(VAADIN_ELEMENT_MIXIN),
                    "vaadin-element-mixin dependency should not be present");
        }
        if (hasOverlayLocking) {
            assertTrue(overrides.has(VAADIN_OVERLAY),
                    "Overlay override was not present");
            assertEquals("$" + VAADIN_OVERLAY,
                    overrides.get(VAADIN_OVERLAY).asString());
        } else {
            assertNull(overrides.get(VAADIN_OVERLAY),
                    "vaadin-overlay dependency should not be present");
        }
    }

    private void verifyVersionLockingWithPnpmOverrides(boolean hasDialogLocking,
            boolean hasElementMixinLocking, boolean hasOverlayLocking)
            throws IOException {
        JsonNode pnpm = getOrCreatePackageJson().get(PNPM);
        assertNotNull(pnpm, "Object for 'pnpm' should exist");
        JsonNode overrides = pnpm.get(OVERRIDES);
        assertNotNull(overrides, "Object for 'overrides' should exist");

        if (hasDialogLocking) {
            assertTrue(overrides.has(VAADIN_DIALOG),
                    "Dialog override was not present");
            assertEquals("$" + VAADIN_DIALOG,
                    overrides.get(VAADIN_DIALOG).asString());
        } else {
            assertNull(overrides.get(VAADIN_DIALOG),
                    "vaadin-dialog dependency should not be present");
        }
        if (hasElementMixinLocking) {
            assertTrue(overrides.has(VAADIN_ELEMENT_MIXIN),
                    "Element-Mixin override was not present");
            assertEquals("$" + VAADIN_ELEMENT_MIXIN,
                    overrides.get(VAADIN_ELEMENT_MIXIN).asString());
        } else {
            assertNull(overrides.get(VAADIN_ELEMENT_MIXIN),
                    "vaadin-element-mixin dependency should not be present");
        }
        if (hasOverlayLocking) {
            assertTrue(overrides.has(VAADIN_OVERLAY),
                    "Overlay override was not present");
            assertEquals("$" + VAADIN_OVERLAY,
                    overrides.get(VAADIN_OVERLAY).asString());
        } else {
            assertNull(overrides.get(VAADIN_OVERLAY),
                    "vaadin-overlay dependency should not be present");
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

    @Test
    void npmIsInUse_pwaOfflineEnabled_workboxOverridesAdded()
            throws IOException {
        createBasicVaadinVersionsJson();
        final TaskUpdatePackages task = createTaskWithPwa(
                createApplicationDependencies(), false, true);
        task.execute();

        ObjectNode pkgJson = getOrCreatePackageJson();
        assertTrue(pkgJson.has(OVERRIDES), "overrides section should exist");
        JsonNode overrides = pkgJson.get(OVERRIDES);

        // Verify workbox-build nested object override is present
        assertTrue(overrides.has("workbox-build"),
                "workbox-build override should be added when PWA offline is enabled");
        JsonNode workboxBuildOverride = overrides.get("workbox-build");
        assertTrue(workboxBuildOverride.isObject(),
                "workbox-build override should be a nested object");
        assertTrue(workboxBuildOverride.has("serialize-javascript"),
                "workbox-build override should contain serialize-javascript");
    }

    @Test
    void npmIsInUse_pwaOfflineDisabled_workboxOverridesNotAdded()
            throws IOException {
        createBasicVaadinVersionsJson();
        final TaskUpdatePackages task = createTaskWithPwa(
                createApplicationDependencies(), false, false);
        task.execute();

        ObjectNode pkgJson = getOrCreatePackageJson();
        if (pkgJson.has(OVERRIDES)) {
            JsonNode overrides = pkgJson.get(OVERRIDES);
            assertFalse(overrides.has("workbox-build"),
                    "workbox-build override should not be added when PWA offline is disabled");
        }
    }

    @Test
    void npmIsInUse_pwaOfflineEnabled_overridesTrackedInVaadinSection()
            throws IOException {
        createBasicVaadinVersionsJson();
        final TaskUpdatePackages task = createTaskWithPwa(
                createApplicationDependencies(), false, true);
        task.execute();

        ObjectNode pkgJson = getOrCreatePackageJson();
        assertTrue(pkgJson.has(VAADIN_DEP_KEY), "vaadin section should exist");
        JsonNode vaadin = pkgJson.get(VAADIN_DEP_KEY);
        assertTrue(vaadin.has(OVERRIDES),
                "vaadin.overrides section should exist");
        JsonNode vaadinOverrides = vaadin.get(OVERRIDES);

        // Verify workbox-build is tracked in vaadin.overrides
        assertTrue(vaadinOverrides.has("workbox-build"),
                "workbox-build should be tracked in vaadin.overrides");
    }

    @Test
    void npmIsInUse_pwaOfflineDisabledAfterEnabled_workboxOverridesRemoved()
            throws IOException {
        createBasicVaadinVersionsJson();

        // First run with PWA offline enabled
        TaskUpdatePackages task = createTaskWithPwa(
                createApplicationDependencies(), false, true);
        task.execute();

        // Verify workbox override was added
        ObjectNode pkgJson = getOrCreatePackageJson();
        assertTrue(pkgJson.has(OVERRIDES));
        assertTrue(pkgJson.get(OVERRIDES).has("workbox-build"),
                "workbox-build override should be present after first run");
        JsonNode workboxOverride = pkgJson.get(OVERRIDES).get("workbox-build");
        assertTrue(workboxOverride.isObject(),
                "workbox-build should be a nested object");

        // Verify nested structure exists
        assertTrue(((ObjectNode) workboxOverride).size() > 0,
                "workbox-build nested object should have at least one child");

        // Second run with PWA offline disabled
        task = createTaskWithPwa(createApplicationDependencies(), false, false);
        task.execute();

        // Verify workbox override was removed
        pkgJson = getOrCreatePackageJson();
        if (pkgJson.has(OVERRIDES)) {
            assertFalse(pkgJson.get(OVERRIDES).has("workbox-build"),
                    "workbox-build override should be removed when PWA offline is disabled");
            // Verify the entire nested object structure is gone, not just the
            // key
            ObjectNode overridesSection = (ObjectNode) pkgJson.get(OVERRIDES);
            for (String key : JacksonUtils.getKeys(overridesSection)) {
                assertFalse(key.equals("workbox-build"),
                        "No workbox-build key should remain in any form");
            }
        }

        // Verify vaadin.overrides was cleaned up properly
        if (pkgJson.has(VAADIN_DEP_KEY)
                && pkgJson.get(VAADIN_DEP_KEY).has(OVERRIDES)) {
            assertFalse(
                    pkgJson.get(VAADIN_DEP_KEY).get(OVERRIDES)
                            .has("workbox-build"),
                    "workbox-build should be removed from vaadin.overrides");
            // Verify empty vaadin.overrides section is removed
            JsonNode vaadinOverrides = pkgJson.get(VAADIN_DEP_KEY)
                    .get(OVERRIDES);
            if (vaadinOverrides != null && vaadinOverrides.size() == 0) {
                assertFalse(pkgJson.get(VAADIN_DEP_KEY).has(OVERRIDES),
                        "Empty vaadin.overrides should be removed");
            }
        }

        // Add user override and re-enable PWA to verify proper coexistence
        pkgJson = getOrCreatePackageJson();
        if (!pkgJson.has(OVERRIDES)) {
            pkgJson.set(OVERRIDES, JacksonUtils.createObjectNode());
        }
        ((ObjectNode) pkgJson.get(OVERRIDES)).put("user-dep", "1.0.0");
        FileUtils.writeStringToFile(packageJson, pkgJson.toPrettyString(),
                StandardCharsets.UTF_8);

        // Third run with PWA re-enabled
        task = createTaskWithPwa(createApplicationDependencies(), false, true);
        task.execute();

        // Verify both user override and workbox override coexist
        pkgJson = getOrCreatePackageJson();
        assertTrue(pkgJson.get(OVERRIDES).has("user-dep"),
                "User override should be preserved");
        assertEquals("1.0.0",
                pkgJson.get(OVERRIDES).get("user-dep").asString());
        assertTrue(pkgJson.get(OVERRIDES).has("workbox-build"),
                "workbox-build override should be re-added");
    }

    @Test
    void npmIsInUse_nestedObjectOverrides_handledCorrectlyInVersionLocking()
            throws IOException {
        createBasicVaadinVersionsJson();

        // Create package.json with a nested object override (not a string)
        ObjectNode pkgJson = getOrCreatePackageJson();
        ObjectNode overrides = JacksonUtils.createObjectNode();
        ObjectNode nestedOverride = JacksonUtils.createObjectNode();
        nestedOverride.put("some-dep", "1.0.0");
        overrides.set("parent-pkg", nestedOverride);
        pkgJson.set(OVERRIDES, overrides);
        FileUtils.writeStringToFile(packageJson, pkgJson.toPrettyString(),
                StandardCharsets.UTF_8);

        // Run the task - should not fail with nested object overrides
        final TaskUpdatePackages task = createTask(
                createApplicationDependencies());
        task.execute();

        // Verify the nested override is preserved (not treated as string)
        pkgJson = getOrCreatePackageJson();
        assertTrue(pkgJson.has(OVERRIDES));
        JsonNode parentPkgOverride = pkgJson.get(OVERRIDES).get("parent-pkg");
        if (parentPkgOverride != null) {
            assertTrue(parentPkgOverride.isObject(),
                    "nested object override should be preserved");
        }
    }

    @Test
    void npmIsInUse_emptyVaadinOverrides_removedFromPackageJson()
            throws IOException {
        // Setup: Create package.json with vaadin.overrides that will be removed
        FileUtils.write(versionJsonFile, "{}", StandardCharsets.UTF_8);

        // First run to create a valid package.json with empty dependencies
        TaskUpdatePackages task = createTask(Map.of());
        task.execute();

        // Now add a Vaadin-managed override (both in vaadin.overrides and main
        // overrides). This simulates an override that was added by Vaadin but
        // is no longer needed.
        ObjectNode pkgJson = getOrCreatePackageJson();
        pkgJson.set(VAADIN_DEP_KEY,
                JacksonUtils.createObjectNode().set(OVERRIDES, JacksonUtils
                        .createObjectNode().put("some-old-override", "value")));

        // Also add to main overrides section (this makes it a "Vaadin-managed"
        // override)
        pkgJson.set(OVERRIDES, JacksonUtils.createObjectNode()
                .put("some-old-override", "value"));

        FileUtils.writeStringToFile(packageJson, pkgJson.toPrettyString(),
                StandardCharsets.UTF_8);

        // Execute task again
        task.execute();

        // Verify vaadin.overrides is removed when empty
        ObjectNode resultJson = getOrCreatePackageJson();
        ObjectNode resultVaadin = (ObjectNode) resultJson.get(VAADIN_DEP_KEY);
        assertFalse(resultVaadin.has(OVERRIDES),
                "Empty vaadin.overrides should be removed from package.json");
    }

    @Test
    void pnpmIsInUse_pwaOfflineEnabled_workboxOverridesFlattened()
            throws IOException {
        createBasicVaadinVersionsJson();
        final TaskUpdatePackages task = createTaskWithPwa(
                createApplicationDependencies(), true, true);
        task.execute();

        ObjectNode pkgJson = getOrCreatePackageJson();

        // Verify npm-style overrides are NOT at root level
        assertFalse(pkgJson.has(OVERRIDES),
                "npm overrides should not exist at root when pnpm is enabled");

        // Verify pnpm.overrides section exists
        assertTrue(pkgJson.has(PNPM), "pnpm section should exist");
        JsonNode pnpm = pkgJson.get(PNPM);
        assertTrue(pnpm.has(OVERRIDES), "pnpm.overrides should exist");
        JsonNode overrides = pnpm.get(OVERRIDES);

        // Verify workbox-build nested overrides are flattened with > separator
        assertTrue(overrides.has("workbox-build>serialize-javascript"),
                "Flattened workbox-build>serialize-javascript should be present");
        assertTrue(overrides.has("workbox-build>@rollup/plugin-terser"),
                "Flattened workbox-build>@rollup/plugin-terser should be present");
        assertTrue(overrides.has("workbox-build>glob"),
                "Flattened workbox-build>glob should be present");

        // Verify the values are strings, not nested objects
        assertTrue(
                overrides.get("workbox-build>serialize-javascript").isString(),
                "Flattened override should be a string value");

        // Verify nested object form does NOT exist
        assertFalse(overrides.has("workbox-build"),
                "Nested object workbox-build should not exist in pnpm overrides");
    }

    @Test
    void pnpmIsInUse_pwaOfflineDisabledAfterEnabled_flattenedOverridesRemoved()
            throws IOException {
        createBasicVaadinVersionsJson();

        // First run with PWA offline enabled (creates flattened overrides)
        TaskUpdatePackages task = createTaskWithPwa(
                createApplicationDependencies(), true, true);
        task.execute();

        // Verify flattened overrides were added
        ObjectNode pkgJson = getOrCreatePackageJson();
        assertTrue(pkgJson.has(PNPM) && pkgJson.get(PNPM).has(OVERRIDES));
        assertTrue(
                pkgJson.get(PNPM).get(OVERRIDES)
                        .has("workbox-build>serialize-javascript"),
                "Flattened override should be present after first run");

        // Second run with PWA offline disabled
        task = createTaskWithPwa(createApplicationDependencies(), true, false);
        task.execute();

        // Verify all flattened workbox overrides were removed
        pkgJson = getOrCreatePackageJson();
        if (pkgJson.has(PNPM) && pkgJson.get(PNPM).has(OVERRIDES)) {
            JsonNode overrides = pkgJson.get(PNPM).get(OVERRIDES);
            assertFalse(overrides.has("workbox-build>serialize-javascript"),
                    "Flattened workbox-build>serialize-javascript should be removed");
            assertFalse(overrides.has("workbox-build>@rollup/plugin-terser"),
                    "Flattened workbox-build>@rollup/plugin-terser should be removed");
            assertFalse(overrides.has("workbox-build>glob"),
                    "Flattened workbox-build>glob should be removed");
        }

        // Also verify vaadin.overrides was cleaned up
        if (pkgJson.has(VAADIN_DEP_KEY)
                && pkgJson.get(VAADIN_DEP_KEY).has(OVERRIDES)) {
            assertFalse(
                    pkgJson.get(VAADIN_DEP_KEY).get(OVERRIDES)
                            .has("workbox-build"),
                    "workbox-build should be removed from vaadin.overrides");
        }
    }

    @Test
    void generatePackageJsonHash_pnpmOverrides_includedInHash()
            throws IOException {
        // Create package.json with pnpm overrides
        ObjectNode pkgJson = getOrCreatePackageJson();
        ObjectNode pnpmSection = JacksonUtils.createObjectNode();
        ObjectNode pnpmOverrides = JacksonUtils.createObjectNode();
        pnpmOverrides.put("some-package", "1.0.0");
        pnpmSection.set(OVERRIDES, pnpmOverrides);
        pkgJson.set(PNPM, pnpmSection);

        String hashWithPnpmOverrides = TaskUpdatePackages
                .generatePackageJsonHash(pkgJson);

        // Modify pnpm overrides and verify hash changes
        pnpmOverrides.put("some-package", "2.0.0");
        String hashWithModifiedOverrides = TaskUpdatePackages
                .generatePackageJsonHash(pkgJson);

        assertNotEquals(hashWithPnpmOverrides, hashWithModifiedOverrides,
                "Hash should change when pnpm overrides are modified");
    }

    @Test
    void generatePackageJsonHash_pnpmOverridesAdded_hashChanges()
            throws IOException {
        // Create package.json without pnpm overrides
        ObjectNode pkgJson = getOrCreatePackageJson();
        String hashWithoutPnpmOverrides = TaskUpdatePackages
                .generatePackageJsonHash(pkgJson);

        // Add pnpm overrides
        ObjectNode pnpmSection = JacksonUtils.createObjectNode();
        ObjectNode pnpmOverrides = JacksonUtils.createObjectNode();
        pnpmOverrides.put("workbox-build>serialize-javascript", "7.0.4");
        pnpmSection.set(OVERRIDES, pnpmOverrides);
        pkgJson.set(PNPM, pnpmSection);

        String hashWithPnpmOverrides = TaskUpdatePackages
                .generatePackageJsonHash(pkgJson);

        assertNotEquals(hashWithoutPnpmOverrides, hashWithPnpmOverrides,
                "Hash should change when pnpm overrides are added");
    }

    @Test
    void pnpmIsInUse_pwaOfflineEnabled_hashIncludesWorkboxOverrides()
            throws IOException {
        createBasicVaadinVersionsJson();

        // First run without PWA - record the hash
        TaskUpdatePackages taskNoPwa = createTaskWithPwa(
                createApplicationDependencies(), true, false);
        taskNoPwa.execute();

        ObjectNode pkgJsonNoPwa = getOrCreatePackageJson();
        String hashWithoutWorkboxOverrides = pkgJsonNoPwa.get(VAADIN_DEP_KEY)
                .get("hash").asString();

        // Reset package.json
        FileUtils.writeStringToFile(packageJson, "{\"dependencies\": {}}",
                StandardCharsets.UTF_8);

        // Second run with PWA offline enabled - should have different hash
        TaskUpdatePackages taskWithPwa = createTaskWithPwa(
                createApplicationDependencies(), true, true);
        taskWithPwa.execute();

        ObjectNode pkgJsonWithPwa = getOrCreatePackageJson();
        String hashWithWorkboxOverrides = pkgJsonWithPwa.get(VAADIN_DEP_KEY)
                .get("hash").asString();

        assertNotEquals(hashWithoutWorkboxOverrides, hashWithWorkboxOverrides,
                "Hash should be different when workbox overrides are added");

        // Verify flattened overrides exist
        assertTrue(pkgJsonWithPwa.has(PNPM));
        assertTrue(pkgJsonWithPwa.get(PNPM).has(OVERRIDES));
        assertTrue(
                pkgJsonWithPwa.get(PNPM).get(OVERRIDES)
                        .has("workbox-build>serialize-javascript"),
                "Flattened workbox override should be present");
    }

    @Test
    void pwaOfflineEnabled_npmToPnpmTransition_workboxOverridesFlattened()
            throws IOException {
        // Create initial package.json in npm mode
        createBasicVaadinVersionsJson();
        TaskUpdatePackages task = createTaskWithPwa(
                createApplicationDependencies(), false, true);
        task.execute();

        // Add user nested override (npm format)
        ObjectNode pkgJson = getOrCreatePackageJson();
        ((ObjectNode) pkgJson.get(OVERRIDES)).set("user-nested", JacksonUtils
                .createObjectNode().put(".", "1.0").put("dep", "2.0"));
        FileUtils.writeStringToFile(packageJson, pkgJson.toPrettyString(),
                StandardCharsets.UTF_8);

        // Run update in pnpm mode
        task = createTaskWithPwa(createApplicationDependencies(), true, true);
        task.execute();

        pkgJson = getOrCreatePackageJson();

        // Verify npm-style overrides are NOT at root level
        assertFalse(pkgJson.has(OVERRIDES),
                "npm overrides should not exist at root when pnpm is enabled");

        // Verify pnpm.overrides section exists
        assertTrue(pkgJson.has(PNPM), "pnpm section should exist");
        JsonNode pnpm = pkgJson.get(PNPM);
        assertTrue(pnpm.has(OVERRIDES), "pnpm.overrides should exist");
        JsonNode overrides = pnpm.get(OVERRIDES);

        // Verify workbox-build nested overrides are flattened with > separator
        assertTrue(overrides.has("workbox-build>serialize-javascript"),
                "Flattened workbox-build>serialize-javascript should be present");
        assertTrue(overrides.has("workbox-build>@rollup/plugin-terser"),
                "Flattened workbox-build>@rollup/plugin-terser should be present");
        assertTrue(overrides.has("workbox-build>glob"),
                "Flattened workbox-build>glob should be present");

        // Verify user overrides are converted to pnpm format
        assertTrue(overrides.has("user-nested"),
                "Flattened user-nested should be present");
        assertTrue(overrides.has("user-nested>dep"),
                "Flattened user-nested>dep should be present");

        // Verify the values are strings, not nested objects
        assertTrue(
                overrides.get("workbox-build>serialize-javascript").isString(),
                "Flattened override should be a string value");

        // Verify nested object form does NOT exist
        assertFalse(overrides.has("workbox-build"),
                "Nested object workbox-build should not exist in pnpm overrides");
    }

    @Test
    void pwaOfflineEnabled_pnpmToNpmTransition_workboxOverridesFlattened()
            throws IOException {
        // Create initial package.json in pnpm mode
        createBasicVaadinVersionsJson();
        TaskUpdatePackages task = createTaskWithPwa(
                createApplicationDependencies(), true, true);
        task.execute();

        // Add user nested override (pnpm format)
        ObjectNode pkgJson = getOrCreatePackageJson();
        ((ObjectNode) pkgJson.get(PNPM).get(OVERRIDES))
                .put("user-nested", "1.0").put("user-nested>dep", "2.0")
                .put("user-nested-reverse>dep", "3.0")
                .put("user-nested-reverse", "4.0");
        FileUtils.writeStringToFile(packageJson, pkgJson.toPrettyString(),
                StandardCharsets.UTF_8);

        // Run update in npm mode
        task = createTaskWithPwa(createApplicationDependencies(), false, true);
        task.execute();

        pkgJson = getOrCreatePackageJson();
        assertTrue(pkgJson.has(OVERRIDES), "overrides section should exist");
        JsonNode overrides = pkgJson.get(OVERRIDES);

        // Verify workbox-build nested object override is present
        assertTrue(overrides.has("workbox-build"),
                "workbox-build override should be added when PWA offline is enabled");
        JsonNode workboxBuildOverride = overrides.get("workbox-build");
        assertTrue(workboxBuildOverride.isObject(),
                "workbox-build override should be a nested object");
        assertTrue(workboxBuildOverride.has("serialize-javascript"),
                "workbox-build override should contain serialize-javascript");

        // Verify user overrides are converted to npm format
        JsonNode nestedOverride = overrides.get("user-nested");
        assertNotNull(nestedOverride, "user-nested override should be present");
        assertTrue(nestedOverride.isObject(),
                "user-nested override should be an object");
        assertTrue(nestedOverride.has("."),
                "user-nested override should have a version");
        assertTrue(nestedOverride.has("dep"),
                "user-nested>dep override should have a version");
        JsonNode nestedReverse = overrides.get("user-nested-reverse");
        assertNotNull(nestedReverse,
                "user-nested-reverse override should be present");
        assertTrue(nestedReverse.isObject(),
                "user-nested-reverse override should be an object");
        assertTrue(nestedReverse.has("."),
                "user-nested-reverse override should have a version");
        assertTrue(nestedReverse.has("dep"),
                "user-nested-reverse>dep override should have a version");

        // Verify pnpm.overrides was removed
        assertFalse(pkgJson.has(PNPM));
    }

    private TaskUpdatePackages createTaskWithPwa(
            Map<String, String> applicationDependencies, boolean enablePnpm,
            boolean pwaOfflineEnabled) {
        final FrontendDependencies frontendDependenciesScanner = Mockito
                .mock(FrontendDependencies.class);
        Mockito.when(frontendDependenciesScanner.getPackages())
                .thenReturn(applicationDependencies);

        PwaConfiguration pwaConfig = Mockito.mock(PwaConfiguration.class);
        Mockito.when(pwaConfig.isOfflineEnabled())
                .thenReturn(pwaOfflineEnabled);
        Mockito.when(frontendDependenciesScanner.getPwaConfiguration())
                .thenReturn(pwaConfig);

        // Use a real ClassFinder to access workbox resources from flow-server
        ClassFinder realFinder = new ClassFinder.DefaultClassFinder(
                this.getClass().getClassLoader());

        Options options = new MockOptions(realFinder, npmFolder)
                .withBuildDirectory(TARGET).withEnablePnpm(enablePnpm)
                .withBundleBuild(true).withReact(false)
                .withFrontendDependenciesScanner(frontendDependenciesScanner);

        return new TaskUpdatePackages(options) {
        };
    }

}
