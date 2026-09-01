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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceFolderUtilTest {

    @TempDir
    File temporaryFolder;

    @Test
    void filesInAFolder_areVisitedWithTheirContent() throws IOException {
        File folder = new File(temporaryFolder, "resources");
        Files.createDirectories(folder.toPath());
        Files.writeString(new File(folder, "one.txt").toPath(), "first",
                StandardCharsets.UTF_8);
        // The files of a folder inside the folder are not part of it
        File nested = new File(folder, "nested");
        Files.createDirectories(nested.toPath());
        Files.writeString(new File(nested, "two.txt").toPath(), "second",
                StandardCharsets.UTF_8);

        List<String> names = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        ResourceFolderUtil.visitFiles(folder.toURI().toURL(), file -> {
            names.add(file.getName());
            contents.add(read(file.open()));
            assertTrue(file.getLocation().endsWith(file.getName()),
                    "The location should point at the file itself");
        });

        assertEquals(List.of("one.txt"), names);
        assertEquals(List.of("first"), contents);
    }

    @Test
    void folderPathContainsSpace_filesInTheJarAreVisited() throws IOException {
        File jar = new File(temporaryFolder, "with space.jar");
        try (JarOutputStream jarStream = new JarOutputStream(
                new FileOutputStream(jar))) {
            writeEntry(jarStream, "my resources/");
            writeEntry(jarStream, "my resources/one.txt");
        }

        List<String> names = new ArrayList<>();
        ResourceFolderUtil.visitFiles(
                new URL("jar:" + jar.toURI().toURL() + "!/my%20resources/"),
                file -> names.add(file.getName()));

        assertEquals(List.of("one.txt"), names);
    }

    @Test
    void unknownProtocol_folderIsReadAsAPath() throws IOException {
        File folder = new File(temporaryFolder, "exploded");
        Files.createDirectories(folder.toPath());
        Files.writeString(new File(folder, "one.txt").toPath(), "first",
                StandardCharsets.UTF_8);

        // An exploded WildFly deployment is served as vfsfile, and other
        // containers have protocols of their own that are folders as well
        URL vfsFile = new URL(null, "vfsfile:" + folder.getAbsolutePath() + "/",
                new NoConnectionHandler());

        List<String> names = new ArrayList<>();
        ResourceFolderUtil.visitFiles(vfsFile,
                file -> names.add(file.getName()));

        assertEquals(List.of("one.txt"), names);
    }

    @Test
    void vfsFolder_onlyItsFilesAreVisited() throws IOException {
        File folder = new File(temporaryFolder, "deployment");
        File nested = new File(folder, "nested");
        Files.createDirectories(nested.toPath());
        Files.writeString(new File(folder, "one.txt").toPath(), "first",
                StandardCharsets.UTF_8);
        Files.writeString(new File(nested, "two.txt").toPath(), "second",
                StandardCharsets.UTF_8);

        URL vfsFolder = new URL("vfs", "deployment", 0, "/my.war/classes/",
                new VirtualFileHandler(folder));

        List<String> contents = new ArrayList<>();
        ResourceFolderUtil.visitFiles(vfsFolder, file -> {
            contents.add(file.getName() + "=" + read(file.open()));
            assertEquals(vfsFolder + file.getName(), file.getLocation(),
                    "The location should point at the file in the folder");
        });

        // The folder inside it is not a file of the folder
        assertEquals(List.of("one.txt=first"), contents);
    }

    @Test
    void folderDoesNotExist_failsWithAnIOException() throws IOException {
        URL missing = new File(temporaryFolder, "missing").toURI().toURL();

        assertThrows(IOException.class,
                () -> ResourceFolderUtil.visitFiles(missing, file -> {
                    throw new AssertionError("No file should be visited");
                }));
    }

    private static void writeEntry(JarOutputStream jarStream, String name)
            throws IOException {
        jarStream.putNextEntry(new JarEntry(name));
        if (!name.endsWith("/")) {
            jarStream.write("content".getBytes(StandardCharsets.UTF_8));
        }
        jarStream.closeEntry();
    }

    private static String read(InputStream content) throws IOException {
        try (InputStream stream = content) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Stands in for the virtual file system of WildFly, which serves a folder
     * of the deployment through the {@code vfs} protocol.
     */
    private static class VirtualFileHandler extends URLStreamHandler {

        private final File folder;

        private VirtualFileHandler(File folder) {
            this.folder = folder;
        }

        @Override
        protected URLConnection openConnection(URL url) {
            return new URLConnection(url) {
                @Override
                public void connect() {
                    // The content is served without a connection
                }

                @Override
                public Object getContent() {
                    return new MockVirtualFile(folder);
                }
            };
        }
    }

    /**
     * Stands in for the virtual file of WildFly, which is only reachable
     * through reflection and creates the files it is asked for.
     */
    public static class MockVirtualFile {

        private final File file;

        public MockVirtualFile(File file) {
            this.file = file;
        }

        public List<MockVirtualFile> getChildren() {
            List<MockVirtualFile> children = new ArrayList<>();
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    children.add(new MockVirtualFile(child));
                }
            }
            return children;
        }

        public String getName() {
            return file.getName();
        }

        public boolean isFile() {
            return file.isFile();
        }

        public InputStream openStream() throws IOException {
            return new FileInputStream(file);
        }
    }

    /**
     * Serves no content, as only the path of the URL is used.
     */
    private static class NoConnectionHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            throw new IOException("Not connectable");
        }
    }
}
