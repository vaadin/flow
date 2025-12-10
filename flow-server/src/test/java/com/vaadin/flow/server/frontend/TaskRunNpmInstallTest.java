/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Assume;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.testcategory.SlowTests;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEV_DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.HASH_KEY;
import static com.vaadin.flow.server.frontend.NodeUpdater.PROJECT_FOLDER;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;

@NotThreadSafe
@Category(SlowTests.class)
public class TaskRunNpmInstallTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private NodeUpdater nodeUpdater;

    private TaskRunNpmInstall task;

    protected File npmFolder;

    protected ClassFinder finder;

    protected Logger logger = Mockito
            .spy(LoggerFactory.getLogger(NodeUpdater.class));

    @Rule
    public ExpectedException exception = ExpectedException.none();

    protected Options options;

    @Before
    public void setUp() throws IOException, NoSuchFieldException {
        npmFolder = temporaryFolder.newFolder();
        options = new MockOptions(npmFolder).withBuildDirectory(TARGET)
                .withBundleBuild(true);
        finder = options.getClassFinder();
        nodeUpdater = new NodeUpdater(Mockito.mock(FrontendDependencies.class),
                options) {

            @Override
            public void execute() {
            }

            @Override
            Logger log() {
                return logger;
            }

        };
        task = createTask(new ArrayList<>());
        ReflectTools.setJavaFieldValue(
                new FrontendTools(
                        new FrontendToolsSettings(npmFolder.getAbsolutePath(),
                                () -> npmFolder.getAbsolutePath())),
                FrontendTools.class.getDeclaredField("activeNodeInstallation"),
                null);
    }

    protected TaskRunNpmInstall createTask(List<String> additionalPostInstall) {
        options.withPostinstallPackages(additionalPostInstall);
        options.withNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                .withNodeDownloadRoot(
                        URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT));

        return new TaskRunNpmInstall(getNodeUpdater(), options);
    }

    @Test
    public void runNpmInstall_emptyDir_npmInstallIsExecuted()
            throws ExecutionFailedException, IOException {
        File nodeModules = options.getNodeModulesFolder();
        nodeModules.mkdir();
        getNodeUpdater().modified = false;

        ensurePackageJson();
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmInstallAndCi_emptyDir_npmInstallAndCiIsExecuted()
            throws ExecutionFailedException, IOException {
        assumeNPMIsInUse();

        File nodeModules = options.getNodeModulesFolder();
        nodeModules.mkdir();
        getNodeUpdater().modified = false;

        ensurePackageJson();

        task.execute();
        Mockito.verify(logger).info(getRunningMsg());

        deleteDirectory(nodeModules);

        options.withCiBuild(true);
        task.execute();
        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmCi_emptyDir_npmCiFails() throws IOException {
        assumeNPMIsInUse();

        File nodeModules = options.getNodeModulesFolder();
        nodeModules.mkdir();
        getNodeUpdater().modified = false;

        ensurePackageJson();

        options.withCiBuild(true);

        Assert.assertThrows(ExecutionFailedException.class, task::execute);
    }

    @Test
    public void runNpmInstall_nodeModulesContainsStaging_npmInstallIsExecuted()
            throws ExecutionFailedException, IOException {
        File nodeModules = options.getNodeModulesFolder();
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
        File nodeModules = options.getNodeModulesFolder();
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
        File nodeModules = options.getNodeModulesFolder();
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
        File nodeModules = options.getNodeModulesFolder();
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
        File nodeModules = options.getNodeModulesFolder();
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
        File nodeModules = options.getNodeModulesFolder();
        nodeModules.mkdir();

        new File(nodeModules, "foo").createNewFile();

        writeLocalHash("");
        getNodeUpdater().modified = false;
        task.execute();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).info(captor.capture(),
                Mockito.matches(getToolName()), Mockito.matches(getCommand()),
                Mockito.matches(nodeModules.getAbsolutePath().replaceAll("\\\\",
                        "\\\\\\\\")),
                Mockito.any(), Mockito.matches(Constants.PACKAGE_JSON));
        Assert.assertEquals(
                "Skipping `{} {}` because the frontend packages are already installed in the folder '{}' and the hash in the file '{}' is the same as in '{}'",
                captor.getValue());
    }

    @Test
    public void runNpmInstall_matchingHashButEmptyModules_npmInstallIsExecuted()
            throws IOException, ExecutionFailedException {
        File nodeModules = options.getNodeModulesFolder();
        nodeModules.mkdir();

        writeLocalHash("");
        getNodeUpdater().modified = false;
        ensurePackageJson();
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    public void writeLocalHash(String hash) throws IOException {
        final ObjectNode localHash = JacksonUtils.createObjectNode();
        localHash.put(HASH_KEY, hash);

        final File localHashFile = new File(options.getNodeModulesFolder(),
                ".vaadin/vaadin.json");
        FileUtils.forceMkdirParent(localHashFile);
        getNodeUpdater().writePackageFile(localHash, localHashFile);
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
    public void runNpmInstall_vaadinHomeNodeIsAFolder_nodeIsReinstalled()
            throws IOException, ExecutionFailedException {

        options.withHomeNodeExecRequired(true)
                .withNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                .withNodeDownloadRoot(
                        URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT));

        assertRunNpmInstallInstallsNewNode_whenVaadinHomeNodeIsAFolder(
                new TaskRunNpmInstall(getNodeUpdater(), options));
    }

    @Test
    public void runNpmInstall_externalUpdateOfPackages_npmInstallIsRerun()
            throws ExecutionFailedException, IOException {
        getNodeUpdater().modified = true;

        // manually fake TaskUpdatePackages.
        ObjectNode packageJson = getNodeUpdater().getPackageJson();
        updatePackageHash(packageJson);
        getNodeUpdater().writePackageFile(packageJson);

        task.execute();

        final File localHashFile = new File(options.getNodeModulesFolder(),
                ".vaadin/vaadin.json");
        Assert.assertTrue("Local has file was not created after install.",
                localHashFile.exists());

        String fileContent = FileUtils.readFileToString(localHashFile,
                UTF_8.name());
        JsonNode localHash = JacksonUtils.readTree(fileContent);
        Assert.assertNotEquals("We should have a non empty hash key", "",
                localHash.get(HASH_KEY).asString());

        // Update package json and hash as if someone had pushed to code repo.
        packageJson = getNodeUpdater().getPackageJson();
        ((ObjectNode) packageJson.get(VAADIN_DEP_KEY).get(DEPENDENCIES))
                .put("a-avataaar", "^1.2.5");
        String hash = packageJson.get(VAADIN_DEP_KEY).get(HASH_KEY).asString();
        updatePackageHash(packageJson);

        Assert.assertNotEquals("Hash should have been updated", hash,
                packageJson.get(VAADIN_DEP_KEY).get(HASH_KEY).asString());

        getNodeUpdater().writePackageFile(packageJson);
        logger = Mockito.mock(Logger.class);

        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    protected void setupEsbuildAndFooInstallation() throws IOException {
        File nodeModules = options.getNodeModulesFolder();
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
        JsonNode packageJson = getNodeUpdater().getPackageJson();
        ((ObjectNode) packageJson.get(DEV_DEPENDENCIES)).put("esbuild",
                "./fake-esbuild");
        ((ObjectNode) packageJson.get(DEV_DEPENDENCIES)).put("foo",
                "./fake-foo");
        FileUtils.write(packageJsonFile, packageJson.toString(),
                StandardCharsets.UTF_8);

    }

    @Test
    public void runNpmInstall_noPostinstallScript_postIntstallNotExecuted()
            throws IOException, ExecutionFailedException {
        setupEsbuildAndFooInstallation();

        // Remove postinstall script from "esbuild"
        File esbuildPackageJson = new File(
                new File(options.getNodeModulesFolder().getParentFile(),
                        "fake-esbuild"),
                "package.json");
        JsonNode esbuildPackageJsonContents = JacksonUtils
                .readTree(IOUtils.toString(
                        getClass().getResourceAsStream(
                                "fake-package-with-postinstall.json"),
                        StandardCharsets.UTF_8));
        ((ObjectNode) esbuildPackageJsonContents.get("scripts"))
                .remove("postinstall");
        FileUtils.write(esbuildPackageJson,
                esbuildPackageJsonContents.toString(), StandardCharsets.UTF_8);

        logger = new MockLogger();
        Assert.assertTrue(logger.isDebugEnabled());
        task.execute();

        Assert.assertFalse(
                "esbuild without postinstall should not have been executed",
                ((MockLogger) logger).getLogs()
                        .contains("Running postinstall for 'esbuild'"));
    }

    @Test
    public void runNpmInstall_postInstall_runOnlyForDefaultPackages()
            throws ExecutionFailedException, IOException {
        setupEsbuildAndFooInstallation();
        task.execute();

        Assert.assertTrue("Postinstall for 'esbuild' was not run",
                new File(new File(options.getNodeModulesFolder(), "esbuild"),
                        "postinstall-file.txt").exists());
        Assert.assertFalse("Postinstall for 'foo' should not have been run",
                new File(new File(options.getNodeModulesFolder(), "foo"),
                        "postinstall-file.txt").exists());
    }

    @Test
    public void runNpmInstall_postInstall_runForDefinedAdditionalPackages()
            throws ExecutionFailedException, IOException {
        setupEsbuildAndFooInstallation();
        task = createTask(Collections.singletonList("foo"));
        task.execute();

        Assert.assertTrue("Postinstall for 'esbuild' was not run",
                new File(new File(options.getNodeModulesFolder(), "esbuild"),
                        "postinstall-file.txt").exists());
        Assert.assertTrue("Postinstall for 'foo' was not run",
                new File(new File(options.getNodeModulesFolder(), "foo"),
                        "postinstall-file.txt").exists());
    }

    // https://github.com/vaadin/flow/issues/17663
    @Test(timeout = 30000)
    public void runNpmInstall_postInstallWritingLotsOfOutput_processDoesNotStuck()
            throws ExecutionFailedException, IOException {
        setupEsbuildAndFooInstallation();

        File nodeModules = options.getNodeModulesFolder();
        File fooPackageJson = new File(
                new File(nodeModules.getParentFile(), "fake-foo"),
                "package.json");
        String fooPackageJsonContents = IOUtils.toString(
                getClass().getResourceAsStream(
                        "fake-package-with-postinstall-writing-to-console.json"),
                StandardCharsets.UTF_8);
        FileUtils.write(fooPackageJson, fooPackageJsonContents,
                StandardCharsets.UTF_8);

        task = createTask(Collections.singletonList("foo"));
        task.execute();

        Assert.assertTrue("Postinstall for 'foo' was not run",
                new File(new File(options.getNodeModulesFolder(), "foo"),
                        "postinstall-console-file.txt").exists());
    }

    @Test
    public void shouldRunNpmInstallWhenFolderChanges() throws Exception {
        setupEsbuildAndFooInstallation();

        String packageJsonHash = getNodeUpdater().getPackageJson()
                .get(VAADIN_DEP_KEY).get(HASH_KEY).asString();
        ObjectNode vaadinJson = JacksonUtils.createObjectNode();
        vaadinJson.put(HASH_KEY, packageJsonHash);
        vaadinJson.put(PROJECT_FOLDER, npmFolder.getAbsolutePath());
        File vaadinJsonFile = getNodeUpdater().getVaadinJsonFile();

        FileUtils.writeStringToFile(vaadinJsonFile, vaadinJson.toString(),
                UTF_8);

        Assert.assertFalse(task.isVaadinHashOrProjectFolderUpdated());
        vaadinJson.put(PROJECT_FOLDER, npmFolder.getAbsolutePath() + "foo");
        FileUtils.writeStringToFile(vaadinJsonFile, vaadinJson.toString(),
                UTF_8);
        Assert.assertTrue(task.isVaadinHashOrProjectFolderUpdated());
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
    public void updatePackageHash(ObjectNode packageJson) {
        final ObjectNode vaadinDep = (ObjectNode) packageJson
                .get(VAADIN_DEP_KEY).get(DEPENDENCIES);
        ObjectNode dependencies = JacksonUtils.createObjectNode();
        for (String key : JacksonUtils.getKeys(vaadinDep)) {
            dependencies.put(key, vaadinDep.get(key).asString());
        }
        ObjectNode vaadinDevDep = (ObjectNode) packageJson.get(VAADIN_DEP_KEY)
                .get(DEV_DEPENDENCIES);
        ObjectNode devDependencies = JacksonUtils.createObjectNode();
        for (String key : JacksonUtils.getKeys(vaadinDevDep)) {
            devDependencies.put(key, vaadinDevDep.get(key).asString());
        }
        packageJson.set(DEPENDENCIES, dependencies);
        packageJson.set(DEV_DEPENDENCIES, devDependencies);
        ((ObjectNode) packageJson.get(VAADIN_DEP_KEY)).put(HASH_KEY,
                TaskUpdatePackages.generatePackageJsonHash(packageJson));
        packageJson.remove(DEPENDENCIES);
        packageJson.remove(DEV_DEPENDENCIES);
    }

    protected void assertRunNpmInstallInstallsNewNode_whenVaadinHomeNodeIsAFolder(
            TaskRunNpmInstall task)
            throws IOException, ExecutionFailedException {
        String userHome = "user.home";
        String originalHome = System.getProperty(userHome);
        File home = temporaryFolder.newFolder();
        System.setProperty(userHome, home.getPath());
        try {
            File homeDir = new File(home, ".vaadin");
            File node = new File(homeDir,
                    FrontendUtils.isWindows()
                            ? "node-" + FrontendTools.DEFAULT_NODE_VERSION
                                    + "/node.exe"
                            : "node-" + FrontendTools.DEFAULT_NODE_VERSION
                                    + "/bin/node");
            FileUtils.forceMkdir(node);

            Assert.assertTrue("node executable should be a directory",
                    node.isDirectory());

            task.execute();

            Assert.assertFalse("node executable should have been reinstalled",
                    node.isDirectory());
            Assert.assertTrue("node executable should be executable",
                    node.canExecute());
        } finally {
            System.setProperty(userHome, originalHome);
        }
    }

    protected String getRunningMsg() {

        return "Running `" + getToolName() + " " + getCommand() + "` to "
                + "resolve and optionally download frontend dependencies. "
                + "This may take a moment, please stand by...";
    }

    private String getCommand() {
        String command = "install";
        if (options.isCiBuild()) {
            if ("pnpm".equals(getToolName())) {
                command += " --frozen-lockfile";
            } else {
                command = "ci";
            }
        }
        return command;
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

    private File ensurePackageJson() throws IOException {
        File file = new File(npmFolder, PACKAGE_JSON);
        if (!file.exists()) {
            JsonNode packageJson = getNodeUpdater().getPackageJson();
            getNodeUpdater().writePackageFile(packageJson);
        }
        return file;
    }

    void deleteDirectory(File dir) throws IOException {
        Files.walk(dir.toPath()).sorted(Comparator.reverseOrder())
                .map(Path::toFile).forEach(File::delete);
    }

    @Test
    public void verifyPackageLockAndClean_lockfileVersion3_fileNotRemoved()
            throws IOException {
        assumeNPMIsInUse();

        File packageLockFile = new File(npmFolder, "package-lock.json");
        String packageLockContent = """
                {
                  "name": "test-project",
                  "version": "1.0.0",
                  "lockfileVersion": 3,
                  "requires": true,
                  "packages": {}
                }
                """;
        FileUtils.write(packageLockFile, packageLockContent,
                StandardCharsets.UTF_8);

        task.verifyPackageLockAndClean();

        Assert.assertTrue(
                "package-lock.json with version 3 should not be removed",
                packageLockFile.exists());
    }

    @Test
    public void verifyPackageLockAndClean_lockfileVersion2_fileRemoved()
            throws IOException {
        assumeNPMIsInUse();

        File packageLockFile = new File(npmFolder, "package-lock.json");
        String packageLockContent = """
                {
                  "name": "test-project",
                  "version": "1.0.0",
                  "lockfileVersion": 2,
                  "requires": true,
                  "packages": {}
                }
                """;
        FileUtils.write(packageLockFile, packageLockContent,
                StandardCharsets.UTF_8);

        task.verifyPackageLockAndClean();

        Assert.assertFalse("package-lock.json with version 2 should be removed",
                packageLockFile.exists());
    }

    @Test
    public void verifyPackageLockAndClean_lockfileVersion1_fileRemoved()
            throws IOException {
        assumeNPMIsInUse();

        File packageLockFile = new File(npmFolder, "package-lock.json");
        String packageLockContent = """
                {
                  "name": "test-project",
                  "version": "1.0.0",
                  "lockfileVersion": 1,
                  "requires": true,
                  "dependencies": {}
                }
                """;
        FileUtils.write(packageLockFile, packageLockContent,
                StandardCharsets.UTF_8);

        task.verifyPackageLockAndClean();

        Assert.assertFalse("package-lock.json with version 1 should be removed",
                packageLockFile.exists());
    }

    @Test
    public void verifyPackageLockAndClean_withSpaces_correctlyParsed()
            throws IOException {
        File packageLockFile = new File(npmFolder, "package-lock.json");
        String packageLockContent = """
                {
                  "name": "test-project",
                  "version": "1.0.0",
                  "lockfileVersion"  :  3,
                  "requires": true,
                  "packages": {}
                }
                """;
        FileUtils.write(packageLockFile, packageLockContent,
                StandardCharsets.UTF_8);

        task.verifyPackageLockAndClean();

        Assert.assertTrue(
                "package-lock.json with version 3 (with spaces) should not be removed",
                packageLockFile.exists());
    }

    @Test
    public void verifyPackageLockAndClean_pnpmEnabled_fileNotChecked()
            throws IOException {
        options.withEnablePnpm(true);
        task = createTask(new ArrayList<>());

        File packageLockFile = new File(npmFolder, "package-lock.json");
        String packageLockContent = """
                {
                  "name": "test-project",
                  "version": "1.0.0",
                  "lockfileVersion": 2,
                  "requires": true,
                  "packages": {}
                }
                """;
        FileUtils.write(packageLockFile, packageLockContent,
                StandardCharsets.UTF_8);

        task.verifyPackageLockAndClean();

        Assert.assertTrue(
                "package-lock.json should not be checked when pnpm is enabled",
                packageLockFile.exists());
    }

    @Test
    public void verifyPackageLockAndClean_noLockfile_doesNotThrow() {
        File packageLockFile = new File(npmFolder, "package-lock.json");
        Assert.assertFalse("package-lock.json should not exist",
                packageLockFile.exists());

        // Should not throw any exception
        task.verifyPackageLockAndClean();

        Assert.assertFalse("package-lock.json should still not exist",
                packageLockFile.exists());
    }

    private void assumeNPMIsInUse() {
        Assume.assumeTrue(getClass().equals(TaskRunNpmInstallTest.class));
    }

}
