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

        public List<MockVirtualFile> getChildrenRecursively() {
            List<MockVirtualFile> children = new ArrayList<>();
            for (MockVirtualFile child : getChildren()) {
                children.add(child);
                children.addAll(child.getChildrenRecursively());
            }
            return children;
        }

        public File getPhysicalFile() {
            return file;
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

        List<String> names = JBossVfsUtil.listFolder(vfsUrl(folder)).stream()
                .map(File::getName).sorted().toList();

        assertEquals(List.of("nested", "one.txt"), names);
    }

    @Test
    void materializeFolder_givesTheFolderItself() throws IOException {
        File folder = createFolder();

        assertEquals(folder, JBossVfsUtil.materializeFolder(vfsUrl(folder)));
    }

    @Test
    void notAVirtualFile_failsWithAnIOException() throws IOException {
        URL url = vfsUrl(new NotAVirtualFile());

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

    private URL vfsUrl(File folder) throws IOException {
        return vfsUrl(new MockVirtualFile(folder));
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
