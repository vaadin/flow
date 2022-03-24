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
import static com.vaadin.flow.testutil.FrontendStubs.createStubNode;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.testcategory.SlowTests;
import com.vaadin.flow.testutil.FrontendStubs;

import elemental.json.Json;
import elemental.json.JsonObject;

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
        FileUtils.write(new File(getNodeUpdater().npmFolder, PACKAGE_JSON),
                "{}", StandardCharsets.UTF_8);
    }

    @Test
    public void runPnpmInstall_overlayVersionIsPinnedViaPlatform_installedOverlayVersionIsSpecifiedByPlatform()
            throws IOException, ExecutionFailedException {
        File packageJson = new File(getNodeUpdater().npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file: dialog doesn't pin its Overlay
        // version which
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

        // The resulting version should be the one specified via
        // platform
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
        File nodeModules = getNodeUpdater().nodeModulesFolder;
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
        File packageJson = new File(getNodeUpdater().npmFolder, PACKAGE_JSON);
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
        File fakeFile = new File(getNodeUpdater().nodeModulesFolder,
                ".fake.file");
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
        assertRunNpmInstallThrows_vaadinHomeNodeIsAFolder(
                new TaskRunNpmInstall(getNodeUpdater(), true, true,
                        FrontendTools.DEFAULT_NODE_VERSION,
                        URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT),
                        false, false, POSTINSTALL_PACKAGES));
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
        getNodeUpdater().versionsPath = "./versions.json";
        task.execute();

        File file = new File(getNodeUpdater().npmFolder, "pnpmfile.js");
        File cjsFile = new File(getNodeUpdater().npmFolder, ".pnpmfile.cjs");
        Assert.assertTrue(file.exists() || cjsFile.exists());
        String content;
        if (file.exists()) {
            content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } else {
            content = FileUtils.readFileToString(cjsFile,
                    StandardCharsets.UTF_8);
        }
        MatcherAssert.assertThat(content,
                CoreMatchers.containsString("JSON.parse(fs.readFileSync"));
    }

    @Test
    public void runPnpmInstall_versionsJsonIsNotFound_pnpmHookFileIsGeneratedFromPackageJson()
            throws IOException, ExecutionFailedException {
        TaskRunNpmInstall task = createTask();
        getNodeUpdater().modified = true;
        getNodeUpdater().versionsPath = "./versions.json";
        task.execute();

        File file = new File(getNodeUpdater().npmFolder, "pnpmfile.js");
        File cjsFile = new File(getNodeUpdater().npmFolder, ".pnpmfile.cjs");
        Assert.assertTrue(file.exists() || cjsFile.exists());
        String content;
        if (file.exists()) {
            content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } else {
            content = FileUtils.readFileToString(cjsFile,
                    StandardCharsets.UTF_8);
        }
        MatcherAssert.assertThat(content,
                CoreMatchers.containsString("JSON.parse(fs.readFileSync"));
    }

    @Test
    public void generateVersionsJson_userHasNoCustomVersions_platformIsMergedWithDevDeps()
            throws IOException {
        File packageJson = new File(getNodeUpdater().npmFolder, PACKAGE_JSON);
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

        JsonObject object = getGeneratedVersionsContent(versions);
        Assert.assertTrue(object.hasKey("@vaadin/vaadin-overlay"));

        // Platform version takes precedence over dev deps
        Assert.assertEquals(PINNED_VERSION,
                object.getString("@vaadin/vaadin-overlay"));
    }

    @Test
    public void generateVersionsJson_userVersionNewerThanPinned_intalledOverlayVersionIsUserVersion()
            throws IOException {
        File packageJson = new File(getNodeUpdater().npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file
        final String customOverlayVersion = "3.3.0";
        // @formatter:off
        FileUtils.write(packageJson,
                "{"
                        + "\"vaadin\": {"
                          + "\"dependencies\": {"
                            + "\"@vaadin/vaadin-dialog\": \"2.3.0\","
                            + "\"@vaadin/vaadin-overlay\": \"" + PINNED_VERSION + "\""

                          + "},"
                          + "\"devDependencies\": {"
                              + "\"@vaadin/vaadin-notification\": \"1.3.9\""
                          + "},"
                        + "},"
                        + "\"dependencies\": {"
                          + "\"@vaadin/vaadin-dialog\": \"2.3.0\","
                          + "\"@vaadin/vaadin-overlay\": \"" + customOverlayVersion + "\""
                        + "},"
                        + "\"devDependencies\": {"
                            + "\"@vaadin/vaadin-notification\": \"1.4.0\""
                        + "},"
                     + "}",
                StandardCharsets.UTF_8);
        // @formatter:on

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

        JsonObject versionsJson = getGeneratedVersionsContent(versions);
        Assert.assertEquals(
                "Generated versions json should have keys for each dependency",
                3, versionsJson.keys().length);
        Assert.assertEquals("Overlay should be pinned to user version",
                customOverlayVersion,
                versionsJson.getString("@vaadin/vaadin-overlay"));
    }

    @Test
    public void generateVersionsJson_userVersionOlderThanPinned_installedOverlayPinnedVersionIsUserVersion()
            throws IOException {
        File packageJson = new File(getNodeUpdater().npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file
        final String customOverlayVersion = "3.1.0";

        // @formatter:off
        FileUtils.write(packageJson,
                "{"
                        + "\"vaadin\": {"
                        + "\"dependencies\": {"
                            + "\"@vaadin/vaadin-dialog\": \"2.3.0\","
                            + "\"@vaadin/vaadin-overlay\": \"" + PINNED_VERSION + "\""
                        + "},"
                        + "\"devDependencies\": {"
                            + "\"@vaadin/vaadin-notification\": \"1.4.0\""
                        + "},"
                        + "},"
                        + "\"dependencies\": {"
                            + "\"@vaadin/vaadin-dialog\": \"2.3.0\","
                            + "\"@vaadin/vaadin-overlay\": \"" + customOverlayVersion + "\""
                        + "},"
                            + "\"devDependencies\": {"
                            + "\"@vaadin/vaadin-notification\": \"1.3.9\""
                            + "},"
                        + "}",
                StandardCharsets.UTF_8);
        // @formatter:on

        File versions = temporaryFolder.newFile();
        // Platform defines a pinned version
        // @formatter:off
        FileUtils.write(versions,String.format(
                "{"
                        + "\"vaadin-overlay\": {"
                        + "\"npmName\": \"@vaadin/vaadin-overlay\","
                        + "\"jsVersion\": \"%s\""
                        + "}"
                        + "}", PINNED_VERSION), StandardCharsets.UTF_8);
        // @formatter:on

        JsonObject versionsJson = getGeneratedVersionsContent(versions);
        Assert.assertEquals(
                "Generated versions json should have keys for package.json dependencies (also dev)",
                3, versionsJson.keys().length);
        Assert.assertEquals("Overlay should be pinned to user version",
                customOverlayVersion,
                versionsJson.getString("@vaadin/vaadin-overlay"));
    }

    @Test
    public void generateVersionsJson_userDefinedVersions_versionOnlyPinnedForNotAddedDependencies()
            throws IOException {
        File packageJson = new File(getNodeUpdater().npmFolder, PACKAGE_JSON);
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

        JsonObject generatedVersions = getGeneratedVersionsContent(versions);

        Assert.assertEquals("Login version is the same for user and platform.",
                loginVersion,
                generatedVersions.getString("@vaadin/vaadin-login"));
        Assert.assertEquals("Menu Bar should be pinned to user version.",
                menuVersion,
                generatedVersions.getString("@vaadin/vaadin-menu-bar"));
        Assert.assertEquals("Notification version should use platform",
                versionsNotificationVersion,
                generatedVersions.getString("@vaadin/vaadin-notification"));
        Assert.assertEquals("Upload should be pinned to user version.",
                uploadVersion,
                generatedVersions.getString("@vaadin/vaadin-upload"));
    }

    @Test
    public void runPnpmInstall_npmRcFileNotFound_newNpmRcFileIsGenerated()
            throws IOException, ExecutionFailedException {
        TaskRunNpmInstall task = createTask();
        task.execute();

        File npmRcFile = new File(getNodeUpdater().npmFolder, ".npmrc");
        Assert.assertTrue(npmRcFile.exists());
        String content = FileUtils.readFileToString(npmRcFile,
                StandardCharsets.UTF_8);
        Assert.assertTrue(content.contains("shamefully-hoist"));
    }

    @Test
    public void runPnpmInstall_npmRcFileGeneratedByVaadinFound_npmRcFileIsGenerated()
            throws IOException, ExecutionFailedException {
        File oldNpmRcFile = new File(getNodeUpdater().npmFolder, ".npmrc");
        // @formatter:off
        String originalContent = "# NOTICE: this is an auto-generated file\n"
                + "shamefully-hoist=true\n"
                + "symlink=true\n";
        // @formatter:on
        FileUtils.writeStringToFile(oldNpmRcFile, originalContent,
                StandardCharsets.UTF_8);

        TaskRunNpmInstall task = createTask();
        task.execute();

        File newNpmRcFile = new File(getNodeUpdater().npmFolder, ".npmrc");
        Assert.assertTrue(newNpmRcFile.exists());
        String content = FileUtils.readFileToString(newNpmRcFile,
                StandardCharsets.UTF_8);
        Assert.assertTrue(content.contains("shamefully-hoist"));
        Assert.assertFalse(content.contains("symlink=true"));
    }

    @Test
    public void runPnpmInstall_customNpmRcFileFound_npmRcFileIsNotGenerated()
            throws IOException, ExecutionFailedException {
        File oldNpmRcFile = new File(getNodeUpdater().npmFolder, ".npmrc");
        // @formatter:off
        String originalContent = "# A custom npmrc file for my project\n"
                + "symlink=true\n";
        // @formatter:on
        FileUtils.writeStringToFile(oldNpmRcFile, originalContent,
                StandardCharsets.UTF_8);

        TaskRunNpmInstall task = createTask();
        task.execute();

        File newNpmRcFile = new File(getNodeUpdater().npmFolder, ".npmrc");
        Assert.assertTrue(newNpmRcFile.exists());
        String content = FileUtils.readFileToString(newNpmRcFile,
                StandardCharsets.UTF_8);
        Assert.assertEquals(originalContent, content);
    }

    @Test
    public void runPnpmInstall_userVersionNewerThanPinned_installedOverlayVersionIsNotSpecifiedByPlatform()
            throws IOException, ExecutionFailedException {
        File packageJson = new File(getNodeUpdater().npmFolder, PACKAGE_JSON);
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
                Json.parse(packageJsonContent), NodeUpdater.DEPENDENCIES);
        // Platform defines a pinned version
        TaskRunNpmInstall task = createTask(
                versionsJsonFilter.getFilteredVersions(
                        Json.parse("{ \"@vaadin/vaadin-overlay\":\""
                                + PINNED_VERSION + "\"}"))
                        .toJson());
        task.execute();

        File overlayPackageJson = new File(getNodeUpdater().nodeModulesFolder,
                "@vaadin/vaadin-overlay/package.json");

        // The resulting version should be the one specified by the user
        JsonObject overlayPackage = Json.parse(FileUtils
                .readFileToString(overlayPackageJson, StandardCharsets.UTF_8));
        Assert.assertEquals(customOverlayVersion,
                overlayPackage.getString("version"));
    }

    @Test
    public void runPnpmInstall_frameworkCollectedVersionNewerThanPinned_installedOverlayVersionIsNotSpecifiedByPlatform()
            throws IOException, ExecutionFailedException {
        File packageJson = new File(getNodeUpdater().npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file
        final String customOverlayVersion = "3.3.0";
        // @formatter:off
        final String packageJsonContent =
            "{"
                + "\"vaadin\": {"
                    + "\"dependencies\": {"
                        + "\"@vaadin/vaadin-dialog\": \"2.3.0\","
                        + "\"@vaadin/vaadin-overlay\": \"" + customOverlayVersion + "\""
                    + "}"
                + "},"
                + "\"dependencies\": {"
                    + "\"@vaadin/vaadin-dialog\": \"2.3.0\","
                    + "\"@vaadin/vaadin-overlay\": \"" + customOverlayVersion + "\""
                + "}"
            + "}";
        // @formatter:on

        FileUtils.write(packageJson, packageJsonContent,
                StandardCharsets.UTF_8);

        final VersionsJsonFilter versionsJsonFilter = new VersionsJsonFilter(
                Json.parse(packageJsonContent), NodeUpdater.DEPENDENCIES);
        // Platform defines a pinned version
        TaskRunNpmInstall task = createTask(
                versionsJsonFilter.getFilteredVersions(
                        Json.parse("{ \"@vaadin/vaadin-overlay\":\""
                                + PINNED_VERSION + "\"}"))
                        .toJson());
        task.execute();

        File overlayPackageJson = new File(getNodeUpdater().nodeModulesFolder,
                "@vaadin/vaadin-overlay/package.json");

        // The resulting version should be the one collected from the
        // annotations in the project
        JsonObject overlayPackage = Json.parse(FileUtils
                .readFileToString(overlayPackageJson, StandardCharsets.UTF_8));
        Assert.assertEquals(customOverlayVersion,
                overlayPackage.getString("version"));
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
        createStubNode(nodeStub, npmStub,
                getNodeUpdater().npmFolder.getAbsolutePath());

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
                new File(
                        new File(getNodeUpdater().nodeModulesFolder, "esbuild"),
                        "postinstall-file.txt").exists());
        Assert.assertFalse("Postinstall for 'foo' should not have been run",
                new File(new File(getNodeUpdater().nodeModulesFolder, "foo"),
                        "postinstall-file.txt").exists());
    }

    @Test
    public void runPnpmInstall_postInstall_runForDefinedAdditionalPackages()
            throws ExecutionFailedException, IOException {
        setupEsbuildAndFooInstallation();
        TaskRunNpmInstall task = createTask(Collections.singletonList("foo"));
        task.execute();

        Assert.assertTrue("Postinstall for 'esbuild' was not run",
                new File(
                        new File(getNodeUpdater().nodeModulesFolder, "esbuild"),
                        "postinstall-file.txt").exists());
        Assert.assertTrue("Postinstall for 'foo' was not run",
                new File(new File(getNodeUpdater().nodeModulesFolder, "foo"),
                        "postinstall-file.txt").exists());
    }

    @Override
    protected String getToolName() {
        return "pnpm";
    }

    protected TaskRunNpmInstall createTask() {
        return createTask(new ArrayList<>());
    }

    @Override
    protected TaskRunNpmInstall createTask(List<String> additionalPostInstall) {
        return new TaskRunNpmInstall(getNodeUpdater(), true, false,
                FrontendTools.DEFAULT_NODE_VERSION,
                URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT), false,
                false, additionalPostInstall);
    }

    protected TaskRunNpmInstall createTask(String versionsContent) {
        return new TaskRunNpmInstall(createAndRunNodeUpdater(versionsContent),
                true, false, FrontendTools.DEFAULT_NODE_VERSION,
                URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT), false,
                false, new ArrayList<>());
    }

    private JsonObject getGeneratedVersionsContent(File versions)
            throws IOException {
        ClassFinder classFinder = getClassFinder();
        Mockito.when(classFinder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(versions.toURI().toURL());

        String path = getNodeUpdater().generateVersionsJson();

        File generatedVersionsFile = new File(getNodeUpdater().npmFolder, path);
        return Json.parse(FileUtils.readFileToString(generatedVersionsFile,
                StandardCharsets.UTF_8));

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
        return new NodeUpdater(finder, Mockito.mock(FrontendDependencies.class),
                npmFolder, generatedPath, null, TARGET,
                Mockito.mock(FeatureFlags.class)) {

            @Override
            public void execute() {
                try {
                    versionsPath = generateVersionsJson();
                } catch (Exception e) {
                    versionsPath = null;
                }
            }

            @Override
            protected String generateVersionsJson() {
                try {
                    if (versionsContent != null) {
                        FileUtils.write(new File(npmFolder, "versions.json"),
                                versionsContent, StandardCharsets.UTF_8);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return "./versions.json";
            }
        };
    }

}
