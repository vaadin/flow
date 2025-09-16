/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
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
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_GENERATED_DIR;

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
    private final File themeJarTargetDirectory;
    private Set<File> resourceLocations = null;

    /**
     * Scans the jar files given defined by {@code resourcesToScan}.
     *
     * @param npmFolder
     *            target directory for the discovered files
     * @param resourcesToScan
     *            folders and jar files to scan.
     */
    TaskCopyFrontendFiles(File npmFolder, Set<File> resourcesToScan) {
        Objects.requireNonNull(npmFolder,
                "Parameter 'npmFolder' must not be " + "null");
        Objects.requireNonNull(resourcesToScan,
                "Parameter 'jarFilesToScan' must not be null");
        this.targetDirectory = new File(npmFolder,
                NODE_MODULES + FLOW_NPM_PACKAGE_NAME);
        resourceLocations = resourcesToScan.stream().filter(File::exists)
                .collect(Collectors.toSet());
        String generatedDir = System.getProperty(PARAM_GENERATED_DIR,
                DEFAULT_GENERATED_DIR);
        this.themeJarTargetDirectory = new File(npmFolder, generatedDir);
    }

    @Override
    public void execute() {
        long start = System.nanoTime();
        log().info("Copying frontend resources from jar files ...");
        TaskCopyLocalFrontendFiles.createTargetFolder(targetDirectory);
        TaskCopyLocalFrontendFiles.createTargetFolder(themeJarTargetDirectory);
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
                        location, RESOURCES_JAR_DEFAULT,
                        themeJarTargetDirectory,
                        WILDCARD_INCLUSION_APP_THEME_JAR);
            }
        }
        long ms = (System.nanoTime() - start) / 1000000;
        log().info("Visited {} resources. Took {} ms.",
                resourceLocations.size(), ms);
    }

    private static Logger log() {
        return LoggerFactory.getLogger("dev-updater");
    }
}
