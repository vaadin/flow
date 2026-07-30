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
package com.vaadin.flow.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.open.OSUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class FileIOUtilsTest {

    @Test
    void projectFolderOnWindows() throws Exception {
        assumeTrue(OSUtils.isWindows());

        URL url = new URL(
                "file:/C:/Users/John%20Doe/Downloads/my-app%20(21)/my-app/target/classes/");
        assertEquals(
                new File("C:\\Users\\John Doe\\Downloads\\my-app (21)\\my-app"),
                FileIOUtils.getProjectFolderFromClasspath(url));
    }

    @Test
    void projectFolderOnMacOrLinux() throws Exception {
        assumeFalse(OSUtils.isWindows());

        URL url = new URL(
                "file:/Users/John%20Doe/Downloads/my-app%20(21)/my-app/target/classes/");
        assertEquals(new File("/Users/John Doe/Downloads/my-app (21)/my-app"),
                FileIOUtils.getProjectFolderFromClasspath(url));
    }

    @Test
    void tempFilesAreTempFiles() {
        assertTrue(FileIOUtils.isProbablyTemporaryFile(new File("foo.txt~")));
        assertFalse(FileIOUtils.isProbablyTemporaryFile(new File("foo.txt")));
    }

    @Test
    void writeIfChanged_writesContentAndLeavesNoTempFiles(@TempDir File dir)
            throws Exception {
        File file = new File(dir, "generated.ts");

        assertTrue(FileIOUtils.writeIfChanged(file, "first"));
        assertEquals("first", Files.readString(file.toPath()));

        assertTrue(FileIOUtils.writeIfChanged(file, "second"));
        assertEquals("second", Files.readString(file.toPath()));

        // The atomic write must not leave temporary files behind, otherwise a
        // file system watcher would keep reacting to spurious files.
        try (var entries = Files.list(dir.toPath())) {
            assertEquals(1, entries.count());
        }
    }

    @Test
    void writeIfChanged_unchangedContentDoesNotRewrite(@TempDir File dir)
            throws Exception {
        File file = new File(dir, "generated.ts");
        assertTrue(FileIOUtils.writeIfChanged(file, "content"));

        Path path = file.toPath();
        Object key = Files
                .readAttributes(path,
                        java.nio.file.attribute.BasicFileAttributes.class)
                .fileKey();
        long lastModified = Files.getLastModifiedTime(path).toMillis();

        // Writing identical content must report "not written" and leave the
        // file untouched so that Vite does not recompile needlessly.
        assertFalse(FileIOUtils.writeIfChanged(file, "content"));
        assertEquals(lastModified, Files.getLastModifiedTime(path).toMillis());
        if (key != null) {
            assertEquals(key,
                    Files.readAttributes(path,
                            java.nio.file.attribute.BasicFileAttributes.class)
                            .fileKey());
        }
    }

    @Test
    void writeIfChanged_fallsBackToNonAtomicMoveWhenAtomicUnsupported(
            @TempDir File dir) throws Exception {
        File file = new File(dir, "generated.ts");

        // Simulate a file system that does not support atomic moves. The write
        // must still succeed via the non-atomic fallback while every other file
        // operation runs for real.
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class,
                Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.move(any(), eq(file.toPath()),
                    eq(StandardCopyOption.ATOMIC_MOVE),
                    eq(StandardCopyOption.REPLACE_EXISTING)))
                    .thenThrow(new AtomicMoveNotSupportedException(null, null,
                            "atomic move not supported"));

            assertTrue(FileIOUtils.writeIfChanged(file, "fallback"));
        }

        assertEquals("fallback", Files.readString(file.toPath()));
        try (var entries = Files.list(dir.toPath())) {
            assertEquals(1, entries.count());
        }
    }

    @Test
    void delete_removesDirectoryContentsRecursively(@TempDir File dir)
            throws Exception {
        File nested = new File(dir, "a/b");
        assertTrue(nested.mkdirs());
        assertTrue(new File(nested, "file.txt").createNewFile());

        FileIOUtils.delete(new File(dir, "a"));

        assertFalse(new File(dir, "a").exists());
    }

    @Test
    void delete_nonExistingFileIsNoOp(@TempDir File dir) throws Exception {
        FileIOUtils.delete(new File(dir, "missing.txt"));
    }

    @Test
    void delete_symlinkedContentsAreLeftUntouched(@TempDir File dir)
            throws Exception {
        assumeFalse(FrontendUtils.isWindows());

        File external = new File(dir, "external");
        assertTrue(external.mkdirs());
        File externalFile = new File(external, "keep.txt");
        assertTrue(externalFile.createNewFile());

        File toDelete = new File(dir, "toDelete");
        assertTrue(toDelete.mkdirs());
        Files.createSymbolicLink(new File(toDelete, "link").toPath(),
                external.toPath());

        FileIOUtils.delete(toDelete);

        assertFalse(toDelete.exists());
        assertTrue(externalFile.exists(),
                "Contents of the symlinked directory should be kept");
    }

    @Test
    void delete_failureExplainsHowToUnblockDeletion(@TempDir File dir)
            throws Exception {
        File file = new File(dir, "locked.txt");

        // Simulate a file that cannot be removed, which on Windows typically
        // means that another process is keeping it open
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class,
                Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.deleteIfExists(file.toPath())).thenThrow(
                    new AccessDeniedException(file.getAbsolutePath()));
            // Registering the stub above invoked the real method, so the file
            // has to be created after it
            assertTrue(file.createNewFile());

            IOException exception = assertThrows(IOException.class,
                    () -> FileIOUtils.delete(file));

            assertTrue(exception.getMessage()
                    .startsWith("Failed to delete " + file.toPath()));
            assertTrue(
                    exception.getMessage().contains(
                            FrontendUtils.isWindows() ? "taskkill" : "lsof"),
                    "The message should tell how to find and stop the process "
                            + "holding the file: " + exception.getMessage());
        }
    }

    @Test
    void deleteQuietly_reportsFailureWithoutThrowing(@TempDir File dir)
            throws Exception {
        File file = new File(dir, "locked.txt");

        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class,
                Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.deleteIfExists(file.toPath())).thenThrow(
                    new AccessDeniedException(file.getAbsolutePath()));
            assertTrue(file.createNewFile());

            assertFalse(FileIOUtils.deleteQuietly(file));
        }

        assertTrue(FileIOUtils.deleteQuietly(file));
        assertFalse(file.exists());
    }
}
