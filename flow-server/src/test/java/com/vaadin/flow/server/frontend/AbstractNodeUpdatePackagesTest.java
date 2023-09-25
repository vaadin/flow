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
 *
 */

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
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

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.Platform;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.testutil.FrontendStubs;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEP_NAME_FLOW_DEPS;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEP_NAME_FLOW_JARS;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static com.vaadin.flow.server.frontend.TaskUpdatePackages.VAADIN_APP_PACKAGE_HASH;
import static elemental.json.impl.JsonUtil.stringify;
import static java.nio.charset.StandardCharsets.UTF_8;

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
        options = new Options(Mockito.mock(Lookup.class), baseDir)
                .withBuildDirectory(TARGET).withBundleBuild(true);
        // .withJarFrontendResourcesFolder(jarResourceFolder);
        packageCreator = new TaskGeneratePackageJson(options);

        classFinder = Mockito.spy(getClassFinder());
        versions = temporaryFolder.newFile();
        FileUtils.write(versions, "{}", StandardCharsets.UTF_8);
        Mockito.when(
                classFinder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versions.toURI().toURL());

        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), options);
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
        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), options);
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        // Add flowDeps
        JsonObject json = packageUpdater.getPackageJson();
        getDependencies(json).put(DEP_NAME_FLOW_DEPS, "target/frontend");
        json.put(VAADIN_APP_PACKAGE_HASH,
                "e05bfd4b6c6bd20c806b3a0ad1be521bfd775c9b6f8f9c997b0ad1fda834805b");
        Files.write(packageJson.toPath(),
                Collections.singletonList(json.toJson()));

        options.withEnablePnpm(true);
        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), options);
        packageUpdater.execute();

        assertPackageJsonFlowDeps();
    }

    @Test
    public void pnpmIsInUse_packageJsonContainsFlowFrontend_removeFlowFrontend()
            throws IOException {
        // use package updater with disabled PNPM
        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), options);
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        // Add old dep
        JsonObject json = packageUpdater.getPackageJson();
        getDependencies(json).put(DEP_NAME_FLOW_JARS, "target/flow-frontend");
        json.put(VAADIN_APP_PACKAGE_HASH,
                "e05bfd4b6c6bd20c806b3a0ad1be521bfd775c9b6f8f9c997b0ad1fda834805b");
        Files.write(packageJson.toPath(),
                Collections.singletonList(json.toJson()));

        options.withEnablePnpm(true);
        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), options);
        packageUpdater.execute();

        assertPackageJsonFlowDeps();
    }

    @Test
    public void pnpmIsInUse_packageLockExists_removePackageLock()
            throws IOException {
        // use package updater with disabled PNPM
        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), options);
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        Files.write(packageLock.toPath(), Collections.singletonList("{}"));

        options.withEnablePnpm(true);

        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), options);
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

        JsonObject packJsonObject = getPackageJson(packageJson);
        JsonObject deps = packJsonObject.get(DEPENDENCIES);
        deps.put(DEP_NAME_FLOW_DEPS, "foobar");

        writePackageJson(packageJson, packJsonObject);

        packageUpdater.execute();

        assertPackageJsonFlowDeps();
    }

    @Test
    public void npmIsInUse_packageJsonContainsFlowFrontend_removeFlowFrontend()
            throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        JsonObject packJsonObject = getPackageJson(packageJson);
        JsonObject deps = packJsonObject.get(DEPENDENCIES);
        deps.put(DEP_NAME_FLOW_JARS, "foobar");

        writePackageJson(packageJson, packJsonObject);

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
        JsonObject json = packageUpdater.getPackageJson();
        final String old_dependency = "old_dependency";
        json.getObject(VAADIN_DEP_KEY).getObject(DEV_DEPENDENCIES)
                .put(old_dependency, "1.1.1");
        json.getObject(DEV_DEPENDENCIES).put(old_dependency, "1.1.1");

        Files.write(packageJson.toPath(),
                Collections.singletonList(json.toJson()));

        // run it again with existing generated package.json and mismatched
        // versions
        packageUpdater.execute();

        json = packageUpdater.getPackageJson();
        Assert.assertFalse("Old dev dependency should be removed from vaadin",
                json.getObject(VAADIN_DEP_KEY).getObject(DEV_DEPENDENCIES)
                        .hasKey(old_dependency));
        Assert.assertFalse(
                "Old dev dependency should be removed from devDependencies",
                json.getObject(DEV_DEPENDENCIES).hasKey(old_dependency));
    }

    @Test // #10032
    public void oldVaadinDevDependency_missmatchWithDevDependency_vaadinDependencyIsUpdated()
            throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();

        // Change the version
        JsonObject json = packageUpdater.getPackageJson();
        final String key = "vite";
        final String version = packageUpdater.getDefaultDevDependencies()
                .get(key);
        json.getObject(VAADIN_DEP_KEY).getObject(DEV_DEPENDENCIES).put(key,
                "v2.8.0");
        json.getObject(DEV_DEPENDENCIES).put(key, version);

        Files.write(packageJson.toPath(),
                Collections.singletonList(json.toJson()));

        // run it again to see that versions are updated
        packageCreator.execute();

        json = packageUpdater.getPackageJson();
        Assert.assertEquals(
                "Vaadin dependency should be updated to latest DevDependency",
                version, json.getObject(VAADIN_DEP_KEY)
                        .getObject(DEV_DEPENDENCIES).getString(key));
        Assert.assertEquals("DevDependency should stay the same as it was",
                version, json.getObject(DEV_DEPENDENCIES).getString(key));
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

        packageUpdater = new TaskUpdatePackages(classFinder,
                Mockito.mock(FrontendDependencies.class), options);

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

        packageUpdater = new TaskUpdatePackages(classFinder,
                frontendDependencies, options);

        // Generate package json in a proper format first
        packageCreator.execute();

        makeNodeModulesAndPackageLock();

        Files.write(packageLock.toPath(),
                Collections.singletonList(stringify(makePackageLock("1.1.1"))));

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
        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), options);
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

        packageUpdater = new TaskUpdatePackages(classFinder,
                frontendDependencies, options);

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

        packageUpdater = new TaskUpdatePackages(classFinder,
                frontendDependencies, options);

        packageCreator.execute();
        packageUpdater.execute();

        // Shuffle the dependencies.
        JsonObject json = getPackageJson(this.packageJson);
        JsonObject dependencies = json.getObject(DEPENDENCIES);
        List<String> dependencyKeys = Arrays.asList(dependencies.keys());

        Collections.shuffle(dependencyKeys);

        JsonObject newDependencies = Json.createObject();
        dependencyKeys.forEach(
                key -> newDependencies.put(key, dependencies.getString(key)));

        json.put(DEPENDENCIES, newDependencies);

        Files.write(this.packageJson.toPath(),
                Collections.singletonList(stringify(json)));

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

        packageUpdater = new TaskUpdatePackages(classFinder,
                frontendDependencies, options);

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

        packageUpdater = new TaskUpdatePackages(classFinder,
                frontendDependencies, options);

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

        packageUpdater = new TaskUpdatePackages(classFinder,
                frontendDependencies, options);

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

        packageUpdater = new TaskUpdatePackages(classFinder,
                frontendDependencies, options);

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

        packageUpdater = new TaskUpdatePackages(classFinder,
                frontendDependencies, options);

        packageCreator.execute();
        JsonObject json = getPackageJson(packageJson);
        json.getObject(DEPENDENCIES).put("@custom/timer", "3.3.0");

        Files.write(packageJson.toPath(),
                Collections.singletonList(json.toJson()));

        packageUpdater.execute();

        JsonObject dependencies = getPackageJson(packageJson)
                .getObject(DEPENDENCIES);
        Assert.assertTrue(dependencies.hasKey("@custom/timer"));
        Assert.assertEquals("3.3.0", dependencies.getString("@custom/timer"));
    }

    @Test
    public void legacyPackageJson_isCleanedCorrectly_pnpm() throws IOException {
        String legacyPackageContent = getLegacyPackageContent();

        Files.write(packageJson.toPath(),
                Collections.singletonList(legacyPackageContent));
        options.withEnablePnpm(true);

        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), options);
        packageUpdater.execute();

        assertPackageJsonFlowDeps();
    }

    @Test
    public void legacyPackageJson_isCleanedCorrectly_npm() throws IOException {
        String legacyPackageContent = getLegacyPackageContent();

        Files.write(packageJson.toPath(),
                Collections.singletonList(legacyPackageContent));

        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), options);
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

        packageUpdater = new TaskUpdatePackages(classFinder,
                frontendDependencies, options);

        packageCreator.execute();
        packageUpdater.execute();

        JsonObject dependencies = getPackageJson(packageJson)
                .getObject(DEPENDENCIES);
        for (Map.Entry<String, String> entry : packages.entrySet()) {
            Assert.assertTrue(dependencies.hasKey(entry.getKey()));
            Assert.assertEquals(entry.getValue(),
                    dependencies.getString(entry.getKey()));
        }

        packages.clear();
        packages.put("@polymer/iron-list", "3.1.0");
        packages.put("@vaadin/vaadin-confirm-dialog", "2.0.0");
        packages.put("@vaadin/vaadin-checkbox", "3.1.1");
        packages.put("@polymer/iron-icon", "3.0.3");
        packages.put("@vaadin/vaadin-time-picker", "2.0.3");

        packageUpdater.execute();

        dependencies = getPackageJson(packageJson).getObject(DEPENDENCIES);
        for (Map.Entry<String, String> entry : packages.entrySet()) {
            Assert.assertTrue(dependencies.hasKey(entry.getKey()));
            Assert.assertEquals(entry.getValue(),
                    dependencies.getString(entry.getKey()));
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

        packageUpdater = new TaskUpdatePackages(classFinder,
                frontendDependencies, options);

        packageCreator.execute();
        packageUpdater.execute();

        packages.remove("@vaadin/vaadin-checkbox");

        JsonObject dependencies = getPackageJson(packageJson)
                .getObject(DEPENDENCIES);
        Assert.assertTrue("vaadin-checkbox is missing from the dependencies",
                dependencies.hasKey("@vaadin/vaadin-checkbox"));

        dependencies = getPackageJson(packageJson).getObject(VAADIN_DEP_KEY)
                .getObject(DEPENDENCIES);
        Assert.assertTrue("vaadin-checkbox is missing from vaadin.dependencies",
                dependencies.hasKey("@vaadin/vaadin-checkbox"));

        // generate it one more time, this should remove the checkbox
        packageUpdater.execute();

        dependencies = getPackageJson(packageJson).getObject(DEPENDENCIES);
        Assert.assertFalse(
                "vaadin-checkbox is still available in the dependencies",
                dependencies.hasKey("@vaadin/vaadin-checkbox"));

        dependencies = getPackageJson(packageJson).getObject(VAADIN_DEP_KEY)
                .getObject(DEPENDENCIES);
        Assert.assertFalse(
                "vaadin-checkbox is still available in vaadin.dependencies",
                dependencies.hasKey("@vaadin/vaadin-checkbox"));

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
        JsonObject json = packageUpdater.getPackageJson();
        Assert.assertTrue(json.hasKey("name"));
        Assert.assertTrue(json.hasKey("license"));

        JsonObject dependencies = json.getObject(DEPENDENCIES);
        for (Map.Entry<String, String> entry : packageUpdater
                .getDefaultDependencies().entrySet()) {
            Assert.assertTrue("Missing '" + entry.getKey() + "' package",
                    dependencies.hasKey(entry.getKey()));
        }

        JsonObject devDependencies = json.getObject(DEV_DEPENDENCIES);
        for (Map.Entry<String, String> entry : packageUpdater
                .getDefaultDevDependencies().entrySet()) {
            Assert.assertTrue("Missing '" + entry.getKey() + "' package",
                    devDependencies.hasKey(entry.getKey()));
        }

        Assert.assertFalse(dependencies.hasKey(DEP_NAME_FLOW_JARS));
    }

    private JsonObject getDependencies(JsonObject json) {
        return json.getObject(DEPENDENCIES);
    }

    private JsonObject makePackageLock(String version) {
        JsonObject object = Json.createObject();
        JsonObject deps = Json.createObject();
        JsonObject shrinkWrap = Json.createObject();
        object.put(DEPENDENCIES, deps);
        shrinkWrap.put("version", version);
        return object;
    }

    private void assertPackageJsonFlowDeps() throws IOException {
        JsonObject packJsonObject = getPackageJson(packageJson);
        JsonObject deps = packJsonObject.get(DEPENDENCIES);
        // No Flow deps
        Assert.assertFalse(deps.hasKey(DEP_NAME_FLOW_DEPS));
        // No Flow resources
        Assert.assertFalse(deps.hasKey(DEP_NAME_FLOW_JARS));
        // No old package hash
        Assert.assertFalse(deps.hasKey(VAADIN_APP_PACKAGE_HASH));
        // Contains initially generated default polymer dep
        Assert.assertTrue(deps.hasKey("@polymer/polymer"));
        // Contains new hash
        Assert.assertTrue(packJsonObject.getObject("vaadin").hasKey("hash"));
    }

    JsonObject getPackageJson(File packageFile) throws IOException {
        JsonObject packageJson = null;
        if (packageFile.exists()) {
            String fileContent = FileUtils.readFileToString(packageFile,
                    UTF_8.name());
            packageJson = Json.parse(fileContent);
        }
        return packageJson;
    }

    void writePackageJson(File packageJsonFile, JsonObject packageJson)
            throws IOException {
        FileUtils.writeStringToFile(packageJsonFile, packageJson.toJson(),
                UTF_8.name());
    }

}
