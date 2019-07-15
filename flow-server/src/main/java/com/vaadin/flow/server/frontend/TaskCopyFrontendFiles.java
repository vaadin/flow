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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.Command;

import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;

/**
 * Copies JavaScript and CSS files from JAR files into a given folder.
 */
public class TaskCopyFrontendFiles implements Command {
    private static final String JAR_SUFFIX = ".jar";
    private static final String[] WILDCARD_INCLUSIONS = new String[] {
            "**/*.js", "**/*.css" };

    private File targetDirectory;
    private transient Set<File> jarFiles = null;

    /**
     * Scans the jar files given defined by {@code jarFilesToScan}.
     *
     * @param npmFolder
     *            target directory for the discovered files
     * @param jarFilesToScan
     *            jar files to scan. Only files ending in " .jar" will be
     *            scanned.
     */
    TaskCopyFrontendFiles(File npmFolder, Set<File> jarFilesToScan) {
        this.targetDirectory = new File(npmFolder, NODE_MODULES + FLOW_NPM_PACKAGE_NAME);
        jarFiles = jarFilesToScan.stream()
                .filter(file -> file.getName().endsWith(JAR_SUFFIX))
                .filter(File::exists)
                .collect(Collectors.toSet());
    }

    @Override
    public void execute() {
        long start = System.nanoTime();
        log().info("Copying frontend resources from jar files ...");
        createTargetFolder();
        JarContentsManager jarContentsManager = new JarContentsManager();
        for (File jarFile : jarFiles) {
            jarContentsManager.copyIncludedFilesFromJarTrimmingBasePath(jarFile,
                    RESOURCES_FRONTEND_DEFAULT, targetDirectory,
                    WILDCARD_INCLUSIONS);
        }
        long ms = (System.nanoTime() - start) / 1000000;
        log().info("Visited {} jar files. Took {} ms.", jarFiles.size(), ms);
    }

    private void createTargetFolder() {
        try {
            FileUtils.forceMkdir(Objects.requireNonNull(targetDirectory));
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to create directory '%s'", targetDirectory), e);
        }
    }

    private static Logger log() {
        return LoggerFactory.getLogger("dev-updater");
    }
}
