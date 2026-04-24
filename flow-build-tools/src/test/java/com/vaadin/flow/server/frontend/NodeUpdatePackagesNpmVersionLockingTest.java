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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.testutil.FrontendStubs;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.NodeUpdater.OVERRIDES;
import static com.vaadin.flow.server.frontend.NodeUpdater.PNPM;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static com.vaadin.flow.server.frontend.TaskUpdatePackages.DEPENDENCIES;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("com.vaadin.flow.testcategory.SlowTests")
class NodeUpdatePackagesNpmVersionLockingTest extends NodeUpdateTestUtil {

    private static final String TEST_DEPENDENCY = "@vaadin/vaadin-overlay";
    private static final String DEPENDENCIES = "dependencies";
    private static final String OVERRIDES = "overrides";
    private static final String PLATFORM_PINNED_DEPENDENCY_VERSION = "3.2.17";
    private static final String USER_PINNED_DEPENDENCY_VERSION = "1.0";
    private static final String RELATIVE_DEPENDENCY_VERSION = "$@vaadin/vaadin-overlay";

    @TempDir
    File temporaryFolder;

    private File baseDir;

    private ClassFinder classFinder;

    @BeforeEach
    void setup() throws Exception {
        baseDir = temporaryFolder;

        FrontendStubs.createStubNode(true, true, baseDir.getAbsolutePath());

        classFinder = Mockito.spy(getClassFinder());
        File versions = Files
                .createTempFile(temporaryFolder.toPath(), "tmp", null).toFile();
        FileUtils.write(versions,
                String.format(
                        "{" + "\"vaadin-overlay\": {"
                                + "\"npmName\": \"@vaadin/vaadin-overlay\","
                                + "\"jsVersion\": \"%s\"" + "}" + "}",
                        PLATFORM_PINNED_DEPENDENCY_VERSION),
                StandardCharsets.UTF_8);
        // @formatter:on

        Mockito.when(
                classFinder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versions.toURI().toURL());
    }

    @Test
    void shouldLockPinnedVersion_whenExistsInDependencies() throws IOException {
        TaskUpdatePackages packageUpdater = createPackageUpdater();
        ObjectNode packageJson = packageUpdater.getPackageJson();
        ((ObjectNode) packageJson.get(DEPENDENCIES)).put(TEST_DEPENDENCY,
                PLATFORM_PINNED_DEPENDENCY_VERSION);
        assertNull(packageJson.get(OVERRIDES));

        packageUpdater.generateVersionsJson(packageJson);
        packageUpdater.lockVersionForNpm(packageJson);

        assertEquals("$" + TEST_DEPENDENCY,
                packageJson.get(OVERRIDES).get(TEST_DEPENDENCY).stringValue());
    }

    @Test
    void shouldNotLockPinnedVersion_whenNotExistsInDependencies()
            throws IOException {
        TaskUpdatePackages packageUpdater = createPackageUpdater();
        ObjectNode packageJson = packageUpdater.getPackageJson();

        assertNull(packageJson.get(OVERRIDES));
        assertNull(packageJson.get(DEPENDENCIES).get(TEST_DEPENDENCY));

        packageUpdater.generateVersionsJson(packageJson);
        packageUpdater.lockVersionForNpm(packageJson);

        assertNull(packageJson.get(OVERRIDES).get(TEST_DEPENDENCY));
    }

    @Test
    void shouldNotUpdatesOverrides_whenHasUserModification()
            throws IOException {
        TaskUpdatePackages packageUpdater = createPackageUpdater();
        ObjectNode packageJson = packageUpdater.getPackageJson();
        ObjectNode overridesSection = JacksonUtils.createObjectNode();
        packageJson.set(OVERRIDES, overridesSection);

        ((ObjectNode) packageJson.get(DEPENDENCIES)).put(TEST_DEPENDENCY,
                USER_PINNED_DEPENDENCY_VERSION);
        overridesSection.put(TEST_DEPENDENCY, USER_PINNED_DEPENDENCY_VERSION);

        packageUpdater.generateVersionsJson(packageJson);
        packageUpdater.lockVersionForNpm(packageJson);

        assertEquals(USER_PINNED_DEPENDENCY_VERSION,
                packageJson.get(OVERRIDES).get(TEST_DEPENDENCY).stringValue());
    }

