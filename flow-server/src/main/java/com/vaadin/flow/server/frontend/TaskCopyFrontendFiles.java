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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.Constants;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.ASSETS;
import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_JAR_DEFAULT;

/**
 * Copies JavaScript and CSS files from JAR files into a given folder.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskCopyFrontendFiles implements FallibleCommand {
    private static final String[] WILDCARD_INCLUSIONS = new String[] {
            "**/*.js", "**/*.js.map", "**/*.css", "**/*.css.map", "**/*.ts",
            "**/*.ts.map", "**/*.tsx", "**/*.tsx.map" };
    private static final String WILDCARD_INCLUSION_APP_THEME_JAR = "**/themes/**/*";
    private final Options options;
    private Set<File> resourceLocations = null;

    /**
     * Scans the jar files given defined by {@code resourcesToScan}.
     *
     * @param targetDirectory
     *            target directory for the discovered files
     * @param resourcesToScan
     *            folders and jar files to scan.
     */
    TaskCopyFrontendFiles(Options options) {
        this.options = options;
        resourceLocations = options.jarFiles.stream().filter(File::exists)
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
            } else {
                handledFiles.addAll(jarContentsManager
                        .copyIncludedFilesFromJarTrimmingBasePath(location,
                                RESOURCES_FRONTEND_DEFAULT, targetDirectory,
                                WILDCARD_INCLUSIONS));
                handledFiles.addAll(jarContentsManager
                        .copyIncludedFilesFromJarTrimmingBasePath(location,
                                COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT,
                                targetDirectory, WILDCARD_INCLUSIONS));
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

        calculateHashForPackagedThemes(targetDirectory);
    }

    private void calculateHashForPackagedThemes(File targetDirectory) {
        File packagedThemesFolder = new File(targetDirectory,
                Constants.APPLICATION_THEME_ROOT);
        if (packagedThemesFolder.exists()) {
            for (File themeFolder : Objects.requireNonNull(
                    packagedThemesFolder.listFiles(File::isDirectory),
                    "Expected at least one theme in the front-end generated themes folder")) {
                File themeJson = new File(themeFolder, "theme.json");
                if (themeJson.exists()) {
                    try {
                        String themeJsonContent = FileUtils.readFileToString(
                                themeJson, StandardCharsets.UTF_8);
                        JsonObject json = Json.parse(themeJsonContent);
                        if (json.hasKey(ASSETS)) {
                            final String contentHash = StringUtil.getHash(
                                    themeJsonContent, StandardCharsets.UTF_8);
                            json.put("hash", contentHash);
                            FileUtils.write(themeJson, json.toJson(),
                                    StandardCharsets.UTF_8);
                        }
                    } catch (IOException e) {
                        log().error(
                                "Failed to write a hash to the {}. Please check if the packaged theme contains a valid theme.json file",
                                themeJson);
                    }
                }
            }
        }
    }

    static Set<String> getFilesInDirectory(File targetDirectory)
            throws IOException {
        try (Stream<Path> stream = Files.walk(targetDirectory.toPath())) {
            return stream.filter(path -> path.toFile().isFile())
                    .map(path -> targetDirectory.toPath().relativize(path)
                            .toString())
                    .collect(Collectors.toSet());
        }
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
