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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedFilesSupportTest {

    @TempDir
    File temporaryFolder;

    @Test
    void writeIfChanged_newFilesTracked() throws IOException {
        GeneratedFilesSupport support = new GeneratedFilesSupport();
        File file = new File(temporaryFolder, "test.txt");
        assertTrue(support.writeIfChanged(file, "TEST"),
                "New file should have been written");
        assertEquals(Files.readString(file.toPath()), "TEST");

        File nested = temporaryFolder.toPath()
                .resolve(Path.of("a", "b", "c.txt")).toFile();
        assertTrue(support.writeIfChanged(nested, "TEST2"),
                "New file should have been written");
        assertEquals(Files.readString(nested.toPath()), "TEST2");

        assertEquals(Set.of(file.toPath().toAbsolutePath(),
                nested.toPath().toAbsolutePath()), support.getFiles());
    }

    @Test
    void writeIfChanged_unchangedFilesTracked() throws IOException {
        GeneratedFilesSupport support = new GeneratedFilesSupport();
        File file = new File(temporaryFolder, "test.txt");
        Files.writeString(file.toPath(), "TEST");
        assertFalse(support.writeIfChanged(file, "TEST"),
                "Existing file with same content should not have been overwritten");
        assertEquals(Files.readString(file.toPath()), "TEST");

        File nested = temporaryFolder.toPath()
                .resolve(Path.of("a", "b", "c.txt")).toFile();
        nested.getParentFile().mkdirs();
        Files.writeString(nested.toPath(), "TEST2");
        assertFalse(support.writeIfChanged(nested, "TEST2"),
                "Existing file with same content should not have been overwritten");
        assertEquals(Files.readString(nested.toPath()), "TEST2");

        assertEquals(Set.of(file.toPath().toAbsolutePath(),
                nested.toPath().toAbsolutePath()), support.getFiles());
    }

    @Test
    void writeIfChanged_updatedFilesTracked() throws IOException {
        GeneratedFilesSupport support = new GeneratedFilesSupport();
        File file = new File(temporaryFolder, "test.txt");
        Files.writeString(file.toPath(), "OLD TEST");
        assertTrue(support.writeIfChanged(file, "TEST"),
                "Existing file should have been updated");
        assertEquals(Files.readString(file.toPath()), "TEST");

        File nested = temporaryFolder.toPath()
                .resolve(Path.of("a", "b", "c.txt")).toFile();
        nested.getParentFile().mkdirs();
        Files.writeString(nested.toPath(), "OLD TEST2");
        assertTrue(support.writeIfChanged(nested, "TEST2"),
                "Existing file should have been updated");
        assertEquals(Files.readString(nested.toPath()), "TEST2");

        assertEquals(Set.of(file.toPath().toAbsolutePath(),
                nested.toPath().toAbsolutePath()), support.getFiles());
    }

    @Test
    void getFile_filterByRootFolder() throws IOException {
        GeneratedFilesSupport support = new GeneratedFilesSupport();
        File file1 = new File(temporaryFolder, "test.txt");
        File file2 = temporaryFolder.toPath()
                .resolve(Path.of("a", "b", "c.txt")).toFile();
        File file3 = temporaryFolder.toPath()
                .resolve(Path.of("a", "z", "n.txt")).toFile();
        File file4 = temporaryFolder.toPath()
                .resolve(Path.of("a", "z", "y.txt")).toFile();

        support.writeIfChanged(file1, "TEST");
        support.writeIfChanged(file2, "TEST");
        support.writeIfChanged(file3, "TEST");
        support.writeIfChanged(file4, "TEST");

        assertEquals(Set.of(file1.toPath().toAbsolutePath(),
                file2.toPath().toAbsolutePath(),
                file3.toPath().toAbsolutePath(), file4.toPath().toAbsolutePath()

        ), support.getFiles());

        assertEquals(
                Set.of(file2.toPath().toAbsolutePath(),
                        file3.toPath().toAbsolutePath(),
                        file4.toPath().toAbsolutePath()),
                support.getFiles(temporaryFolder.toPath().resolve("a")));

        assertEquals(Set.of(file2.toPath().toAbsolutePath()), support
                .getFiles(temporaryFolder.toPath().resolve(Path.of("a", "b"))));

        assertEquals(
                Set.of(file3.toPath().toAbsolutePath(),
                        file4.toPath().toAbsolutePath()),
                support.getFiles(
                        temporaryFolder.toPath().resolve(Path.of("a", "z"))));

    }

}
