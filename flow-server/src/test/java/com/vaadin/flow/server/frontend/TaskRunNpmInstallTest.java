/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import elemental.json.Json;
import elemental.json.JsonObject;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.event.Level;

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
    protected File generatedPath;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private boolean ciBuild;

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();
        generatedPath = new File(npmFolder, "generated");
        generatedPath.mkdir();
        finder = Mockito.mock(ClassFinder.class);
        nodeUpdater = new NodeUpdater(finder,
                Mockito.mock(FrontendDependencies.class), npmFolder,
                generatedPath, TARGET, Mockito.mock(FeatureFlags.class)) {

            @Override
            public void execute() {
            }

            @Override
            Logger log() {
                return logger;
            }

        };
        task = createTask(new ArrayList<>(), false);
    }

    protected TaskRunNpmInstall createTask(List<String> additionalPostInstall,
            boolean ciBuild) {
        this.ciBuild = ciBuild;
        return new TaskRunNpmInstall(getNodeUpdater(), false, false,
                FrontendTools.DEFAULT_NODE_VERSION,
                URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT), false,
                false, additionalPostInstall, ciBuild);
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
    public void runNpmInstallAndCi_emptyDir_npmInstallAndCiIsExecuted()
            throws ExecutionFailedException, IOException {
        Assume.assumeTrue(getClass().equals(TaskRunNpmInstallTest.class));

        File nodeModules = getNodeUpdater().nodeModulesFolder;
        nodeModules.mkdir();
        getNodeUpdater().modified = false;

        ensurePackageJson();

        task.execute();
        Mockito.verify(logger).info(getRunningMsg());

        deleteDirectory(nodeModules);

        task = createTask(new ArrayList<>(), true);
        task.execute();
        Mockito.verify(logger).info(getRunningMsg());
    }

    @Test
    public void runNpmCi_emptyDir_npmCiFails() throws IOException {
        Assume.assumeTrue(getClass().equals(TaskRunNpmInstallTest.class));

        File nodeModules = getNodeUpdater().nodeModulesFolder;
        nodeModules.mkdir();
        getNodeUpdater().modified = false;

        ensurePackageJson();

        task = createTask(new ArrayList<>(), true);

        Assert.assertThrows(ExecutionFailedException.class, task::execute);
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
                        false, false, new ArrayList<>(), ciBuild));
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

    @Test
    public void runNpmInstall_noPostinstallScript_postIntstallNotExecuted()
            throws IOException, ExecutionFailedException {
        setupEsbuildAndFooInstallation();

        // Remove postinstall script from "esbuild"
        File esbuildPackageJson = new File(
                new File(getNodeUpdater().nodeModulesFolder.getParentFile(),
                        "fake-esbuild"),
                "package.json");
        JsonObject esbuildPackageJsonContents = Json.parse(IOUtils.toString(
                getClass().getResourceAsStream(
                        "fake-package-with-postinstall.json"),
                StandardCharsets.UTF_8));
        esbuildPackageJsonContents.getObject("scripts").remove("postinstall");
        FileUtils.write(esbuildPackageJson, esbuildPackageJsonContents.toJson(),
                StandardCharsets.UTF_8);

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
                new File(
                        new File(getNodeUpdater().nodeModulesFolder, "esbuild"),
                        "postinstall-file.txt").exists());
        Assert.assertFalse("Postinstall for 'foo' should not have been run",
                new File(new File(getNodeUpdater().nodeModulesFolder, "foo"),
                        "postinstall-file.txt").exists());
    }

    @Test
    public void runNpmInstall_postInstall_runForDefinedAdditionalPackages()
            throws ExecutionFailedException, IOException {
        setupEsbuildAndFooInstallation();
        task = createTask(Collections.singletonList("foo"), false);
        task.execute();

        Assert.assertTrue("Postinstall for 'esbuild' was not run",
                new File(
                        new File(getNodeUpdater().nodeModulesFolder, "esbuild"),
                        "postinstall-file.txt").exists());
        Assert.assertTrue("Postinstall for 'foo' was not run",
                new File(new File(getNodeUpdater().nodeModulesFolder, "foo"),
                        "postinstall-file.txt").exists());
    }

    @Test
    public void shouldRunNpmInstallWhenFolderChanges() throws Exception {
        setupEsbuildAndFooInstallation();

        String packageJsonHash = getNodeUpdater().getPackageJson()
                .getObject(VAADIN_DEP_KEY).getString(HASH_KEY);
        JsonObject vaadinJson = Json.createObject();
        vaadinJson.put(HASH_KEY, packageJsonHash);
        vaadinJson.put(PROJECT_FOLDER,
                getNodeUpdater().npmFolder.getAbsolutePath());
        File vaadinJsonFile = getNodeUpdater().getVaadinJsonFile();

        FileUtils.writeStringToFile(vaadinJsonFile, vaadinJson.toJson(), UTF_8);

        Assert.assertFalse(task.isVaadinHashOrProjectFolderUpdated());
        vaadinJson.put(PROJECT_FOLDER,
                getNodeUpdater().npmFolder.getAbsolutePath() + "foo");
        FileUtils.writeStringToFile(vaadinJsonFile, vaadinJson.toJson(), UTF_8);
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

    protected String getRunningMsg() {

        return "Running `" + getToolName() + " " + getCommand() + "` to "
                + "resolve and optionally download frontend dependencies. "
                + "This may take a moment, please stand by...";
    }

    private String getCommand() {
        String command = "install";
        if (ciBuild) {
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
            JsonObject packageJson = getNodeUpdater().getPackageJson();
            getNodeUpdater().writePackageFile(packageJson);
        }
        return file;
    }

    void deleteDirectory(File dir) throws IOException {
        Files.walk(dir.toPath()).sorted(Comparator.reverseOrder())
                .map(Path::toFile).forEach(File::delete);
    }

}
