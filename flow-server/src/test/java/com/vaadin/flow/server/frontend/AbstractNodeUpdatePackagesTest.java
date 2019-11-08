/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import java.util.Collections;
import java.util.HashMap;
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
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.TaskUpdatePackages.APP_PACKAGE_HASH;
import static elemental.json.impl.JsonUtil.stringify;
import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractNodeUpdatePackagesTest
        extends NodeUpdateTestUtil {

    private static final String DEPENDENCIES = "dependencies";

    private static final String SHRINKWRAP = "@vaadin/vaadin-shrinkwrap";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TaskUpdatePackages packageUpdater;
    private TaskCreatePackageJson packageCreator;
    private File baseDir;
    private File generatedDir;
    private File mainPackageJson;
    private File appPackageJson;

    private File mainNodeModules;
    private File packageLock;
    private File appNodeModules;
    private File flowDepsPackageJson;

    @Before
    public void setup() throws Exception {
        baseDir = temporaryFolder.getRoot();

        generatedDir = new File(baseDir, DEFAULT_GENERATED_DIR);

        NodeUpdateTestUtil.createStubNode(true, true,
                baseDir.getAbsolutePath());

        packageCreator = new TaskCreatePackageJson(baseDir, generatedDir, null);

        ClassFinder classFinder = getClassFinder();
        packageUpdater = new TaskUpdatePackages(classFinder,
                getScanner(classFinder), baseDir, generatedDir, false);
        mainPackageJson = new File(baseDir, PACKAGE_JSON);
        appPackageJson = new File(generatedDir, PACKAGE_JSON);

        mainNodeModules = new File(baseDir, FrontendUtils.NODE_MODULES);
        appNodeModules = new File(generatedDir, FrontendUtils.NODE_MODULES);
        packageLock = new File(baseDir, "package-lock.json");

        File atVaadin = new File(mainNodeModules, "@vaadin");
        File flowDeps = new File(atVaadin, "flow-deps");

        flowDepsPackageJson = new File(flowDeps, PACKAGE_JSON);
    }

    protected abstract FrontendDependenciesScanner getScanner(
            ClassFinder finder);

    @Test
    public void should_CreatePackageJson() throws Exception {
        Assert.assertFalse(mainPackageJson.exists());
        packageCreator.execute();
        Assert.assertTrue(mainPackageJson.exists());
        Assert.assertTrue(appPackageJson.exists());
    }

    @Test
    public void should_not_ModifyPackageJson_WhenAlreadyExists()
            throws Exception {
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
        assertAppPackageJsonContent();
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
        getDependencies(packageUpdater.getAppPackageJson()).put(SHRINKWRAP,
                "1.1.1");

        // move app package json to the main package json
        mainPackageJson.delete();
        Files.move(appPackageJson.toPath(), mainPackageJson.toPath());
        appPackageJson.delete();

        // run it again with existing generated package.json and mismatched
        // versions
        packageUpdater.execute();

        assertVersionAndCleanUp();
    }

    /**
     * @throws IOException
     */
    private void assertVersionAndCleanUp() throws IOException {
        JsonValue value = getDependencies(packageUpdater.getAppPackageJson())
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
        getDependencies(packageUpdater.getAppPackageJson()).put(SHRINKWRAP,
                "1.1.1");

        // move app package json to the flow-deps package json
        flowDepsPackageJson.delete();
        Files.move(appPackageJson.toPath(), flowDepsPackageJson.toPath());
        appPackageJson.delete();

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
    public void versionsDoNotMatch_inMainJson_cleanUp() throws IOException {
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
                baseDir, generatedDir, false);

        // Generate package json in a proper format first
        packageCreator.execute();

        makeNodeModulesAndPackageLock();

        JsonObject packageJson = getPackageJson(mainPackageJson);
        packageJson.put(SHRINKWRAP, "1.1.1");
        Files.write(packageLock.toPath(),
                Collections.singletonList(stringify(packageJson)));

        packageUpdater.execute();

        assertVersionAndCleanUp();
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
                baseDir, generatedDir, false);

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
                getScanner(classFinder), baseDir, generatedDir, true);
        packageUpdater.execute();

        // clean up happened
        assertCleanUp();
    }

    @Test
    public void generateAppPackageJsonFromScratch_hashCalculated_updaterIsModified()
            throws IOException {
        packageCreator.execute();
        packageUpdater.execute();

        JsonObject mainJson = getPackageJson(mainPackageJson);
        Assert.assertTrue(mainJson.hasKey(TaskUpdatePackages.APP_PACKAGE_HASH));

        Assert.assertTrue(packageUpdater.modified);
    }

    @Test
    public void regenerateAppPackageJson_sameContent_updaterIsNotModified() {
        packageCreator.execute();
        packageUpdater.execute();

        // delete generated file
        appPackageJson.delete();

        // regenerate it (with the same content)
        packageCreator.execute();
        packageUpdater.execute();

        // the modified flag should be false (because the hash written in the
        // main package json matches the content of the generated file) and "npm
        // install" won't be executed
        // as a result of this flag value
        Assert.assertFalse(packageUpdater.modified);
    }

    @Test
    public void generateAppPackageJson_sameDependencies_updaterIsNotModified() {
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
                baseDir, generatedDir, false);

        packageCreator.execute();
        packageUpdater.execute();

        // delete generated file
        appPackageJson.delete();

        // generate it one more time, the content will be different since
        // packageCreator has not added its content
        packageUpdater.execute();

        Assert.assertFalse(
                "Modification flag should be false when no dependencies changed.",
                packageUpdater.modified);
    }

    @Test
    public void generateAppPackageJson_removedDependencies_updaterIsModified() {
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
                baseDir, generatedDir, false);

        packageCreator.execute();
        packageUpdater.execute();

        // delete generated file
        appPackageJson.delete();

        packages.remove("@vaadin/vaadin-checkbox");

        // generate it one more time, the content will be different since
        // packageCreator has not added its content
        packageUpdater.execute();

        Assert.assertTrue(
                "Modification flag should be true when dependency removed.",
                packageUpdater.modified);
    }

    @Test
    public void generateAppPackageJson_addedDependencies_updaterIsModified() {
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
                baseDir, generatedDir, false);

        packageCreator.execute();
        packageUpdater.execute();

        // delete generated file
        appPackageJson.delete();

        packages.put("@vaadin/vaadin-list-box", "1.1.1");

        // generate it one more time, the content will be different since
        // packageCreator has not added its content
        packageUpdater.execute();

        Assert.assertTrue(
                "Modification flag should be true when dependency added.",
                packageUpdater.modified);
    }

    @Test
    public void generateAppPackageJson_noDependencies_updaterIsNotModified() {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, false);

        packageCreator.execute();
        packageUpdater.execute();

        Assert.assertFalse(
                "Modification flag should be false when there was no dependencies.",
                packageUpdater.modified);

        // delete generated file
        appPackageJson.delete();

        // generate it one more time, the content will be different since
        // packageCreator has not added its content
        packageUpdater.execute();

        Assert.assertFalse(
                "Modification flag should be false when there has never been dependencies.",
                packageUpdater.modified);
    }

    @Test
    public void updatedMainPackageJson_noDependencies_updaterIsMarkedModified()
            throws IOException {
        FrontendDependencies frontendDependencies = Mockito
                .mock(FrontendDependencies.class);

        Map<String, String> packages = new HashMap<>();
        Mockito.when(frontendDependencies.getPackages()).thenReturn(packages);

        packageUpdater = new TaskUpdatePackages(null, frontendDependencies,
                baseDir, generatedDir, false);

        // Set a package Hash
        JsonObject mainJson = Json.createObject();
        mainJson.put(APP_PACKAGE_HASH, "ow20f39ghs93");
        Files.write(mainPackageJson.toPath(),
                Collections.singletonList(stringify(mainJson)));

        packageCreator.execute();
        mainJson = getPackageJson(mainPackageJson);
        Assert.assertEquals(
                "Main package should have added dependency and rewritten the hash.",
                TaskCreatePackageJson.FORCE_INSTALL_HASH,
                mainJson.get(APP_PACKAGE_HASH).asString());
        packageUpdater.execute();

        Assert.assertTrue(
                "Modification flag should be true when main package was updated.",
                packageUpdater.modified);

        mainJson = getPackageJson(mainPackageJson);
        Assert.assertNotEquals(
                "Main hash should have been updated to an actual hash.",
                TaskCreatePackageJson.FORCE_INSTALL_HASH,
                mainJson.get(APP_PACKAGE_HASH).asString());
    }

    private void makeNodeModulesAndPackageLock() throws IOException {
        // Make two node_modules folders and package lock
        mainNodeModules.mkdirs();
        appNodeModules.mkdirs();
        Files.write(packageLock.toPath(), Collections.singletonList("{}"));
        flowDepsPackageJson.getParentFile().mkdirs();
        flowDepsPackageJson.createNewFile();
        Files.write(flowDepsPackageJson.toPath(),
                Collections.singletonList("{}"));

        // self control
        Assert.assertTrue(mainNodeModules.exists());
        Assert.assertTrue(appNodeModules.exists());
        Assert.assertTrue(packageLock.exists());
    }

    private void assertCleanUp() throws IOException {
        Assert.assertFalse(mainNodeModules.exists());
        Assert.assertFalse(appNodeModules.exists());
        Assert.assertFalse(packageLock.exists());
    }

    private void assertMainPackageJsonContent() throws IOException {
        JsonObject json = packageUpdater.getMainPackageJson();
        Assert.assertTrue(json.hasKey("name"));
        Assert.assertTrue(json.hasKey("license"));

        JsonObject dependencies = json.getObject(DEPENDENCIES);
        Assert.assertTrue("Missing @webcomponents/webcomponentsjs package",
                dependencies.hasKey("@webcomponents/webcomponentsjs"));

        JsonObject devDependencies = json.getObject("devDependencies");
        Assert.assertTrue("Missing webpack dev package",
                devDependencies.hasKey("webpack"));
        Assert.assertTrue("Missing webpack-cli dev package",
                devDependencies.hasKey("webpack-cli"));
        Assert.assertTrue("Missing webpack-dev-server dev package",
                devDependencies.hasKey("webpack-dev-server"));
        Assert.assertTrue(
                "Missing webpack-babel-multi-target-plugin dev package",
                devDependencies.hasKey("webpack-babel-multi-target-plugin"));
        Assert.assertTrue("Missing copy-webpack-plugin dev package",
                devDependencies.hasKey("copy-webpack-plugin"));
    }

    private void assertAppPackageJsonContent() throws IOException {
        JsonObject json = packageUpdater.getAppPackageJson();
        Assert.assertTrue(json.hasKey("name"));
        Assert.assertTrue(json.hasKey("license"));

        JsonObject dependencies = getDependencies(json);

        Assert.assertTrue("Missing @vaadin/vaadin-button package",
                dependencies.hasKey("@vaadin/vaadin-button"));
    }

    private JsonObject getDependencies(JsonObject json) {
        return json.getObject(DEPENDENCIES);
    }

    private void updateVersion() throws IOException {
        // Change the version
        JsonObject json = packageUpdater.getAppPackageJson();
        getDependencies(json).put(SHRINKWRAP, "1.1.1");
        Files.write(appPackageJson.toPath(),
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
