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
import java.util.ArrayList;
import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NotThreadSafe
@Tag("com.vaadin.flow.testcategory.SlowTests")
class TaskRunPnpmInstallTest extends TaskRunNpmInstallTest {

    private static final String PINNED_VERSION = "3.2.17";
    private static final List<String> POSTINSTALL_PACKAGES = List
            .of("@vaadin/vaadin-usage-statistics");

    @Override
    @BeforeEach
    void setUp() throws IOException, NoSuchFieldException {
        super.setUp();

        // create an empty package.json so as pnpm can be run without
        // error
        Files.writeString(new File(npmFolder, PACKAGE_JSON).toPath(), "{}");
    }

    @Override
    @Test
    void runNpmInstall_toolIsChanged_nodeModulesIsRemoved()
            throws ExecutionFailedException, IOException {
        File nodeModules = options.getNodeModulesFolder();
        Files.createDirectories(nodeModules.toPath());

        // create a fake file in the node modules dir to check that it's
        // removed
        File fakeFile = new File(nodeModules, ".fake.file");
        fakeFile.createNewFile();

        getNodeUpdater().modified = true;
        createTask().execute();

        assertFalse(fakeFile.exists());
    }

    @Override
    @Test
    void runNpmInstall_toolIsNotChanged_nodeModulesIsNotRemoved()
            throws ExecutionFailedException, IOException {
        File packageJson = new File(npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // create some package.json file so pnpm does some installation
        // into
        // node_modules folder
        Files.writeString(packageJson.toPath(),
                "{\"dependencies\": {" + "\"pnpm\": \"5.15.1\"}}");

        getNodeUpdater().modified = true;
        createTask().execute();

        // create a fake file in the node modules dir to check that it's
        // removed
        File fakeFile = new File(options.getNodeModulesFolder(), ".fake.file");
        fakeFile.createNewFile();

        getNodeUpdater().modified = true;
        createTask().execute();

        assertTrue(fakeFile.exists());
    }

    @Test
    void generateVersionsJson_userHasNoCustomVersions_platformIsMergedWithDevDeps()
            throws IOException {
        File packageJson = new File(npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file
        Files.writeString(packageJson.toPath(), "{}");

        File versions = File.createTempFile("tmp", null, temporaryFolder);
        // Platform defines a pinned version
        // @formatter:off
        Files.writeString(versions.toPath(), String.format(
                "{"
                  + "\"vaadin-overlay\": {"
                    + "\"npmName\": \"@vaadin/vaadin-overlay\","
                    + "\"jsVersion\": \"%s\""
                  + "}"
                + "}", PINNED_VERSION));
        // @formatter:on

        JsonNode object = getGeneratedVersionsContent(versions, packageJson);
        assertTrue(object.has("@vaadin/vaadin-overlay"));

        // Platform version takes precedence over dev deps
        assertEquals(PINNED_VERSION,
                object.get("@vaadin/vaadin-overlay").asString());
    }

    @Test
    void generateVersionsJson_userDefinedVersions_versionOnlyPinnedForNotAddedDependencies()
            throws IOException {
        File packageJson = new File(npmFolder, PACKAGE_JSON);
        packageJson.createNewFile();

        // Write package json file
        String loginVersion = "1.1.0-alpha1";
        String menuVersion = "1.1.0-alpha2";
        String notificationVersion = "1.4.0";
        String uploadVersion = "4.2.0";
        // @formatter:off
        Files.writeString(packageJson.toPath(), String.format(
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
                uploadVersion));
        // @formatter:on
        // Platform defines a pinned version

        String versionsLoginVersion = "1.1.0-alpha1";
        String versionsMenuBarVersion = "1.1.0-alpha1";
        String versionsNotificationVersion = "1.5.0-alpha1";
        String versionsUploadVersion = "4.2.0-beta2";

        File versions = File.createTempFile("tmp", null, temporaryFolder);
        // @formatter:off
        Files.writeString(versions.toPath(), String.format(
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
                versionsNotificationVersion, versionsUploadVersion));
        // @formatter:on

        JsonNode generatedVersions = getGeneratedVersionsContent(versions,
                packageJson);

        assertEquals(loginVersion,
                generatedVersions.get("@vaadin/vaadin-login").asString(),
                "Login version is the same for user and platform.");
        assertEquals(versionsNotificationVersion,
                generatedVersions.get("@vaadin/vaadin-notification").asString(),
                "Notification version should use platform");
    }

    @Test
    void runPnpmInstall_npmRcFileNotFound_newNpmRcFileIsGenerated()
            throws IOException, ExecutionFailedException {
        TaskRunNpmInstall task = createTask();
        task.execute();

        File npmRcFile = new File(npmFolder, ".npmrc");
        assertTrue(npmRcFile.exists());
        String content = Files.readString(npmRcFile.toPath());
        assertTrue(content.contains("shamefully-hoist"));
    }

    @Test
    void runPnpmInstall_npmRcFileGeneratedByVaadinFound_npmRcFileIsGenerated()
            throws IOException, ExecutionFailedException {
        File oldNpmRcFile = new File(npmFolder, ".npmrc");
        // @formatter:off
        String originalContent = "# NOTICE: this is an auto-generated file\n"
                + "shamefully-hoist=true\n"
                + "symlink=true\n";
        // @formatter:on
        Files.writeString(oldNpmRcFile.toPath(), originalContent);

        TaskRunNpmInstall task = createTask();
        task.execute();

        File newNpmRcFile = new File(npmFolder, ".npmrc");
        assertTrue(newNpmRcFile.exists());
        String content = Files.readString(newNpmRcFile.toPath());
        assertTrue(content.contains("shamefully-hoist"));
        assertFalse(content.contains("symlink=true"));
    }

    @Test
    void runPnpmInstall_customNpmRcFileFound_npmRcFileIsNotGenerated()
            throws IOException, ExecutionFailedException {
        File oldNpmRcFile = new File(npmFolder, ".npmrc");
        // @formatter:off
        String originalContent = "# A custom npmrc file for my project\n"
                + "symlink=true\n";
        // @formatter:on
        Files.writeString(oldNpmRcFile.toPath(), originalContent);

        TaskRunNpmInstall task = createTask();
        task.execute();

        File newNpmRcFile = new File(npmFolder, ".npmrc");
        assertTrue(newNpmRcFile.exists());
        String content = Files.readString(newNpmRcFile.toPath());
        assertEquals(originalContent, content);
    }

    @Test
    void runPnpmInstall_userVersionNewerThanPinned_installedOverlayVersionIsNotSpecifiedByPlatform()
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
        Files.writeString(packageJson.toPath(), packageJsonContent);

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
        JsonNode overlayPackage = JacksonUtils
                .readTree(Files.readString(overlayPackageJson.toPath()));
        assertEquals(customOverlayVersion,
                overlayPackage.get("version").asString());
    }

    @Test
    void runPnpmInstall_postInstall_runOnlyForDefaultPackages()
            throws ExecutionFailedException, IOException {
        setupPostinstallPackages();
        TaskRunNpmInstall task = createTask();
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
    void runPnpmInstall_postInstall_runForDefinedAdditionalPackages()
            throws ExecutionFailedException, IOException {
        setupPostinstallPackages();
        TaskRunNpmInstall task = createTask(List.of("foo"));
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

        TaskRunNpmInstall task = createTask(List.of("foo"));
        task.execute();

        assertTrue(
                new File(new File(options.getNodeModulesFolder(), "foo"),
                        "postinstall-console-file.txt").exists(),
                "Postinstall for 'foo' was not run");
    }

    @Test
    void runPnpmInstallAndCi_emptyDir_pnpmInstallAndCiIsExecuted()
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

    @Test
    void runPnpmInstall_devMode_usesNoFrozenLockfile()
            throws ExecutionFailedException, IOException {
        TaskRunNpmInstall task = createTask();
        getNodeUpdater().modified = true;

        task.execute();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).info(
                Mockito.eq("using '{}' for frontend package installation"),
                captor.capture());
        assertTrue(captor.getValue().contains("--no-frozen-lockfile"),
                "pnpm install in dev mode should use --no-frozen-lockfile");
    }

    @Test
    void runPnpmInstall_ciBuild_usesFrozenLockfile()
            throws ExecutionFailedException, IOException {
        TaskRunNpmInstall ciTask = createCiTask();
        getNodeUpdater().modified = true;

        ciTask.execute();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).info(
                Mockito.eq("using '{}' for frontend package installation"),
                captor.capture());
        String command = captor.getValue();
        assertTrue(command.contains("--frozen-lockfile"),
                "pnpm install in CI build should use --frozen-lockfile");
        assertFalse(command.contains("--no-frozen-lockfile"),
                "pnpm install in CI build should not use --no-frozen-lockfile");
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

        ObjectNode packageJson = JacksonUtils
                .readTree(Files.readString(packageJsonFile.toPath()));
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
