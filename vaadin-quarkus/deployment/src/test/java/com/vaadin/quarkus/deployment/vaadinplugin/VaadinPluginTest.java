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
package com.vaadin.quarkus.deployment.vaadinplugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.builder.BuildException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.Constants;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VaadinPluginTest {

    @TempDir
    Path projectDir;

    private VaadinPlugin plugin;
    private File tokenFile;
    private Path generatedResourcesDir;
    private File buildDir;
    private ApplicationModel model;

    @BeforeEach
    void setUp() throws Exception {
        buildDir = projectDir.resolve("target").toFile();

        WorkspaceModule module = mock(WorkspaceModule.class);
        when(module.getModuleDir()).thenReturn(projectDir.toFile());
        when(module.getBuildDir()).thenReturn(buildDir);
        when(module.hasMainSources()).thenReturn(false);

        model = mock(ApplicationModel.class);
        when(model.getApplicationModule()).thenReturn(module);

        plugin = createPlugin(new File(Constants.VAADIN_SERVLET_RESOURCES));
        generatedResourcesDir = buildDir.toPath()
                .resolve("classes/" + Constants.VAADIN_SERVLET_RESOURCES);
        tokenFile = new File(generatedResourcesDir.toFile(),
                FrontendUtils.TOKEN_FILE);
    }

    @Test
    void removeTokenFile_tokenFilePresent_deleted() throws Exception {
        Files.createDirectories(tokenFile.getParentFile().toPath());
        Files.writeString(tokenFile.toPath(), "{ \"productionMode\": true }");
        assertTrue(tokenFile.exists(), "Test setup should create token file");

        plugin.removeTokenFile();

        assertFalse(tokenFile.exists(),
                "Token file should have been deleted, otherwise it is packaged "
                        + "twice and a later dev mode run starts in production mode");
    }

    @Test
    void removeTokenFile_tokenFileMissing_doesNotFail() {
        assertFalse(tokenFile.exists(),
                "Test setup should not create token file");

        assertDoesNotThrow(() -> plugin.removeTokenFile());
    }

    @Test
    void removeTokenFile_tokenFileCannotBeDeleted_doesNotFailBuild()
            throws Exception {
        Files.createDirectories(tokenFile.getParentFile().toPath());
        Files.writeString(tokenFile.toPath(), "{ \"productionMode\": true }");
        File configDir = tokenFile.getParentFile();
        // A file to probe the deletion with, so that the probe does not consume
        // the token file the assertion needs
        File probe = new File(configDir, "probe.txt");
        Files.writeString(probe.toPath(), "probe");

        assumeTrue(configDir.setWritable(false),
                "A directory cannot be made read only on this file system");
        try {
            assumeFalse(probe.delete(),
                    "Test runs as a user that can delete from a read only "
                            + "directory, likely root");

            assertDoesNotThrow(() -> plugin.removeTokenFile(),
                    "A token file that cannot be deleted must not fail a build "
                            + "whose frontend build succeeded: the token file "
                            + "has already been added to the application, so "
                            + "the copy left behind is a duplicate and not a "
                            + "missing file");
            assertTrue(tokenFile.exists(),
                    "Test setup should have prevented the deletion");
        } finally {
            configDir.setWritable(true);
        }
    }

    @Test
    void emitGeneratedFiles_outputDirectoryOutsideBuildDirectory_failsBuild()
            throws Exception {
        // An absolute generated resource output directory is taken as it is, so
        // it can end up outside the build output directory, and then the path
        // the files would be added to the application under names no resource
        Path outsideDir = projectDir.resolve("outside");
        Files.createDirectories(
                outsideDir.resolve(FrontendUtils.TOKEN_FILE).getParent());
        Files.writeString(outsideDir.resolve(FrontendUtils.TOKEN_FILE),
                "{ \"productionMode\": true }");
        VaadinPlugin outsidePlugin = createPlugin(outsideDir.toFile());

        BiConsumer<String, byte[]> emitter = (path, content) -> {
        };
        BuildException exception = assertThrows(BuildException.class,
                () -> outsidePlugin.emitGeneratedFiles(emitter),
                "A generated resources directory outside the build output "
                        + "directory must fail the build, otherwise the files "
                        + "are added to the application under a name relative "
                        + "to nothing, or Path.relativize throws where the two "
                        + "directories have different roots");
        assertTrue(exception.getMessage().contains(outsideDir.toString()),
                "Failure should name the directory the Vaadin build writes "
                        + "into, was: " + exception.getMessage());
    }

    @Test
    void emitGeneratedFiles_generatedResources_emittedRelativeToOutputDirectory()
            throws Exception {
        writeGeneratedFile(FrontendUtils.TOKEN_FILE,
                "{ \"productionMode\": true }");
        writeGeneratedFile("build/indexhtml-1234.js", "console.log('hi');");

        Map<String, byte[]> emitted = new LinkedHashMap<>();
        plugin.emitGeneratedFiles(emitted::put);

        assertEquals(Set.of(
                Constants.VAADIN_SERVLET_RESOURCES + FrontendUtils.TOKEN_FILE,
                Constants.VAADIN_SERVLET_RESOURCES + "build/indexhtml-1234.js"),
                emitted.keySet(),
                "Generated files should be emitted with their path relative to "
                        + "the build output directory");
        assertEquals("{ \"productionMode\": true }",
                new String(
                        emitted.get(Constants.VAADIN_SERVLET_RESOURCES
                                + FrontendUtils.TOKEN_FILE),
                        StandardCharsets.UTF_8),
                "Emitted content should be the content of the file on disk");
    }

    @Test
    void emitGeneratedFiles_unreadableTokenFile_failsBuild() throws Exception {
        assertUnreadableFileFailsBuild(FrontendUtils.TOKEN_FILE);
    }

    @Test
    void emitGeneratedFiles_unreadableBundleFile_failsBuild() throws Exception {
        assertUnreadableFileFailsBuild("build/indexhtml-1234.js");
    }

    @Test
    void emitGeneratedFiles_noGeneratedResources_emitsNothing() {
        assertFalse(Files.exists(generatedResourcesDir),
                "Test setup should not create the generated resources directory");

        Map<String, byte[]> emitted = new LinkedHashMap<>();
        assertDoesNotThrow(() -> plugin.emitGeneratedFiles(emitted::put));

        assertTrue(emitted.isEmpty(),
                "Nothing should be emitted when the Vaadin build produced no files");
    }

    @Test
    void verifyAlwaysEmitted_tokenFileEmitted_doesNotFail() throws Exception {
        writeGeneratedFile(FrontendUtils.TOKEN_FILE,
                "{ \"productionMode\": true }");
        GeneratedResourceEmitter emitter = emitterForOutputDirectory();

        emitter.accept(
                Constants.VAADIN_SERVLET_RESOURCES + FrontendUtils.TOKEN_FILE,
                "{ \"productionMode\": true }"
                        .getBytes(StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> plugin.verifyAlwaysEmitted(emitter),
                "Everything that has to reach the application did, so the "
                        + "token file can be deleted from the output directory");
    }

    @Test
    void verifyAlwaysEmitted_tokenFileNotEmitted_failsBuild() throws Exception {
        writeGeneratedFile(FrontendUtils.TOKEN_FILE,
                "{ \"productionMode\": true }");
        GeneratedResourceEmitter emitter = emitterForOutputDirectory();

        emitter.accept(
                Constants.VAADIN_SERVLET_RESOURCES + "build/indexhtml-1234.js",
                "console.log('hi');".getBytes(StandardCharsets.UTF_8));

        BuildException exception = assertThrows(BuildException.class,
                () -> plugin.verifyAlwaysEmitted(emitter),
                "A token file that did not reach the application must fail the "
                        + "build, because it is deleted from the output "
                        + "directory right after and the application would be "
                        + "packaged without it while the build reports success");
        assertTrue(exception.getMessage().contains(tokenFile.getName()),
                "Failure should name the file that was not emitted, was: "
                        + exception.getMessage());
    }

    @Test
    void verifyAlwaysEmitted_callerSuppliedEmitter_doesNotFail() {
        BiConsumer<String, byte[]> emitter = (path, content) -> {
        };

        assertDoesNotThrow(() -> plugin.verifyAlwaysEmitted(emitter),
                "An emitter this plugin did not create keeps no track of what "
                        + "it emitted, so there is nothing to check");
    }

    /**
     * Asserts that the build fails when the given generated file cannot be
     * read, whichever file it is: skipping any of them packages an application
     * that is missing it, and skipping the token file in particular leaves the
     * application with no {@literal flow-build-info.json} at all, because
     * {@link VaadinPlugin#removeTokenFile()} deletes the copy in the build
     * output directory right after.
     * <p>
     * The assertion needs a file the build cannot read, which the test is not
     * always able to produce. It is skipped when the file cannot be made
     * unreadable, as on Windows, where {@link File#setReadable(boolean)}
     * reports a failure, and when the test user can read any file regardless of
     * its permissions, typically root.
     *
     * @param relativePath
     *            path of the file to make unreadable, relative to the generated
     *            resources directory.
     */
    private void assertUnreadableFileFailsBuild(String relativePath)
            throws Exception {
        // Writes both files, so that the build fails because of the unreadable
        // one and not because it is the only file it found.
        writeGeneratedFile(FrontendUtils.TOKEN_FILE,
                "{ \"productionMode\": true }");
        writeGeneratedFile("build/indexhtml-1234.js", "console.log('hi');");

        File unreadableFile = generatedResourcesDir.resolve(relativePath)
                .toFile();
        assumeTrue(unreadableFile.setReadable(false),
                "A file cannot be made unreadable on this file system");
        assumeFalse(unreadableFile.canRead(),
                "Test runs as a user that can read any file, likely root");

        BiConsumer<String, byte[]> emitter = (path, content) -> {
        };
        try {
            BuildException exception = assertThrows(BuildException.class,
                    () -> plugin.emitGeneratedFiles(emitter),
                    "A file produced by the Vaadin build that cannot be read must "
                            + "fail the build, otherwise the application is packaged "
                            + "without it and the build reports success");
            assertTrue(
                    exception.getMessage()
                            .contains(unreadableFile.getAbsolutePath()),
                    "Failure should name the file that could not be read, was: "
                            + exception.getMessage());
        } finally {
            unreadableFile.setReadable(true);
        }
    }

    /**
     * Creates a plugin whose Vaadin build writes into the given directory,
     * relative to the build output directory or absolute.
     *
     * @param generatedResourceOutputDirectory
     *            the configured generated resource output directory.
     * @return the plugin to exercise.
     */
    private VaadinPlugin createPlugin(File generatedResourceOutputDirectory)
            throws Exception {
        VaadinBuildTimeConfig config = mock(VaadinBuildTimeConfig.class);
        when(config.generatedResourceOutputDirectory())
                .thenReturn(generatedResourceOutputDirectory);
        // Prevents the clean frontend files task from being created, it is not
        // needed to exercise the token file removal.
        when(config.cleanFrontendFiles()).thenReturn(false);

        return VaadinPlugin.of(config, model, buildDir.toPath());
    }

    /**
     * Creates an emitter for an application Quarkus packages from the build
     * output directory, which is the shape that skips what packaging covers.
     *
     * @return the emitter to exercise.
     */
    private GeneratedResourceEmitter emitterForOutputDirectory() {
        Path outputDir = buildDir.toPath().resolve("classes");
        return GeneratedResourceEmitter.of(List.of(outputDir),
                generatedResourcesDir, outputDir, item -> {
                });
    }

    private void writeGeneratedFile(String relativePath, String content)
            throws Exception {
        Path file = generatedResourcesDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    @Test
    void of_workspaceNotInTheModel_readsWhatCodeGenerationSaved()
            throws Exception {
        WorkspaceModule saved = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("com.example", "demo", "1.0"))
                .setModuleDir(projectDir)
                .setBuildDir(projectDir.resolve("target")).build();
        Files.createDirectories(buildDir.toPath());
        WorkspaceInfo.save(saved, buildDir.toPath());

        ApplicationModel modelWithoutWorkspace = mock(ApplicationModel.class);
        when(modelWithoutWorkspace.getApplicationModule()).thenReturn(null);
        VaadinBuildTimeConfig config = mock(VaadinBuildTimeConfig.class);
        when(config.cleanFrontendFiles()).thenReturn(false);

        // The fallback path exists because a Quarkus build only knows the
        // workspace when workspace-discovery is on; without it the build steps
        // have to read back what the code generation phase wrote.
        assertNotNull(VaadinPlugin.of(config, modelWithoutWorkspace,
                buildDir.toPath()));
    }

    @Test
    void of_workspaceNeitherInTheModelNorSaved_failsWithAnActionableMessage() {
        ApplicationModel modelWithoutWorkspace = mock(ApplicationModel.class);
        when(modelWithoutWorkspace.getApplicationModule()).thenReturn(null);
        VaadinBuildTimeConfig config = mock(VaadinBuildTimeConfig.class);
        when(config.cleanFrontendFiles()).thenReturn(false);

        BuildException exception = assertThrows(BuildException.class,
                () -> VaadinPlugin.of(config, modelWithoutWorkspace,
                        projectDir.resolve("no-such-directory")));
        assertTrue(exception.getMessage().contains("workspace-discovery"),
                "the message has to say what the user can turn on: "
                        + exception.getMessage());
    }

    @Test
    void clean_noCleanTask_doesNothing() {
        // cleanFrontendFiles is off for the fixture, so there is no task and
        // clean() has to be a no-op rather than a NullPointerException - it
        // runs as a Quarkus build closeable, where a failure is invisible.
        assertDoesNotThrow(() -> plugin.clean());
    }

    @Test
    void clean_taskSucceeds_removesNothingAndDoesNotFail() throws Exception {
        VaadinPlugin pluginWithCleanTask = pluginWithCleanTask(projectDir);

        // Nothing was ever generated, so there is nothing to remove; the
        // point is that the ordinary path completes.
        assertDoesNotThrow(pluginWithCleanTask::clean);
    }

    @Test
    void clean_taskFails_logsRatherThanPropagating() throws Exception {
        Path npmFolder = Files.createDirectory(projectDir.resolve("ui"));
        VaadinPlugin pluginWithCleanTask = pluginWithCleanTask(npmFolder);

        // The task lists the npm folder when it runs, and listFiles() answers
        // null for anything that is not a directory, so replacing the folder
        // with a file after the task was built makes execute() fail.
        Files.delete(npmFolder);
        Files.writeString(npmFolder, "");

        // clean() runs as a Quarkus build closeable, which cannot fail the
        // build and logs what escapes at debug level only, so anything thrown
        // out of here would go unnoticed.
        assertDoesNotThrow(pluginWithCleanTask::clean);
    }

    /**
     * Creates a plugin that has a clean frontend files task, rooted at the
     * given npm folder.
     *
     * @param npmFolder
     *            the npm folder the task should clean.
     * @return the plugin to exercise.
     */
    private VaadinPlugin pluginWithCleanTask(Path npmFolder) throws Exception {
        VaadinBuildTimeConfig config = mock(VaadinBuildTimeConfig.class);
        when(config.generatedResourceOutputDirectory())
                .thenReturn(new File(Constants.VAADIN_SERVLET_RESOURCES));
        when(config.cleanFrontendFiles()).thenReturn(true);
        when(config.frontendDirectory())
                .thenReturn(new File("src/main/frontend"));
        when(config.generatedTsFolder()).thenReturn(Optional.empty());
        when(config.npmFolder()).thenReturn(Optional.of(npmFolder.toFile()));

        return VaadinPlugin.of(config, model, buildDir.toPath());
    }
}
