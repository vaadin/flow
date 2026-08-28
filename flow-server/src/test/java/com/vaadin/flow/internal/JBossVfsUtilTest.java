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
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JBossVfsUtilTest {

    @TempDir
    File temporaryFolder;

    /**
     * Stands in for the virtual file of WildFly, which is only reachable
     * through reflection.
     */
    public static class MockVirtualFile {

        private final File file;

        private final List<File> materialized;

        public MockVirtualFile(File file, List<File> materialized) {
            this.file = file;
            this.materialized = materialized;
        }

        public List<MockVirtualFile> getChildren() {
            List<MockVirtualFile> children = new ArrayList<>();
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    children.add(new MockVirtualFile(child, materialized));
                }
            }
            return children;
        }

        public List<MockVirtualFile> getChildrenRecursively() {
            List<MockVirtualFile> children = new ArrayList<>();
            for (MockVirtualFile child : getChildren()) {
                children.add(child);
                children.addAll(child.getChildrenRecursively());
            }
            return children;
        }

        public File getPhysicalFile() {
            // The real implementation creates the file on disk when asked for
            materialized.add(file);
            return file;
        }
    }

    /**
     * Stands in for a container that serves something else than the virtual
     * file the protocol promises.
     */
    public static class MalformedVirtualFile {

        private final Object children;

        private final Object physicalFile;

        private final RuntimeException failure;

        private MalformedVirtualFile(Object children, Object physicalFile,
                RuntimeException failure) {
            this.children = children;
            this.physicalFile = physicalFile;
            this.failure = failure;
        }

        static MalformedVirtualFile withChildren(Object children) {
            return new MalformedVirtualFile(children, new File("folder"), null);
        }

        static MalformedVirtualFile withPhysicalFile(Object physicalFile) {
            return new MalformedVirtualFile(List.of(), physicalFile, null);
        }

        static MalformedVirtualFile failing(RuntimeException failure) {
            return new MalformedVirtualFile(List.of(), new File("folder"),
                    failure);
        }

        public Object getChildren() {
            return getChildrenRecursively();
        }

        public Object getChildrenRecursively() {
            if (failure != null) {
                throw failure;
            }
            return children;
        }

        public Object getPhysicalFile() {
            return physicalFile;
        }
    }

    /**
     * Serves nothing that looks like a virtual file.
     */
    public static class NotAVirtualFile {
    }

    @Test
    void listFolder_givesTheFilesOfTheFolder() throws IOException {
        File folder = createFolder();

        List<String> names = JBossVfsUtil
                .listFolder(vfsUrl(folder, new ArrayList<>())).stream()
                .map(File::getName).sorted().toList();

        assertEquals(List.of("nested", "one.txt"), names);
    }

    @Test
    void materializeFolder_givesTheFolderWithEverythingInItCreated()
            throws IOException {
        File folder = createFolder();
        List<File> materialized = new ArrayList<>();

        assertEquals(folder,
                JBossVfsUtil.materializeFolder(vfsUrl(folder, materialized)));

        // Every file and folder inside it has to be created as well, as the
        // caller reads them through the folder
        assertEquals(
                List.of(folder, new File(folder, "nested"),
                        new File(new File(folder, "nested"), "two.txt"),
                        new File(folder, "one.txt")),
                materialized.stream().sorted().toList());
    }

    @Test
    void notAVirtualFile_failsWithAnIOException() throws IOException {
        URL url = vfsUrl(new NotAVirtualFile());

        assertThrows(IOException.class, () -> JBossVfsUtil.listFolder(url));
    }

    @Test
    void nothingIsServed_failsWithAnIOException() throws IOException {
        URL url = vfsUrl(null);

        assertThrows(IOException.class, () -> JBossVfsUtil.listFolder(url));
    }

    @Test
    void childrenAreNotFiles_failsWithAnIOException() throws IOException {
        URL url = vfsUrl(
                MalformedVirtualFile.withChildren("not a list of children"));

        assertThrows(IOException.class, () -> JBossVfsUtil.listFolder(url));
    }

    @Test
    void recursiveChildrenAreNotFiles_failsWithAnIOException()
            throws IOException {
        URL url = vfsUrl(
                MalformedVirtualFile.withChildren("not a list of children"));

        assertThrows(IOException.class,
                () -> JBossVfsUtil.materializeFolder(url));
    }

    @Test
    void folderIsNotAFile_failsWithAnIOException() throws IOException {
        URL url = vfsUrl(
                MalformedVirtualFile.withPhysicalFile("not a file path"));

        assertThrows(IOException.class,
                () -> JBossVfsUtil.materializeFolder(url));
    }

    @Test
    void virtualFileFails_failsWithAnIOException() throws IOException {
        URL url = vfsUrl(MalformedVirtualFile
                .failing(new IllegalStateException("Deployment closed")));

        assertThrows(IOException.class, () -> JBossVfsUtil.listFolder(url));
    }

    private File createFolder() throws IOException {
        File folder = new File(temporaryFolder, "deployment");
        File nested = new File(folder, "nested");
        Files.createDirectories(nested.toPath());
        Files.writeString(new File(folder, "one.txt").toPath(), "first",
                StandardCharsets.UTF_8);
        Files.writeString(new File(nested, "two.txt").toPath(), "second",
                StandardCharsets.UTF_8);
        return folder;
    }

    private URL vfsUrl(File folder, List<File> materialized)
            throws IOException {
        return vfsUrl(new MockVirtualFile(folder, materialized));
    }

    private URL vfsUrl(Object content) throws IOException {
        return new URL(JBossVfsUtil.PROTOCOL, "deployment", 0, "/my.war",
                new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL url) {
                        return new URLConnection(url) {
                            @Override
                            public void connect() {
                            }

                            @Override
                            public Object getContent() {
                                return content;
                            }
                        };
                    }
                });
    }
}
