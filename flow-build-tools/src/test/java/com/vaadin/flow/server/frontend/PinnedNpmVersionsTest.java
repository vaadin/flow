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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.MockLogger;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.Platform;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.tests.util.MockOptions;

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
                false, keepEverything());

        assertEquals("25.1.0", dependencies.get("@vaadin/button").asString());
        assertEquals("25.1.0", dependencies.get("@vaadin/grid").asString());
    }

    @Test
    void samePackageInMultipleFiles_newestVersionIsUsed() throws IOException {
        // The newest wins whether it is read before or after the older one
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "core": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.2"
                    },
                    "grid": {
                      "npmName": "@vaadin/grid",
                      "jsVersion": "25.1.1"
                    }
                  }
                }
                """, """
                {
                  "components": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.1"
                    },
                    "grid": {
                      "npmName": "@vaadin/grid",
                      "jsVersion": "25.1.2"
                    }
                  }
                }
                """);

        ObjectNode dependencies = pinnedNpmVersions.getDependencies(false,
                false, keepEverything());
        assertEquals("25.1.2", dependencies.get("@vaadin/button").asString());
        assertEquals("25.1.2", dependencies.get("@vaadin/grid").asString());
        assertEquals("25.1.2", pinnedNpmVersions.getAllDependencies()
                .get("@vaadin/button").asString());
        assertEquals("25.1.2", pinnedNpmVersions.getAllDependencies()
                .get("@vaadin/grid").asString());
    }

    @Test
    void versionsFileIsBroken_theOtherFilesArePinnedAllTheSame()
            throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "core": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """, "{ this is not json",
                // A versions file is not a list either
                """
                        [
                          { "npmName": "@vaadin/grid" }
                        ]
                        """);

        // A jar shipping a broken versions file cannot take the build down
        // with it
        assertTrue(pinnedNpmVersions
                .getDependencies(false, false, keepEverything())
                .has("@vaadin/button"));
    }

    @Test
    void versionThatCannotBeComparedIsReplacedByOneThatCan()
            throws IOException {
        // The version that can be compared wins whether it is read before or
        // after the one that cannot
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "core": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "a folder link"
                    },
                    "grid": {
                      "npmName": "@vaadin/grid",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """, """
                {
                  "components": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.0"
                    },
                    "grid": {
                      "npmName": "@vaadin/grid",
                      "jsVersion": "a folder link"
                    }
                  }
                }
                """);

        ObjectNode dependencies = pinnedNpmVersions.getAllDependencies();
        assertEquals("25.1.0", dependencies.get("@vaadin/button").asString());
        assertEquals("25.1.0", dependencies.get("@vaadin/grid").asString());
    }

    @Test
    void theCoreFileOfThePlatformGivesTheVaadinVersion() throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "platform": "42.0.0",
                  "core": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """, """
                {
                  "platform": "25.1.0",
                  "react": {
                    "react-components": {
                      "npmName": "@vaadin/react-components",
                      "jsVersion": "25.1.0",
                      "mode": "react"
                    }
                  }
                }
                """);

        // The core versions file of the platform declares the React
        // components, and no other jar does
        assertEquals(Optional.of("25.1.0"),
                pinnedNpmVersions.getVaadinVersion());
    }

    @Test
    void onlyThePlatformFileGivesTheVaadinVersion() throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "platform": "42.0.0",
                  "core": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """, """
                {
                  "platform": "25.1.0",
                  "core": {
                    "vaadin-core": {
                      "npmName": "@vaadin/vaadin-core",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """);

        // The file of a jar that is not the platform cannot say what the
        // Vaadin version is, whatever it declares
        assertEquals(Optional.of("25.1.0"),
                pinnedNpmVersions.getVaadinVersion());
    }

    @Test
    void differentVaadinVersionsInMultipleFiles_newestVersionIsUsed()
            throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "platform": "25.1.1",
                  "core": {
                    "vaadin-core": {
                      "npmName": "@vaadin/vaadin-core",
                      "jsVersion": "25.1.1"
                    }
                  }
                }
                """, """
                {
                  "platform": "25.1.2",
                  "core": {
                    "vaadin-core": {
                      "npmName": "@vaadin/vaadin-core",
                      "jsVersion": "25.1.2"
                    }
                  }
                }
                """);

        assertEquals(Optional.of("25.1.2"),
                pinnedNpmVersions.getVaadinVersion());
    }

    @Test
    void allDependenciesAreCollectedWhicheverModeTheyApplyTo()
            throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "core": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.0"
                    },
                    "board": {
                      "npmName": "@vaadin/board",
                      "jsVersion": "25.1.0",
                      "mode": "react"
                    }
                  }
                }
                """);

        ObjectNode dependencies = pinnedNpmVersions.getAllDependencies();

        // A package of another mode is pinned as well, as it can still be
        // pulled in transitively
        assertTrue(dependencies.has("@vaadin/button"));
        assertTrue(dependencies.has("@vaadin/board"));
    }

    @Test
    void packageLeftOutOfOneFileByItsMode_isPinnedByTheFileDeclaringIt()
            throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "core": {
                    "grid": {
                      "npmName": "@vaadin/grid",
                      "jsVersion": "25.1.0",
                      "mode": "react"
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

        // The first file leaves the package out because web components are
        // not wanted, which says nothing about the file declaring it for
        // every mode
        assertTrue(
                pinnedNpmVersions.getDependencies(false, true, keepEverything())
                        .has("@vaadin/grid"));
    }

    @Test
    void packageWithoutAVersion_theOtherPackagesArePinnedAllTheSame()
            throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "core": {
                    "grid": {
                      "npmName": "@vaadin/grid"
                    },
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """);

        // A package a versions file gives no version for cannot be pinned,
        // which is no reason to give up on the packages that have one
        ObjectNode dependencies = pinnedNpmVersions.getDependencies(false,
                false, keepEverything());
        assertFalse(dependencies.has("@vaadin/grid"));
        assertTrue(dependencies.has("@vaadin/button"));
        assertTrue(
                pinnedNpmVersions.getAllDependencies().has("@vaadin/button"));
    }

    @Test
    void packageWithoutAVersion_isWarnedAboutOnceForTheBuild()
            throws IOException {
        String versionsFile = """
                {
                  "core": {
                    "grid": {
                      "npmName": "@vaadin/grid"
                    }
                  }
                }
                """;
        File folder = new File(temporaryFolder, "jar" + fileCount++);
        folder.mkdirs();
        File file = new File(folder, "versions.json");
        Files.writeString(file.toPath(), versionsFile, StandardCharsets.UTF_8);
        String origin = file.getAbsolutePath();
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResources(Constants.PINNED_NPM_VERSIONS_FOLDER))
                .thenReturn(List.of(folder.toURI().toURL()));
        Options options = new MockOptions(finder, temporaryFolder);
        ExclusionFilter exclusionFilter = new ExclusionFilter(options, true,
                false);

        MockLogger logger = new MockLogger();
        try (MockedStatic<LoggerFactory> loggerFactory = Mockito
                .mockStatic(LoggerFactory.class)) {
            loggerFactory.when(
                    () -> LoggerFactory.getLogger(PinnedNpmVersions.class))
                    .thenReturn(logger);
            loggerFactory.when(
                    () -> LoggerFactory.getLogger(VersionsJsonConverter.class))
                    .thenReturn(new MockLogger());

            // The build asks the versions for one thing after another
            exclusionFilter.exclude(Map.of());
            options.getPinnedNpmVersions().getAllDependencies();
        }

        List<String> warnings = logger.getLogs().lines()
                .filter(line -> line.startsWith(MockLogger.WARN)).toList();
        assertEquals(1, warnings.size(),
                "The file should be warned about once, logs were "
                        + logger.getLogs());
        assertTrue(warnings.get(0).contains("@vaadin/grid"),
                "The warning should name the package without a version, was "
                        + warnings.get(0));
        assertTrue(warnings.get(0).contains(origin),
                "The warning should name the file, was " + warnings.get(0));
    }

    @Test
    void exclusionsOfAllFilesAreCollected() throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "core": {
                    "grid": {
                      "npmName": "@vaadin/grid",
                      "jsVersion": "25.1.0",
                      "exclusions": ["@vaadin/legacy-grid"]
                    }
                  }
                }
                """, """
                {
                  "components": {
                    "grid-pro": {
                      "npmName": "@vaadin/grid-pro",
                      "jsVersion": "25.1.0",
                      "exclusions": ["@vaadin/legacy-grid-pro"]
                    }
                  }
                }
                """);

        assertEquals(Set.of("@vaadin/legacy-grid", "@vaadin/legacy-grid-pro"),
                pinnedNpmVersions.getExclusions(false, false));
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
                false, keepEverything());

        assertFalse(dependencies.has("@vaadin/button"));
        assertTrue(dependencies.has("@vaadin/grid"));
    }

    @Test
    void packageOfThePlatformItself_isNotPinnedAndDoesNotHideTheOthers()
            throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "core": {
                    "vaadin-core": {
                      "npmName": "@vaadin/vaadin-core",
                      "jsVersion": "25.1.0"
                    },
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """);

        ObjectNode dependencies = pinnedNpmVersions.getAllDependencies();

        assertFalse(dependencies.has("@vaadin/vaadin-core"));
        // The packages declared after it are collected all the same
        assertTrue(dependencies.has("@vaadin/button"));
    }

    @Test
    void vaadinVersionIsReadFromTheFileDeclaringIt() throws IOException {
        // The first file of the platform does not declare the version
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "core": {
                    "vaadin-core": {
                      "npmName": "@vaadin/vaadin-core",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """, """
                {
                  "platform": "25.1.0",
                  "components": {
                    "vaadin-core": {
                      "npmName": "@vaadin/vaadin-core",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """);

        assertEquals(Optional.of("25.1.0"),
                pinnedNpmVersions.getVaadinVersion());
    }

    @Test
    void noPlatformFile_theVersionOfThePlatformItselfIsUsed()
            throws IOException {
        PinnedNpmVersions pinnedNpmVersions = createPinnedNpmVersions("""
                {
                  "platform": "42.0.0",
                  "core": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """);

        // No file of the platform, so the version of the Vaadin on the
        // classpath is used and what the file says is ignored
        try (MockedStatic<Platform> platform = Mockito
                .mockStatic(Platform.class)) {
            platform.when(() -> Platform.getVaadinVersion(Mockito.any()))
                    .thenReturn(Optional.of("25.1.0"));

            assertEquals(Optional.of("25.1.0"),
                    pinnedNpmVersions.getVaadinVersion());
        }
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
                    false, keepEverything());
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

        assertTrue(new PinnedNpmVersions(finder)
                .getDependencies(false, false, keepEverything())
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

    @Test
    void versionOfOneFileIsFilteredOut_theVersionOfTheOtherIsUsed()
            throws IOException {
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
                  "core": {
                    "button": {
                      "npmName": "@vaadin/button",
                      "jsVersion": "26.0.0-SNAPSHOT"
                    }
                  }
                }
                """);

        // The filter drops a SNAPSHOT, and dropping it must not take the
        // version another file declares with it
        assertEquals("25.1.0",
                pinnedNpmVersions
                        .getDependencies(false, false, keepEverything())
                        .get("@vaadin/button").asString());
    }

    @Test
    void filterIsGivenTheFileTheVersionsCameFrom() throws IOException {
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
                  "core": {
                    "grid": {
                      "npmName": "@vaadin/grid",
                      "jsVersion": "25.1.0"
                    }
                  }
                }
                """);
        List<String> origins = new ArrayList<>();
        VersionsJsonFilter filter = new VersionsJsonFilter(
                JacksonUtils.createObjectNode(), NodeUpdater.DEPENDENCIES) {
            @Override
            ObjectNode getFilteredVersions(ObjectNode versionsJson,
                    String versionOrigin) {
                origins.add(versionOrigin);
                return super.getFilteredVersions(versionsJson, versionOrigin);
            }
        };

        pinnedNpmVersions.getDependencies(false, false, filter);

        // Each file is filtered on its own, with the location it was read from
        assertEquals(2, origins.size());
        assertTrue(
                origins.stream()
                        .allMatch(origin -> origin.endsWith("/versions.json")),
                "The filter should be given the location of each versions file, was "
                        + origins);
    }

    private static VersionsJsonFilter keepEverything() {
        return new VersionsJsonFilter(JacksonUtils.createObjectNode(),
                NodeUpdater.DEPENDENCIES);
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
