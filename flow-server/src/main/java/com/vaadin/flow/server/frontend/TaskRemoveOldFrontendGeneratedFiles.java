/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ExecutionFailedException;

/**
 * Deletes old files from frontend generated folder.
 * <p>
 * </p>
 * This task must be performed last, because it will delete all files in
 * frontend generated folder that have not been tracked by the
 * {@link GeneratedFilesSupport} instance provided by the current
 * {@link NodeTasks} execution.
 * <p>
 * </p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see NodeTasks
 * @see GeneratedFilesSupport
 */
public class TaskRemoveOldFrontendGeneratedFiles implements FallibleCommand {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TaskRemoveOldFrontendGeneratedFiles.class);
    private final Path frontendGeneratedFolder;

    private final Set<Path> existingFiles = new HashSet<>();
    private GeneratedFilesSupport generatedFilesSupport;

    public TaskRemoveOldFrontendGeneratedFiles(Options options) {
        frontendGeneratedFolder = options.getFrontendGeneratedFolder().toPath();
        if (frontendGeneratedFolder.toFile().exists()) {
            try (Stream<Path> files = Files.walk(frontendGeneratedFolder)) {
                files.filter(Files::isRegularFile)
                        .map(p -> p.normalize().toAbsolutePath())
                        .collect(Collectors.toCollection(() -> existingFiles));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (generatedFilesSupport != null) {
            Set<Path> generatedFiles = generatedFilesSupport
                    .getFiles(frontendGeneratedFolder);
            HashSet<Path> toDelete = new HashSet<>(existingFiles);
            toDelete.removeAll(generatedFiles);
            LOGGER.debug("Cleaning generated frontend files from {}: {}",
                    frontendGeneratedFolder, toDelete);

            for (Path path : toDelete) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Cannot delete old generated file {}",
                                path, e);
                    } else {
                        LOGGER.warn("Cannot delete old generated file {}",
                                path);
                    }
                }
            }
        }
    }

    @Override
    public void setGeneratedFileSupport(GeneratedFilesSupport support) {
        this.generatedFilesSupport = support;
    }

}
