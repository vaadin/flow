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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.jar.JarFile;

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
     * Stands in for the virtual file of a jar of the deployment, which is read
     * through streams rather than through files on disk.
     */
    public static class MockVirtualJar {

        private final File file;

        private final MockVirtualJar root;

        public MockVirtualJar(File file) {
            this(file, null);
        }

        private MockVirtualJar(File file, MockVirtualJar root) {
            this.file = file;
            this.root = root;
        }

        public List<MockVirtualJar> getChildrenRecursively() {
            List<MockVirtualJar> children = new ArrayList<>();
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    MockVirtualJar virtualChild = new MockVirtualJar(child,
                            root == null ? this : root);
                    children.add(virtualChild);
                    children.addAll(virtualChild.getChildrenRecursively());
                }
            }
            return children;
        }

        public boolean isFile() {
            return file.isFile();
        }

        public InputStream openStream() throws IOException {
            return new FileInputStream(file);
        }

        public String getPathNameRelativeTo(MockVirtualJar parent) {
            return parent.file.toPath().relativize(file.toPath()).toString()
                    .replace(File.separatorChar, '/');
        }
    }

    /**
     * Stands in for a jar whose entries do not behave as the protocol promises.
     */
    public static class MalformedVirtualJar {

        private final Object isFile;

        private final Object stream;

        private MalformedVirtualJar(Object isFile, Object stream) {
            this.isFile = isFile;
            this.stream = stream;
        }

        static MalformedVirtualJar withIsFile(Object isFile) {
            return new MalformedVirtualJar(isFile,
                    new ByteArrayInputStream(new byte[0]));
        }

        static MalformedVirtualJar withContent(Object stream) {
            return new MalformedVirtualJar(true, stream);
        }

        public List<MalformedVirtualJar> getChildrenRecursively() {
            return List.of(this);
        }

        public Object isFile() {
            return isFile;
        }

        public Object openStream() {
            return stream;
        }

        public String getPathNameRelativeTo(MalformedVirtualJar parent) {
            return "one.txt";
        }
    }

    /**
     * Stands in for a jar that cannot say where its entries are.
     */
    public static class VirtualJarWithoutPaths {

        public List<VirtualJarWithoutPaths> getChildrenRecursively() {
            return List.of(this);
        }

        public boolean isFile() {
            return true;
        }

        public InputStream openStream() {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    /**
     * Stands in for a container that serves something else than the virtual
     * file the protocol promises.
     */
    public static class MalformedVirtualFile {

        private final Object children;

        private final Object recursiveChildren;

        private final Object physicalFile;

        private final RuntimeException failure;

        private MalformedVirtualFile(Object children, Object recursiveChildren,
                Object physicalFile, RuntimeException failure) {
            this.children = children;
            this.recursiveChildren = recursiveChildren;
            this.physicalFile = physicalFile;
            this.failure = failure;
        }

        static MalformedVirtualFile withChildren(Object children) {
            return new MalformedVirtualFile(children, List.of(),
                    new File("folder"), null);
        }

        static MalformedVirtualFile withRecursiveChildren(
                Object recursiveChildren) {
            return new MalformedVirtualFile(List.of(), recursiveChildren,
                    new File("folder"), null);
        }

        static MalformedVirtualFile withPhysicalFile(Object physicalFile) {
            return new MalformedVirtualFile(List.of(), List.of(), physicalFile,
                    null);
        }

        static MalformedVirtualFile failing(RuntimeException failure) {
            return new MalformedVirtualFile(List.of(), List.of(),
                    new File("folder"), failure);
        }

        public Object getChildren() {
            if (failure != null) {
                throw failure;
            }
            return children;
        }

        public Object getChildrenRecursively() {
            if (failure != null) {
                throw failure;
            }
            return recursiveChildren;
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
    void materializeFiles_createsAndGivesTheFilesOfTheFolder()
            throws IOException {
        File folder = createFolder();
        List<File> materialized = new ArrayList<>();

        List<File> files = JBossVfsUtil
                .materializeFiles(vfsUrl(folder, materialized));

        // The files are given as the virtual file system created them, rather
        // than being looked up in the folder afterwards
        List<File> expected = List.of(new File(folder, "nested"),
                new File(folder, "one.txt"));
        assertEquals(expected, files.stream().sorted().toList());
        assertEquals(expected, materialized.stream().sorted().toList());
    }

    @Test
    void materializeFolder_createsEverythingInItAndGivesTheFolder()
            throws IOException {
        File folder = createFolder();
        List<File> materialized = new ArrayList<>();

        assertEquals(folder,
                JBossVfsUtil.materializeFolder(vfsUrl(folder, materialized)));

        // The files of the folders inside it are created as well, as the
        // caller reads the folder as a whole
        assertEquals(
                List.of(folder, new File(folder, "nested"),
                        new File(new File(folder, "nested"), "two.txt"),
                        new File(folder, "one.txt")),
                materialized.stream().sorted().toList());
    }

    @Test
    void materializeJar_packsTheFilesOfTheJar() throws IOException {
        File folder = createFolder();
        URL url = vfsUrl(new MockVirtualJar(folder), "/my.war/lib/fake.jar");

        File jar = JBossVfsUtil.materializeJar(url);

        try (JarFile jarFile = new JarFile(jar)) {
            assertEquals(List.of("nested/two.txt", "one.txt"),
                    jarFile.stream().map(JarEntry::getName).sorted().toList());
            assertEquals("first",
                    new String(
                            jarFile.getInputStream(jarFile.getEntry("one.txt"))
                                    .readAllBytes(),
                            StandardCharsets.UTF_8));
        }
    }

    @Test
    void materializeJar_urlIsNotAJar_failsWithAnIOException()
            throws IOException {
        URL url = vfsUrl(new MockVirtualJar(createFolder()),
                "/my.war/lib/fake.zip");

        assertThrows(IOException.class, () -> JBossVfsUtil.materializeJar(url));
    }

    @Test
    void materializeJar_entryIsNeitherFileNorFolder_failsWithAnIOException()
            throws IOException {
        URL url = vfsUrl(MalformedVirtualJar.withIsFile("not a boolean"),
                "/my.war/lib/fake.jar");

        assertThrows(IOException.class, () -> JBossVfsUtil.materializeJar(url));
    }

    @Test
    void materializeJar_entryCannotBeRead_failsWithAnIOException()
            throws IOException {
        URL url = vfsUrl(MalformedVirtualJar.withContent("not a stream"),
                "/my.war/lib/fake.jar");

        assertThrows(IOException.class, () -> JBossVfsUtil.materializeJar(url));
    }

    @Test
    void materializeJar_entryHasNoPath_failsWithAnIOException()
            throws IOException {
        URL url = vfsUrl(new VirtualJarWithoutPaths(), "/my.war/lib/fake.jar");

        assertThrows(IOException.class, () -> JBossVfsUtil.materializeJar(url));
    }

    @Test
    void notAVirtualFile_failsWithAnIOException() throws IOException {
        URL url = vfsUrl(new NotAVirtualFile());

        assertThrows(IOException.class,
                () -> JBossVfsUtil.materializeFiles(url));
    }

    @Test
    void nothingIsServed_failsWithAnIOException() throws IOException {
        URL url = vfsUrl(null);

        assertThrows(IOException.class,
                () -> JBossVfsUtil.materializeFiles(url));
    }

    @Test
    void childrenAreNotFiles_failsWithAnIOException() throws IOException {
        URL url = vfsUrl(
                MalformedVirtualFile.withChildren("not a list of children"));

        assertThrows(IOException.class,
                () -> JBossVfsUtil.materializeFiles(url));
    }

    @Test
    void recursiveChildrenAreNotFiles_failsWithAnIOException()
            throws IOException {
        // Only the recursive children are malformed, so this fails for the
        // folder, which is materialized as a whole, and not for its files
        URL url = vfsUrl(MalformedVirtualFile
                .withRecursiveChildren("not a list of children"));

        assertEquals(List.of(), JBossVfsUtil.materializeFiles(url));
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

        assertThrows(IOException.class,
                () -> JBossVfsUtil.materializeFiles(url));
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
        return vfsUrl(content, "/my.war");
    }

    private URL vfsUrl(Object content, String path) throws IOException {
        return new URL(JBossVfsUtil.PROTOCOL, "deployment", 0, path,
                new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL url) {
                        return new URLConnection(url) {
                            @Override
                            public void connect() {
                                // The content is served without a connection
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
