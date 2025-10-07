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
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GeneratedFilesSupportTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void writeIfChanged_newFilesTracked() throws IOException {
        GeneratedFilesSupport support = new GeneratedFilesSupport();
        File file = new File(temporaryFolder.getRoot(), "test.txt");
        Assert.assertTrue("New file should have been written",
                support.writeIfChanged(file, "TEST"));
        Assert.assertEquals(Files.readString(file.toPath()), "TEST");

        File nested = temporaryFolder.getRoot().toPath()
                .resolve(Path.of("a", "b", "c.txt")).toFile();
        Assert.assertTrue("New file should have been written",
                support.writeIfChanged(nested, "TEST2"));
        Assert.assertEquals(Files.readString(nested.toPath()), "TEST2");

        Assert.assertEquals(Set.of(file.toPath().toAbsolutePath(),
                nested.toPath().toAbsolutePath()), support.getFiles());
    }

    @Test
    public void writeIfChanged_unchangedFilesTracked() throws IOException {
        GeneratedFilesSupport support = new GeneratedFilesSupport();
        File file = new File(temporaryFolder.getRoot(), "test.txt");
        Files.writeString(file.toPath(), "TEST");
        Assert.assertFalse(
                "Existing file with same content should not have been overwritten",
                support.writeIfChanged(file, "TEST"));
        Assert.assertEquals(Files.readString(file.toPath()), "TEST");

        File nested = temporaryFolder.getRoot().toPath()
                .resolve(Path.of("a", "b", "c.txt")).toFile();
        nested.getParentFile().mkdirs();
        Files.writeString(nested.toPath(), "TEST2");
        Assert.assertFalse(
                "Existing file with same content should not have been overwritten",
                support.writeIfChanged(nested, "TEST2"));
        Assert.assertEquals(Files.readString(nested.toPath()), "TEST2");

        Assert.assertEquals(Set.of(file.toPath().toAbsolutePath(),
                nested.toPath().toAbsolutePath()), support.getFiles());
    }

    @Test
    public void writeIfChanged_updatedFilesTracked() throws IOException {
        GeneratedFilesSupport support = new GeneratedFilesSupport();
        File file = new File(temporaryFolder.getRoot(), "test.txt");
        Files.writeString(file.toPath(), "OLD TEST");
        Assert.assertTrue("Existing file should have been updated",
                support.writeIfChanged(file, "TEST"));
        Assert.assertEquals(Files.readString(file.toPath()), "TEST");

        File nested = temporaryFolder.getRoot().toPath()
                .resolve(Path.of("a", "b", "c.txt")).toFile();
        nested.getParentFile().mkdirs();
        Files.writeString(nested.toPath(), "OLD TEST2");
        Assert.assertTrue("Existing file should have been updated",
                support.writeIfChanged(nested, "TEST2"));
        Assert.assertEquals(Files.readString(nested.toPath()), "TEST2");

        Assert.assertEquals(Set.of(file.toPath().toAbsolutePath(),
                nested.toPath().toAbsolutePath()), support.getFiles());
    }

    @Test
    public void getFile_filterByRootFolder() throws IOException {
        GeneratedFilesSupport support = new GeneratedFilesSupport();
        File file1 = new File(temporaryFolder.getRoot(), "test.txt");
        File file2 = temporaryFolder.getRoot().toPath()
                .resolve(Path.of("a", "b", "c.txt")).toFile();
        File file3 = temporaryFolder.getRoot().toPath()
                .resolve(Path.of("a", "z", "n.txt")).toFile();
        File file4 = temporaryFolder.getRoot().toPath()
                .resolve(Path.of("a", "z", "y.txt")).toFile();

        support.writeIfChanged(file1, "TEST");
        support.writeIfChanged(file2, "TEST");
        support.writeIfChanged(file3, "TEST");
        support.writeIfChanged(file4, "TEST");

        Assert.assertEquals(Set.of(file1.toPath().toAbsolutePath(),
                file2.toPath().toAbsolutePath(),
                file3.toPath().toAbsolutePath(), file4.toPath().toAbsolutePath()

        ), support.getFiles());

        Assert.assertEquals(
                Set.of(file2.toPath().toAbsolutePath(),
                        file3.toPath().toAbsolutePath(),
                        file4.toPath().toAbsolutePath()),
                support.getFiles(
                        temporaryFolder.getRoot().toPath().resolve("a")));

        Assert.assertEquals(Set.of(file2.toPath().toAbsolutePath()),
                support.getFiles(temporaryFolder.getRoot().toPath()
                        .resolve(Path.of("a", "b"))));

        Assert.assertEquals(
                Set.of(file3.toPath().toAbsolutePath(),
                        file4.toPath().toAbsolutePath()),
                support.getFiles(temporaryFolder.getRoot().toPath()
                        .resolve(Path.of("a", "z"))));

    }

}
