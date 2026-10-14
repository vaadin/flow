/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.testcategory.SlowTests;
import com.vaadin.flow.testutil.FrontendStubs;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.TARGET;

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

    private ClassFinder classFinder;

    @Before
    public void setup() throws Exception {
        baseDir = temporaryFolder.getRoot();

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

        Mockito.when(
                classFinder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versions.toURI().toURL());
    }

    @Test
    public void shouldLockPinnedVersion_whenExistsInDependencies()
            throws IOException {
        TaskUpdatePackages packageUpdater = createPackageUpdater();
        ObjectNode packageJson = packageUpdater.getPackageJson();
        ((ObjectNode) packageJson.get(DEPENDENCIES)).put(TEST_DEPENDENCY,
                PLATFORM_PINNED_DEPENDENCY_VERSION);
        Assert.assertNull(packageJson.get(OVERRIDES));

        packageUpdater.generateVersionsJson(packageJson);
        packageUpdater.lockVersionForNpm(packageJson);

        Assert.assertEquals("$" + TEST_DEPENDENCY,
                packageJson.get(OVERRIDES).get(TEST_DEPENDENCY).textValue());
    }

    @Test
    public void shouldNotLockPinnedVersion_whenNotExistsInDependencies()
            throws IOException {
        TaskUpdatePackages packageUpdater = createPackageUpdater();
        ObjectNode packageJson = packageUpdater.getPackageJson();

        Assert.assertNull(packageJson.get(OVERRIDES));
        Assert.assertNull(packageJson.get(DEPENDENCIES).get(TEST_DEPENDENCY));

        packageUpdater.generateVersionsJson(packageJson);
        packageUpdater.lockVersionForNpm(packageJson);

        Assert.assertNull(packageJson.get(OVERRIDES).get(TEST_DEPENDENCY));
    }

    @Test
    public void shouldNotUpdatesOverrides_whenHasUserModification()
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

        Assert.assertEquals(USER_PINNED_DEPENDENCY_VERSION,
                packageJson.get(OVERRIDES).get(TEST_DEPENDENCY).textValue());
    }

    @Test
    public void shouldRemoveUnusedLocking() throws IOException {
        // Test when there is no vaadin-version-core.json available
        Mockito.when(
                classFinder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(null);

        TaskUpdatePackages packageUpdater = createPackageUpdater(true);
        ObjectNode packageJson = packageUpdater.getPackageJson();
        ((ObjectNode) packageJson.get(DEPENDENCIES)).put(TEST_DEPENDENCY,
                PLATFORM_PINNED_DEPENDENCY_VERSION);
        Assert.assertNull(packageJson.get(OVERRIDES));

        packageUpdater.generateVersionsJson(packageJson);
        Assert.assertTrue(packageUpdater.versionsJson.toString()
                .contains(TEST_DEPENDENCY));

        ((ObjectNode) packageJson.get(DEPENDENCIES)).remove(TEST_DEPENDENCY);

        packageUpdater.versionsJson = null;
        packageUpdater.generateVersionsJson(packageJson);
        Assert.assertFalse(packageUpdater.versionsJson.toString()
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
