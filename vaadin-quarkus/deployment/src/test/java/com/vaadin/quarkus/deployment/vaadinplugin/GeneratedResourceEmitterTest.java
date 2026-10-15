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

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.Constants;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GeneratedResourceEmitterTest {

    /**
     * Emitted paths are relative to the resources output directory, and the
     * Vaadin build writes below {@literal META-INF/VAADIN} in it.
     */
    private static final String BUNDLE = Constants.VAADIN_SERVLET_RESOURCES
            + "webapp/VAADIN/build/generated-flow-imports-D6_me9xd.js";

    private static final String TOKEN = Constants.VAADIN_SERVLET_RESOURCES
            + FrontendUtils.TOKEN_FILE;

    /**
     * The token file under the casing that a file system ignoring case reports
     * when the directory was already there under another one.
     */
    private static final String TOKEN_IN_ANOTHER_CASE = Constants.VAADIN_SERVLET_RESOURCES
            + "Config/Flow-Build-Info.json";

    private final List<GeneratedResourceBuildItem> emitted = new ArrayList<>();

    private final BuildProducer<GeneratedResourceBuildItem> producer = emitted::add;

    @TempDir
    Path tempDir;

    private Path classesDir;

    @BeforeEach
    void setUp() {
        classesDir = tempDir.resolve("classes");
    }

    @Test
    void emit_outputDirectoryIsPackaged_emitsOnlyTokenFile()
            throws IOException {
        // What a Maven build looks like: Quarkus packages target/classes and
        // the Vaadin build writes into target/classes/META-INF/VAADIN
        GeneratedResourceEmitter emitter = emitterFor(List.of(classesDir),
                classesDir);

        assertTrue(emitter.isPackagedFromOutputDirectory(),
                "The generated resources directory is inside the directory "
                        + "the application is packaged from");

        emitAll(emitter);

        assertIterableEquals(List.of(TOKEN), emittedNames(),
                "The bundle is packaged from the output directory, but the "
                        + "token file is deleted from it once the build is "
                        + "done, so only the token file has to be emitted");
    }

    @Test
    void emit_outputDirectoryIsUnderOneOfSeveralRoots_emitsOnlyTokenFile()
            throws IOException {
        // What a Gradle build looks like: two packaged roots, and the Vaadin
        // build writes below the resources one
        Path classesRoot = tempDir.resolve("classes").resolve("java")
                .resolve("main");
        Path resourcesRoot = tempDir.resolve("resources").resolve("main");

        GeneratedResourceEmitter emitter = emitterFor(
                List.of(classesRoot, resourcesRoot), resourcesRoot);

        assertTrue(emitter.isPackagedFromOutputDirectory(),
                "The generated resources directory is under the second root");

        emitAll(emitter);

        assertIterableEquals(List.of(TOKEN), emittedNames());
    }

    @Test
    void emit_outputDirectoryIsNotPackaged_emitsEverything() {
        // The generated resources directory was configured to somewhere the
        // application is not packaged from
        Path generatedDir = tempDir.resolve("generated").resolve("vaadin");

        GeneratedResourceEmitter emitter = GeneratedResourceEmitter
                .of(List.of(classesDir), generatedDir, classesDir, producer);

        assertFalse(emitter.isPackagedFromOutputDirectory());

        emitAll(emitter);

        assertIterableEquals(List.of(BUNDLE, TOKEN), emittedNames(),
                "Packaging cannot pick anything up from a directory it does "
                        + "not copy, so everything has to be emitted");
    }

    @Test
    void emit_packagedRootIsSealedArchive_emitsEverything() throws IOException {
        Path archive = tempDir.resolve("application.jar");
        try (ZipOutputStream zip = new ZipOutputStream(
                Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("README"));
            zip.closeEntry();
        }

        // When the application is packaged from a JAR, Quarkus does not report
        // the JAR itself as the archive root: it mounts it as a file system and
        // reports the root of that one, so the root is a path of a different
        // file system than the generated resources directory. That is what
        // makes Path.startsWith return false and what this test has to
        // reproduce, so it mounts a real JAR rather than using a plain path.
        try (FileSystem archiveFs = FileSystems.newFileSystem(
                URI.create("jar:" + archive.toUri()), Map.of())) {
            GeneratedResourceEmitter emitter = emitterFor(
                    archiveFs.getRootDirectories(), classesDir);

            assertFalse(emitter.isPackagedFromOutputDirectory(),
                    "A root mounted from a JAR was sealed before this build "
                            + "ran, so nothing written to the output directory "
                            + "afterwards reaches the application on its own");

            emitAll(emitter);

            assertIterableEquals(List.of(BUNDLE, TOKEN), emittedNames());
        }
    }

    @Test
    void emit_noPackagedRoots_emitsEverything() throws IOException {
        GeneratedResourceEmitter emitter = emitterFor(List.of(), classesDir);

        assertFalse(emitter.isPackagedFromOutputDirectory());

        emitAll(emitter);

        assertIterableEquals(List.of(BUNDLE, TOKEN), emittedNames());
    }

    @Test
    void emit_pathsResemblingTokenFile_emitsOnlyTheTokenFile()
            throws IOException {
        GeneratedResourceEmitter emitter = emitterFor(List.of(classesDir),
                classesDir);
        String notTheTokenFile = Constants.VAADIN_SERVLET_RESOURCES
                + "config/not-flow-build-info.json";
        String inAnotherDirectory = Constants.VAADIN_SERVLET_RESOURCES
                + "backup-config/flow-build-info.json";
        writeGeneratedFile(classesDir, notTheTokenFile);
        writeGeneratedFile(classesDir, inAnotherDirectory);

        emitter.accept(notTheTokenFile, content());
        emitter.accept(inAnotherDirectory, content());
        emitter.accept(TOKEN, content());

        assertIterableEquals(List.of(TOKEN), emittedNames(),
                "Only the token file itself is emitted, not the files whose "
                        + "path happens to end like it");
    }

    @Test
    void emit_tokenFileReportedInAnotherCase_emitsTokenFile()
            throws IOException {
        // Where names that differ only in case are one file, the walk reports
        // the casing that is on disk, which is the casing of a directory that
        // was already there rather than the one the Vaadin build asked for. It
        // is still the file the build deletes from the output directory.
        assumeFalse(isCaseSensitive(tempDir),
                "The file system tells names that differ only in case apart");

        GeneratedResourceEmitter emitter = emitterFor(List.of(classesDir),
                classesDir);

        emitter.accept(TOKEN_IN_ANOTHER_CASE, content());

        assertIterableEquals(List.of(TOKEN_IN_ANOTHER_CASE), emittedNames(),
                "The token file has to be recognized whatever casing the walk "
                        + "reports for it, otherwise it is emitted nowhere, "
                        + "deleted from the output directory anyway, and the "
                        + "application ships with no flow-build-info.json");
    }

    @Test
    void emit_fileDifferingFromTokenFileOnlyInCase_emitsNothing()
            throws IOException {
        // Where names that differ only in case are different files, this is a
        // file the Vaadin build neither wrote nor deletes, so packaging picks
        // it up from the output directory like any other generated file
        assumeTrue(isCaseSensitive(tempDir),
                "The file system reports one file under either casing");

        GeneratedResourceEmitter emitter = emitterFor(List.of(classesDir),
                classesDir);
        writeGeneratedFile(classesDir, TOKEN_IN_ANOTHER_CASE);

        emitter.accept(TOKEN_IN_ANOTHER_CASE, content());

        assertIterableEquals(List.of(), emittedNames(),
                "A file that is not the token file must not be emitted, or the "
                        + "artifact gets a second copy of one that packaging "
                        + "already took from the output directory");
    }

    @Test
    void emit_tokenFileUnderAnotherName_emitsTokenFile() throws IOException {
        // The whole point of asking the file system rather than comparing
        // paths is that one file can be reachable under more than one name.
        // A file system ignoring case is where that happens in a real build,
        // and a link stands in for it here, because the file system this test
        // runs on may well tell casings apart.
        GeneratedResourceEmitter emitter = emitterFor(List.of(classesDir),
                classesDir);
        Path generatedResources = generatedResourcesDirectory(classesDir);
        assumeTrue(
                createSymbolicLink(generatedResources.resolve("Config"),
                        generatedResources.resolve("config")),
                "A link to the config directory cannot be created here");
        String underAnotherName = Constants.VAADIN_SERVLET_RESOURCES
                + "Config/flow-build-info.json";

        emitter.accept(underAnotherName, content());

        assertIterableEquals(List.of(underAnotherName), emittedNames(),
                "A name that reaches the token file is the token file, and "
                        + "has to be emitted whatever name the walk reported");
    }

    @Test
    void notEmitted_everythingEmitted_reportsNothing() throws IOException {
        GeneratedResourceEmitter emitter = emitterFor(List.of(classesDir),
                classesDir);

        emitAll(emitter);

        assertIterableEquals(List.of(), emitter.notEmitted(),
                "The token file was emitted, so nothing is missing from the "
                        + "application");
    }

    @Test
    void notEmitted_tokenFileNotEmitted_reportsTokenFile() throws IOException {
        // What a file system answering in a way nobody anticipated looks like
        // from here: the walk found the token file, but it was not recognized
        // as one and so reached neither the application nor the artifact
        GeneratedResourceEmitter emitter = emitterFor(List.of(classesDir),
                classesDir);

        emitter.accept(BUNDLE, content());

        assertIterableEquals(
                List.of(generatedResourcesDirectory(classesDir)
                        .resolve(FrontendUtils.TOKEN_FILE)),
                emitter.notEmitted(),
                "The token file is deleted from the output directory right "
                        + "after this, so not emitting it has to be reported "
                        + "rather than leave the application without it");
    }

    @Test
    void notEmitted_outputDirectoryIsNotPackaged_reportsNothing()
            throws IOException {
        // Everything is emitted here without being recognized one by one, and
        // the token file still has to be accounted for
        Path archive = tempDir.resolve("application.jar");
        try (ZipOutputStream zip = new ZipOutputStream(
                Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("README"));
            zip.closeEntry();
        }

        try (FileSystem archiveFs = FileSystems.newFileSystem(
                URI.create("jar:" + archive.toUri()), Map.of())) {
            GeneratedResourceEmitter emitter = emitterFor(
                    archiveFs.getRootDirectories(), classesDir);

            emitAll(emitter);

            assertIterableEquals(List.of(), emitter.notEmitted(),
                    "The token file was emitted with everything else");
        }
    }

    /**
     * The directory the Vaadin build writes its generated resources into,
     * {@literal META-INF/VAADIN} below the resources output directory.
     */
    private static Path generatedResourcesDirectory(Path resourcesOutputDir) {
        return resourcesOutputDir.resolve("META-INF").resolve("VAADIN");
    }

    /**
     * Creates an emitter for an application packaged from the given roots, with
     * the files the Vaadin build produced written below the given resources
     * output directory.
     * <p>
     * The files are written because the emitter asks the file system which of
     * them have to be added to the application whichever way it is packaged.
     *
     * @param packagedRootDirectories
     *            the directories the application is packaged from.
     * @param resourcesOutputDir
     *            the resources output directory the Vaadin build writes below.
     * @return the emitter to exercise.
     */
    private GeneratedResourceEmitter emitterFor(
            Iterable<Path> packagedRootDirectories, Path resourcesOutputDir)
            throws IOException {
        writeGeneratedFile(resourcesOutputDir, BUNDLE);
        writeGeneratedFile(resourcesOutputDir, TOKEN);
        return GeneratedResourceEmitter.of(packagedRootDirectories,
                generatedResourcesDirectory(resourcesOutputDir),
                resourcesOutputDir, producer);
    }

    /**
     * Whether the file system tells two names that differ only in case apart,
     * which is what decides whether the casing the walk reports is the token
     * file or a file that is not it.
     *
     * @param directory
     *            the directory to probe, which this writes into.
     * @return {@literal true} if the two names are two files.
     */
    private static boolean isCaseSensitive(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path probe = directory.resolve("case-sensitivity-probe");
        Path probeInAnotherCase = directory.resolve("CASE-SENSITIVITY-PROBE");
        Files.writeString(probe, "probe");
        try {
            return !Files.exists(probeInAnotherCase);
        } finally {
            Files.deleteIfExists(probe);
        }
    }

    private void writeGeneratedFile(Path resourcesOutputDir, String name)
            throws IOException {
        Path file = resourcesOutputDir.resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, content());
    }

    private void emitAll(GeneratedResourceEmitter emitter) {
        emitter.accept(BUNDLE, content());
        emitter.accept(TOKEN, content());
    }

    private static byte[] content() {
        return "content".getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Links one name to another, where the file system allows it.
     *
     * @param link
     *            the name to create.
     * @param target
     *            the name it has to reach.
     * @return {@literal true} if the link was created.
     */
    private static boolean createSymbolicLink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
            return true;
        } catch (IOException | UnsupportedOperationException e) {
            return false;
        }
    }

    private List<String> emittedNames() {
        return emitted.stream().map(GeneratedResourceBuildItem::getName)
                .toList();
    }
}
