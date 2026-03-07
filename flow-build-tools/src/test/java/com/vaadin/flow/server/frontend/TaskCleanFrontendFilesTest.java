/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static org.junit.jupiter.api.Assertions.fail;

class TaskCleanFrontendFilesTest {
    @TempDir
    File rootFolder;

    private File projectRoot;
    private File frontendDirectory;
    private ClassFinder classFinder;
    private Options options;

    @BeforeEach
    void init() {
        projectRoot = rootFolder;
        frontendDirectory = new File(projectRoot, "target/frontend");
        classFinder = Mockito.mock(ClassFinder.class);
        options = new Options(null, classFinder, projectRoot)
                .withFrontendDirectory(frontendDirectory)
                .withFrontendGeneratedFolder(
                        new File(frontendDirectory, "generated"));
    }

    @Test
    void createdFileAreRemoved() throws IOException, ExecutionFailedException {
        TaskCleanFrontendFiles clean = new TaskCleanFrontendFiles(options);

        final Set<String> generatedFiles = Stream
                .of(FrontendUtils.VITE_CONFIG,
                        FrontendUtils.VITE_GENERATED_CONFIG, "node_modules",
                        Constants.PACKAGE_JSON, Constants.PACKAGE_LOCK_JSON,
                        TaskGenerateTsConfig.TSCONFIG_JSON,
                        TaskGenerateTsDefinitions.TS_DEFINITIONS, ".npmrc")
                .collect(Collectors.toSet());
        createFiles(generatedFiles);

        clean.execute();

        assertFilesNotExist(generatedFiles);
    }

    @Test
    void existingFrontendFiles_onlyCreatedFileAreRemoved()
            throws IOException, ExecutionFailedException {
        final Set<String> existingfiles = Stream
                .of(FrontendUtils.VITE_CONFIG, Constants.PACKAGE_JSON,
                        "node_modules", Constants.PACKAGE_LOCK_JSON)
                .collect(Collectors.toSet());
        createFiles(existingfiles);

        TaskCleanFrontendFiles clean = new TaskCleanFrontendFiles(options);

        final Set<String> generatedFiles = Stream
                .of(FrontendUtils.VITE_GENERATED_CONFIG,
                        TaskGenerateTsConfig.TSCONFIG_JSON,
                        TaskGenerateTsDefinitions.TS_DEFINITIONS, ".npmrc")
                .collect(Collectors.toSet());
        createFiles(generatedFiles);

        clean.execute();

        assertFilesNotExist(generatedFiles);
        assertFilesExist(existingfiles);
    }

    @Test
    void nodeModulesFolderIsCleared()
            throws IOException, ExecutionFailedException {
        TaskCleanFrontendFiles clean = new TaskCleanFrontendFiles(options);

        final File nodeModules = new File(rootFolder, "node_modules");
        new File(nodeModules, "file").createNewFile();
        final File directory = new File(nodeModules, "directory");
        directory.mkdir();
        new File(directory, "file.fi").createNewFile();

        clean.execute();

        assertFilesNotExist(Collections.singleton("node_modules"));
    }

    @Test
    void packageJsonExists_nodeModulesFolderIsKept()
            throws IOException, ExecutionFailedException {
        createFiles(Collections.singleton(Constants.PACKAGE_JSON));
        TaskCleanFrontendFiles clean = new TaskCleanFrontendFiles(options);

        final File nodeModules = new File(rootFolder, "node_modules");
        new File(nodeModules, "file").createNewFile();
        final File directory = new File(nodeModules, "directory");
        directory.mkdir();
        new File(directory, "file.fi").createNewFile();

        clean.execute();

        assertFilesExist(Collections.singleton("node_modules"));
    }

    @Test
    void hillaIsUsed_nodeModulesFolderIsKept()
            throws IOException, ExecutionFailedException {
        TaskCleanFrontendFiles clean;
        try (MockedStatic<FrontendBuildUtils> util = Mockito
                .mockStatic(FrontendBuildUtils.class)) {
            util.when(() -> FrontendBuildUtils.isHillaUsed(Mockito.any(),
                    Mockito.any(ClassFinder.class))).thenReturn(true);
            clean = new TaskCleanFrontendFiles(options);
        }

        final File nodeModules = new File(rootFolder, "node_modules");
        new File(nodeModules, "file").createNewFile();
        final File directory = new File(nodeModules, "directory");
        directory.mkdir();
        new File(directory, "file.fi").createNewFile();

        clean.execute();

        assertFilesExist(Collections.singleton("node_modules"));
    }

    @Test
    void hillaIsNotUsed_fileRoutesExists_fileRoutesClearedEagerly()
            throws IOException, ExecutionFailedException {
        TaskCleanFrontendFiles clean;
        final File nodeModules = new File(rootFolder,
                "target/frontend/generated");
        nodeModules.mkdirs();
        new File(nodeModules, "file-routes.ts").createNewFile();
        new File(nodeModules, "file-routes.json").createNewFile();

        try (MockedStatic<FrontendBuildUtils> util = Mockito
                .mockStatic(FrontendBuildUtils.class)) {
            util.when(() -> FrontendBuildUtils.isHillaUsed(Mockito.any(),
                    Mockito.any(ClassFinder.class))).thenReturn(false);
            new TaskCleanFrontendFiles(options);
        }

        assertFilesNotExist(Set.of("target/frontend/generated/file-routes.ts",
                "target/frontend/generated/file-routes.json"));
    }

    private void createFiles(Set<String> filesToCreate) throws IOException {
        for (String file : filesToCreate) {
            new File(rootFolder, file);
        }
    }

    private void assertFilesNotExist(Set<String> files) {
        Set<String> existingFiles = new HashSet<>();
        for (String file : files) {
            if (new File(projectRoot, file).exists()) {
                existingFiles.add(file);
            }
        }

        if (!existingFiles.isEmpty()) {
            StringBuilder fileList = new StringBuilder();
            existingFiles.forEach(file -> fileList.append(file).append("\n"));
            fail(String.format(
                    "Found files that should have been removed: %s\n",
                    fileList));
        }
    }

    private void assertFilesExist(Set<String> files) {
        Set<String> existingFiles = new HashSet<>(files);
        for (String file : files) {
            if (new File(projectRoot, file).exists()) {
                existingFiles.remove(file);
            }
        }

        if (!existingFiles.isEmpty()) {
            StringBuilder fileList = new StringBuilder();
            existingFiles.forEach(file -> fileList.append(file).append("\n"));
            fail(String.format("Missing files that should exist: %s\n",
                    fileList));
        }
    }
}
