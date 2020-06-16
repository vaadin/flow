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
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;

import javax.validation.constraints.AssertTrue;
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

        // ensure there is a valid pnpm installed in the system
        new FrontendTools(getNodeUpdater().npmFolder.getAbsolutePath(),
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath())
                        .ensurePnpm();
        // create an empty package.json so as pnpm can be run without error
        FileUtils.write(new File(getNodeUpdater().npmFolder, PACKAGE_JSON),
                "{}", StandardCharsets.UTF_8);
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
        File nodeModules = getNodeUpdater().nodeModulesFolder;
        FileUtils.forceMkdir(nodeModules);

        // create a fake file in the node modules dir to check that it's removed
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
    public void runNpmInstall_vaadinHomeNodeIsAFolder_throws()
            throws IOException, ExecutionFailedException {
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

    @Test
    public void generateVersionsJson_userHasNoCustomVersions_platformIsMergedWithDevDeps()
            throws IOException, ExecutionFailedException {
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

        File devDeps = temporaryFolder.newFile();
        // Platform defines a pinned version
        // @formatter:off
        FileUtils.write(devDeps,
                "{"
                     + "\"@vaadin/vaadin-notification\":  \"1.3.9\","
                     + "\"@vaadin/vaadin-overlay\":  \"3.3.0\""
                     + "}", StandardCharsets.UTF_8);
        // @formatter:on

        JsonObject object = getGeneratedVersionsContent(versions, devDeps);
        Assert.assertTrue(object.hasKey("@vaadin/vaadin-overlay"));
        Assert.assertTrue(object.hasKey("@vaadin/vaadin-notification"));

        // Platform version takes precedence over dev deps
        Assert.assertEquals(PINNED_VERSION,
                object.getString("@vaadin/vaadin-overlay"));
        Assert.assertEquals("1.3.9",
                object.getString("@vaadin/vaadin-notification"));
    }

    @Test
    public void generateVersionsJson_userVersionNewerThanPinned_intalledOverlayVersionIsUserVersion()
            throws IOException, ExecutionFailedException {
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

        File devDeps = temporaryFolder.newFile();
        // Platform defines a pinned version
        // @formatter:off
        FileUtils.write(devDeps,
                "{"
                     + "\"@vaadin/vaadin-notification\":  \"1.3.9\""
                     + "}", StandardCharsets.UTF_8);
        // @formatter:on

        JsonObject versionsJson = getGeneratedVersionsContent(versions, devDeps);
        Assert.assertEquals("Generated versions json should have 2 keys",
                2,
                versionsJson.keys().length);
        Assert.assertEquals("Overlay should be pinned to user version",
                customOverlayVersion,
                versionsJson.getString("@vaadin/vaadin-overlay"));
        Assert.assertEquals("Notification should be pinned to user version",
                "1.4.0",
                versionsJson.getString("@vaadin/vaadin-notification"));
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

        File devDeps = temporaryFolder.newFile();
        // Platform defines a pinned version
        // @formatter:off
        FileUtils.write(devDeps,
                "{"
                     + "\"@vaadin/vaadin-notification\":  \"1.4.0\""
                     + "}", StandardCharsets.UTF_8);
        // @formatter:on

        JsonObject versionsJson = getGeneratedVersionsContent(versions, devDeps);
        Assert.assertEquals("Generated versions json should have 2 keys",
                2,
                versionsJson.keys().length);
        Assert.assertEquals("Overlay should be pinned to user version",
                customOverlayVersion,
                versionsJson.getString("@vaadin/vaadin-overlay"));
        Assert.assertEquals("Notification should be pinned to user version",
                "1.3.9",
                versionsJson.getString("@vaadin/vaadin-notification"));
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

        File devDeps = temporaryFolder.newFile();
        // Platform defines a pinned version
        // @formatter:off
        FileUtils.write(devDeps,
                "{"
                     + "\"@vaadin/vaadin-button\":  \"2.2.2\""
                     + "}", StandardCharsets.UTF_8);
        // @formatter:on

        JsonObject generatedVersions = getGeneratedVersionsContent(versions,
                devDeps);

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
        Assert.assertEquals("Button version should use dev dependency", "2.2.2",
                generatedVersions.getString("@vaadin/vaadin-button"));
    }

    @Test
    public void generateVersionsJson_noVersions_noDevDeps_returnNull()
            throws IOException {
        TaskRunNpmInstall task = createTask();

        Assert.assertNull(task.generateVersionsJson());
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

    private TaskRunNpmInstall createTaskWithDevDepsLocked(String devDepsPath) {
        return new TaskRunNpmInstall(getClassFinder(), getNodeUpdater(), true,
                false) {
            @Override
            protected String getDevDependenciesFilePath() {
                return devDepsPath;
            }
        };
    }

    private JsonObject getGeneratedVersionsContent(File versions, File devDeps)
            throws MalformedURLException, IOException {
        String devDepsPath = "foo";

        ClassFinder classFinder = getClassFinder();
        Mockito.when(classFinder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(versions.toURI().toURL());
        Mockito.when(classFinder.getResource(devDepsPath))
                .thenReturn(devDeps.toURI().toURL());

        TaskRunNpmInstall task = createTaskWithDevDepsLocked(devDepsPath);

        String path = task.generateVersionsJson();

        File generatedVersionsFile = new File(path);
        return Json.parse(FileUtils.readFileToString(generatedVersionsFile,
                StandardCharsets.UTF_8));

    }

    private void verifyVersionIsNotPinned(File versions, File devDeps)
            throws MalformedURLException, IOException {
        Assert.assertEquals(
                "Generated versions json should not contain anything: package.json overrides the version",
                0,
                getGeneratedVersionsContent(versions, devDeps).keys().length);
    }

}
