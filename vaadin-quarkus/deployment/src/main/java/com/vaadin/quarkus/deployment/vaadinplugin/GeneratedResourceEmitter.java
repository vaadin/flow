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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;

import com.vaadin.flow.internal.FrontendUtils;

/**
 * Registers the files produced by the Vaadin build with the Quarkus
 * application, skipping the ones packaging already picks up on its own.
 * <p>
 * Quarkus packages the application from the archive root. When that root is the
 * directory the Vaadin build writes into, packaging walks the directory at the
 * time the artifact is assembled and therefore already contains everything the
 * build produced; emitting those files again only adds a second copy to the
 * artifact. When the root is an archive that the build tool sealed before the
 * Quarkus augmentation started, packaging cannot see anything written
 * afterwards and emitting is the only way the files reach the application.
 * <p>
 * Files that the build removes from the output directory once it is done are
 * the exception: packaging cannot pick those up from the directory either, so
 * they are always emitted. {@link #ALWAYS_EMITTED} is the list of them.
 */
final class GeneratedResourceEmitter implements BiConsumer<String, byte[]> {

    /**
     * Resources that are emitted whichever way the application is packaged,
     * because the build deletes them from the output directory once the
     * frontend build is done.
     * <p>
     * Add an entry here when a generated file gains that treatment, otherwise
     * it silently stops being packaged. Paths are relative to the generated
     * resources directory, and a file is recognized as one of them by asking
     * the file system, never by comparing its path.
     *
     * @see VaadinPlugin#removeTokenFile()
     */
    private static final Set<String> ALWAYS_EMITTED = Set
            .of(FrontendUtils.TOKEN_FILE);

    private final BuildProducer<GeneratedResourceBuildItem> producer;
    private final boolean packagedFromOutputDirectory;
    private final Path buildOutputDirectory;
    private final Set<Path> alwaysEmittedFiles;
    private final Set<Path> emittedFiles = new LinkedHashSet<>();

    private GeneratedResourceEmitter(
            BuildProducer<GeneratedResourceBuildItem> producer,
            boolean packagedFromOutputDirectory, Path buildOutputDirectory,
            Set<Path> alwaysEmittedFiles) {
        this.producer = producer;
        this.packagedFromOutputDirectory = packagedFromOutputDirectory;
        this.buildOutputDirectory = buildOutputDirectory;
        this.alwaysEmittedFiles = alwaysEmittedFiles;
    }

    /**
     * Creates an emitter for the given application archive roots.
     *
     * @param packagedRootDirectories
     *            the directories Quarkus packages the application from, as
     *            reported by
     *            {@link io.quarkus.deployment.builditem.ArchiveRootBuildItem#getRootDirectories()}.
     * @param generatedResourcesDirectory
     *            the directory the Vaadin build writes its generated resources
     *            into.
     * @param buildOutputDirectory
     *            the build output directory the emitted names are relative to,
     *            used to find the file a name was read from.
     * @param producer
     *            the producer registering the files with the application.
     * @return an emitter that skips what packaging already covers.
     */
    static GeneratedResourceEmitter of(Iterable<Path> packagedRootDirectories,
            Path generatedResourcesDirectory, Path buildOutputDirectory,
            BuildProducer<GeneratedResourceBuildItem> producer) {
        return new GeneratedResourceEmitter(producer,
                isPackagedFromDirectory(packagedRootDirectories,
                        generatedResourcesDirectory),
                buildOutputDirectory,
                alwaysEmittedFiles(generatedResourcesDirectory));
    }

