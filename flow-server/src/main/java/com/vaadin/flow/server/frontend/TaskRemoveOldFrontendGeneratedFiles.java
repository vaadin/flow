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
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FileIOUtils;

/**
 * Deletes old files from frontend generated folder.
 * <p>
 * This task must be performed last, because it will delete all files in
 * frontend generated folder that have not been tracked by the
 * {@link GeneratedFilesSupport} instance provided by the current
 * {@link NodeTasks} execution.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see NodeTasks
 * @see GeneratedFilesSupport
 */
public class TaskRemoveOldFrontendGeneratedFiles implements FallibleCommand {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TaskRemoveOldFrontendGeneratedFiles.class);
    private final Path frontendGeneratedFolder;
    private final File frontendFolder;

    private final Set<Path> existingFiles = new HashSet<>();
    private GeneratedFilesSupport generatedFilesSupport;

    public TaskRemoveOldFrontendGeneratedFiles(Options options) {
        frontendFolder = options.getFrontendDirectory();
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
            toDelete.removeIf(isKnownUnhandledFile());
            LOGGER.debug("Cleaning generated frontend files from {}: {}",
                    frontendGeneratedFolder, toDelete);
            for (Path path : toDelete) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Cannot delete old generated file {}",
                                path, ex);
                    } else {
                        LOGGER.warn("Cannot delete old generated file {}",
                                path);
                    }
                }
            }
            // Remove empty directories
            try {
                Files.walkFileTree(frontendGeneratedFolder,
                        new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult postVisitDirectory(Path dir,
                                    IOException exc) throws IOException {
                                if (FileIOUtils.isEmptyDirectory(dir)) {
                                    Files.deleteIfExists(dir);
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
            } catch (IOException ex) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "Cannot delete empty folder generated under {}",
                            frontendGeneratedFolder, ex);
                } else {
                    LOGGER.warn("Cannot delete empty folder generated under {}",
                            frontendGeneratedFolder);
                }

            }
        }
    }

    private Predicate<Path> isKnownUnhandledFile() {
        Path flowGeneratedImports = normalizePath(
                FrontendUtils.getFlowGeneratedImports(frontendFolder).toPath()
                        .toAbsolutePath());
        Path flowGeneratedWebComponentImports = normalizePath(FrontendUtils
                .getFlowGeneratedWebComponentsImports(frontendFolder).toPath()
                .toAbsolutePath());
        Set<Path> knownFiles = new HashSet<>();
        knownFiles.add(flowGeneratedImports);
        knownFiles.add(flowGeneratedWebComponentImports);
        knownFiles.add(flowGeneratedImports
                .resolveSibling(FrontendUtils.IMPORTS_D_TS_NAME));
        knownFiles.add(normalizePath(frontendGeneratedFolder.resolve(
                new File(TaskGenerateReactFiles.FLOW_FLOW_TSX).toPath())));
        knownFiles.add(normalizePath(frontendGeneratedFolder.resolve(
                new File(TaskGenerateReactFiles.JSX_TRANSFORM_DEV_RUNTIME)
                        .toPath())));
        knownFiles.add(normalizePath(frontendGeneratedFolder
                .resolve(new File(TaskGenerateReactFiles.JSX_TRANSFORM_RUNTIME)
                        .toPath())));
        knownFiles.add(normalizePath(frontendGeneratedFolder
                .resolve(new File(TaskGenerateReactFiles.JSX_TRANSFORM_INDEX)
                        .toPath())));
        knownFiles.add(normalizePath(
                frontendGeneratedFolder.resolve(FrontendUtils.ROUTES_TSX)));
        knownFiles.add(normalizePath(
                frontendGeneratedFolder.resolve(FrontendUtils.ROUTES_TS)));
        knownFiles.add(normalizePath(
                frontendGeneratedFolder.resolve("file-routes.ts")));
        knownFiles.add(normalizePath(
                frontendGeneratedFolder.resolve("file-routes.json")));
        knownFiles.add(normalizePath(
                frontendGeneratedFolder.resolve("css.generated.js")));
        knownFiles.add(normalizePath(
                frontendGeneratedFolder.resolve("css.generated.d.ts")));
        knownFiles.addAll(hillaGeneratedFiles());
        return path -> knownFiles.contains(path) || path.getFileName()
                .toString().matches("theme(\\.(js|d\\.ts)|-.*\\.generated.js)");
    }

    private Set<Path> hillaGeneratedFiles() {
        Set<Path> generatedFiles = new HashSet<>();
        Path hillaGeneratedFilesList = frontendGeneratedFolder
                .resolve("generated-file-list.txt");
        generatedFiles.add(normalizePath(hillaGeneratedFilesList));
        if (Files.exists(hillaGeneratedFilesList)) {
            try {
                Files.readAllLines(hillaGeneratedFilesList).stream()
                        .map(file -> new File(file).toPath())
                        .map(file -> normalizePath(
                                frontendGeneratedFolder.resolve(file)))
                        .forEach(generatedFiles::add);
            } catch (IOException e) {
                LOGGER.debug("Cannot read generated-file-list.txt files");
            }
        }
        return generatedFiles;
    }

    private static Path normalizePath(Path path) {
        return path.toAbsolutePath().normalize();
    }

    @Override
    public void setGeneratedFileSupport(GeneratedFilesSupport support) {
        this.generatedFilesSupport = support;
    }

}
