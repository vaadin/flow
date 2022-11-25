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
 */
package com.vaadin.flow.server.frontend;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEV_DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.HASH_KEY;
import static com.vaadin.flow.server.frontend.NodeUpdater.PROJECT_FOLDER;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.testcategory.SlowTests;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonObject;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.event.Level;

@NotThreadSafe
public class TaskRunNpmInstallTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected NodeUpdater nodeUpdater;

    protected File npmFolder;

    protected ClassFinder finder;

    protected Logger logger = Mockito
            .spy(LoggerFactory.getLogger(NodeUpdater.class));
    protected File generatedPath;

    @Rule
    public ExpectedException exception = ExpectedException.none();


    public void writeLocalHash(String hash) throws IOException {
        final JsonObject localHash = Json.createObject();
        localHash.put(HASH_KEY, hash);

        final File localHashFile = new File(getNodeUpdater().nodeModulesFolder,
                ".vaadin/vaadin.json");
        FileUtils.forceMkdirParent(localHashFile);
        getNodeUpdater().writePackageFile(localHash, localHashFile);
    }

    protected void setupEsbuildAndFooInstallation() throws IOException {
        File nodeModules = getNodeUpdater().nodeModulesFolder;
        nodeModules.mkdir();
        getNodeUpdater().modified = false;

        // Fake that we have installed "esbuild"
        File esbuildPackageJson = new File(
                new File(nodeModules.getParentFile(), "fake-esbuild"),
                "package.json");
        String esbuildPackageJsonContents = IOUtils.toString(
                getClass().getResourceAsStream(
                        "fake-package-with-postinstall.json"),
                StandardCharsets.UTF_8);
        FileUtils.write(esbuildPackageJson, esbuildPackageJsonContents,
                StandardCharsets.UTF_8);

        // Fake that we have installed "foo"
        File fooPackageJson = new File(
                new File(nodeModules.getParentFile(), "fake-foo"),
                "package.json");
        String fooPackageJsonContents = IOUtils.toString(
                getClass().getResourceAsStream(
                        "fake-package-with-postinstall.json"),
                StandardCharsets.UTF_8);
        FileUtils.write(fooPackageJson, fooPackageJsonContents,
                StandardCharsets.UTF_8);

        File packageJsonFile = ensurePackageJson();
        JsonObject packageJson = getNodeUpdater().getPackageJson();
        packageJson.getObject(DEV_DEPENDENCIES).put("esbuild",
                "./fake-esbuild");
        packageJson.getObject(DEV_DEPENDENCIES).put("foo", "./fake-foo");
        FileUtils.write(packageJsonFile, packageJson.toJson(),
                StandardCharsets.UTF_8);

    }

    /**
     * Update the vaadin package hash to match dependencies. The hash is
     * calculated from dependencies and devDependencies but not from the vaadin
     * object. We copy the vaadin object dependencies and calculate the hash,
     * then we remove the dependencies and devDependencies to not have to
     * install anything for the test to keep the running time in ~1.4s instead
     * of ~50s
     *
     * @param packageJson
     *            package.json json object
     */
    public void updatePackageHash(JsonObject packageJson) {
        final JsonObject vaadinDep = packageJson.getObject(VAADIN_DEP_KEY)
                .getObject(DEPENDENCIES);
        JsonObject dependencies = Json.createObject();
        for (String key : vaadinDep.keys()) {
            dependencies.put(key, vaadinDep.getString(key));
        }
        JsonObject vaadinDevDep = packageJson.getObject(VAADIN_DEP_KEY)
                .getObject(DEV_DEPENDENCIES);
        JsonObject devDependencies = Json.createObject();
        for (String key : vaadinDevDep.keys()) {
            devDependencies.put(key, vaadinDevDep.getString(key));
        }
        packageJson.put(DEPENDENCIES, dependencies);
        packageJson.put(DEV_DEPENDENCIES, devDependencies);
        packageJson.getObject(VAADIN_DEP_KEY).put(HASH_KEY,
                TaskUpdatePackages.generatePackageJsonHash(packageJson));
        packageJson.remove(DEPENDENCIES);
        packageJson.remove(DEV_DEPENDENCIES);
    }

    protected void assertRunNpmInstallThrows_vaadinHomeNodeIsAFolder(
            TaskRunNpmInstall task)
            throws IOException, ExecutionFailedException {
        String userHome = "user.home";
        String originalHome = System.getProperty(userHome);
        File home = temporaryFolder.newFolder();
        System.setProperty(userHome, home.getPath());
        try {
            File homeDir = new File(home, ".vaadin");
            File node = new File(homeDir,
                    FrontendUtils.isWindows() ? "node/node.exe" : "node/node");
            FileUtils.forceMkdir(node);

            task.execute();
        } finally {
            System.setProperty(userHome, originalHome);
        }
    }

    private String getRunningMsg() {
        return "Running `" + getToolName() + " install` to "
                + "resolve and optionally download frontend dependencies. "
                + "This may take a moment, please stand by...";
    }

    protected NodeUpdater getNodeUpdater() {
        return nodeUpdater;
    }

    protected ClassFinder getClassFinder() {
        return finder;
    }

    protected String getToolName() {
        return "pnpm";
    }

    private File ensurePackageJson() throws IOException {
        File file = new File(npmFolder, PACKAGE_JSON);
        if (!file.exists()) {
            JsonObject packageJson = getNodeUpdater().getPackageJson();
            getNodeUpdater().writePackageFile(packageJson);
        }
        return file;
    }

}
