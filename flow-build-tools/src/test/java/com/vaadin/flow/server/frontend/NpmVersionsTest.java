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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpmVersionsTest {

    @TempDir
    File temporaryFolder;

    private int fileCount;

    @Test
    void dependenciesFromAllVersionsFilesAreMerged() throws IOException {
        NpmVersions npmVersions = createNpmVersions("""
                {
                  "core": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """, """
                {
                  "components": {
                    "grid": {
                      "npmName": "@vaadin/grid",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """);

        ObjectNode dependencies = npmVersions.getDependencies(false, false);

        assertEquals("25.1.0", dependencies.get("@vaadin/button").asString());
        assertEquals("25.1.0", dependencies.get("@vaadin/grid").asString());
    }

    @Test
    void samePackageInMultipleFiles_newestVersionIsUsed() throws IOException {
        NpmVersions npmVersions = createNpmVersions("""
                {
                  "core": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.2"
                    }
                  }
                }
                """, """
                {
                  "components": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.1"
                    }
                  }
                }
                """);

        assertEquals("25.1.2", npmVersions.getDependencies(false, false)
                .get("@vaadin/button").asString());
        assertEquals("25.1.2", npmVersions.getAllDependencies()
                .get("@vaadin/button").asString());
    }

    @Test
    void packageExcludedInOneFile_isExcludedFromAllFiles() throws IOException {
        NpmVersions npmVersions = createNpmVersions("""
                {
                  "core": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """, """
                {
                  "components": {
                    "grid": {
                      "npmName": "@vaadin/grid",
                      "jsVersion": "25.1.0",
                      "exclusions": ["@vaadin/button"]
                    }
                  }
                }
                """);

        ObjectNode dependencies = npmVersions.getDependencies(false, false);

        assertFalse(dependencies.has("@vaadin/button"));
        assertTrue(dependencies.has("@vaadin/grid"));
    }

    @Test
    void platformVersionIsReadFromTheFileDeclaringIt() throws IOException {
        NpmVersions npmVersions = createNpmVersions("""
                {
                  "components": {
                    "grid": {
                      "npmName": "@vaadin/grid",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """, """
                {
                  "platform": "25.1.0"
                }
                """);

        assertEquals(Optional.of("25.1.0"), npmVersions.getPlatformVersion());
    }

    @Test
    void versionsFilesInsideAJarAreRead() throws IOException {
        // No folder entries in the jar, as not all jars have them
        File jar = new File(temporaryFolder, "components.jar");
        try (JarOutputStream jarStream = new JarOutputStream(
                new FileOutputStream(jar))) {
            writeJarEntry(jarStream,
                    Constants.NPM_VERSIONS_FOLDER + "button.json", """
                            {
                              "core": {
                                "button": {
                                  "npmName": "@vaadin/button",
                                  "jsVersion": "25.1.0"
                                }
                              }
                            }
                            """);
            writeJarEntry(jarStream,
                    Constants.NPM_VERSIONS_FOLDER + "grid.json", """
                            {
                              "core": {
                                "grid": {
                                  "npmName": "@vaadin/grid",
                                  "jsVersion": "25.1.0"
                                }
                              }
                            }
                            """);
            // Only the files directly in the folder are versions files
            writeJarEntry(jarStream,
                    Constants.NPM_VERSIONS_FOLDER + "nested/other.json", """
                            {
                              "core": {
                                "dialog": {
                                  "npmName": "@vaadin/dialog",
                                  "jsVersion": "25.1.0"
                                }
                              }
                            }
                            """);
        }

        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResources(Constants.NPM_VERSIONS_FOLDER))
                .thenReturn(
                        List.of(URI
                                .create("jar:" + jar.toURI() + "!/"
                                        + Constants.NPM_VERSIONS_FOLDER)
                                .toURL()));

        ObjectNode dependencies = new NpmVersions(finder).getDependencies(false,
                false);

        assertTrue(dependencies.has("@vaadin/button"));
        assertTrue(dependencies.has("@vaadin/grid"));
        assertFalse(dependencies.has("@vaadin/dialog"));
    }

    private static void writeJarEntry(JarOutputStream jarStream, String name,
            String content) throws IOException {
        jarStream.putNextEntry(new JarEntry(name));
        jarStream.write(content.getBytes(StandardCharsets.UTF_8));
        jarStream.closeEntry();
    }

    /**
     * Writes each versions file content into its own folder, as if each came
     * from a different jar.
     */
    private NpmVersions createNpmVersions(String... versionsFiles)
            throws IOException {
        List<URL> folders = new ArrayList<>();
        for (String content : versionsFiles) {
            File folder = new File(temporaryFolder, "jar" + fileCount++);
            folder.mkdirs();
            Files.writeString(new File(folder, "versions.json").toPath(),
                    content, StandardCharsets.UTF_8);
            folders.add(folder.toURI().toURL());
        }
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResources(Constants.NPM_VERSIONS_FOLDER))
                .thenReturn(folders);
        return new NpmVersions(finder);
    }
}
