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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.testutil.FrontendStubs;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.TARGET;
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
                packageJson.get(OVERRIDES).get(TEST_DEPENDENCY).textValue());
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
                packageJson.get(OVERRIDES).get(TEST_DEPENDENCY).textValue());
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

    private TaskUpdatePackages createPackageUpdater(boolean enablePnpm) {
        FrontendDependenciesScanner scanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        Options options = new MockOptions(baseDir).withEnablePnpm(enablePnpm)
                .withBuildDirectory(TARGET).withProductionMode(true);

        return new TaskUpdatePackages(scanner, options);
    }

    private TaskUpdatePackages createPackageUpdater() {
        return createPackageUpdater(false);
    }
}
