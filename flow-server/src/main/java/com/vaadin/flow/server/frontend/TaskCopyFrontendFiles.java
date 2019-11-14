/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;

/**
 * Copies JavaScript and CSS files from JAR files into a given folder.
 *
 * @since 2.0
 */
public class TaskCopyFrontendFiles implements FallibleCommand {
    private static final String[] WILDCARD_INCLUSIONS = new String[] {
            "**/*.js", "**/*.css" };

    private File targetDirectory;
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
                        WILDCARD_INCLUSIONS);
                jarContentsManager.copyIncludedFilesFromJarTrimmingBasePath(
                        location, COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT,
                        targetDirectory, WILDCARD_INCLUSIONS);
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