    @Test
    void shouldUpdatesOverrides_whenNoVaadinOverrides_changingVersion()
            throws IOException {
        TaskUpdatePackages packageUpdater = createPackageUpdater(false,
                JacksonUtils.createObjectNode().put(TEST_DEPENDENCY,
                        PLATFORM_PINNED_DEPENDENCY_VERSION));
        ObjectNode packageJson = packageUpdater.getPackageJson();
        ObjectNode overridesSection = JacksonUtils.createObjectNode();
        packageJson.set(OVERRIDES, overridesSection);

        ((ObjectNode) packageJson.get(DEPENDENCIES)).put(TEST_DEPENDENCY,
                USER_PINNED_DEPENDENCY_VERSION);
        overridesSection.put(TEST_DEPENDENCY, USER_PINNED_DEPENDENCY_VERSION);

        packageUpdater.generateVersionsJson(packageJson);
        packageUpdater.lockVersionForNpm(packageJson);

        assertEquals(PLATFORM_PINNED_DEPENDENCY_VERSION,
                packageJson.get(OVERRIDES).get(TEST_DEPENDENCY).stringValue());
    }

    @Test
    void shouldRemoveUnusedLocking() throws IOException {
        // Test when there is no vaadin-version-core.json available
        Mockito.when(
                classFinder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(null);

        TaskUpdatePackages packageUpdater = createPackageUpdater(true);
        ObjectNode packageJson = packageUpdater.getPackageJson();
        ((ObjectNode) packageJson.get(DEPENDENCIES)).put(TEST_DEPENDENCY,
                PLATFORM_PINNED_DEPENDENCY_VERSION);
        assertNull(packageJson.get(OVERRIDES));

        packageUpdater.generateVersionsJson(packageJson);
        assertTrue(packageUpdater.versionsJson.toString()
                .contains(TEST_DEPENDENCY));

        ((ObjectNode) packageJson.get(DEPENDENCIES)).remove(TEST_DEPENDENCY);

        packageUpdater.versionsJson = null;
        packageUpdater.generateVersionsJson(packageJson);
        assertFalse(packageUpdater.versionsJson.toString()
                .contains(TEST_DEPENDENCY));

    }

    @Test
    void shouldHandleNestedObjectOverrides_withoutError() throws IOException {
        final ObjectNode testOverrides = JacksonUtils.readTree("""
                {
                  "parent-package": {
                    "nested-dep": "1.0.0"
                  },
                  "level1": {
                    "level2": {
                      "deep-dep": "2.0.0"
                    }
                  },
                  "flat-dep": "3.0.0"
                }
                """);

        TaskUpdatePackages packageUpdater = createPackageUpdater(false,
                testOverrides);
        ObjectNode packageJson = packageUpdater.getPackageJson();

        // Add dependency
        ((ObjectNode) packageJson.get(DEPENDENCIES)).put(TEST_DEPENDENCY,
                PLATFORM_PINNED_DEPENDENCY_VERSION);

        packageUpdater.generateVersionsJson(packageJson);

        // Should not throw and should handle nested objects correctly
        packageUpdater.lockVersionForNpm(packageJson);

        // Verify flat override stays flat
        JsonNode overrides = packageJson.get(OVERRIDES);
        assertTrue(overrides.has("flat-dep"),
                "Flat override should remain unchanged");
        assertEquals("3.0.0", overrides.get("flat-dep").stringValue());

        // Verify nested object override is preserved
        assertTrue(overrides.has("parent-package"),
                "Nested object override should be preserved");
        assertTrue(overrides.get("parent-package").isObject(),
                "Nested override should remain an object");
        assertEquals("1.0.0",
                overrides.get("parent-package").get("nested-dep").stringValue(),
                "Nested override value should be preserved");

        // Verify vaadin.overrides tracks the nested structure correctly
        assertTrue(packageJson.has(VAADIN_DEP_KEY));
        JsonNode vaadinSection = packageJson.get(VAADIN_DEP_KEY);
        assertTrue(vaadinSection.has(OVERRIDES),
                "vaadin.overrides should exist");
        JsonNode vaadinOverrides = vaadinSection.get(OVERRIDES);
        assertTrue(vaadinOverrides.has("parent-package"),
                "vaadin.overrides should track nested override");
        assertTrue(vaadinOverrides.get("parent-package").isObject(),
                "vaadin.overrides should preserve nested structure");
        assertEquals("1.0.0",
                vaadinOverrides.get("parent-package").get("nested-dep")
                        .stringValue(),
                "vaadin.overrides should track exact nested values");

        // Verify deep nesting is preserved
        JsonNode level1Override = overrides.get("level1");
        assertTrue(level1Override.isObject());
        JsonNode level2Override = level1Override.get("level2");
        assertTrue(level2Override.isObject());
        assertEquals("2.0.0", level2Override.get("deep-dep").stringValue());
    }

    @Test
    void pnpmIsInUse_nestedOverrides_flattenedWithSeparator()
            throws IOException {
        final ObjectNode testOverrides = JacksonUtils.readTree("""
                {
                  "parent-package": {
                    "nested-dep": "1.0.0"
                  },
                  "level1": {
                    "level2": {
                      "deep-dep": "2.0.0"
                    }
                  },
                  "flat-dep": "3.0.0"
                }
                """);

        TaskUpdatePackages packageUpdater = createPackageUpdater(true,
                testOverrides);
        ObjectNode packageJson = packageUpdater.getPackageJson();

        // Add dependency
        ((ObjectNode) packageJson.get(DEPENDENCIES)).put(TEST_DEPENDENCY,
                PLATFORM_PINNED_DEPENDENCY_VERSION);

        packageUpdater.generateVersionsJson(packageJson);

        // Run version locking
        packageUpdater.lockVersionForNpm(packageJson);

        // Verify pnpm flattens overrides with > separator
        JsonNode overrides = packageJson.get(PNPM).get(OVERRIDES);
        assertTrue(overrides.has("parent-package>nested-dep"),
                "Nested override should be flattened with > separator for pnpm");
        assertEquals("1.0.0",
                overrides.get("parent-package>nested-dep").stringValue(),
                "Flattened override should have correct value");

        // Verify flat override stays flat
        assertTrue(overrides.has("flat-dep"),
                "Flat override should remain unchanged");
        assertEquals("3.0.0", overrides.get("flat-dep").stringValue());

        // Verify vaadin.overrides tracks the original nested structure
        JsonNode vaadinOverrides = packageJson.get(VAADIN_DEP_KEY)
                .get(OVERRIDES);
        assertTrue(vaadinOverrides.has("parent-package"),
                "vaadin.overrides should track nested structure");
        assertTrue(vaadinOverrides.get("parent-package").isObject(),
                "vaadin.overrides should preserve object format");

        // Verify deep nesting is fully flattened
        assertTrue(overrides.has("level1>level2>deep-dep"),
                "Deeply nested override should be fully flattened");
        assertEquals("2.0.0",
                overrides.get("level1>level2>deep-dep").stringValue());
    }

    @Test
    void pnpmIsInUse_userFlatOverride_preserved() throws IOException {
        TaskUpdatePackages packageUpdater = createPackageUpdater(true);
        ObjectNode packageJson = packageUpdater.getPackageJson();

        // User adds a flat override in pnpm format
        ObjectNode overridesSection = JacksonUtils.createObjectNode();
        overridesSection.put("user-package>user-dep", "5.0.0");
        JacksonUtils.setNestedKey(packageJson, List.of(PNPM, OVERRIDES),
                overridesSection,
                (nonObjectNode) -> JacksonUtils.createObjectNode());

        packageUpdater.generateVersionsJson(packageJson);
        packageUpdater.lockVersionForNpm(packageJson);

        // Verify user's override is preserved
        JsonNode overrides = JacksonUtils.getNestedKey(packageJson,
                List.of(PNPM, OVERRIDES));
        assertNotNull(overrides);
        assertTrue(overrides.has("user-package>user-dep"),
                "User's override should be preserved");
        assertEquals("5.0.0",
                overrides.get("user-package>user-dep").stringValue());

        // Run again to ensure it remains stable
        packageUpdater.lockVersionForNpm(packageJson);

        overrides = JacksonUtils.getNestedKey(packageJson,
                List.of(PNPM, OVERRIDES));
        assertNotNull(overrides);
        assertTrue(overrides.has("user-package>user-dep"),
                "User's override should remain preserved on second run");
        assertEquals("5.0.0",
                overrides.get("user-package>user-dep").stringValue());
    }

    private TaskUpdatePackages createPackageUpdater(boolean enablePnpm,
            ObjectNode testOverrides) {
        FrontendDependenciesScanner scanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Options options = new MockOptions(baseDir).withEnablePnpm(enablePnpm)
                .withBuildDirectory(TARGET).withProductionMode(true)
                .withFrontendDependenciesScanner(scanner);

        return new TaskUpdatePackages(options) {
            @Override
            ObjectNode getDefaultOverrides() {
                final ObjectNode overrides = super.getDefaultOverrides();
                overrides.setAll(testOverrides);
                return overrides;
            }
        };
    }

    private TaskUpdatePackages createPackageUpdater(boolean enablePnpm) {
        return createPackageUpdater(enablePnpm,
                JacksonUtils.createObjectNode());
    }

    private TaskUpdatePackages createPackageUpdater() {
        return createPackageUpdater(false);
    }
}