    /**
     * Registers a file the Vaadin build produced, unless packaging already
     * picks it up from the output directory.
     *
     * @param path
     *            the name the file is added to the application under, its path
     *            relative to the build output directory, with {@literal /} as
     *            the separator whatever the platform, as
     *            {@link VaadinPlugin#emitGeneratedFiles(BiConsumer)} hands it
     *            over.
     * @param content
     *            the content of the file.
     * @throws UncheckedIOException
     *             if it cannot be told whether the file is one of the
     *             {@link #ALWAYS_EMITTED} ones.
     */
    @Override
    public void accept(String path, byte[] content) {
        Optional<Path> alwaysEmitted = alwaysEmitted(path);
        alwaysEmitted.ifPresent(emittedFiles::add);
        if (!packagedFromOutputDirectory || alwaysEmitted.isPresent()) {
            producer.produce(new GeneratedResourceBuildItem(path, content));
        }
    }

    /**
     * Whether packaging copies the generated resources directory into the
     * application by itself.
     *
     * @return {@literal true} if the files written into the generated resources
     *         directory reach the application without being emitted.
     */
    boolean isPackagedFromOutputDirectory() {
        return packagedFromOutputDirectory;
    }

    /**
     * The files that have to reach the application whichever way it is packaged
     * and have not been handed over.
     * <p>
     * Recognizing those files is this class' job, and it asks the file system
     * rather than compare paths so that it holds on every file system. This
     * reports what was missed anyway, so that a file system answering in a way
     * nobody anticipated fails the build rather than the application being
     * packaged without the file.
     *
     * @return the files of {@link #ALWAYS_EMITTED} that were not emitted, empty
     *         when everything that had to be emitted was.
     * @see VaadinPlugin#removeTokenFile()
     */
    Set<Path> notEmitted() {
        Set<Path> notEmitted = new LinkedHashSet<>(alwaysEmittedFiles);
        notEmitted.removeAll(emittedFiles);
        return notEmitted;
    }

    private static Set<Path> alwaysEmittedFiles(
            Path generatedResourcesDirectory) {
        return ALWAYS_EMITTED.stream().map(generatedResourcesDirectory::resolve)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isPackagedFromDirectory(
            Iterable<Path> packagedRootDirectories,
            Path generatedResourcesDirectory) {
        for (Path root : packagedRootDirectories) {
            // Path.startsWith is false for paths of different file systems, so
            // a root mounted from a JAR never matches, which is exactly the
            // case where the archive was sealed before this build ran.
            if (generatedResourcesDirectory.startsWith(root)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The file that has to be emitted whichever way the application is packaged
     * that the given name is, if it is one of them.
     * <p>
     * Asked for every file, and not only for the ones that would otherwise be
     * skipped, so that {@link #notEmitted()} knows what reached the application
     * whichever way it was packaged.
     *
     * @param path
     *            the name a generated file is added to the application under.
     * @return the file it is, or empty when it is not one of them.
     */
    private Optional<Path> alwaysEmitted(String path) {
        Path emittedFile = buildOutputDirectory.resolve(path);
        return alwaysEmittedFiles.stream()
                .filter(alwaysEmitted -> isSameFile(emittedFile, alwaysEmitted))
                .findFirst();
    }

    private static boolean isSameFile(Path emittedFile, Path alwaysEmitted) {
        // Whether two names are one file is for the file system to answer, so
        // it is asked rather than told: comparing the paths instead would
        // read a name the build did not choose as a different file, which is
        // what a file system that ignores case reports when the directory was
        // already there under another casing. Path comparison only follows the
        // file system on Windows, and on macOS the JDK gives a volume that
        // ignores case the case sensitive semantics of Unix.
        //
        // The file names are compared first only to keep this from opening
        // every file the build produced. Two names for one file can differ
        // only by case, so ignoring case there rules out nothing that the
        // file system would have matched.
        if (!emittedFile.getFileName().toString()
                .equalsIgnoreCase(alwaysEmitted.getFileName().toString())) {
            return false;
        }
        try {
            return Files.isSameFile(emittedFile, alwaysEmitted);
        } catch (NoSuchFileException e) {
            // The build produced no such file, so nothing it did produce is it
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to tell whether "
                    + emittedFile + " is " + alwaysEmitted
                    + ", a file that has to be added to the application because the Vaadin build deletes it from the build output directory.",
                    e);
        }
    }
}
