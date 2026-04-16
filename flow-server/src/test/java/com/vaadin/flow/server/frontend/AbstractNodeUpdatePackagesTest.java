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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import com.vaadin.flow.internal.FileIOUtils;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.Platform;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.testutil.FrontendStubs;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEP_NAME_FLOW_DEPS;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEP_NAME_FLOW_JARS;
import static com.vaadin.flow.server.frontend.NodeUpdater.OVERRIDES;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static com.vaadin.flow.server.frontend.TaskUpdatePackages.VAADIN_APP_PACKAGE_HASH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractNodeUpdatePackagesTest
        extends NodeUpdateTestUtil {

    private static final String DEPENDENCIES = "dependencies";
    private static final String DEV_DEPENDENCIES = "devDependencies";
    private static final String VAADIN_VERSION = "vaadinVersion";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TaskUpdatePackages packageUpdater;
    private TaskGeneratePackageJson packageCreator;
    private File baseDir;

    private File packageJson;

    private ClassFinder classFinder;

    private File mainNodeModules;
    private File packageLock;
    private Options options;
    private File versions;

    @Before
    public void setup() throws Exception {
        baseDir = temporaryFolder.getRoot();

        FrontendStubs.createStubNode(true, true, baseDir.getAbsolutePath());
        classFinder = Mockito.spy(getClassFinder());
        options = new MockOptions(classFinder, baseDir)
                .withBuildDirectory(TARGET).withBundleBuild(true)
                .withFrontendDependenciesScanner(getScanner(classFinder));
        packageCreator = new TaskGeneratePackageJson(options);
        versions = temporaryFolder.newFile();
        FileUtils.write(versions, "{}", StandardCharsets.UTF_8);
        Mockito.when(
                classFinder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versions.toURI().toURL());

        packageUpdater = new TaskUpdatePackages(options);
        packageJson = new File(baseDir, PACKAGE_JSON);

        mainNodeModules = new File(baseDir, FrontendUtils.NODE_MODULES);
        packageLock = new File(baseDir, "package-lock.json");

    }

    protected abstract FrontendDependenciesScanner getScanner(
            ClassFinder finder);

    @Test
    public void should_CreatePackageJson() {
        Assert.assertFalse(packageJson.exists());
        packageCreator.execute();
        Assert.assertTrue(packageJson.exists());
    }

    @Test
    public void should_not_ModifyPackageJson_WhenAlreadyExists() {
        packageCreator.execute();
        Assert.assertTrue(packageCreator.modified);

        packageCreator.execute();
        Assert.assertFalse(packageCreator.modified);
    }

    @Test
    public void should_AddNewDependencies() throws Exception {
        packageCreator.execute();
        packageUpdater.execute();
        Assert.assertTrue(packageCreator.modified);
        Assert.assertTrue(packageUpdater.modified);
        assertMainPackageJsonContent();
    }

    @Test
    public void pnpmIsInUse_packageJsonContainsFlowDeps_removeFlowDeps()
            throws IOException {
        // use package updater with disabled PNPM
        packageUpdater = new TaskUpdatePackages(options);
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        // Add flowDeps
        ObjectNode json = packageUpdater.getPackageJson();
        getDependencies(json).put(DEP_NAME_FLOW_DEPS, "target/frontend");
        json.put(VAADIN_APP_PACKAGE_HASH,
                "e05bfd4b6c6bd20c806b3a0ad1be521bfd775c9b6f8f9c997b0ad1fda834805b");
        Files.write(packageJson.toPath(),
                Collections.singletonList(json.toString()));

        options.withEnablePnpm(true);
        packageUpdater = new TaskUpdatePackages(options);
        packageUpdater.execute();

        assertPackageJsonFlowDeps();
    }

    @Test
    public void pnpmIsInUse_packageJsonContainsFlowFrontend_removeFlowFrontend()
            throws IOException {
        // use package updater with disabled PNPM
        packageUpdater = new TaskUpdatePackages(options);
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        // Add old dep
        ObjectNode json = packageUpdater.getPackageJson();
        getDependencies(json).put(DEP_NAME_FLOW_JARS, "target/flow-frontend");
        json.put(VAADIN_APP_PACKAGE_HASH,
                "e05bfd4b6c6bd20c806b3a0ad1be521bfd775c9b6f8f9c997b0ad1fda834805b");
        Files.write(packageJson.toPath(),
                Collections.singletonList(json.toString()));

        options.withEnablePnpm(true);
        packageUpdater = new TaskUpdatePackages(options);
        packageUpdater.execute();

        assertPackageJsonFlowDeps();
    }

    @Test
    public void pnpmIsInUse_packageLockExists_removePackageLock()
            throws IOException {
        // use package updater with disabled PNPM
        packageUpdater = new TaskUpdatePackages(options);
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        Files.write(packageLock.toPath(), Collections.singletonList("{}"));

        options.withEnablePnpm(true);

        packageUpdater = new TaskUpdatePackages(options);
        packageUpdater.execute();
        Assert.assertFalse("npm package-lock should be removed for pnpm",
                packageLock.exists());
    }

    @Test
    public void npmIsInUse_packageJsonContainsFlowDeps_removeFlowDeps()
            throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        JsonNode packJsonNode = getPackageJson(packageJson);
        ObjectNode deps = (ObjectNode) packJsonNode.get(DEPENDENCIES);
        deps.put(DEP_NAME_FLOW_DEPS, "foobar");

        writePackageJson(packageJson, packJsonNode);

        packageUpdater.execute();

        assertPackageJsonFlowDeps();
    }

    @Test
    public void npmIsInUse_packageJsonContainsFlowFrontend_removeFlowFrontend()
            throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        JsonNode packJsonNode = getPackageJson(packageJson);
        ObjectNode deps = (ObjectNode) packJsonNode.get(DEPENDENCIES);
        deps.put(DEP_NAME_FLOW_JARS, "foobar");

        writePackageJson(packageJson, packJsonNode);

        packageUpdater.execute();

        assertPackageJsonFlowDeps();
    }

    @Test
    public void npmIsInUse_packageLockJsonContainsNonPMPMDeps_packageLockNotRemoved()
            throws IOException {
        // use package updater with disabled PNPM
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        Files.write(packageLock.toPath(), Collections.singletonList("{  }"));

        packageUpdater.execute();
        Assert.assertTrue(packageLock.exists());
    }

    // Some npm-dependency pinning tests are in TaskRunNpmInstallTest

    @Test
    public void unmatchedDevDependency_devDependencyIsRemoved()
            throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        // Change the version
        JsonNode json = packageUpdater.getPackageJson();
        final String old_dependency = "old_dependency";
        ((ObjectNode) json.get(VAADIN_DEP_KEY).get(DEV_DEPENDENCIES))
                .put(old_dependency, "1.1.1");
        ((ObjectNode) json.get(DEV_DEPENDENCIES)).put(old_dependency, "1.1.1");

        Files.write(packageJson.toPath(),
                Collections.singletonList(json.toString()));

        // run it again with existing generated package.json and mismatched
        // versions
        packageUpdater.execute();

        json = packageUpdater.getPackageJson();
        Assert.assertFalse("Old dev dependency should be removed from vaadin",
                json.get(VAADIN_DEP_KEY).get(DEV_DEPENDENCIES)
                        .has(old_dependency));
        Assert.assertFalse(
                "Old dev dependency should be removed from devDependencies",
                json.get(DEV_DEPENDENCIES).has(old_dependency));
    }

    @Test // #10032
    public void oldVaadinDevDependency_missmatchWithDevDependency_vaadinDependencyIsUpdated()
            throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();

        // Change the version
        JsonNode json = packageUpdater.getPackageJson();
        final String key = "vite";
        final String version = packageUpdater.getDefaultDevDependencies()
                .get(key);
        ((ObjectNode) json.get(VAADIN_DEP_KEY).get(DEV_DEPENDENCIES)).put(key,
                "v2.8.0");
        ((ObjectNode) json.get(DEV_DEPENDENCIES)).put(key, version);

        Files.write(packageJson.toPath(),
                Collections.singletonList(json.toString()));

        // run it again to see that versions are updated
        packageCreator.execute();

        json = packageUpdater.getPackageJson();
        Assert.assertEquals(
                "Vaadin dependency should be updated to latest DevDependency",
                version, json.get(VAADIN_DEP_KEY).get(DEV_DEPENDENCIES).get(key)
                        .asString());
        Assert.assertEquals("DevDependency should stay the same as it was",
                version, json.get(DEV_DEPENDENCIES).get(key).asString());
    }

    @Test
    public void versionsDoNotMatch_inVaadinJson_cleanUpNpm()
            throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();

        makeNodeModulesAndPackageLock();

        packageUpdater.updateVaadinJsonContents(
                Collections.singletonMap(VAADIN_VERSION, "1.1.1"));

        FileUtils.write(versions, "{\"platform\": \"1.2.3\"}",
                StandardCharsets.UTF_8);
        packageUpdater.execute();
        assertCleanUp();
    }

    @Test
    public void versionsDoNotMatch_inVaadinJson_cleanUpPnpm()
            throws IOException {
        options.withEnablePnpm(true);
        options = options.withFrontendDependenciesScanner(
                Mockito.mock(FrontendDependencies.class));

        packageUpdater = new TaskUpdatePackages(options);

        // Generate package json in a proper format first
        packageCreator.execute();

        makeNodeModulesAndPackageLock();

        packageUpdater.updateVaadinJsonContents(
                Collections.singletonMap(VAADIN_VERSION, "1.1.1"));

        try (MockedStatic<Platform> platform = Mockito
                .mockStatic(Platform.class)) {
            platform.when(Platform::getVaadinVersion)
                    .thenReturn(Optional.of("1.2.3"));
            packageUpdater.execute();
            // nothing is removed except package-lock
            Assert.assertTrue(mainNodeModules.exists());
            // package-lock is removed
            Assert.assertFalse(packageLock.exists());
        }
    }

    @Test
    public void versionsMatch_noCleanUp() throws IOException {
        // TODO: Fixme
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        packages.put("@vaadin/vaadin-confirm-dialog", "1.1.4");
        packages.put("@vaadin/vaadin-checkbox", "2.2.10");
        packages.put("@polymer/iron-icon", "3.0.1");
        packages.put("@vaadin/vaadin-time-picker", "2.0.2");
        // packages.put(VAADIN_CORE, "1.1.1");

        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        // Generate package json in a proper format first
        packageCreator.execute();

        makeNodeModulesAndPackageLock();

        Files.write(packageLock.toPath(),
                Collections.singletonList(makePackageLock("1.1.1").toString()));

        packageUpdater.execute();

        // nothing is removed
        Assert.assertTrue(mainNodeModules.exists());
        Assert.assertTrue(packageLock.exists());
    }

    @Test
    public void versionsMatch_forceCleanUp_cleanUp() throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        makeNodeModulesAndPackageLock();

        ClassFinder classFinder = getClassFinder();
        // create a new package updater, with forced clean up enabled
        options.enableNpmFileCleaning(true);
        packageUpdater = new TaskUpdatePackages(options);
        packageUpdater.execute();

        // clean up happened
        assertCleanUp();
    }

    @Test
    public void generatePackageJson_sameDependencies_updaterIsNotModified() {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        packages.put("@vaadin/vaadin-confirm-dialog", "1.1.4");
        packages.put("@vaadin/vaadin-checkbox", "2.2.10");
        packages.put("@polymer/iron-icon", "3.0.1");
        packages.put("@vaadin/vaadin-time-picker", "2.0.2");

        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        packageCreator.execute();
        packageUpdater.execute();

        // generate it one more time, the content will be different since
        // packageCreator has not added its content
        packageUpdater.execute();

        Assert.assertFalse(
                "Modification flag should be false when no dependencies changed.",
                packageUpdater.modified);
    }

    @Test
    public void generatePackageJson_sameDependenciesInDifferentOrder_updaterIsNotModified()
            throws IOException {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        packages.put("@vaadin/vaadin-confirm-dialog", "1.1.4");
        packages.put("@vaadin/vaadin-checkbox", "2.2.10");
        packages.put("@polymer/iron-icon", "3.0.1");
        packages.put("@vaadin/vaadin-time-picker", "2.0.2");

        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        packageCreator.execute();
        packageUpdater.execute();

        // Shuffle the dependencies.
        ObjectNode json = (ObjectNode) getPackageJson(this.packageJson);
        JsonNode dependencies = json.get(DEPENDENCIES);
        List<String> dependencyKeys = JacksonUtils.getKeys(dependencies);

        Collections.shuffle(dependencyKeys);

        ObjectNode newDependencies = JacksonUtils.createObjectNode();
        dependencyKeys.forEach(key -> newDependencies.put(key,
                dependencies.get(key).asString()));

        json.set(DEPENDENCIES, newDependencies);

        Files.write(this.packageJson.toPath(),
                Collections.singletonList(json.toString()));

        // generate it one more time, the content will be different since
        // packageCreator has not added its content
        packageUpdater.execute();

        Assert.assertFalse(
                "Modification flag should be false when no dependencies changed.",
                packageUpdater.modified);
    }

    @Test
    public void generatePackageJson_removedDependencies_updaterIsModified() {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        packages.put("@vaadin/vaadin-confirm-dialog", "1.1.4");
        packages.put("@vaadin/vaadin-checkbox", "2.2.10");
        packages.put("@polymer/iron-icon", "3.0.1");
        packages.put("@vaadin/vaadin-time-picker", "2.0.2");

        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        packageCreator.execute();
        packageUpdater.execute();

        packages.remove("@vaadin/vaadin-checkbox");

        // generate it one more time, the content will be different since
        // packageCreator has not added its content
        packageUpdater.execute();

        Assert.assertTrue(
                "Modification flag should be true when dependency removed.",
                packageUpdater.modified);
    }

    @Test
    public void generatePackageJson_addedDependencies_updaterIsModified() {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        packages.put("@vaadin/vaadin-confirm-dialog", "1.1.4");
        packages.put("@vaadin/vaadin-checkbox", "2.2.10");
        packages.put("@polymer/iron-icon", "3.0.1");
        packages.put("@vaadin/vaadin-time-picker", "2.0.2");

        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        packageCreator.execute();
        packageUpdater.execute();

        packages.put("@vaadin/vaadin-list-box", "1.1.1");

        packageUpdater.execute();

        Assert.assertTrue(
                "Modification flag should be true when dependency added.",
                packageUpdater.modified);
    }

    @Test
    public void generatePackageJson_noDependencies_updaterIsNotModified() {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        packageCreator.execute();
        packageUpdater.execute();

        Assert.assertTrue(
                "Modification flag should be true as we have added default dependencies.",
                packageUpdater.modified);

        // generate it one more time
        packageUpdater.execute();

        Assert.assertFalse(
                "Modification flag should be false when there has never been dependencies.",
                packageUpdater.modified);
    }

    @Test
    public void updatedPackageJson_noDependencies_creatorAndUpdatedIsMarkedModified() {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        packageCreator.execute();
        packageUpdater.execute();

        Assert.assertTrue(
                "Modification flag should be true when main package was created.",
                packageCreator.modified);
        Assert.assertTrue(
                "Modification flag should be true as we should have updated the hash for default dependencies.",
                packageUpdater.modified);
    }

    @Test
    public void userAddedDependencies_notCleanedByUpdater() throws IOException {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        packages.put("@vaadin/vaadin-confirm-dialog", "1.1.4");
        packages.put("@vaadin/vaadin-checkbox", "2.2.10");
        packages.put("@polymer/iron-icon", "3.0.1");
        packages.put("@vaadin/vaadin-time-picker", "2.0.2");

        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        packageCreator.execute();
        JsonNode json = getPackageJson(packageJson);
        ((ObjectNode) json.get(DEPENDENCIES)).put("@custom/timer", "3.3.0");

        Files.write(packageJson.toPath(),
                Collections.singletonList(json.toString()));

        packageUpdater.execute();

        JsonNode dependencies = getPackageJson(packageJson).get(DEPENDENCIES);
        Assert.assertTrue(dependencies.has("@custom/timer"));
        Assert.assertEquals("3.3.0",
                dependencies.get("@custom/timer").asString());
    }

    @Test
    public void legacyPackageJson_isCleanedCorrectly_pnpm() throws IOException {
        String legacyPackageContent = getLegacyPackageContent();

        Files.write(packageJson.toPath(),
                Collections.singletonList(legacyPackageContent));
        options.withEnablePnpm(true);

        packageUpdater = new TaskUpdatePackages(options);
        packageUpdater.execute();

        assertPackageJsonFlowDeps();
    }

    @Test
    public void legacyPackageJson_isCleanedCorrectly_npm() throws IOException {
        String legacyPackageContent = getLegacyPackageContent();

        Files.write(packageJson.toPath(),
                Collections.singletonList(legacyPackageContent));

        packageUpdater = new TaskUpdatePackages(options);
        packageUpdater.execute();

        assertPackageJsonFlowDeps();
    }

    @Test
    public void frameworkManagedPackages_versionsAreUpdated()
            throws IOException {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        packages.put("@vaadin/vaadin-confirm-dialog", "1.1.4");
        packages.put("@vaadin/vaadin-checkbox", "2.2.10");
        packages.put("@polymer/iron-icon", "3.0.1");
        packages.put("@vaadin/vaadin-time-picker", "2.0.2");

        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        packageCreator.execute();
        packageUpdater.execute();

        JsonNode dependencies = getPackageJson(packageJson).get(DEPENDENCIES);
        for (Map.Entry<String, String> entry : packages.entrySet()) {
            Assert.assertTrue(dependencies.has(entry.getKey()));
            Assert.assertEquals(entry.getValue(),
                    dependencies.get(entry.getKey()).asString());
        }

        packages.clear();
        packages.put("@polymer/iron-list", "3.1.0");
        packages.put("@vaadin/vaadin-confirm-dialog", "2.0.0");
        packages.put("@vaadin/vaadin-checkbox", "3.1.1");
        packages.put("@polymer/iron-icon", "3.0.3");
        packages.put("@vaadin/vaadin-time-picker", "2.0.3");

        packageUpdater.execute();

        dependencies = getPackageJson(packageJson).get(DEPENDENCIES);
        for (Map.Entry<String, String> entry : packages.entrySet()) {
            Assert.assertTrue(dependencies.has(entry.getKey()));
            Assert.assertEquals(entry.getValue(),
                    dependencies.get(entry.getKey()).asString());
        }
    }

    @Test
    public void removedFrameworkDependencies_dependencyIsRemoved()
            throws IOException {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        packages.put("@vaadin/vaadin-confirm-dialog", "1.1.4");
        packages.put("@vaadin/vaadin-checkbox", "2.2.10");
        packages.put("@polymer/iron-icon", "3.0.1");
        packages.put("@vaadin/vaadin-time-picker", "2.0.2");

        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        packageCreator.execute();
        packageUpdater.execute();

        packages.remove("@vaadin/vaadin-checkbox");

        JsonNode dependencies = getPackageJson(packageJson).get(DEPENDENCIES);
        Assert.assertTrue("vaadin-checkbox is missing from the dependencies",
                dependencies.has("@vaadin/vaadin-checkbox"));

        dependencies = getPackageJson(packageJson).get(VAADIN_DEP_KEY)
                .get(DEPENDENCIES);
        Assert.assertTrue("vaadin-checkbox is missing from vaadin.dependencies",
                dependencies.has("@vaadin/vaadin-checkbox"));

        // generate it one more time, this should remove the checkbox
        packageUpdater.execute();

        dependencies = getPackageJson(packageJson).get(DEPENDENCIES);
        Assert.assertFalse(
                "vaadin-checkbox is still available in the dependencies",
                dependencies.has("@vaadin/vaadin-checkbox"));

        dependencies = getPackageJson(packageJson).get(VAADIN_DEP_KEY)
                .get(DEPENDENCIES);
        Assert.assertFalse(
                "vaadin-checkbox is still available in vaadin.dependencies",
                dependencies.has("@vaadin/vaadin-checkbox"));

    }

    public String getLegacyPackageContent() {
        return "{\n" + "  \"name\": \"no-name\",\n"
                + "  \"license\": \"UNLICENSED\",\n" + "  \"vaadin\": {\n"
                + "    \"dependencies\": {\n"
                + "      \"@vaadin/router\": \"^1.6.0\",\n"
                + "      \"@polymer/polymer\": \"3.4.1\",\n"
                + "      \"@vaadin/vaadin-ordered-layout\": \"1.1.0\",\n"
                + "      \"@vaadin/vaadin-combo-box\": \"5.0.11\",\n"
                + "      \"@vaadin/vaadin-lumo-styles\": \"1.6.0\",\n"
                + "      \"@vaadin/vaadin-material-styles\": \"1.3.2\"\n"
                + "    },\n" + "    \"devDependencies\": {\n"
                + "      \"webpack-dev-server\": \"3.10.3\"\n" + "    },\n"
                + "    \"hash\": \"72bdea1adb5aa0d1259db10a6f76872d996db31d2c312d0c7849eb39de92835e\"\n"
                + "  },\n" + "  \"dependencies\": {\n"
                + "    \"@vaadin/router\": \"^1.6.0\",\n"
                + "    \"@polymer/polymer\": \"3.4.1\",\n"
                + "    \"@vaadin/flow-deps\": \"./target/frontend\",\n"
                + "    \"@vaadin/vaadin-ordered-layout\": \"1.1.0\",\n"
                + "    \"@vaadin/vaadin-combo-box\": \"5.0.11\",\n"
                + "    \"@vaadin/vaadin-lumo-styles\": \"1.6.0\",\n"
                + "    \"@vaadin/vaadin-material-styles\": \"1.3.2\"\n"
                + "  },\n" + "  \"devDependencies\": {\n"
                + "    \"webpack-dev-server\": \"3.10.3\"\n" + "  },\n"
                + "\"vaadinAppPackageHash\": \"e05bfd4b6c6bd20c806b3a0ad1be521bfd775c9b6f8f9c997b0ad1fda834805b\"\n"
                + "}\n";
    }

    private void makeNodeModulesAndPackageLock() throws IOException {
        // Make two node_modules folders and package lock
        mainNodeModules.mkdirs();
        Files.write(packageLock.toPath(), Collections.singletonList("{}"));

        // self control
        Assert.assertTrue(mainNodeModules.exists());
        Assert.assertTrue(packageLock.exists());
    }

    private void assertCleanUp() {
        Assert.assertFalse(mainNodeModules.exists());
        Assert.assertTrue("package-lock should not be removed",
                packageLock.exists());
    }

    private void assertMainPackageJsonContent() throws IOException {
        JsonNode json = packageUpdater.getPackageJson();
        Assert.assertTrue(json.has("name"));
        Assert.assertTrue(json.has("license"));

        JsonNode dependencies = json.get(DEPENDENCIES);
        for (Map.Entry<String, String> entry : packageUpdater
                .getDefaultDependencies().entrySet()) {
            Assert.assertTrue("Missing '" + entry.getKey() + "' package",
                    dependencies.has(entry.getKey()));
        }

        JsonNode devDependencies = json.get(DEV_DEPENDENCIES);
        for (Map.Entry<String, String> entry : packageUpdater
                .getDefaultDevDependencies().entrySet()) {
            Assert.assertTrue("Missing '" + entry.getKey() + "' package",
                    devDependencies.has(entry.getKey()));
        }

        Assert.assertFalse(dependencies.has(DEP_NAME_FLOW_JARS));
    }

    private ObjectNode getDependencies(JsonNode json) {
        return (ObjectNode) json.get(DEPENDENCIES);
    }

    private ObjectNode makePackageLock(String version) {
        ObjectNode object = JacksonUtils.createObjectNode();
        JsonNode deps = JacksonUtils.createObjectNode();
        ObjectNode shrinkWrap = JacksonUtils.createObjectNode();
        object.set(DEPENDENCIES, deps);
        shrinkWrap.put("version", version);
        return object;
    }

    private void assertPackageJsonFlowDeps() throws IOException {
        JsonNode packJsonNode = getPackageJson(packageJson);
        JsonNode deps = packJsonNode.get(DEPENDENCIES);
        // No Flow deps
        Assert.assertFalse(deps.has(DEP_NAME_FLOW_DEPS));
        // No Flow resources
        Assert.assertFalse(deps.has(DEP_NAME_FLOW_JARS));
        // No old package hash
        Assert.assertFalse(deps.has(VAADIN_APP_PACKAGE_HASH));
        // No initially generated default polymer dep
        Assert.assertFalse(deps.has("@polymer/polymer"));
        // Contains new hash
        Assert.assertTrue(packJsonNode.get("vaadin").has("hash"));
    }

    JsonNode getPackageJson(File packageFile) throws IOException {
        JsonNode packageJson = null;
        if (packageFile.exists()) {
            String fileContent = FileUtils.readFileToString(packageFile,
                    UTF_8.name());
            packageJson = JacksonUtils.readTree(fileContent);
        }
        return packageJson;
    }

    void writePackageJson(File packageJsonFile, JsonNode packageJson)
            throws IOException {
        FileIOUtils.writeIfChanged(packageJsonFile, packageJson.toString());
    }

    @Test
    public void generatePackageJson_userOverridesChanged_updaterIsNotModified()
            throws IOException {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        // Generate initial state
        packageCreator.execute();
        packageUpdater.execute();

        // Run again to establish baseline (not modified)
        packageUpdater.execute();
        assertFalse("Modification flag should be false after second run.",
                packageUpdater.modified);

        // User adds an override to package.json
        ObjectNode json = (ObjectNode) getPackageJson(packageJson);
        ObjectNode overrides = JacksonUtils.createObjectNode();
        overrides.put("some-dep", "$some-dep");
        json.set(OVERRIDES, overrides);
        writePackageJson(packageJson, json);

        // Run again - should detect change due to overrides
        packageUpdater.execute();

        assertTrue("Modification flag should be true when user overrides are added.",
                packageUpdater.modified);
        // Verify the user's override is not removed
        json = (ObjectNode) getPackageJson(packageJson);
        overrides = (ObjectNode) json.get("overrides");
        assertNotNull(overrides);
        assertEquals("$some-dep", overrides.get("some-dep").asString());
    }

    @Test
    public void generatePackageJson_vaadinOverridesChanged_updaterIsModified()
            throws IOException {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        // Generate initial state
        packageCreator.execute();
        packageUpdater.execute();

        // User adds an override to package.json
        ObjectNode json = (ObjectNode) getPackageJson(packageJson);
        ObjectNode overrides = JacksonUtils.createObjectNode();
        overrides.put("some-dep", "$some-dep");
        ObjectNode nestedOverride = JacksonUtils.createObjectNode();
        nestedOverride.put("nested-dep", "1.0.0");
        overrides.set("parent-package", nestedOverride);
        json.set(OVERRIDES, overrides);
        writePackageJson(packageJson, json);

        // Run again to establish baseline (not modified)
        packageUpdater.execute();

        // Simulate Vaadin overrides change
        packageUpdater = new TaskUpdatePackages(options) {
            @Override
            ObjectNode getDefaultOverrides() {
                final ObjectNode defaultOverrides = super.getDefaultOverrides();
                defaultOverrides.put("@vaadin/flat-override", "2.0.0");
                defaultOverrides.set("@vaadin/nested-override",
                        JacksonUtils.createObjectNode().put(".", "3.0.0")
                                .put("dep", "4.0.0"));
                return defaultOverrides;
            }
        };
        // Run again - should detect change due to overrides
        packageUpdater.execute();

        assertTrue("Modification flag should be true when Vaadin overrides are added.",
                packageUpdater.modified);

        // Run again - should detect no change
        packageUpdater.execute();

        assertFalse("Modification flag should be false when overrides are unchanged.",
                packageUpdater.modified);

        // Verify the user's override is not removed
        json = (ObjectNode) getPackageJson(packageJson);
        overrides = (ObjectNode) json.get("overrides");
        assertNotNull(overrides);
        assertEquals("Flat user override should be preserved",
                "$some-dep", overrides.get("some-dep").asString());
        assertTrue("Nested user override should remain an object",
                overrides.get("parent-package").isObject());
        assertEquals("Nested user override value should be preserved",
                "1.0.0",
                overrides.get("parent-package").get("nested-dep").asString());

        // Verify Vaadin overrides are present
        assertEquals(StringNode.valueOf("2.0.0"),
                overrides.get("@vaadin/flat-override"));
        assertEquals(StringNode.valueOf("3.0.0"), JacksonUtils.getNestedKey(
                overrides, List.of("@vaadin/nested-override", ".")));
        assertEquals(StringNode.valueOf("4.0.0"), JacksonUtils.getNestedKey(
                overrides, List.of("@vaadin/nested-override", "dep")));
        // Verify Vaadin overrides are present
        assertEquals(StringNode.valueOf("2.0.0"), JacksonUtils.getNestedKey(
                json, List.of("vaadin", "overrides", "@vaadin/flat-override")));
        assertEquals(StringNode.valueOf("3.0.0"),
                JacksonUtils.getNestedKey(json, List.of("vaadin", "overrides",
                        "@vaadin/nested-override", ".")));
        assertEquals(StringNode.valueOf("4.0.0"),
                JacksonUtils.getNestedKey(json, List.of("vaadin", "overrides",
                        "@vaadin/nested-override", "dep")));
    }

    @Test
    public void generatePackageJson_sameOverrides_updaterIsNotModified()
            throws IOException {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options) {
            @Override
            ObjectNode getDefaultOverrides() {
                final ObjectNode defaultOverrides = super.getDefaultOverrides();
                defaultOverrides.put("@vaadin/flat-override", "2.0.0");
                defaultOverrides.set("@vaadin/nested-override",
                        JacksonUtils.createObjectNode().put(".", "3.0.0")
                                .put("dep", "4.0.0"));
                return defaultOverrides;
            }
        };

        // Generate initial state
        packageCreator.execute();
        packageUpdater.execute();

        // Add multiple types of overrides: flat, nested, and mixed
        ObjectNode json = (ObjectNode) getPackageJson(packageJson);
        ObjectNode overrides = JacksonUtils.createObjectNode();
        overrides.put("some-dep", "$some-dep");
        ObjectNode nestedOverride = JacksonUtils.createObjectNode();
        nestedOverride.put("nested-dep", "1.0.0");
        overrides.set("parent-package", nestedOverride);
        json.set(OVERRIDES, overrides);
        writePackageJson(packageJson, json);

        // Run to register the override
        packageUpdater.execute();

        // Capture vaadin.overrides state after first run
        json = (ObjectNode) getPackageJson(packageJson);
        JsonNode vaadinOverridesAfterFirstRun = json.get(VAADIN_DEP_KEY)
                .get(OVERRIDES);
        assertNotNull("Vaadin overrides should be preserved.",
                vaadinOverridesAfterFirstRun);

        // Run again with same overrides - should not be modified
        packageUpdater.execute();

        assertFalse("Modification flag should be false when overrides are unchanged.",
                packageUpdater.modified);

        // Verify user overrides are preserved
        json = (ObjectNode) getPackageJson(packageJson);
        overrides = (ObjectNode) json.get(OVERRIDES);
        assertNotNull(overrides);
        assertEquals("Flat user override should be preserved",
                "$some-dep", overrides.get("some-dep").asString());
        assertTrue("Nested user override should remain an object",
                overrides.get("parent-package").isObject());
        assertEquals("Nested user override value should be preserved",
                "1.0.0",
                overrides.get("parent-package").get("nested-dep").asString());

        // Run a third time to ensure stability
        packageUpdater.execute();
        assertFalse("Modification flag should remain false on third run.",
                packageUpdater.modified);

        // Verify vaadin.overrides consistency across runs
        json = (ObjectNode) getPackageJson(packageJson);
        JsonNode vaadinOverridesAfterThirdRun = json.get(VAADIN_DEP_KEY) != null
                ? json.get(VAADIN_DEP_KEY).get(OVERRIDES)
                : null;
        assertTrue("vaadin.overrides should remain consistent across runs",
                JacksonUtils.jsonEquals(vaadinOverridesAfterFirstRun,
                        vaadinOverridesAfterThirdRun));

        // Run a fourth time to ensure long-term stability
        packageUpdater.execute();
        assertFalse("Modification flag should remain false on fourth run.",
                packageUpdater.modified);
    }

    @Test
    public void generatePackageJson_userModifiesVaadinOverride_optOut()
            throws IOException {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options) {
            @Override
            ObjectNode getDefaultOverrides() {
                final ObjectNode defaultOverrides = super.getDefaultOverrides();
                defaultOverrides.put("@vaadin/flat-override", "2.0.0");
                defaultOverrides.set("@vaadin/nested-override",
                        JacksonUtils.createObjectNode().put(".", "3.0.0")
                                .put("dep", "4.0.0"));
                return defaultOverrides;
            }
        };

        packageCreator.execute();
        packageUpdater.execute();

        // Get initial state - Vaadin adds platform overrides
        ObjectNode json = (ObjectNode) getPackageJson(packageJson);
        ObjectNode overrides = (ObjectNode) json.get(OVERRIDES);

        // Verify Vaadin added an override for a platform dependency
        assertNotNull("Expected an override for Vaadin managed dependency",
                overrides.get("@polymer/iron-list"));

        // User modifies the Vaadin-managed overrides
        overrides.put("@polymer/iron-list", "99.99.1");
        overrides.put("@vaadin/flat-override", "99.99.2");
        JacksonUtils.setNestedKey(overrides,
                List.of("@vaadin/nested-override", "dep"),
                StringNode.valueOf("99.99.3"),
                (plainValue) -> JacksonUtils.createObjectNode());
        writePackageJson(packageJson, json);

        // Run updater again
        packageUpdater.execute();

        // Verify the user's modification is preserved (opt-out behavior)
        json = (ObjectNode) getPackageJson(packageJson);
        overrides = (ObjectNode) json.get(OVERRIDES);
        assertEquals("User's modified override should be preserved (opted out)",
                "99.99.1", overrides.get("@polymer/iron-list").asString());
        assertEquals("User's modified override should be preserved (opted out)",
                "99.99.2",
                overrides.get("@vaadin/flat-override").asString());
        assertEquals("User's modified override should be preserved (opted out)",
                StringNode.valueOf("99.99.3"),
                JacksonUtils.getNestedKey(overrides,
                        List.of("@vaadin/nested-override", "dep")));
    }

    @Test
    public void generatePackageJson_mixedOptOutAndUpdates_handledCorrectly()
            throws IOException {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        packages.put("@polymer/paper-button", "3.0.2");
        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);
        options = options.withFrontendDependenciesScanner(frontendDependencies);

        packageUpdater = new TaskUpdatePackages(options);

        packageCreator.execute();
        packageUpdater.execute();

        // Set up initial state with multiple user overrides
        ObjectNode json = (ObjectNode) getPackageJson(packageJson);
        if (!json.has(OVERRIDES)) {
            json.set(OVERRIDES, JacksonUtils.createObjectNode());
        }
        ObjectNode overrides = (ObjectNode) json.get(OVERRIDES);
        overrides.put("dep-1", "1.0.0");
        overrides.put("dep-2", "2.0.0");
        overrides.put("user-dep", "3.0.0");
        writePackageJson(packageJson, json);

        // Register the overrides
        packageUpdater.execute();

        // User modifies one Vaadin override but leaves another alone
        json = (ObjectNode) getPackageJson(packageJson);
        overrides = (ObjectNode) json.get(OVERRIDES);
        overrides.put("@polymer/iron-list", "99.0.0");
        writePackageJson(packageJson, json);

        // Run updater
        packageUpdater.execute();

        // Verify outcomes:
        json = (ObjectNode) getPackageJson(packageJson);
        overrides = (ObjectNode) json.get(OVERRIDES);

        assertEquals("User-modified override should be preserved (opted out)",
                "99.0.0", overrides.get("@polymer/iron-list").asString());

        assertEquals("User's own override should always be preserved",
                "3.0.0", overrides.get("user-dep").asString());

        // Verify stability on subsequent runs
        packageUpdater.execute();
        json = (ObjectNode) getPackageJson(packageJson);
        overrides = (ObjectNode) json.get(OVERRIDES);
        assertEquals("Opted-out override should remain stable",
                "99.0.0", overrides.get("@polymer/iron-list").asString());
        assertEquals("User override should remain stable",
                "3.0.0", overrides.get("user-dep").asString());
    }

}
