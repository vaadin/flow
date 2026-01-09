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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FrontendUtils;

import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_JAR_DEFAULT;

/**
 * Copies all frontend resources from JAR files into a given folder.
 * <p>
 * The task considers "frontend resources" all files placed in
 * {@literal META-INF/frontend}, {@literal META-INF/resources/frontend} and
 * {@literal META-INF/resources/[**]/themes} folders.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskCopyFrontendFiles
        extends AbstractFileGeneratorFallibleCommand {
    private static final String WILDCARD_INCLUSION_APP_THEME_JAR = "**/themes/**/*";
    private final Options options;
    private final Set<File> resourceLocations;

    /**
     * Scans the jar files given defined by {@code resourcesToScan}.
     *
     * @param options
     *            build options
     */
    TaskCopyFrontendFiles(Options options) {
        this.options = options;
        resourceLocations = options.getJarFiles().stream().filter(File::exists)
                .collect(Collectors.toSet());
    }

    @Override
    public void execute() {
        long start = System.nanoTime();
        log().info("Copying frontend resources from jar files ...");
        File targetDirectory = options.getJarFrontendResourcesFolder();
        TaskCopyLocalFrontendFiles.createTargetFolder(targetDirectory);
        Set<String> existingFiles;
        try {
            existingFiles = getFilesInDirectory(targetDirectory);
        } catch (IOException e) {
            // If we do not find the existing files, we will not delete anything
            existingFiles = new HashSet<>();
            log().error("Unable to list contents of the directory "
                    + targetDirectory.getAbsolutePath());
        }
        JarContentsManager jarContentsManager = new JarContentsManager();
        Set<String> handledFiles = new HashSet<>();
        for (File location : resourceLocations) {
            if (location.isDirectory()) {
                handledFiles
                        .addAll(TaskCopyLocalFrontendFiles.copyLocalResources(
                                new File(location, RESOURCES_FRONTEND_DEFAULT),
                                targetDirectory));
                handledFiles.addAll(TaskCopyLocalFrontendFiles
                        .copyLocalResources(new File(location,
                                COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT),
                                targetDirectory));
                // copies from resources, but excludes already copied from
                // resources/frontend
                handledFiles
                        .addAll(TaskCopyLocalFrontendFiles.copyLocalResources(
                                new File(location, RESOURCES_JAR_DEFAULT),
                                targetDirectory, FrontendUtils.FRONTEND));
            } else {
                handledFiles.addAll(jarContentsManager
                        .copyIncludedFilesFromJarTrimmingBasePath(location,
                                RESOURCES_FRONTEND_DEFAULT, targetDirectory,
                                "**/*"));
                handledFiles.addAll(jarContentsManager
                        .copyIncludedFilesFromJarTrimmingBasePath(location,
                                COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT,
                                targetDirectory, "**/*"));
                handledFiles.addAll(jarContentsManager
                        .copyIncludedFilesFromJarTrimmingBasePath(location,
                                RESOURCES_JAR_DEFAULT, targetDirectory,
                                WILDCARD_INCLUSION_APP_THEME_JAR));
            }
        }
        existingFiles.removeAll(handledFiles);
        existingFiles.forEach(
                filename -> new File(targetDirectory, filename).delete());
        long ms = (System.nanoTime() - start) / 1000000;
        log().info("Visited {} resources. Took {} ms.",
                resourceLocations.size(), ms);
        track(handledFiles.stream().map(relativePath -> targetDirectory.toPath()
                .resolve(relativePath).toFile()).toList());
    }

    static Set<String> getFilesInDirectory(File targetDirectory,
            String... relativePathExclusions) throws IOException {
        try (Stream<Path> stream = Files.walk(targetDirectory.toPath())) {
            return stream.filter(path -> path.toFile().isFile()
                    && TaskCopyLocalFrontendFiles.keepFile(targetDirectory,
                            relativePathExclusions, path.toFile()))
                    .map(path -> targetDirectory.toPath().relativize(path)
                            .toString().replaceAll("\\\\", "/"))
                    .collect(Collectors.toSet());
        }
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

}
