/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEAULT_FLOW_RESOURCES_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEP_NAME_FLOW_DEPS;
import static com.vaadin.flow.server.frontend.TaskUpdatePackages.VAADIN_APP_PACKAGE_HASH;
import static elemental.json.impl.JsonUtil.stringify;
import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractNodeUpdatePackagesTest
        extends NodeUpdateTestUtil {

    private static final String DEPENDENCIES = "dependencies";
    private static final String DEV_DEPENDENCIES = "devDependencies";

    private static final String SHRINKWRAP = "@vaadin/vaadin-shrinkwrap";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TaskUpdatePackages packageUpdater;
    private TaskGeneratePackageJson packageCreator;
    private File baseDir;
    private File generatedDir;
    private File resourcesDir;

    private File packageJson;

    private ClassFinder classFinder;

    private File mainNodeModules;
    private File packageLock;
    private File appNodeModules;


    @Before
    public void setup() throws Exception {
        baseDir = temporaryFolder.getRoot();

        generatedDir = new File(baseDir, DEFAULT_GENERATED_DIR);
        resourcesDir = new File(baseDir, DEAULT_FLOW_RESOURCES_FOLDER);

        NodeUpdateTestUtil.createStubNode(true, true, false,
                baseDir.getAbsolutePath());

        packageCreator = new TaskGeneratePackageJson(baseDir, generatedDir, resourcesDir);

        classFinder = getClassFinder();
        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), baseDir, generatedDir, resourcesDir, false, false);
        packageJson = new File(baseDir, PACKAGE_JSON);

        mainNodeModules = new File(baseDir, FrontendUtils.NODE_MODULES);
        appNodeModules = new File(generatedDir, FrontendUtils.NODE_MODULES);
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
    public void versions_doNotMatch_inGeneratedPackage_cleanUp()
            throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        updateVersion();

        makeNodeModulesAndPackageLock();

        // run it again with existing generated package.json and mismatched
        // versions
        packageUpdater.execute();

        assertVersionAndCleanUp();
    }

    @Test
    public void versions_doNotMatch_inMainPackage_cleanUp() throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        updateVersion();

        makeNodeModulesAndPackageLock();

        // Change the versions
        getDependencies(packageUpdater.getPackageJson()).put(SHRINKWRAP,
                "1.1.1");

        // run it again with existing generated package.json and mismatched
        // versions
        packageUpdater.execute();

        assertVersionAndCleanUp();
    }

    @Test
    public void pnpmIsInUse_packageJsonContainsFlowDeps_removeFlowDeps()
            throws IOException {
        // use package updater with disabled PNPM
        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), baseDir, generatedDir, null, false, false);
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        // Add flowDeps
        JsonObject json = packageUpdater.getPackageJson();
        getDependencies(json).put(DEP_NAME_FLOW_DEPS, "target/frontend");
        json.put(VAADIN_APP_PACKAGE_HASH, "e05bfd4b6c6bd20c806b3a0ad1be521bfd775c9b6f8f9c997b0ad1fda834805b");
        Files.write(packageJson.toPath(),
                Collections.singletonList(json.toJson()));

        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), baseDir, generatedDir, null, false, true);
        packageUpdater.execute();

        assertPackageJsonFlowDeps();
    }

    @Test
    public void pnpmIsInUse_packageLockExists_removePackageLock()
            throws IOException {
        // use package updater with disabled PNPM
        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), baseDir, generatedDir, null, false, false);
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        Files.write(packageLock.toPath(), Collections.singletonList("{}"));

        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), baseDir, generatedDir, null, false, true);
        packageUpdater.execute();
        Assert.assertFalse(packageLock.exists());
    }

    @Test
    public void npmIsInUse_packageJsonContainsFlowDeps_removeFlowDeps()
            throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        JsonObject packJsonObject = getPackageJson(packageJson);
        JsonObject deps = packJsonObject.get(DEPENDENCIES);

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

    /**
     * @throws IOException
     */
    private void assertVersionAndCleanUp() throws IOException {
        JsonValue value = getDependencies(packageUpdater.getPackageJson())
                .get(SHRINKWRAP);
        Assert.assertEquals("1.2.3", value.asString());

        // Check that clean up was done
        assertCleanUp();
    }

    @Test
    public void versions_doNotMatch_inFlowDepsPackage_cleanUp()
            throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        updateVersion();

        makeNodeModulesAndPackageLock();

        // Change the version
        JsonObject json = packageUpdater.getPackageJson();
        getDependencies(json).put(SHRINKWRAP, "1.1.1");
        Files.write(packageJson.toPath(),
                Collections.singletonList(json.toJson()));

        // run it again with existing generated package.json and mismatched
        // versions
        packageUpdater.execute();

        assertVersionAndCleanUp();
    }

    @Test
    public void versions_doNotMatch_inPackageLock_cleanUp() throws IOException {
        makeNodeModulesAndPackageLock();

        Files.write(packageLock.toPath(),
                Collections.singletonList(stringify(makePackageLock("1.1.1"))));

        packageCreator.execute();
        packageUpdater.execute();

        assertVersionAndCleanUp();
    }

    @Test
    public void versionsDoNotMatch_inMainJson_npm_cleanUp() throws IOException {
        versionsDoNotMatch_inMainJson_cleanUp(false);
        assertVersionAndCleanUp();
    }

    @Test
    public void versionsDoNotMatch_inMainJson_pnpm_cleanUp()
            throws IOException {
        versionsDoNotMatch_inMainJson_cleanUp(true);
        JsonValue value = getDependencies(packageUpdater.getPackageJson())
                .get(SHRINKWRAP);
        Assert.assertEquals("1.2.3", value.asString());

        // nothing is removed except package-lock
        Assert.assertTrue(mainNodeModules.exists());
        Assert.assertTrue(appNodeModules.exists());
        // package-lock is removed
        Assert.assertFalse(packageLock.exists());
    }

    @Test
    public void versionsMatch_noCleanUp() throws IOException {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        packages.put("@vaadin/vaadin-confirm-dialog", "1.1.4");
        packages.put("@vaadin/vaadin-checkbox", "2.2.10");
        packages.put("@polymer/iron-icon", "3.0.1");
        packages.put("@vaadin/vaadin-time-picker", "2.0.2");
        packages.put(SHRINKWRAP, "1.1.1");

        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, null, false, false);

        // Generate package json in a proper format first
        packageCreator.execute();

        makeNodeModulesAndPackageLock();

        Files.write(packageLock.toPath(),
                Collections.singletonList(stringify(makePackageLock("1.1.1"))));

        packageUpdater.execute();

        // nothing is removed
        Assert.assertTrue(mainNodeModules.exists());
        Assert.assertTrue(appNodeModules.exists());
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
        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), baseDir, generatedDir, null, true, false);
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

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, null, false, false);

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

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, null, false, false);

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

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, null, false, false);

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

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, null, false, false);

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

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, null, false, false);

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

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, null, false, false);

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

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, null, false, false);

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

        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), baseDir, generatedDir, null, false,
                true);
        packageUpdater.execute();

        assertPackageJsonFlowDeps();
    }

    @Test
    public void legacyPackageJson_isCleanedCorrectly_npm() throws IOException {
        String legacyPackageContent = getLegacyPackageContent();

        Files.write(packageJson.toPath(),
                Collections.singletonList(legacyPackageContent));

        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), baseDir, generatedDir, null, false,
                false);
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

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, null, false, false);

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

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, null, false, false);

        packageCreator.execute();
        packageUpdater.execute();

        packages.remove("@vaadin/vaadin-checkbox");

        JsonObject dependencies = getPackageJson(packageJson)
                .getObject(DEPENDENCIES);
        Assert.assertTrue("vaadin-checkbox is missing from the dependencies",
                dependencies.hasKey("@vaadin/vaadin-checkbox"));

        // generate it one more time, this should remove the checkbox
        packageUpdater.execute();

        dependencies = getPackageJson(packageJson).getObject(DEPENDENCIES);
        Assert.assertFalse(
                "vaadin-checkbox is still available in the dependencies",
                dependencies.hasKey("@vaadin/vaadin-checkbox"));

    }

    public String getLegacyPackageContent() {
        return "{\n" + "  \"name\": \"no-name\",\n"
                + "  \"license\": \"UNLICENSED\",\n" + "  \"vaadin\": {\n"
                + "    \"dependencies\": {\n"
                + "      \"@vaadin/router\": \"^1.6.0\",\n"
                + "      \"@polymer/polymer\": \"3.2.0\",\n"
                + "      \"@vaadin/vaadin-ordered-layout\": \"1.1.0\",\n"
                + "      \"@vaadin/vaadin-combo-box\": \"5.0.11\",\n"
                + "      \"@vaadin/vaadin-lumo-styles\": \"1.6.0\",\n"
                + "      \"@vaadin/vaadin-material-styles\": \"1.3.2\"\n"
                + "    },\n" + "    \"devDependencies\": {\n"
                + "      \"webpack-dev-server\": \"3.10.3\"\n" + "    },\n"
                + "    \"hash\": \"72bdea1adb5aa0d1259db10a6f76872d996db31d2c312d0c7849eb39de92835e\"\n"
                + "  },\n" + "  \"dependencies\": {\n"
                + "    \"@vaadin/router\": \"^1.6.0\",\n"
                + "    \"@polymer/polymer\": \"3.2.0\",\n"
                + "    \"@vaadin/flow-deps\": \"./target/frontend\",\n"
                + "    \"@vaadin/flow-frontend\": \"./target/flow-frontend\",\n"
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
        appNodeModules.mkdirs();
        Files.write(packageLock.toPath(), Collections.singletonList("{}"));

        // self control
        Assert.assertTrue(mainNodeModules.exists());
        Assert.assertTrue(appNodeModules.exists());
        Assert.assertTrue(packageLock.exists());
    }

    private void assertCleanUp() {
        Assert.assertFalse(mainNodeModules.exists());
        Assert.assertFalse(appNodeModules.exists());
        Assert.assertFalse(packageLock.exists());
    }

    private void assertMainPackageJsonContent() throws IOException {
        JsonObject json = packageUpdater.getPackageJson();
        Assert.assertTrue(json.hasKey("name"));
        Assert.assertTrue(json.hasKey("license"));

        JsonObject dependencies = json.getObject(DEPENDENCIES);
        for (Map.Entry<String, String> entry : NodeUpdater
                .getDefaultDependencies().entrySet()) {
            Assert.assertTrue("Missing '" + entry.getKey() + "' package",
                    dependencies.hasKey(entry.getKey()));
        }

        JsonObject devDependencies = json.getObject(DEV_DEPENDENCIES);
        for (Map.Entry<String, String> entry : NodeUpdater
                .getDefaultDevDependencies().entrySet()) {
            Assert.assertTrue("Missing '" + entry.getKey() + "' package",
                    devDependencies.hasKey(entry.getKey()));
        }
    }

    private JsonObject getDependencies(JsonObject json) {
        return json.getObject(DEPENDENCIES);
    }

    private void updateVersion() throws IOException {
        // Change the version
        JsonObject json = packageUpdater.getPackageJson();
        getDependencies(json).put(SHRINKWRAP, "1.1.1");
        Files.write(packageJson.toPath(),
                Collections.singletonList(stringify(json)));
    }

    private JsonObject makePackageLock(String version) {
        JsonObject object = Json.createObject();
        JsonObject deps = Json.createObject();
        JsonObject shrinkWrap = Json.createObject();
        object.put(DEPENDENCIES, deps);
        deps.put(SHRINKWRAP, shrinkWrap);
        shrinkWrap.put("version", version);
        return object;
    }

    private void versionsDoNotMatch_inMainJson_cleanUp(boolean isPnpm)
            throws IOException {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        packages.put("@polymer/iron-list", "3.0.2");
        packages.put("@vaadin/vaadin-confirm-dialog", "1.1.4");
        packages.put("@vaadin/vaadin-checkbox", "2.2.10");
        packages.put("@polymer/iron-icon", "3.0.1");
        packages.put("@vaadin/vaadin-time-picker", "2.0.2");
        packages.put(SHRINKWRAP, "1.2.3");

        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, null, false, isPnpm);

        // Generate package json in a proper format first
        packageCreator.execute();

        makeNodeModulesAndPackageLock();

        JsonObject packageJson = getPackageJson(this.packageJson);
        packageJson.put(SHRINKWRAP, "1.1.1");
        Files.write(packageLock.toPath(),
                Collections.singletonList(stringify(packageJson)));

        packageUpdater.execute();
    }

    private void assertPackageJsonFlowDeps() throws IOException {
        JsonObject packJsonObject = getPackageJson(packageJson);
        JsonObject deps = packJsonObject.get(DEPENDENCIES);
        // No Flow deps
        Assert.assertFalse(deps.hasKey(DEP_NAME_FLOW_DEPS));
        // No old package hash
        Assert.assertFalse(deps.hasKey(VAADIN_APP_PACKAGE_HASH));
        // Contains initially generated default polymer dep
        Assert.assertTrue(deps.hasKey("@polymer/polymer"));
        // Contains new hash
        Assert.assertTrue(
                packJsonObject.getObject("vaadin")
                        .hasKey("hash"));
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

}
