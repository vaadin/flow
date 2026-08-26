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
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

class PinnedNpmVersionsTest {

    @TempDir
    File temporaryFolder;

    private int fileCount;

    @Test
    void dependenciesFromAllVersionsFilesAreMerged() throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
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

        ObjectNode dependencies = pinnedNpmVersions.getDependencies(false,
                false);

        assertEquals("25.1.0", dependencies.get("@vaadin/button").asString());
        assertEquals("25.1.0", dependencies.get("@vaadin/grid").asString());
    }

    @Test
    void samePackageInMultipleFiles_newestVersionIsUsed() throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
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

        assertEquals("25.1.2", pinnedNpmVersions.getDependencies(false, false)
                .get("@vaadin/button").asString());
        assertEquals("25.1.2", pinnedNpmVersions.getAllDependencies()
                .get("@vaadin/button").asString());
    }

    @Test
    void packageExcludedInOneFile_isExcludedFromAllFiles() throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
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

        ObjectNode dependencies = pinnedNpmVersions.getDependencies(false,
                false);

        assertFalse(dependencies.has("@vaadin/button"));
        assertTrue(dependencies.has("@vaadin/grid"));
    }

    @Test
    void platformVersionIsReadFromTheFileDeclaringIt() throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
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

        assertEquals(Optional.of("25.1.0"),
                pinnedNpmVersions.getPlatformVersion());
    }

    @Test
    void versionsFilesInsideAJarAreRead() throws IOException {
        File jar = new File(temporaryFolder, "components.jar");
        writeJar(jar, Map.of(
                Constants.PINNED_NPM_VERSIONS_FOLDER + "button.json", """
                        {
                          "core": {
                            "button": {
                              "npmName": "@vaadin/button",
                              "jsVersion": "25.1.0"
                            }
                          }
                        }
                        """, Constants.PINNED_NPM_VERSIONS_FOLDER + "grid.json",
                """
                        {
                          "core": {
                            "grid": {
                              "npmName": "@vaadin/grid",
                              "jsVersion": "25.1.0"
                            }
                          }
                        }
                        """,
                // Only the files directly in the folder are versions files
                Constants.PINNED_NPM_VERSIONS_FOLDER + "nested/other.json", """
                        {
                          "core": {
                            "dialog": {
                              "npmName": "@vaadin/dialog",
                              "jsVersion": "25.1.0"
                            }
                          }
                        }
                        """));

        // Looked up through a real class loader, the way the versions files
        // are found when the build runs
        ObjectNode dependencies;
        try (URLClassLoader classLoader = new URLClassLoader(
                new URL[] { jar.toURI().toURL() }, null)) {
            ClassFinder finder = new ClassFinder.DefaultClassFinder(
                    classLoader);
            dependencies = new PinnedNpmVersions(finder).getDependencies(false,
                    false);
        }

        assertTrue(dependencies.has("@vaadin/button"));
        assertTrue(dependencies.has("@vaadin/grid"));
        assertFalse(dependencies.has("@vaadin/dialog"));
    }

    @Test
    void versionsFilesAreReadFromAJarWithoutFolderEntries() throws IOException {
        File jar = new File(temporaryFolder, "no-folder-entries.jar");
        writeJar(jar,
                Map.of(Constants.PINNED_NPM_VERSIONS_FOLDER + "button.json", """
                        {
                          "core": {
                            "button": {
                              "npmName": "@vaadin/button",
                              "jsVersion": "25.1.0"
                            }
                          }
                        }
                        """), false);

        // A class loader does not report a folder that has no entry of its
        // own, but the files are still read for a handler that reports it
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResources(Constants.PINNED_NPM_VERSIONS_FOLDER))
                .thenReturn(List.of(URI
                        .create("jar:" + jar.toURI() + "!/"
                                + Constants.PINNED_NPM_VERSIONS_FOLDER)
                        .toURL()));

        assertTrue(new PinnedNpmVersions(finder).getDependencies(false, false)
                .has("@vaadin/button"));
    }

    /**
     * Writes a jar with the given entries, including the folder entries a class
     * loader needs to find a folder inside a jar.
     */
    private static void writeJar(File jar, Map<String, String> entries)
            throws IOException {
        writeJar(jar, entries, true);
    }

    private static void writeJar(File jar, Map<String, String> entries,
            boolean withFolderEntries) throws IOException {
        Set<String> folders = new LinkedHashSet<>();
        if (withFolderEntries) {
            entries.keySet().forEach(name -> {
                for (int slash = name.indexOf('/'); slash != -1; slash = name
                        .indexOf('/', slash + 1)) {
                    folders.add(name.substring(0, slash + 1));
                }
            });
        }
        try (JarOutputStream jarStream = new JarOutputStream(
                new FileOutputStream(jar))) {
            for (String folder : folders) {
                jarStream.putNextEntry(new JarEntry(folder));
                jarStream.closeEntry();
            }
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                jarStream.putNextEntry(new JarEntry(entry.getKey()));
                jarStream.write(
                        entry.getValue().getBytes(StandardCharsets.UTF_8));
                jarStream.closeEntry();
            }
        }
    }

    /**
     * Writes each versions file content into its own folder, as if each came
     * from a different jar.
     */
    private PinnedNpmVersions createPinnedNpmVersions(String... versionsFiles)
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
        Mockito.when(finder.getResources(Constants.PINNED_NPM_VERSIONS_FOLDER))
                .thenReturn(folders);
        return new PinnedNpmVersions(finder);
    }
}
