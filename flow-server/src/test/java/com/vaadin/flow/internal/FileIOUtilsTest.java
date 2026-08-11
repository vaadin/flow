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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

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
    void delete_removesPlainFileAndEmptyDirectory(@TempDir File dir)
            throws Exception {
        File file = new File(dir, "file.txt");
        assertTrue(file.createNewFile());
        File empty = new File(dir, "empty");
        assertTrue(empty.mkdirs());

        FileIOUtils.delete(file);
        FileIOUtils.delete(empty);

        assertFalse(file.exists());
        assertFalse(empty.exists());
    }

    @Test
    void delete_pathOverloadRemovesDirectoryContentsRecursively(
            @TempDir File dir) throws Exception {
        File nested = new File(dir, "a/b");
        assertTrue(nested.mkdirs());
        assertTrue(new File(nested, "file.txt").createNewFile());

        FileIOUtils.delete(new File(dir, "a").toPath());

        assertFalse(new File(dir, "a").exists());
    }

    @Test
    void delete_nonExistingFileIsNoOp(@TempDir File dir) throws Exception {
        FileIOUtils.delete(new File(dir, "missing.txt"));
        FileIOUtils.delete(new File(dir, "missing/nested.txt"));
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
    void delete_symlinkAsTargetIsRemovedAsLink(@TempDir File dir)
            throws Exception {
        assumeFalse(FrontendUtils.isWindows());

        File external = new File(dir, "external");
        assertTrue(external.mkdirs());
        File externalFile = new File(external, "keep.txt");
        assertTrue(externalFile.createNewFile());

        Path link = new File(dir, "link").toPath();
        Files.createSymbolicLink(link, external.toPath());

        FileIOUtils.delete(link);

        assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
        assertTrue(externalFile.exists(),
                "Contents of the symlinked directory should be kept");
    }

    @Test
    void delete_junctionIsRemovedAsLink(@TempDir File dir) throws Exception {
        assumeFalse(FrontendUtils.isWindows());

        File external = new File(dir, "external");
        assertTrue(external.mkdirs());
        File externalFile = new File(external, "keep.txt");
        assertTrue(externalFile.createNewFile());

        Path junction = new File(dir, "junction").toPath();
        Files.createSymbolicLink(junction, external.toPath());

        // Junctions can only be created on Windows, where they are reported as
        // a directory that is also "other". Deleting one has to remove the link
        // itself instead of traversing into the linked contents.
        BasicFileAttributes junctionAttributes = Mockito
                .mock(BasicFileAttributes.class);
        Mockito.when(junctionAttributes.isDirectory()).thenReturn(true);
        Mockito.when(junctionAttributes.isOther()).thenReturn(true);

        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class,
                Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.readAttributes(junction,
                    BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS))
                    .thenReturn(junctionAttributes);

            FileIOUtils.delete(junction);

            // The simulated attributes are what makes this meaningful: the
            // junction is reported as a directory, so only the "other" check
            // can be the reason its contents were not traversed
            files.verify(() -> Files.readAttributes(junction,
                    BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
            files.verify(() -> Files.walkFileTree(Mockito.eq(junction),
                    Mockito.any()), Mockito.never());
        }

        assertFalse(Files.exists(junction, LinkOption.NOFOLLOW_LINKS));
        assertTrue(externalFile.exists(),
                "Contents behind a junction should be kept");
    }

    @Test
    void delete_junctionInsideDirectoryIsRemovedAsLink(@TempDir File dir)
            throws Exception {
        assumeFalse(FrontendUtils.isWindows());

        File external = new File(dir, "external");
        assertTrue(external.mkdirs());
        File externalFile = new File(external, "keep.txt");
        assertTrue(externalFile.createNewFile());

        File directory = new File(dir, "bundle");
        assertTrue(directory.mkdirs());
        Path junction = new File(directory, "junction").toPath();
        Files.createSymbolicLink(junction, external.toPath());

        // A junction encountered while walking the tree is reported as a
        // directory that is also "other", see
        // delete_junctionIsRemovedAsLink. Traversing into it would delete the
        // linked contents instead of the link.
        BasicFileAttributes junctionAttributes = Mockito
                .mock(BasicFileAttributes.class);
        Mockito.when(junctionAttributes.isDirectory()).thenReturn(true);
        Mockito.when(junctionAttributes.isOther()).thenReturn(true);

        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class,
                Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.readAttributes(junction,
                    BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS))
                    .thenReturn(junctionAttributes);

            FileIOUtils.delete(directory);
        }

        assertFalse(directory.exists());
        assertTrue(externalFile.exists(),
                "Contents behind a junction should be kept");
    }

    @Test
    void delete_failureInsideDirectoryNamesTheBlockingFile(@TempDir File dir)
            throws Exception {
        File directory = new File(dir, "bundle");
        assertTrue(directory.mkdirs());
        File locked = new File(directory, "locked.txt");
        AccessDeniedException cause = new AccessDeniedException(
                locked.getAbsolutePath());

        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class,
                Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.deleteIfExists(locked.toPath()))
                    .thenThrow(cause);
            assertTrue(locked.createNewFile());

            IOException exception = assertThrows(IOException.class,
                    () -> FileIOUtils.delete(directory));

            // The message has to point at the file that blocks the deletion,
            // not at the directory the deletion was started from
            assertTrue(
                    exception.getMessage()
                            .startsWith("Failed to delete " + locked.toPath()),
                    exception.getMessage());
            assertEquals(cause, exception.getCause());
        }
    }

    @Test
    void delete_failureExplainsHowToUnblockDeletion(@TempDir File dir)
            throws Exception {
        File file = new File(dir, "locked.txt");

        // Simulate a file that cannot be removed, which on Windows typically
        // means that another process is keeping it open
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class,
                Mockito.CALLS_REAL_METHODS);
                MockedStatic<FrontendUtils> platform = Mockito.mockStatic(
                        FrontendUtils.class, Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.deleteIfExists(file.toPath())).thenThrow(
                    new AccessDeniedException(file.getAbsolutePath()));
            // Registering the stub above invoked the real method, so the file
            // has to be created after it
            assertTrue(file.createNewFile());

            platform.when(FrontendUtils::isWindows).thenReturn(true);
            IOException windows = assertThrows(IOException.class,
                    () -> FileIOUtils.delete(file));

            assertTrue(windows.getMessage()
                    .startsWith("Failed to delete " + file.toPath()));
            assertTrue(windows.getMessage().contains("taskkill"),
                    "The message should tell how to stop the process holding "
                            + "the file: " + windows.getMessage());
            assertTrue(windows.getMessage().contains(file.getAbsolutePath()),
                    "The path placeholder should be replaced with the actual "
                            + "path: " + windows.getMessage());

            platform.when(FrontendUtils::isWindows).thenReturn(false);
            IOException other = assertThrows(IOException.class,
                    () -> FileIOUtils.delete(file));

            assertTrue(other.getMessage().contains("lsof"),
                    "The message should tell how to find the process using the "
                            + "file: " + other.getMessage());
        }
    }

    @Test
    void delete_unreadableEntryReportsFailure(@TempDir File dir)
            throws Exception {
        File directory = new File(dir, "bundle");
        assertTrue(directory.mkdirs());
        File unreadable = new File(directory, "unreadable.txt");
        assertTrue(unreadable.createNewFile());
        AccessDeniedException cause = new AccessDeniedException(
                unreadable.getAbsolutePath());

        // An entry whose attributes cannot be read has to be reported the same
        // way as one that cannot be deleted
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class,
                Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.readAttributes(unreadable.toPath(),
                    BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS))
                    .thenThrow(cause);

            IOException exception = assertThrows(IOException.class,
                    () -> FileIOUtils.delete(directory));

            assertTrue(
                    exception.getMessage().startsWith(
                            "Failed to delete " + unreadable.toPath()),
                    exception.getMessage());
            assertEquals(cause, exception.getCause());
        }
    }

    @Test
    void delete_unreadableTargetReportsFailure(@TempDir File dir)
            throws Exception {
        File file = new File(dir, "unreadable.txt");
        assertTrue(file.createNewFile());
        AccessDeniedException cause = new AccessDeniedException(
                file.getAbsolutePath());

        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class,
                Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.readAttributes(file.toPath(),
                    BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS))
                    .thenThrow(cause);

            IOException exception = assertThrows(IOException.class,
                    () -> FileIOUtils.delete(file));

            assertTrue(
                    exception.getMessage()
                            .startsWith("Failed to delete " + file.toPath()),
                    exception.getMessage());
            assertEquals(cause, exception.getCause());
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

    @Test
    void deleteQuietly_removesDirectoryContentsRecursively(@TempDir File dir)
            throws Exception {
        File nested = new File(dir, "a/b");
        assertTrue(nested.mkdirs());
        assertTrue(new File(nested, "file.txt").createNewFile());

        assertTrue(FileIOUtils.deleteQuietly(new File(dir, "a")));
        assertFalse(new File(dir, "a").exists());

        File other = new File(dir, "c");
        assertTrue(other.mkdirs());
        assertTrue(new File(other, "file.txt").createNewFile());

        assertTrue(FileIOUtils.deleteQuietly(other.toPath()));
        assertFalse(other.exists());
    }

    @Test
    @SuppressWarnings("deprecation")
    void deleteFileQuietly_delegatesToDeleteQuietly(@TempDir File dir)
            throws Exception {
        File file = new File(dir, "file.txt");
        assertTrue(file.createNewFile());

        assertTrue(FileIOUtils.deleteFileQuietly(file));

        assertFalse(file.exists());
        assertFalse(FileIOUtils.deleteFileQuietly(null));
    }

    @Test
    void deleteQuietly_missingFileSucceedsAndNullFails(@TempDir File dir) {
        assertTrue(FileIOUtils.deleteQuietly(new File(dir, "missing.txt")));
        assertTrue(FileIOUtils
                .deleteQuietly(new File(dir, "missing.txt").toPath()));

        assertFalse(FileIOUtils.deleteQuietly((File) null));
        assertFalse(FileIOUtils.deleteQuietly((Path) null));
    }
}
