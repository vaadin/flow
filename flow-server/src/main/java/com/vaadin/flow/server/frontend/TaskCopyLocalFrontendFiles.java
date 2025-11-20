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
import java.io.FileFilter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies JavaScript files from the given local frontend folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskCopyLocalFrontendFiles
        extends AbstractFileGeneratorFallibleCommand {

    private final Options options;

    /**
     * Copy project local frontend files from defined frontendResourcesDirectory
     * (by default 'src/main/resources/META-INF/resources/frontend'). This
     * enables running jar projects locally.
     *
     */
    TaskCopyLocalFrontendFiles(Options options) {
        this.options = options;
    }

    private static boolean shouldApplyWriteableFlag() {
        return !Boolean.parseBoolean(System.getProperty(
                "vaadin.frontend.disableWritableFlagCheckOnCopy", "false"));
    }

    @Override
    public void execute() {
        File target = options.getJarFrontendResourcesFolder();
        File localResourcesFolder = options.getLocalResourcesFolder();
        createTargetFolder(target);

        if (localResourcesFolder != null
                && localResourcesFolder.isDirectory()) {
            log().info("Copying project local frontend resources.");
            Set<String> files = copyLocalResources(localResourcesFolder,
                    target);
            track(files.stream()
                    .map(path -> target.toPath().resolve(path).toFile())
                    .toList());

            log().info("Copying frontend directory completed.");
        } else {
            log().debug("Found no local frontend resources for the project");
        }
    }

    /**
     * Copies the local resources from specified source directory to within the
     * specified target directory ignoring the file exclusions defined as a
     * relative paths to source directory.
     *
     * @param source
     *            directory to copy the files from
     * @param target
     *            directory to copy the files to
     * @param relativePathExclusions
     *            files or directories that shouldn't be copied, relative to
     *            source directory
     * @return set of copied files
     */
    static Set<String> copyLocalResources(File source, File target,
            String... relativePathExclusions) {
        if (!source.isDirectory() || !target.isDirectory()) {
            return Collections.emptySet();
        }
        try {
            long start = System.nanoTime();
            Set<String> handledFiles = new HashSet<>(TaskCopyFrontendFiles
                    .getFilesInDirectory(source, relativePathExclusions));
            FileIOUtils.copyDirectory(source, target,
                    withoutExclusions(source, relativePathExclusions));
            if (shouldApplyWriteableFlag()) {
                try (Stream<Path> fileStream = Files
                        .walk(Paths.get(target.getPath()))) {
                    // used with try-with-resources as defined in walk API note
                    fileStream.filter(file -> !Files.isWritable(file)).forEach(
                            filePath -> filePath.toFile().setWritable(true));
                }
            }
            long ms = (System.nanoTime() - start) / 1000000;
            log().info("Copied {} local frontend files. Took {} ms.",
                    handledFiles.size(), ms);
            return handledFiles;
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to copy project frontend resources from '%s' to '%s'",
                    source, target), e);
        }
    }

    static void createTargetFolder(File target) {
        try {
            Files.createDirectories(Objects.requireNonNull(target).toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    String.format("Failed to create directory '%s'", target),
                    e);
        }
    }

    static boolean keepFile(File source, String[] relativePathExclusions,
            File fileToCheck) {
        for (String exclusion : relativePathExclusions) {
            File basePath = new File(source, exclusion);
            if (fileToCheck.getPath().startsWith(basePath.getPath())) {
                return false;
            }
        }
        return true;
    }

    private static FileFilter withoutExclusions(File source,
            String[] relativePathExclusions) {
        return file -> keepFile(source, relativePathExclusions, file);
    }

    private static Logger log() {
        return LoggerFactory.getLogger(TaskCopyLocalFrontendFiles.class);
    }
}
