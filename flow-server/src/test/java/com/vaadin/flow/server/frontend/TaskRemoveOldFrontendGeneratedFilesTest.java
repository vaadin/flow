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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.file.AccumulatorPathVisitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.tests.util.MockOptions;

public class TaskRemoveOldFrontendGeneratedFilesTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File generatedFolder;
    private Options options;

    @Before
    public void setUp() throws Exception {
        options = new MockOptions(temporaryFolder.getRoot());
        generatedFolder = options.getFrontendGeneratedFolder();
    }

    @Test
    public void execute_shouldDeleteNotGenerateFrontedFiles() throws Exception {
        TaskRemoveOldFrontendGeneratedFiles task = new TaskRemoveOldFrontendGeneratedFiles(
                options);

        GeneratedFilesSupport support = new GeneratedFilesSupport();
        File file1 = new File(generatedFolder, "test.txt");
        File file2 = generatedFolder.toPath()
                .resolve(Path.of("a", "b", "c.txt")).toFile();
        File file3 = generatedFolder.toPath()
                .resolve(Path.of("a", "z", "n.txt")).toFile();
        File file4 = generatedFolder.toPath()
                .resolve(Path.of("a", "z", "y.txt")).toFile();

        support.writeIfChanged(file1, "TEST");
        support.writeIfChanged(file2, "TEST");
        support.writeIfChanged(file3, "TEST");
        support.writeIfChanged(file4, "TEST");

        task.setGeneratedFileSupport(support);
        task.execute();
        assertOnlyExpectedGeneratedFilesExists(file1, file2, file3, file4);

        task = new TaskRemoveOldFrontendGeneratedFiles(options);
        support = new GeneratedFilesSupport();
        support.writeIfChanged(file2, "TEST");
        support.writeIfChanged(file4, "TEST");
        task.setGeneratedFileSupport(support);
        task.execute();
        assertOnlyExpectedGeneratedFilesExists(file2, file4);
    }

    @Test
    public void execute_existingFiles_nothingTracked_deleteAll()
            throws Exception {
        for (File file : Set.of(new File(generatedFolder, "test.txt"),
                generatedFolder.toPath().resolve(Path.of("a", "b", "c.txt"))
                        .toFile(),
                generatedFolder.toPath().resolve(Path.of("a", "z", "n.txt"))
                        .toFile(),
                generatedFolder.toPath().resolve(Path.of("a", "z", "y.txt"))
                        .toFile())) {
            file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), "TEST");
        }

        TaskRemoveOldFrontendGeneratedFiles task = new TaskRemoveOldFrontendGeneratedFiles(
                options);
        task.setGeneratedFileSupport(new GeneratedFilesSupport());
        task.execute();
        Assert.assertFalse("Generated folder has not been deleted",
                generatedFolder.exists());
    }

    @Test
    public void execute_frontendGeneratedFolderNotExistsAtTaskCreation_nothingIsDeleted()
            throws Exception {
        Files.deleteIfExists(generatedFolder.toPath());
        TaskRemoveOldFrontendGeneratedFiles task = new TaskRemoveOldFrontendGeneratedFiles(
                options);

        generatedFolder.mkdirs();
        Set<File> files = Set.of(new File(generatedFolder, "test.txt"),
                generatedFolder.toPath().resolve(Path.of("a", "b", "c.txt"))
                        .toFile(),
                generatedFolder.toPath().resolve(Path.of("a", "z", "n.txt"))
                        .toFile(),
                generatedFolder.toPath().resolve(Path.of("a", "z", "y.txt"))
                        .toFile());
        for (File file : files) {
            file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), "TEST");
        }

        task.setGeneratedFileSupport(new GeneratedFilesSupport());
        task.execute();
        assertOnlyExpectedGeneratedFilesExists(files.toArray(File[]::new));
    }

    @Test
    public void execute_missingGeneratedFileSupport_nothingIsDeleted()
            throws Exception {

        Set<File> files = Set.of(new File(generatedFolder, "test.txt"),
                generatedFolder.toPath().resolve(Path.of("a", "b", "c.txt"))
                        .toFile(),
                generatedFolder.toPath().resolve(Path.of("a", "z", "n.txt"))
                        .toFile(),
                generatedFolder.toPath().resolve(Path.of("a", "z", "y.txt"))
                        .toFile());
        for (File file : files) {
            file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), "TEST");
        }
        TaskRemoveOldFrontendGeneratedFiles task = new TaskRemoveOldFrontendGeneratedFiles(
                options);
        task.setGeneratedFileSupport(null);
        task.execute();
        assertOnlyExpectedGeneratedFilesExists(files.toArray(File[]::new));

    }

    @Test
    public void execute_knownFiles_notDeleted() throws Exception {
        Set<File> knownFiles = Set.of(generatedFolder.toPath()
                .resolve(Path.of("flow", "generated-flow-imports.js")).toFile(),
                generatedFolder.toPath()
                        .resolve(Path.of("flow", "generated-flow-imports.d.ts"))
                        .toFile(),
                generatedFolder.toPath()
                        .resolve(Path.of("flow",
                                "generated-flow-webcomponent-imports.js"))
                        .toFile(),
                new File(generatedFolder, "routes.tsx"),
                new File(generatedFolder, "routes.ts"),
                generatedFolder.toPath().resolve(Path.of("flow", "Flow.tsx"))
                        .toFile(),
                new File(generatedFolder, "file-routes.ts"),
                new File(generatedFolder, "css.generated.js"),
                new File(generatedFolder, "css.generated.d.ts"));
        for (File file : knownFiles) {
            file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), "TEST");
        }

        TaskRemoveOldFrontendGeneratedFiles task = new TaskRemoveOldFrontendGeneratedFiles(
                options);
        task.setGeneratedFileSupport(new GeneratedFilesSupport());
        task.execute();

        assertOnlyExpectedGeneratedFilesExists(knownFiles.toArray(File[]::new));
    }

    @Test
    public void execute_entriesInGeneratedFileList_notDeleted()
            throws Exception {
        Set<File> generatedFiles = new HashSet<>(
                Set.of(new File(generatedFolder, "test.txt"),
                        generatedFolder.toPath()
                                .resolve(Path.of("a", "b", "c.txt")).toFile(),
                        generatedFolder.toPath()
                                .resolve(Path.of("a", "z", "n.txt")).toFile(),
                        generatedFolder.toPath()
                                .resolve(Path.of("a", "z", "y.txt")).toFile()));
        for (File file : generatedFiles) {
            file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), "TEST");
        }
        File generatedFilesList = new File(generatedFolder,
                "generated-file-list.txt");
        Files.writeString(generatedFilesList.toPath(), generatedFiles.stream()
                .map(file -> generatedFolder.toPath().relativize(file.toPath()))
                .map(Path::toString)
                .collect(Collectors.joining(System.lineSeparator())));
        generatedFiles.add(generatedFilesList);

        TaskRemoveOldFrontendGeneratedFiles task = new TaskRemoveOldFrontendGeneratedFiles(
                options);
        task.setGeneratedFileSupport(new GeneratedFilesSupport());
        task.execute();

        assertOnlyExpectedGeneratedFilesExists(
                generatedFiles.toArray(File[]::new));
    }

    private void assertOnlyExpectedGeneratedFilesExists(File... expectedFiles)
            throws IOException {
        AccumulatorPathVisitor visitor = new AccumulatorPathVisitor();
        Files.walkFileTree(generatedFolder.toPath(), visitor);
        Assert.assertEquals(
                "Expect exactly currently generated files to exists",
                Stream.of(expectedFiles).map(
                        f -> generatedFolder.toPath().relativize(f.toPath()))
                        .collect(Collectors.toSet()),
                Set.copyOf(visitor.relativizeFiles(generatedFolder.toPath(),
                        false, null)));
    }

}