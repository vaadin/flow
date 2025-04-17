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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.slf4j.Logger;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.testcategory.SlowTests;
import com.vaadin.flow.testutil.FrontendStubs;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.testutil.FrontendStubs.createStubNode;

@NotThreadSafe
@Category(SlowTests.class)
public class TaskRunPnpmInstallTest extends TaskRunNpmInstallTest {

    private static final String PINNED_VERSION = "3.2.17";
    private static final List<String> POSTINSTALL_PACKAGES = Collections
            .singletonList("esbuild");

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();

        // create an empty package.json so as pnpm can be run without
        // error
        FileUtils.write(new File(npmFolder, PACKAGE_JSON), "{}",
                StandardCharsets.UTF_8);
    }

    @Override
    @Test
    public void runNpmInstall_toolIsChanged_nodeModulesIsRemoved()
            throws ExecutionFailedException, IOException {
        File nodeModules = options.getNodeModulesFolder();
        FileUtils.forceMkdir(nodeModules);

        // create a fake file in the node modules dir to check that it's
        // removed
        File fakeFile = new File(nodeModules, ".fake.file");
        fakeFile.createNewFile();

        getNodeUpdater().modified = true;
        createTask().execute();

        Assert.assertFalse(fakeFile.exists());
    }

    @Override
    @Test
    public void runNpmInstall_toolIsNotChanged_nodeModulesIsNotRemoved()
            throws ExecutionFailedException, IOException {
        File packageJson = new File(npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // create some package.json file so pnpm does some installation
        // into
        // node_modules folder
        FileUtils.write(packageJson,
                "{\"dependencies\": {" + "\"pnpm\": \"5.15.1\"}}",
                StandardCharsets.UTF_8);

        getNodeUpdater().modified = true;
        createTask().execute();

        // create a fake file in the node modules dir to check that it's
        // removed
        File fakeFile = new File(options.getNodeModulesFolder(), ".fake.file");
        fakeFile.createNewFile();

        getNodeUpdater().modified = true;
        createTask().execute();

        Assert.assertTrue(fakeFile.exists());
    }

    @Override
    public void runNpmInstall_vaadinHomeNodeIsAFolder_throws()
            throws IOException, ExecutionFailedException {
        exception.expectMessage(
                "it's either not a file or not a 'node' executable.");
        options.withHomeNodeExecRequired(true).withEnablePnpm(true)
                .withNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                .withNodeDownloadRoot(
                        URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT));
        options.withPostinstallPackages(POSTINSTALL_PACKAGES);

        assertRunNpmInstallThrows_vaadinHomeNodeIsAFolder(
                new TaskRunNpmInstall(getNodeUpdater(), options));
    }

    @Test
    public void generateVersionsJson_userHasNoCustomVersions_platformIsMergedWithDevDeps()
            throws IOException {
        File packageJson = new File(npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file
        FileUtils.write(packageJson, "{}", StandardCharsets.UTF_8);

        File versions = temporaryFolder.newFile();
        // Platform defines a pinned version
        // @formatter:off
        FileUtils.write(versions, String.format(
                "{"
                  + "\"vaadin-overlay\": {"
                    + "\"npmName\": \"@vaadin/vaadin-overlay\","
                    + "\"jsVersion\": \"%s\""
                  + "}"
                + "}", PINNED_VERSION), StandardCharsets.UTF_8);
        // @formatter:on

        JsonNode object = getGeneratedVersionsContent(versions, packageJson);
        Assert.assertTrue(object.has("@vaadin/vaadin-overlay"));

        // Platform version takes precedence over dev deps
        Assert.assertEquals(PINNED_VERSION,
                object.get("@vaadin/vaadin-overlay").textValue());
    }

    @Test
    public void generateVersionsJson_userDefinedVersions_versionOnlyPinnedForNotAddedDependencies()
            throws IOException {
        File packageJson = new File(npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file
        String loginVersion = "1.1.0-alpha1";
        String menuVersion = "1.1.0-alpha2";
        String notificationVersion = "1.4.0";
        String uploadVersion = "4.2.0";
        // @formatter:off
        FileUtils.write(packageJson, String.format(
                "{"
                    + "\"vaadin\": {"
                      + "\"dependencies\": {"
                        + "\"@vaadin/vaadin-login\": \"%s\","
                        + "\"@vaadin/vaadin-menu-bar\": \"%s\","
                        + "\"@vaadin/vaadin-notification\": \"%s\","
                        + "\"@vaadin/vaadin-upload\": \"%s\""
                      + "}"
                    + "},"
                    + "\"dependencies\": {"
                      + "\"@vaadin/vaadin-login\": \"%s\","
                      + "\"@vaadin/vaadin-menu-bar\": \"%s\","
                      + "\"@vaadin/vaadin-notification\": \"%s\","
                      + "\"@vaadin/vaadin-upload\": \"%s\""
                    + "}"
                + "}",
                loginVersion, "1.0.0", notificationVersion,
                "4.0.0", loginVersion, menuVersion, notificationVersion,
                uploadVersion), StandardCharsets.UTF_8);
        // @formatter:on
        // Platform defines a pinned version

        String versionsLoginVersion = "1.1.0-alpha1";
        String versionsMenuBarVersion = "1.1.0-alpha1";
        String versionsNotificationVersion = "1.5.0-alpha1";
        String versionsUploadVersion = "4.2.0-beta2";

        File versions = temporaryFolder.newFile();
        // @formatter:off
        FileUtils.write(versions,String.format(
                "{"
                    + "\"vaadin-login\": {"
                        + "\"npmName\": \"@vaadin/vaadin-login\","
                        + "\"jsVersion\": \"%s\""
                    + "},"
                    + "\"vaadin-menu-bar\": {"
                        + "\"npmName\": \"@vaadin/vaadin-menu-bar\","
                        + "\"jsVersion\": \"%s\""
                    + "},"
                    + "\"vaadin-notification\": {"
                        + "\"npmName\": \"@vaadin/vaadin-notification\","
                        + "\"jsVersion\": \"%s\""
                    + "},"
                    + "\"vaadin-upload\": {"
                        + "\"npmName\": \"@vaadin/vaadin-upload\","
                        + "\"jsVersion\": \"%s\""
                    + "}"
                + "}",
                versionsLoginVersion, versionsMenuBarVersion,
                versionsNotificationVersion, versionsUploadVersion), StandardCharsets.UTF_8);
        // @formatter:on

        JsonNode generatedVersions = getGeneratedVersionsContent(versions,
                packageJson);

        Assert.assertEquals("Login version is the same for user and platform.",
                loginVersion,
                generatedVersions.get("@vaadin/vaadin-login").textValue());
        Assert.assertEquals("Notification version should use platform",
                versionsNotificationVersion, generatedVersions
                        .get("@vaadin/vaadin-notification").textValue());
    }

    @Test
    public void runPnpmInstall_npmRcFileNotFound_newNpmRcFileIsGenerated()
            throws IOException, ExecutionFailedException {
        TaskRunNpmInstall task = createTask();
        task.execute();

        File npmRcFile = new File(npmFolder, ".npmrc");
        Assert.assertTrue(npmRcFile.exists());
        String content = FileUtils.readFileToString(npmRcFile,
                StandardCharsets.UTF_8);
        Assert.assertTrue(content.contains("shamefully-hoist"));
    }

    @Test
    public void runPnpmInstall_npmRcFileGeneratedByVaadinFound_npmRcFileIsGenerated()
            throws IOException, ExecutionFailedException {
        File oldNpmRcFile = new File(npmFolder, ".npmrc");
        // @formatter:off
        String originalContent = "# NOTICE: this is an auto-generated file\n"
                + "shamefully-hoist=true\n"
                + "symlink=true\n";
        // @formatter:on
        FileUtils.writeStringToFile(oldNpmRcFile, originalContent,
                StandardCharsets.UTF_8);

        TaskRunNpmInstall task = createTask();
        task.execute();

        File newNpmRcFile = new File(npmFolder, ".npmrc");
        Assert.assertTrue(newNpmRcFile.exists());
        String content = FileUtils.readFileToString(newNpmRcFile,
                StandardCharsets.UTF_8);
        Assert.assertTrue(content.contains("shamefully-hoist"));
        Assert.assertFalse(content.contains("symlink=true"));
    }

    @Test
    public void runPnpmInstall_customNpmRcFileFound_npmRcFileIsNotGenerated()
            throws IOException, ExecutionFailedException {
        File oldNpmRcFile = new File(npmFolder, ".npmrc");
        // @formatter:off
        String originalContent = "# A custom npmrc file for my project\n"
                + "symlink=true\n";
        // @formatter:on
        FileUtils.writeStringToFile(oldNpmRcFile, originalContent,
                StandardCharsets.UTF_8);

        TaskRunNpmInstall task = createTask();
        task.execute();

        File newNpmRcFile = new File(npmFolder, ".npmrc");
        Assert.assertTrue(newNpmRcFile.exists());
        String content = FileUtils.readFileToString(newNpmRcFile,
                StandardCharsets.UTF_8);
        Assert.assertEquals(originalContent, content);
    }

    @Test
    public void runPnpmInstall_userVersionNewerThanPinned_installedOverlayVersionIsNotSpecifiedByPlatform()
            throws IOException, ExecutionFailedException {
        File packageJson = new File(npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file
        final String customOverlayVersion = "3.3.0";
        // @formatter:off
        final String packageJsonContent =
            "{"
                + "\"dependencies\": {"
                    + "\"@vaadin/vaadin-dialog\": \"2.3.0\","
                    + "\"@vaadin/vaadin-overlay\": \"" + customOverlayVersion + "\""
                + "}"
            + "}";
        // @formatter:on
        FileUtils.write(packageJson, packageJsonContent,
                StandardCharsets.UTF_8);

        final VersionsJsonFilter versionsJsonFilter = new VersionsJsonFilter(
                JacksonUtils.readTree(packageJsonContent),
                NodeUpdater.DEPENDENCIES);
        // Platform defines a pinned version
        TaskRunNpmInstall task = createTask(versionsJsonFilter
                .getFilteredVersions(
                        JacksonUtils.readTree("{ \"@vaadin/vaadin-overlay\":\""
                                + PINNED_VERSION + "\"}"),
                        "test-versions.json")
                .toString());
        task.execute();

        File overlayPackageJson = new File(options.getNodeModulesFolder(),
                "@vaadin/vaadin-overlay/package.json");

        // The resulting version should be the one specified by the user
        JsonNode overlayPackage = JacksonUtils.readTree(FileUtils
                .readFileToString(overlayPackageJson, StandardCharsets.UTF_8));
        Assert.assertEquals(customOverlayVersion,
                overlayPackage.get("version").textValue());
    }

    @Test
    public void runPnpmInstall_checkFolderIsAcceptableByNpm_throwsOnWindows()
            throws ExecutionFailedException, IOException {
        Assume.assumeTrue("This test is only for Windows, since the issue with "
                + "whitespaces in npm processed directories reproduces only on "
                + "Windows", FrontendUtils.isWindows());

        // given
        File npmCacheFolder = temporaryFolder.newFolder("Foo Bar");
        FrontendStubs.ToolStubInfo nodeStub = FrontendStubs.ToolStubInfo.none();
        FrontendStubs.ToolStubInfo npmStub = FrontendStubs.ToolStubInfo
                .builder(FrontendStubs.Tool.NPM).withVersion("6.0.0")
                .withCacheDir(npmCacheFolder.getAbsolutePath()).build();
        createStubNode(nodeStub, npmStub, npmFolder.getAbsolutePath());

        exception.expect(ExecutionFailedException.class);
        exception.expectMessage(CoreMatchers.containsString(
                "The path to npm cache contains whitespaces, and the currently installed npm version doesn't accept this."));

        TaskRunNpmInstall task = createTask();
        getNodeUpdater().modified = true;

        // when
        task.execute();

        // then exception is thrown
    }

    @Test
    public void runPnpmInstall_postInstall_runOnlyForDefaultPackages()
            throws ExecutionFailedException, IOException {
        setupEsbuildAndFooInstallation();
        TaskRunNpmInstall task = createTask();
        task.execute();

        Assert.assertTrue("Postinstall for 'esbuild' was not run",
                new File(new File(options.getNodeModulesFolder(), "esbuild"),
                        "postinstall-file.txt").exists());
        Assert.assertFalse("Postinstall for 'foo' should not have been run",
                new File(new File(options.getNodeModulesFolder(), "foo"),
                        "postinstall-file.txt").exists());
    }

    @Test
    public void runPnpmInstall_postInstall_runForDefinedAdditionalPackages()
            throws ExecutionFailedException, IOException {
        setupEsbuildAndFooInstallation();
        TaskRunNpmInstall task = createTask(Collections.singletonList("foo"));
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

        TaskRunNpmInstall task = createTask(Collections.singletonList("foo"));
        task.execute();

        Assert.assertTrue("Postinstall for 'foo' was not run",
                new File(new File(options.getNodeModulesFolder(), "foo"),
                        "postinstall-console-file.txt").exists());
    }

    @Test
    public void runPnpmInstallAndCi_emptyDir_pnpmInstallAndCiIsExecuted()
            throws ExecutionFailedException, IOException {
        TaskRunNpmInstall task = createTask();

        File nodeModules = options.getNodeModulesFolder();
        nodeModules.mkdir();
        getNodeUpdater().modified = false;

        task.execute();
        Mockito.verify(logger).info(getRunningMsg());

        deleteDirectory(nodeModules);

        TaskRunNpmInstall ciTask = createCiTask();
        ciTask.execute();
        Mockito.verify(logger).info(getRunningMsg());
    }

    @Override
    protected String getToolName() {
        return "pnpm";
    }

    protected TaskRunNpmInstall createTask() {
        return createTask(new ArrayList<>());
    }

    private TaskRunNpmInstall createCiTask() {
        NodeUpdater updater = getNodeUpdater();
        options.withEnablePnpm(true)
                .withNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                .withNodeDownloadRoot(
                        URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT))
                .withCiBuild(true);
        return new TaskRunNpmInstall(updater, options);
    }

    @Override
    protected TaskRunNpmInstall createTask(List<String> additionalPostInstall) {
        NodeUpdater updater = createAndRunNodeUpdater(null);
        options.withEnablePnpm(true)
                .withNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                .withNodeDownloadRoot(
                        URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT))
                .withPostinstallPackages(additionalPostInstall);
        return new TaskRunNpmInstall(updater, options);
    }

    protected TaskRunNpmInstall createTask(String versionsContent) {
        NodeUpdater updater = createAndRunNodeUpdater(versionsContent);
        options.withEnablePnpm(true)
                .withNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                .withNodeDownloadRoot(
                        URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT));
        return new TaskRunNpmInstall(updater, options);
    }

    private JsonNode getGeneratedVersionsContent(File versions,
            File packageJsonFile) throws IOException {
        ClassFinder classFinder = getClassFinder();
        Mockito.when(
                classFinder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versions.toURI().toURL());

        ObjectNode packageJson = JacksonUtils.readTree(FileUtils
                .readFileToString(packageJsonFile, StandardCharsets.UTF_8));
        getNodeUpdater().generateVersionsJson(packageJson);
        return getNodeUpdater().versionsJson;
    }

    private NodeUpdater createAndRunNodeUpdater(String versionsContent) {
        NodeUpdater nodeUpdater = createNodeUpdater(versionsContent);
        try {
            nodeUpdater.execute();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "NodeUpdater failed to genereate the versions.json file");
        }

        return nodeUpdater;
    }

    private NodeUpdater createNodeUpdater(String versionsContent) {
        options.withBuildDirectory(TARGET);

        return new NodeUpdater(Mockito.mock(FrontendDependencies.class),
                options) {

            @Override
            public void execute() {
                try {
                    generateVersionsJson(JacksonUtils.createObjectNode());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            ObjectNode getPlatformPinnedDependencies() {
                if (versionsContent != null) {
                    return JacksonUtils.readTree(versionsContent);
                } else {
                    return JacksonUtils.createObjectNode();
                }
            }

            @Override
            Logger log() {
                return logger;
            }
        };
    }

}
