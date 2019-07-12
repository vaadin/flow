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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static elemental.json.impl.JsonUtil.stringify;

public class NodeUpdatePackagesTest extends NodeUpdateTestUtil {

    private static final String DEPENDENCIES = "dependencies";

    private static final String SHRINKWRAP = "@vaadin/vaadin-shrinkwrap";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TaskUpdatePackages packageUpdater;
    private TaskCreatePackageJson packageCreator;
    private File mainPackageJson;
    private File appPackageJson;

    private File mainNodeModules;
    private File packageLock;
    private File appNodeModules;
    private File flowDepsPackageJson;

    @Before
    public void setup() throws Exception {
        File baseDir = temporaryFolder.getRoot();

        File generatedDir = new File(baseDir, DEFAULT_GENERATED_DIR);

        NodeUpdateTestUtil.createStubNode(true, true,
                baseDir.getAbsolutePath());

        packageCreator = new TaskCreatePackageJson(baseDir, generatedDir);

        packageUpdater = new TaskUpdatePackages(getClassFinder(), null, baseDir,
                generatedDir, false);
        mainPackageJson = new File(baseDir, PACKAGE_JSON);
        appPackageJson = new File(generatedDir, PACKAGE_JSON);

        mainNodeModules = new File(baseDir, FrontendUtils.NODE_MODULES);
        appNodeModules = new File(generatedDir, FrontendUtils.NODE_MODULES);
        packageLock = new File(baseDir, "package-lock.json");

        File atVaadin = new File(mainNodeModules, "@vaadin");
        File flowDeps = new File(atVaadin, "flow-deps");

        flowDepsPackageJson = new File(flowDeps, PACKAGE_JSON);
    }

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
    public void versionsMatch_noCleanUp() throws IOException {
        // Generate package json in a proper format first
        packageCreator.execute();
        packageUpdater.execute();

        makeNodeModulesAndPackageLock();

        // run it again with existing generated package.json and matched
        // version
        packageUpdater.execute();

        // nothing is removed
        Assert.assertTrue(mainNodeModules.exists());
        Assert.assertTrue(appNodeModules.exists());
        Assert.assertTrue(packageLock.exists());
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

}
