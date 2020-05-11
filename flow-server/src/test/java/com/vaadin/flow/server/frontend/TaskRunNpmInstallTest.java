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
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.TaskRunNpmInstall.SKIPPING_NPM_INSTALL;
import static com.vaadin.flow.server.frontend.TaskUpdatePackages.APP_PACKAGE_HASH;
import static com.vaadin.flow.server.frontend.TaskUpdatePackages.DEPENDENCIES;
import static com.vaadin.flow.server.frontend.TaskUpdatePackages.DEV_DEPENDENCIES;
import static com.vaadin.flow.server.frontend.TaskUpdatePackages.HASH_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TaskRunNpmInstallTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private NodeUpdater nodeUpdater;

    private TaskRunNpmInstall task;

    private File npmFolder;

    private Logger logger = Mockito.mock(Logger.class);

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();
        nodeUpdater = new NodeUpdater(Mockito.mock(ClassFinder.class),
                Mockito.mock(FrontendDependencies.class), npmFolder,
                new File("")) {

            @Override
            public void execute() {
            }

            @Override
            Logger log() {
                return logger;
            }

        };
        task = new TaskRunNpmInstall(nodeUpdater);

    }

    @Test
    public void runNpmInstall_emptyDir_npmInstallIsExecuted()
            throws ExecutionFailedException {
        File nodeModules = new File(npmFolder, NODE_MODULES);
        nodeModules.mkdir();
        nodeUpdater.modified = false;
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }
    @Test
    public void runNpmInstall_nodeModulesContainsStaging_npmInstallIsExecuted()
            throws ExecutionFailedException {
        File nodeModules = nodeUpdater.nodeModulesFolder;
        nodeModules.mkdir();
        File staging = new File(nodeModules, ".staging");
        staging.mkdir();
        nodeUpdater.modified = false;
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmInstall_nonEmptyDirNoLocalHash_npmInstallIsExecuted()
            throws IOException, ExecutionFailedException {
        File nodeModules = new File(npmFolder, NODE_MODULES);
        nodeModules.mkdir();
        new File(nodeModules, "foo").createNewFile();
        nodeUpdater.modified = false;
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmInstall_dirContainsOnlyFlowNpmPackage_npmInstallIsNotExecuted()
            throws ExecutionFailedException {
        File nodeModules = new File(npmFolder, NODE_MODULES);
        nodeModules.mkdir();
        new File(nodeModules, "@vaadin/flow-frontend/").mkdirs();
        nodeUpdater.modified = false;
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmInstall_modified_npmInstallIsExecuted()
            throws ExecutionFailedException {
        nodeUpdater.modified = true;
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmInstall_nonEmptyDirNoHashMatch_npmInstallIsExecuted()
            throws IOException, ExecutionFailedException {
        File nodeModules = nodeUpdater.nodeModulesFolder;
        nodeModules.mkdir();
        JsonObject packageJson = Json.createObject();
        packageJson.put("name", "no-name");
        packageJson.put("license", "UNLICENSED");
        packageJson.put(HASH_KEY, "");
        nodeUpdater.writeMainPackageFile(packageJson);
        new File(nodeModules, "foo").createNewFile();
        writeLocalHash("faulty");
        nodeUpdater.modified = false;
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmInstall_matchingHash_npmInstallIsNotExecuted()
            throws IOException, ExecutionFailedException {
        File nodeModules = nodeUpdater.nodeModulesFolder;
        nodeModules.mkdir();

        new File(nodeModules, "foo").createNewFile();

        writeLocalHash("");
        nodeUpdater.modified = false;
        task.execute();

        Mockito.verify(logger).info(SKIPPING_NPM_INSTALL);
    }

    @Test
    public void runNpmInstall_matchingHashButEmptyModules_npmInstallIsExecuted()
            throws IOException, ExecutionFailedException {
        File nodeModules = nodeUpdater.nodeModulesFolder;
        nodeModules.mkdir();

        writeLocalHash("");
        nodeUpdater.modified = false;
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    public void writeLocalHash(String hash) throws IOException {
        final JsonObject localHash = Json.createObject();
        localHash.put(HASH_KEY, hash);

        final File localHashFile = new File(nodeUpdater.nodeModulesFolder,
                ".vaadin/vaadin.json");
        FileUtils.forceMkdirParent(localHashFile);
        nodeUpdater.writePackageFile(localHash, localHashFile);
    }

    @Test
    public void runNpmInstall_externalUpdateOfPackages_npmInstallIsRerun()
            throws ExecutionFailedException, IOException {
        nodeUpdater.modified = true;

        // Prefill packageJson files and  manually fake TaskUpdatePackages.
        JsonObject appPackage = nodeUpdater.getAppPackageJson();
        nodeUpdater.updateAppDefaultDependencies(appPackage);
        nodeUpdater
                .addDependency(appPackage, DEPENDENCIES, "@vaadin/vaadin-crud",
                        "1.1.0");
        nodeUpdater
                .addDependency(appPackage, DEPENDENCIES, "@vaadin/vaadin-icons",
                        "4.3.1");
        nodeUpdater
                .addDependency(appPackage, DEPENDENCIES, "@vaadin/vaadin-grid",
                        "5.5.2");

        final String appHash = TaskUpdatePackages
                .calculatePackageHash(appPackage);

        JsonObject mainPackage = nodeUpdater.getMainPackageJson();
        nodeUpdater.updateMainDefaultDependencies(mainPackage, "3.2.0");
        mainPackage.put(APP_PACKAGE_HASH, appHash);
        mainPackage.put(HASH_KEY,
                TaskUpdatePackages.calculatePackageHash(mainPackage));
        // Clear dependencies to not actually install anything with npm
        mainPackage.remove(DEPENDENCIES);
        mainPackage.remove(DEV_DEPENDENCIES);
        nodeUpdater.writePackageFile(mainPackage,
                new File(npmFolder, PACKAGE_JSON));

        task.execute();

        final File localHashFile = new File(nodeUpdater.nodeModulesFolder,
                ".vaadin/vaadin.json");
        Assert.assertTrue("Local has file was not created after install.",
                localHashFile.exists());

        String fileContent = FileUtils
                .readFileToString(localHashFile, UTF_8.name());
        JsonObject localHash = Json.parse(fileContent);
        Assert.assertNotEquals("We should have a non empty hash key", "",
                localHash.getString(HASH_KEY));

        // Update package json and hash as if someone had pushed to code repo.
        mainPackage = nodeUpdater.getMainPackageJson();

        String hash = mainPackage.getString(HASH_KEY);

        nodeUpdater.updateMainDefaultDependencies(mainPackage, "3.2.0");
        mainPackage.getObject(DEPENDENCIES).put("a-avataaar", "^1.2.5");
        mainPackage.put(HASH_KEY,
                TaskUpdatePackages.calculatePackageHash(mainPackage));
        // Clear dependencies to not actually install anything with npm
        mainPackage.remove(DEPENDENCIES);
        mainPackage.remove(DEV_DEPENDENCIES);

        Assert.assertNotEquals("Hash should have been updated", hash,
                mainPackage.getString(HASH_KEY));

        nodeUpdater.writePackageFile(mainPackage,
                new File(npmFolder, PACKAGE_JSON));
        logger = Mockito.mock(Logger.class);

        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    private String getRunningMsg() {
        return "Running `npm install` to "
                + "resolve and optionally download frontend dependencies. "
                + "This may take a moment, please stand by...";
    }

}
