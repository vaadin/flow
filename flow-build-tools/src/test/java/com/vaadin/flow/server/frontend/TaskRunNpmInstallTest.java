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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.MockLogger;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEV_DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.HASH_KEY;
import static com.vaadin.flow.server.frontend.NodeUpdater.PROJECT_FOLDER;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@NotThreadSafe
@Tag("com.vaadin.flow.testcategory.SlowTests")
class TaskRunNpmInstallTest {

    @TempDir
    File temporaryFolder;

    private NodeUpdater nodeUpdater;

    private TaskRunNpmInstall task;

    protected File npmFolder;

    protected ClassFinder finder;

    protected Logger logger = Mockito
            .spy(LoggerFactory.getLogger(NodeUpdater.class));

    protected Options options;

    @BeforeEach
    void setUp() throws IOException, NoSuchFieldException {
        npmFolder = Files.createTempDirectory(temporaryFolder.toPath(), "tmp")
                .toFile();
        options = new MockOptions(npmFolder).withBuildDirectory(TARGET)
                .withBundleBuild(true);
        finder = options.getClassFinder();
        nodeUpdater = new NodeUpdater(options) {

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
    void runNpmInstall_emptyDir_npmInstallIsExecuted()
            throws ExecutionFailedException, IOException {
        File nodeModules = options.getNodeModulesFolder();
        nodeModules.mkdir();
        getNodeUpdater().modified = false;

        ensurePackageJson();
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    void runNpmInstallAndCi_emptyDir_npmInstallAndCiIsExecuted()
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
    void runNpmCi_emptyDir_npmCiFails() throws IOException {
        assumeNPMIsInUse();

        File nodeModules = options.getNodeModulesFolder();
        nodeModules.mkdir();
        getNodeUpdater().modified = false;

        ensurePackageJson();

        options.withCiBuild(true);

        assertThrows(ExecutionFailedException.class, task::execute);
    }

    @Test
    void runNpmInstall_nodeModulesContainsStaging_npmInstallIsExecuted()
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
    void runNpmInstall_toolIsChanged_nodeModulesIsRemoved()
            throws ExecutionFailedException, IOException {
        File nodeModules = options.getNodeModulesFolder();
        nodeModules.mkdir();

        ensurePackageJson();

        getNodeUpdater().modified = true;
        File yaml = new File(nodeModules, ".modules.yaml");
        yaml.createNewFile();
        task.execute();

        assertFalse(yaml.exists());
    }

    @Test
    void runNpmInstall_toolIsNotChanged_nodeModulesIsNotRemoved()
            throws ExecutionFailedException, IOException {
        File nodeModules = options.getNodeModulesFolder();
        nodeModules.mkdir();

        getNodeUpdater().modified = true;
        File fakeFile = new File(nodeModules, ".fake.file");
        fakeFile.createNewFile();
        ensurePackageJson();
        task.execute();

        assertTrue(fakeFile.exists());
    }

    @Test
    void runNpmInstall_nonEmptyDirNoLocalHash_npmInstallIsExecuted()
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
    void runNpmInstall_nonEmptyDirNoHashMatch_npmInstallIsExecuted()
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
    void runNpmInstall_matchingHash_npmInstallIsNotExecuted()
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
        assertEquals(
                "Skipping `{} {}` because the frontend packages are already installed in the folder '{}' and the hash in the file '{}' is the same as in '{}'",
                captor.getValue());
    }

    @Test
    void runNpmInstall_matchingHashButEmptyModules_npmInstallIsExecuted()
            throws IOException, ExecutionFailedException {
        File nodeModules = options.getNodeModulesFolder();
        nodeModules.mkdir();

        writeLocalHash("");
        getNodeUpdater().modified = false;
        ensurePackageJson();
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    void writeLocalHash(String hash) throws IOException {
        final ObjectNode localHash = JacksonUtils.createObjectNode();
        localHash.put(HASH_KEY, hash);

        final File localHashFile = new File(options.getNodeModulesFolder(),
                ".vaadin/vaadin.json");
        Files.createDirectories(localHashFile.toPath().getParent());
        getNodeUpdater().writePackageFile(localHash, localHashFile);
    }

    @Test
    void runNpmInstall_modified_npmInstallIsExecuted()
            throws ExecutionFailedException, IOException {
        getNodeUpdater().modified = true;
        ensurePackageJson();
        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    void runNpmInstall_vaadinHomeNodeIsAFolder_nodeIsReinstalled()
            throws IOException, ExecutionFailedException {

        options.withHomeNodeExecRequired(true)
                .withNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                .withNodeDownloadRoot(
                        URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT));

        assertRunNpmInstallInstallsNewNode_whenVaadinHomeNodeIsAFolder(
                new TaskRunNpmInstall(getNodeUpdater(), options));
    }

    @Test
    void runNpmInstall_externalUpdateOfPackages_npmInstallIsRerun()
            throws ExecutionFailedException, IOException {
        getNodeUpdater().modified = true;

        // manually fake TaskUpdatePackages.
        ObjectNode packageJson = getNodeUpdater().getPackageJson();
        updatePackageHash(packageJson);
        getNodeUpdater().writePackageFile(packageJson);

        task.execute();

        final File localHashFile = new File(options.getNodeModulesFolder(),
                ".vaadin/vaadin.json");
        assertTrue(localHashFile.exists(),
                "Local has file was not created after install.");

        String fileContent = Files.readString(localHashFile.toPath());
        JsonNode localHash = JacksonUtils.readTree(fileContent);
        assertNotEquals("", localHash.get(HASH_KEY).asString(),
                "We should have a non empty hash key");

        // Update package json and hash as if someone had pushed to code repo.
        packageJson = getNodeUpdater().getPackageJson();
        ((ObjectNode) packageJson.get(VAADIN_DEP_KEY).get(DEPENDENCIES))
                .put("a-avataaar", "^1.2.5");
        String hash = packageJson.get(VAADIN_DEP_KEY).get(HASH_KEY).asString();
        updatePackageHash(packageJson);

        assertNotEquals(hash,
                packageJson.get(VAADIN_DEP_KEY).get(HASH_KEY).asString(),
                "Hash should have been updated");

        getNodeUpdater().writePackageFile(packageJson);
        logger = Mockito.mock(Logger.class);

        task.execute();

        Mockito.verify(logger).info(getRunningMsg());
    }

    protected void setupPostinstallPackages() throws IOException {
        File nodeModules = options.getNodeModulesFolder();
        nodeModules.mkdir();
        getNodeUpdater().modified = false;

        // Pre-populate node_modules/@vaadin/vaadin-usage-statistics/ so that
        // shouldRunNpmInstall() sees a non-empty node_modules directory.
        // Note: cleanUp() deletes node_modules before the actual install runs,
        // so this copy won't survive — the install source is fake-stats/ below.
        File statsDir = new File(nodeModules,
                "@vaadin/vaadin-usage-statistics");
        statsDir.mkdirs();
        File statsPackageJson = new File(statsDir, "package.json");
        String statsPackageJsonContents = new String(getClass()
                .getResourceAsStream("fake-package-with-postinstall.json")
                .readAllBytes(), StandardCharsets.UTF_8);
        Files.writeString(statsPackageJson.toPath(), statsPackageJsonContents);

        // Local install source for "@vaadin/vaadin-usage-statistics".
        // Must live outside node_modules/ because cleanUp() deletes it
        // before npm/pnpm install. Using a local path avoids fetching a
        // non-existent version from the registry.
        File fakeStatsPackageJson = new File(
                new File(nodeModules.getParentFile(), "fake-stats"),
                "package.json");
        fakeStatsPackageJson.getParentFile().mkdirs();
        Files.writeString(fakeStatsPackageJson.toPath(),
                statsPackageJsonContents);

        // Fake that we have installed "foo" (not in the default postinstall
        // list unless explicitly added)
        File fooPackageJson = new File(
                new File(nodeModules.getParentFile(), "fake-foo"),
                "package.json");
        String fooPackageJsonContents = new String(getClass()
                .getResourceAsStream("fake-package-with-postinstall.json")
                .readAllBytes(), StandardCharsets.UTF_8);
        fooPackageJson.getParentFile().mkdirs();
        Files.writeString(fooPackageJson.toPath(), fooPackageJsonContents);

        File packageJsonFile = ensurePackageJson();
        JsonNode packageJson = getNodeUpdater().getPackageJson();
        ((ObjectNode) packageJson.get(DEV_DEPENDENCIES))
                .put("@vaadin/vaadin-usage-statistics", "./fake-stats");
        ((ObjectNode) packageJson.get(DEV_DEPENDENCIES)).put("foo",
                "./fake-foo");
        Files.writeString(packageJsonFile.toPath(), packageJson.toString());

    }

    @Test
    void runNpmInstall_noPostinstallScript_postIntstallNotExecuted()
            throws IOException, ExecutionFailedException {
        setupPostinstallPackages();

        // Remove postinstall script from "@vaadin/vaadin-usage-statistics"
        // in both the source directory and the node_modules copy, so that
        // even after npm/pnpm re-installs from source, no postinstall exists
        JsonNode statsPackageJsonContents = JacksonUtils
                .readTree(new String(getClass()
                        .getResourceAsStream(
                                "fake-package-with-postinstall.json")
                        .readAllBytes(), StandardCharsets.UTF_8));
        ((ObjectNode) statsPackageJsonContents.get("scripts"))
                .remove("postinstall");
        String noPostinstallContent = statsPackageJsonContents.toString();

        File statsSourcePackageJson = new File(
                new File(options.getNpmFolder(), "fake-stats"), "package.json");
        Files.writeString(statsSourcePackageJson.toPath(),
                noPostinstallContent);

        File statsNodeModulesPackageJson = new File(
                new File(options.getNodeModulesFolder(),
                        "@vaadin/vaadin-usage-statistics"),
                "package.json");
        Files.writeString(statsNodeModulesPackageJson.toPath(),
                noPostinstallContent);

        logger = new MockLogger();
        assertTrue(logger.isDebugEnabled());
        task.execute();

        assertFalse(((MockLogger) logger).getLogs().contains(
                "Running postinstall for '@vaadin/vaadin-usage-statistics'"),
                "@vaadin/vaadin-usage-statistics without postinstall should not have been executed");
    }

    @Test
    void runNpmInstall_postInstall_runOnlyForDefaultPackages()
            throws ExecutionFailedException, IOException {
        setupPostinstallPackages();
        task.execute();

        assertTrue(
                new File(
                        new File(options.getNodeModulesFolder(),
                                "@vaadin/vaadin-usage-statistics"),
                        "postinstall-file.txt").exists(),
                "Postinstall for '@vaadin/vaadin-usage-statistics' was not run");
        assertFalse(
                new File(new File(options.getNodeModulesFolder(), "foo"),
                        "postinstall-file.txt").exists(),
                "Postinstall for 'foo' should not have been run");
    }

    @Test
    void runNpmInstall_postInstall_runForDefinedAdditionalPackages()
            throws ExecutionFailedException, IOException {
        setupPostinstallPackages();
        task = createTask(List.of("foo"));
        task.execute();

        assertTrue(
                new File(
                        new File(options.getNodeModulesFolder(),
                                "@vaadin/vaadin-usage-statistics"),
                        "postinstall-file.txt").exists(),
                "Postinstall for '@vaadin/vaadin-usage-statistics' was not run");
        assertTrue(
                new File(new File(options.getNodeModulesFolder(), "foo"),
                        "postinstall-file.txt").exists(),
                "Postinstall for 'foo' was not run");
    }

    // https://github.com/vaadin/flow/issues/17663
    @Test
    @Timeout(30)
    void runNpmInstall_postInstallWritingLotsOfOutput_processDoesNotStuck()
            throws ExecutionFailedException, IOException {
        setupPostinstallPackages();

        File nodeModules = options.getNodeModulesFolder();
        File fooPackageJson = new File(
                new File(nodeModules.getParentFile(), "fake-foo"),
                "package.json");
        String fooPackageJsonContents = new String(getClass()
                .getResourceAsStream(
                        "fake-package-with-postinstall-writing-to-console.json")
                .readAllBytes(), StandardCharsets.UTF_8);
        Files.writeString(fooPackageJson.toPath(), fooPackageJsonContents);

        task = createTask(List.of("foo"));
        task.execute();

        assertTrue(
                new File(new File(options.getNodeModulesFolder(), "foo"),
                        "postinstall-console-file.txt").exists(),
                "Postinstall for 'foo' was not run");
    }

    @Test
    void shouldRunNpmInstallWhenFolderChanges() throws Exception {
        setupPostinstallPackages();

        String packageJsonHash = getNodeUpdater().getPackageJson()
                .get(VAADIN_DEP_KEY).get(HASH_KEY).asString();
        ObjectNode vaadinJson = JacksonUtils.createObjectNode();
        vaadinJson.put(HASH_KEY, packageJsonHash);
        vaadinJson.put(PROJECT_FOLDER, npmFolder.getAbsolutePath());
        File vaadinJsonFile = getNodeUpdater().getVaadinJsonFile();
        Files.createDirectories(vaadinJsonFile.toPath().getParent());
        Files.writeString(vaadinJsonFile.toPath(), vaadinJson.toString());

        assertFalse(task.isVaadinHashOrProjectFolderUpdated());
        vaadinJson.put(PROJECT_FOLDER, npmFolder.getAbsolutePath() + "foo");
        Files.writeString(vaadinJsonFile.toPath(), vaadinJson.toString());
        assertTrue(task.isVaadinHashOrProjectFolderUpdated());
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
    void updatePackageHash(ObjectNode packageJson) {
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
        File home = Files.createTempDirectory(temporaryFolder.toPath(), "tmp")
                .toFile();
        System.setProperty(userHome, home.getPath());
        try {
            File homeDir = new File(home, ".vaadin");
            File node = new File(homeDir,
                    FrontendUtils.isWindows()
                            ? "node-" + FrontendTools.DEFAULT_NODE_VERSION
                                    + "/node.exe"
                            : "node-" + FrontendTools.DEFAULT_NODE_VERSION
                                    + "/bin/node");
            Files.createDirectories(node.toPath());

            assertTrue(node.isDirectory(),
                    "node executable should be a directory");

            task.execute();

            assertFalse(node.isDirectory(),
                    "node executable should have been reinstalled");
            assertTrue(node.canExecute(),
                    "node executable should be executable");
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
        } else if ("pnpm".equals(getToolName())) {
            command += " --no-frozen-lockfile";
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
    void verifyPackageLockAndClean_lockfileVersion3_fileNotRemoved()
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
        Files.writeString(packageLockFile.toPath(), packageLockContent);

        task.verifyPackageLockAndClean();

        assertTrue(packageLockFile.exists(),
                "package-lock.json with version 3 should not be removed");
    }

    @Test
    void verifyPackageLockAndClean_lockfileVersion2_fileRemoved()
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
        Files.writeString(packageLockFile.toPath(), packageLockContent);

        task.verifyPackageLockAndClean();

        assertFalse(packageLockFile.exists(),
                "package-lock.json with version 2 should be removed");
    }

    @Test
    void verifyPackageLockAndClean_lockfileVersion1_fileRemoved()
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
        Files.writeString(packageLockFile.toPath(), packageLockContent);

        task.verifyPackageLockAndClean();

        assertFalse(packageLockFile.exists(),
                "package-lock.json with version 1 should be removed");
    }

    @Test
    void verifyPackageLockAndClean_withSpaces_correctlyParsed()
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
        Files.writeString(packageLockFile.toPath(), packageLockContent);

        task.verifyPackageLockAndClean();

        assertTrue(packageLockFile.exists(),
                "package-lock.json with version 3 (with spaces) should not be removed");
    }

    @Test
    void verifyPackageLockAndClean_pnpmEnabled_fileNotChecked()
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
        Files.writeString(packageLockFile.toPath(), packageLockContent);

        task.verifyPackageLockAndClean();

        assertTrue(packageLockFile.exists(),
                "package-lock.json should not be checked when pnpm is enabled");
    }

    @Test
    void verifyPackageLockAndClean_noLockfile_doesNotThrow() {
        File packageLockFile = new File(npmFolder, "package-lock.json");
        assertFalse(packageLockFile.exists(),
                "package-lock.json should not exist");

        // Should not throw any exception
        task.verifyPackageLockAndClean();

        assertFalse(packageLockFile.exists(),
                "package-lock.json should still not exist");
    }

    private void assumeNPMIsInUse() {
        assumeTrue(getClass().equals(TaskRunNpmInstallTest.class));
    }

}
