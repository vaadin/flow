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
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
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

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.testcategory.SlowTests;

import elemental.json.Json;
import elemental.json.JsonObject;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
@Category(SlowTests.class)
public class TaskRunNpmInstallTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private NodeUpdater nodeUpdater;

    private TaskRunNpmInstall task;

    private File npmFolder;

    private ClassFinder finder;

    private Logger logger = Mockito
            .spy(LoggerFactory.getLogger(NodeUpdater.class));
    protected File generatedPath;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();
        generatedPath = new File(npmFolder, "generated");
        generatedPath.mkdir();
        finder = Mockito.mock(ClassFinder.class);
        nodeUpdater = new NodeUpdater(finder,
                Mockito.mock(FrontendDependencies.class), npmFolder,
                generatedPath, null, TARGET, Mockito.mock(FeatureFlags.class)) {

            @Override
            public void execute() {
            }

            @Override
            Logger log() {
                return logger;
            }

            @Override
            protected String generateVersionsJson() {
                try {
                    FileUtils.write(
                            new File(getNodeUpdater().npmFolder,
                                    "versions.json"),
                            "", StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return "./versions.json";
            }

        };
        task = createTask();
    }

    protected TaskRunNpmInstall createTask() {
        return new TaskRunNpmInstall(getNodeUpdater(), false, false,
                FrontendTools.DEFAULT_NODE_VERSION,
                URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT), false,
                false);
    }

    @Test
    public void runNpmInstall_emptyDir_npmInstallIsExecuted()
            throws ExecutionFailedException, IOException {
        File nodeModules = getNodeUpdater().nodeModulesFolder;
        nodeModules.mkdir();
        getNodeUpdater().modified = false;

        ensurePackageJson();
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmInstall_nodeModulesContainsStaging_npmInstallIsExecuted()
            throws ExecutionFailedException, IOException {
        File nodeModules = getNodeUpdater().nodeModulesFolder;
        nodeModules.mkdir();
        File staging = new File(nodeModules, ".staging");
        staging.mkdir();
        getNodeUpdater().modified = false;
        ensurePackageJson();
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmInstall_toolIsChanged_nodeModulesIsRemoved()
            throws ExecutionFailedException, IOException {
        File nodeModules = getNodeUpdater().nodeModulesFolder;
        nodeModules.mkdir();

        ensurePackageJson();

        getNodeUpdater().modified = true;
        File yaml = new File(nodeModules, ".modules.yaml");
        yaml.createNewFile();
        task.execute();

        Assert.assertFalse(yaml.exists());
    }

    @Test
    public void runNpmInstall_toolIsNotChanged_nodeModulesIsNotRemoved()
            throws ExecutionFailedException, IOException {
        File nodeModules = getNodeUpdater().nodeModulesFolder;
        nodeModules.mkdir();

        getNodeUpdater().modified = true;
        File fakeFile = new File(nodeModules, ".fake.file");
        fakeFile.createNewFile();
        ensurePackageJson();
        task.execute();

        Assert.assertTrue(fakeFile.exists());
    }

    @Test
    public void runNpmInstall_nonEmptyDirNoLocalHash_npmInstallIsExecuted()
            throws IOException, ExecutionFailedException {
        File nodeModules = getNodeUpdater().nodeModulesFolder;
        nodeModules.mkdir();
        new File(nodeModules, "foo").createNewFile();
        getNodeUpdater().modified = false;
        ensurePackageJson();
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmInstall_nonEmptyDirNoHashMatch_npmInstallIsExecuted()
            throws IOException, ExecutionFailedException {
        File nodeModules = getNodeUpdater().nodeModulesFolder;
        nodeModules.mkdir();
        new File(nodeModules, "foo").createNewFile();
        writeLocalHash("faulty");
        getNodeUpdater().modified = false;

        ensurePackageJson();
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmInstall_matchingHash_npmInstallIsNotExecuted()
            throws IOException, ExecutionFailedException {
        File nodeModules = getNodeUpdater().nodeModulesFolder;
        nodeModules.mkdir();

        new File(nodeModules, "foo").createNewFile();

        writeLocalHash("");
        getNodeUpdater().modified = false;
        task.execute();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).info(captor.capture(),
                Mockito.matches(getToolName()),
                Mockito.matches(nodeModules.getAbsolutePath().replaceAll("\\\\",
                        "\\\\\\\\")),
                Mockito.any(), Mockito.matches(Constants.PACKAGE_JSON));
        Assert.assertEquals(
                "Skipping `{} install` because the frontend packages are already installed in the folder '{}' and the hash in the file '{}' is the same as in '{}'",
                captor.getValue());
    }

    @Test
    public void runNpmInstall_matchingHashButEmptyModules_npmInstallIsExecuted()
            throws IOException, ExecutionFailedException {
        File nodeModules = getNodeUpdater().nodeModulesFolder;
        nodeModules.mkdir();

        writeLocalHash("");
        getNodeUpdater().modified = false;
        ensurePackageJson();
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    public void writeLocalHash(String hash) throws IOException {
        final JsonObject localHash = Json.createObject();
        localHash.put(HASH_KEY, hash);

        final File localHashFile = new File(getNodeUpdater().nodeModulesFolder,
                ".vaadin/vaadin.json");
        FileUtils.forceMkdirParent(localHashFile);
        getNodeUpdater().writePackageFile(localHash, localHashFile);
    }

    @Test
    public void runNpmInstall_dirContainsOnlyFlowNpmPackage_npmInstallIsExecuted()
            throws ExecutionFailedException, IOException {
        File nodeModules = getNodeUpdater().nodeModulesFolder;
        nodeModules.mkdir();
        new File(nodeModules, "@vaadin/flow-frontend/").mkdirs();
        getNodeUpdater().modified = false;
        ensurePackageJson();
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmInstall_modified_npmInstallIsExecuted()
            throws ExecutionFailedException, IOException {
        getNodeUpdater().modified = true;
        ensurePackageJson();
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmInstall_vaadinHomeNodeIsAFolder_throws()
            throws IOException, ExecutionFailedException {
        exception.expectMessage(
                "it's either not a file or not a 'node' executable.");
        assertRunNpmInstallThrows_vaadinHomeNodeIsAFolder(
                new TaskRunNpmInstall(getNodeUpdater(), false, true,
                        FrontendTools.DEFAULT_NODE_VERSION,
                        URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT),
                        false, false));
    }

    @Test
    public void runNpmInstall_externalUpdateOfPackages_npmInstallIsRerun()
            throws ExecutionFailedException, IOException {
        getNodeUpdater().modified = true;

        // manually fake TaskUpdatePackages.
        JsonObject packageJson = getNodeUpdater().getPackageJson();
        updatePackageHash(packageJson);
        getNodeUpdater().writePackageFile(packageJson);

        task.execute();

        final File localHashFile = new File(getNodeUpdater().nodeModulesFolder,
                ".vaadin/vaadin.json");
        Assert.assertTrue("Local has file was not created after install.",
                localHashFile.exists());

        String fileContent = FileUtils.readFileToString(localHashFile,
                UTF_8.name());
        JsonObject localHash = Json.parse(fileContent);
        Assert.assertNotEquals("We should have a non empty hash key", "",
                localHash.getString(HASH_KEY));

        // Update package json and hash as if someone had pushed to code repo.
        packageJson = getNodeUpdater().getPackageJson();
        packageJson.getObject(VAADIN_DEP_KEY).getObject(DEPENDENCIES)
                .put("a-avataaar", "^1.2.5");
        String hash = packageJson.getObject(VAADIN_DEP_KEY).getString(HASH_KEY);
        updatePackageHash(packageJson);

        Assert.assertNotEquals("Hash should have been updated", hash,
                packageJson.getObject(VAADIN_DEP_KEY).getString(HASH_KEY));

        getNodeUpdater().writePackageFile(packageJson);
        logger = Mockito.mock(Logger.class);

        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
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
        return "npm";
    }

    private void ensurePackageJson() throws IOException {
        File file = new File(npmFolder, PACKAGE_JSON);
        if (!file.exists()) {
            JsonObject packageJson = getNodeUpdater().getPackageJson();
            getNodeUpdater().writePackageFile(packageJson);
        }
    }

}
