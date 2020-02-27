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
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;

public class TaskRunPnpmInstallTest extends TaskRunNpmInstallTest {

    private static final String PINNED_VERSION = "3.2.17";

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();
        FrontendUtils.ensurePnpm(getNodeUpdater().npmFolder.getAbsolutePath());
    }

    @Test
    public void runPnpmInstall_overlayVersionIsPinnedViaPlatform_installedOverlayVersionIsSpecifiedByPlatform()
            throws IOException, ExecutionFailedException {
        File packageJson = new File(getNodeUpdater().npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file: dialog doesn't pin its Overlay version which
        // is transitive dependency.
        FileUtils.write(packageJson,
                "{\"dependencies\": {"
                        + "\"@vaadin/vaadin-dialog\": \"2.2.1\"}}",
                StandardCharsets.UTF_8);

        // Platform defines a pinned version
        TaskRunNpmInstall task = createTask(
                "{ \"@vaadin/vaadin-overlay\":\"" + PINNED_VERSION + "\"}");
        task.execute();

        File overlayPackageJson = new File(getNodeUpdater().nodeModulesFolder,
                "@vaadin/vaadin-overlay/package.json");

        // The resulting version should be the one specified via platform
        // versions file
        JsonObject overlayPackage = Json.parse(FileUtils
                .readFileToString(overlayPackageJson, StandardCharsets.UTF_8));
        Assert.assertEquals(PINNED_VERSION,
                overlayPackage.getString("version"));
    }

    @Override
    @Test
    public void runNpmInstall_toolIsChanged_nodeModulesIsRemoved()
            throws ExecutionFailedException, IOException {
        File packageJson = new File(getNodeUpdater().npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // create some package.json file so pnpm does some installation into
        // node_modules folder
        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"pnpm\": \"4.5.0\"}}",
                StandardCharsets.UTF_8);

        getNodeUpdater().modified = true;
        createTask().execute();

        // create a fake file in the node modules dir to check that it's removed
        File fakeFile = new File(getNodeUpdater().nodeModulesFolder,
                ".fake.file");
        fakeFile.createNewFile();

        getNodeUpdater().modified = true;
        createTask().execute();

        Assert.assertTrue(fakeFile.exists());
    }

    @Override
    @Test
    public void runNpmInstall_toolIsNotChanged_nodeModulesIsNotRemoved()
            throws ExecutionFailedException, IOException {
        getNodeUpdater().modified = true;
        createTask().execute();

        File nodeModules = getNodeUpdater().nodeModulesFolder;
        FileUtils.forceMkdir(nodeModules);

        // create a fake file in the node modules dir to check that it's removed
        File fakeFile = new File(nodeModules, ".fake.file");
        fakeFile.createNewFile();

        getNodeUpdater().modified = true;
        createTask().execute();

        Assert.assertTrue(fakeFile.exists());
    }

    @Override
    @Test(expected = ExecutionFailedException.class)
    public void runNpmInstall_vaadinHomeNodeIsAFolder_throws()
            throws IOException, ExecutionFailedException {
        assertRunNpmInstallThrows_vaadinHomeNodeIsAFolder(new TaskRunNpmInstall(
                getClassFinder(), getNodeUpdater(), true, true));
        exception.expectMessage(
                "it's either not a file or not a 'node' executable.");
        assertRunNpmInstallThrows_vaadinHomeNodeIsAFolder(new TaskRunNpmInstall(
                getClassFinder(), getNodeUpdater(), true, true));
    }

    @Test
    public void runPnpmInstall_versionsJsonIsFound_pnpmHookFileIsGenerated()
            throws IOException, ExecutionFailedException {
        ClassFinder classFinder = getClassFinder();
        File versions = temporaryFolder.newFile();
        FileUtils.write(versions, "{}", StandardCharsets.UTF_8);
        Mockito.when(classFinder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(versions.toURI().toURL());

        TaskRunNpmInstall task = createTask();
        getNodeUpdater().modified = true;
        task.execute();

        File file = new File(getNodeUpdater().npmFolder, "pnpmfile.js");
        Assert.assertTrue(file.exists());
        String content = FileUtils.readFileToString(file,
                StandardCharsets.UTF_8);
        Assert.assertThat(content,
                CoreMatchers.containsString("JSON.parse(fs.readFileSync"));
    }

    @Test
    public void runPnpmInstall_versionsJsonIsNotFound_pnpmHookFileIsNotGenerated()
            throws IOException, ExecutionFailedException {
        TaskRunNpmInstall task = createTask();
        getNodeUpdater().modified = true;
        task.execute();

        File file = new File(getNodeUpdater().npmFolder, "pnpmfile.js");
        Assert.assertFalse(file.exists());
    }

    @Override
    protected String getToolName() {
        return "pnpm";
    }

    @Override
    protected TaskRunNpmInstall createTask() {
        return new TaskRunNpmInstall(getClassFinder(), getNodeUpdater(), true,
                false);
    }

    protected TaskRunNpmInstall createTask(String versionsContent) {
        return new TaskRunNpmInstall(getClassFinder(), getNodeUpdater(), true,
                false) {
            @Override
            protected String generateVersionsJson() {
                try {
                    FileUtils.write(
                            new File(getNodeUpdater().npmFolder,
                                    "versions.json"),
                            versionsContent, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return "./versions.json";
            }
        };
    }

}
