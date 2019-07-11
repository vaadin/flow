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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.Command;

import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;

/**
 * Copies JavaScript and CSS files from JAR files into a given folder.
 */
public class TaskCopyFrontendFiles implements Command {
    private static final String JAR_SUFFIX = ".jar";
    private static final String PATH_SEPARATOR_PROPERTY = "path.separator";
    private static final String CLASS_PATH_PROPERTY = "java.class.path";
    private static final String[] WILDCARD_INCLUSIONS = new String[] {
            "**/*.js", "**/*.css" };

    private File targetDirectory;
    private HashSet<File> jarFiles = null;

    /**
     * Scans all jar files found in the class path defined by property
     * {@code java.class.path}.
     * 
     * @param targetDirectory
     *            target directory for the discovered files
     */
    TaskCopyFrontendFiles(File targetDirectory) {
        Objects.requireNonNull(targetDirectory,
                "Parameter 'targetDirectory' cannot be null!");

        this.targetDirectory = targetDirectory;
    }

    /**
     * Scans the jar files given defined by {@code jarFilesToScan}. If {@code
     * jarFilesToScan} is null, acts as
     * {@link #TaskCopyFrontendFiles(java.io.File)} would.
     * 
     * @param targetDirectory
     *            target directory for the discovered files
     * @param jarFilesToScan
     *            jar files to scan. Only files ending in " .jar" will be
     *            scanned.
     */
    TaskCopyFrontendFiles(File targetDirectory, Set<File> jarFilesToScan) {
        this(targetDirectory);
        if (jarFilesToScan != null) {
            jarFiles = jarFilesToScan.stream()
                    .filter(file -> file.getName().endsWith(JAR_SUFFIX))
                    .collect(Collectors.toCollection(HashSet::new));
        }
    }

    @Override
    public void execute() {
        setJarFiles();

        log().info("Found {} jars to copy files from.", jarFiles.size());

        createTargetFolder();

        JarContentsManager jarContentsManager = new JarContentsManager();
        for (File jarFile : jarFiles) {
            jarContentsManager.copyIncludedFilesFromJarTrimmingBasePath(jarFile,
                    RESOURCES_FRONTEND_DEFAULT, targetDirectory,
                    WILDCARD_INCLUSIONS);
        }
    }

    private void setJarFiles() {
        if (jarFiles == null) {
            final String separator =
                    System.getProperty(PATH_SEPARATOR_PROPERTY);
            jarFiles = Stream
                    .of(System.getProperty(CLASS_PATH_PROPERTY).split(separator))
                    .filter(path -> path.endsWith(JAR_SUFFIX)).map(File::new)
                    .filter(File::exists)
                    .collect(Collectors.toCollection(HashSet::new));
        }
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
        return LoggerFactory.getLogger(TaskCopyFrontendFiles.class);
    }
}
