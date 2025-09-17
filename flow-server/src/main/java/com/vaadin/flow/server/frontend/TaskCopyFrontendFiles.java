/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class TaskCopyFrontendFiles implements FallibleCommand {
    private static final String WILDCARD_INCLUSION_APP_THEME_JAR = "**/themes/**/*";
    private final File targetDirectory;
    private Set<File> resourceLocations = null;

    /**
     * Scans the jar files given defined by {@code resourcesToScan}.
     *
     * @param targetDirectory
     *            target directory for the discovered files
     * @param resourcesToScan
     *            folders and jar files to scan.
     */
    TaskCopyFrontendFiles(File targetDirectory, Set<File> resourcesToScan) {
        Objects.requireNonNull(targetDirectory,
                "Parameter 'targetDirectory' must not be " + "null");
        Objects.requireNonNull(resourcesToScan,
                "Parameter 'jarFilesToScan' must not be null");
        this.targetDirectory = targetDirectory;
        resourceLocations = resourcesToScan.stream().filter(File::exists)
                .collect(Collectors.toSet());
    }

    @Override
    public void execute() {
        long start = System.nanoTime();
        log().info("Copying frontend resources from jar files ...");
        TaskCopyLocalFrontendFiles.createTargetFolder(targetDirectory);
        JarContentsManager jarContentsManager = new JarContentsManager();
        for (File location : resourceLocations) {
            if (location.isDirectory()) {
                TaskCopyLocalFrontendFiles.copyLocalResources(
                        new File(location, RESOURCES_FRONTEND_DEFAULT),
                        targetDirectory);
                TaskCopyLocalFrontendFiles.copyLocalResources(
                        new File(location,
                                COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT),
                        targetDirectory);
            } else {
                jarContentsManager.copyIncludedFilesFromJarTrimmingBasePath(
                        location, RESOURCES_FRONTEND_DEFAULT, targetDirectory,
                        "**/*");
                jarContentsManager.copyIncludedFilesFromJarTrimmingBasePath(
                        location, COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT,
                        targetDirectory, "**/*");
                jarContentsManager.copyIncludedFilesFromJarTrimmingBasePath(
                        location, RESOURCES_JAR_DEFAULT, targetDirectory,
                        WILDCARD_INCLUSION_APP_THEME_JAR);
            }
        }
        long ms = (System.nanoTime() - start) / 1000000;
        log().info("Visited {} resources. Took {} ms.",
                resourceLocations.size(), ms);
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
