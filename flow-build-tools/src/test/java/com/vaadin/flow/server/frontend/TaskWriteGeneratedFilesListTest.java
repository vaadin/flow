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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.tests.util.MockOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskWriteGeneratedFilesListTest {

    @TempDir
    File temporaryFolder;
    private File generatedFolder;
    private Options options;

    @BeforeEach
    void setUp() {
        options = new MockOptions(temporaryFolder);
        generatedFolder = options.getFrontendGeneratedFolder();
    }

    @Test
    void execute_listsOnlyTrackedFilesUnderGeneratedFolder() throws Exception {
        GeneratedFilesSupport support = new GeneratedFilesSupport();
        File index = new File(generatedFolder, FrontendUtils.INDEX_TSX);
        File reactAdapter = generatedFolder.toPath()
                .resolve(Path.of("flow", "ReactAdapter.tsx")).toFile();
        File outside = new File(temporaryFolder, "outside.tsx");
        support.writeIfChanged(index, "A");
        support.writeIfChanged(reactAdapter, "B");
        support.writeIfChanged(outside, "C");

        TaskWriteGeneratedFilesList task = new TaskWriteGeneratedFilesList(
                options);
        task.setGeneratedFileSupport(support);
        task.execute();

        File manifest = new File(generatedFolder,
                FrontendUtils.GENERATED_FILES_LIST_NAME);
        assertTrue(manifest.exists(), "Manifest should be written");

        Set<Path> listed = Files.readAllLines(manifest.toPath()).stream()
                .filter(line -> !line.isBlank())
                .map(line -> generatedFolder.toPath().resolve(line).normalize())
                .collect(Collectors.toSet());
        assertEquals(
                Set.of(index.toPath().normalize(),
                        reactAdapter.toPath().normalize()),
                listed,
                "Manifest should list tracked files under the generated folder, "
                        + "relative to it, and exclude files outside it");
    }

    @Test
    void execute_missingGeneratedFileSupport_noManifestWritten() {
        TaskWriteGeneratedFilesList task = new TaskWriteGeneratedFilesList(
                options);
        task.setGeneratedFileSupport(null);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(task::execute);
        assertTrue(
                !new File(generatedFolder,
                        FrontendUtils.GENERATED_FILES_LIST_NAME).exists(),
                "No manifest should be written without generated file support");
    }
}
