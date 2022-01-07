/*
 * Copyright 2000-2022 Vaadin Ltd.
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
 *
 */

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.testcategory.SlowTests;
import com.vaadin.flow.testutil.FrontendStubs;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;

@Category(SlowTests.class)
public class NodeUpdatePackagesNpmVersionLockingTest
        extends NodeUpdateTestUtil {

    private static final String TEST_DEPENDENCY = "@vaadin/vaadin-overlay";
    private static final String DEPENDENCIES = "dependencies";
    private static final String OVERRIDES = "overrides";
    private static final String PLATFORM_PINNED_DEPENDENCY_VERSION = "3.2.17";
    private static final String USER_PINNED_DEPENDENCY_VERSION = "1.0";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File baseDir;
    private File generatedDir;
    private File resourcesDir;

    private ClassFinder classFinder;

    private FeatureFlags featureFlags;

    @Before
    public void setup() throws Exception {
        baseDir = temporaryFolder.getRoot();

        featureFlags = Mockito.mock(FeatureFlags.class);

        generatedDir = new File(baseDir,
                Paths.get(TARGET, DEFAULT_GENERATED_DIR).toString());
        resourcesDir = new File(baseDir,
                Paths.get(TARGET, DEFAULT_FLOW_RESOURCES_FOLDER).toString());

        FrontendStubs.createStubNode(true, true, baseDir.getAbsolutePath());

        classFinder = Mockito.spy(getClassFinder());
        File versions = temporaryFolder.newFile();
        FileUtils.write(versions,
                String.format(
                        "{" + "\"vaadin-overlay\": {"
                                + "\"npmName\": \"@vaadin/vaadin-overlay\","
                                + "\"jsVersion\": \"%s\"" + "}" + "}",
                        PLATFORM_PINNED_DEPENDENCY_VERSION),
                StandardCharsets.UTF_8);
        // @formatter:on

        Mockito.when(classFinder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(versions.toURI().toURL());
    }

    @Test
    public void shoudlLockPinnedVersion_whenExistsInDependencies()
            throws IOException {
        TaskUpdatePackages packageUpdater = createPackageUpdater();
        JsonObject packageJson = packageUpdater.getPackageJson();
        packageJson.getObject(DEPENDENCIES).put(TEST_DEPENDENCY,
                PLATFORM_PINNED_DEPENDENCY_VERSION);
        Assert.assertNull(packageJson.getObject(OVERRIDES));

        String versionsPath = packageUpdater.generateVersionsJson();
        packageUpdater.lockVersionForNpm(packageJson, versionsPath);

        Assert.assertEquals("$" + TEST_DEPENDENCY,
                packageJson.getObject(OVERRIDES).getString(TEST_DEPENDENCY));
    }

    @Test
    public void shoudlNotLockPinnedVersion_whenNotExistsInDependencies()
            throws IOException {
        TaskUpdatePackages packageUpdater = createPackageUpdater();
        JsonObject packageJson = packageUpdater.getPackageJson();

        Assert.assertNull(packageJson.getObject(OVERRIDES));
        Assert.assertNull(
                packageJson.getObject(DEPENDENCIES).get(TEST_DEPENDENCY));

        String versionsPath = packageUpdater.generateVersionsJson();
        packageUpdater.lockVersionForNpm(packageJson, versionsPath);

        Assert.assertNull(
                packageJson.getObject(OVERRIDES).get(TEST_DEPENDENCY));
    }

    @Test
    public void shouldNotUpdatesOverrides_whenHasUserModification()
            throws IOException {
        TaskUpdatePackages packageUpdater = createPackageUpdater();
        JsonObject packageJson = packageUpdater.getPackageJson();
        JsonObject overridesSection = Json.createObject();
        packageJson.put(OVERRIDES, overridesSection);

        packageJson.getObject(DEPENDENCIES).put(TEST_DEPENDENCY,
                USER_PINNED_DEPENDENCY_VERSION);
        overridesSection.put(TEST_DEPENDENCY, USER_PINNED_DEPENDENCY_VERSION);

        String versionsPath = packageUpdater.generateVersionsJson();
        packageUpdater.lockVersionForNpm(packageJson, versionsPath);

        Assert.assertEquals(USER_PINNED_DEPENDENCY_VERSION,
                packageJson.getObject(OVERRIDES).getString(TEST_DEPENDENCY));
    }

    @Test
    public void shouldDoNothingForNpmLocking_whenInPnpmMode()
            throws IOException {
        TaskUpdatePackages packageUpdater = createPackageUpdater(true);
        JsonObject packageJson = packageUpdater.getPackageJson();
        packageJson.getObject(DEPENDENCIES).put(TEST_DEPENDENCY,
                PLATFORM_PINNED_DEPENDENCY_VERSION);
        Assert.assertNull(packageJson.getObject(OVERRIDES));

        String versionsPath = packageUpdater.generateVersionsJson();
        packageUpdater.lockVersionForNpm(packageJson, versionsPath);

        Assert.assertNull(packageJson.getObject(OVERRIDES));
    }

    private TaskUpdatePackages createPackageUpdater(boolean enablePnpm) {
        FrontendDependenciesScanner scanner = Mockito
                .mock(FrontendDependenciesScanner.class);
        return new TaskUpdatePackages(classFinder, scanner, baseDir,
                generatedDir, resourcesDir, false, enablePnpm, TARGET,
                featureFlags);
    }

    private TaskUpdatePackages createPackageUpdater() {
        return createPackageUpdater(false);
    }
}
