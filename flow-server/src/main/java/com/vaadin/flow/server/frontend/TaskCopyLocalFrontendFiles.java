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
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.Command;

import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;

/**
 * Copies JavaScript files from the given local frontend folder.
 */
public class TaskCopyLocalFrontendFiles implements Command {

    private final File targetDirectory;
    private final File frontendResourcesDirectory;

    /**
     *
     * Copy project local frontend files from defined frontendResourcesDirectory
     * (by default 'src/main/resources/META-INF/resources/frontend').
     * This enables running jar projects locally.
     *
     * @param npmFolder
     *            target directory for the discovered files
     */
    TaskCopyLocalFrontendFiles(File npmFolder, File frontendResourcesDirectory) {
        this.targetDirectory = new File(npmFolder, NODE_MODULES + FLOW_NPM_PACKAGE_NAME);
        this.frontendResourcesDirectory = frontendResourcesDirectory;
    }

    @Override
    public void execute() {
        createTargetFolder();

        if (frontendResourcesDirectory != null && frontendResourcesDirectory
                .isDirectory()) {
            log().info("Copying project local frontend resources.");
            try {
                FileUtils.copyDirectory(frontendResourcesDirectory,
                        targetDirectory);
            } catch (IOException e) {
                throw new UncheckedIOException(String.format(
                        "Failed to copy project frontend resources from '%s' to '%s'",
                        frontendResourcesDirectory, targetDirectory), e);
            }
            log().info("Copying frontend directory completed.");
        } else {
            log().debug("Found no local frontend resources for the project");
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
        return LoggerFactory.getLogger(TaskCopyLocalFrontendFiles.class);
    }
}
