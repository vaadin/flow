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
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinnedNpmVersionsTest {

    @TempDir
    File temporaryFolder;

    private static final String CORE_VERSIONS = """
            {
              "platform": "25.1.0",
              "core": {
                "button": {
                  "npmName": "@vaadin/button",
                  "jsVersion": "25.1.0"
                },
                "grid": {
                  "npmName": "@vaadin/grid",
                  "jsVersion": "25.1.0",
                  "exclusions": ["@vaadin/legacy-grid"]
                }
              }
            }
            """;

    private static final String VAADIN_VERSIONS = """
            {
              "platform": "25.1.0",
              "vaadin": {
                "grid-pro": {
                  "npmName": "@vaadin/grid-pro",
                  "jsVersion": "25.1.0"
                }
              }
            }
            """;

    @Test
    void noVersionsFile_isEmpty() throws IOException {
        PinnedNpmVersions versions = createVersions(null, null);

        assertTrue(versions.isEmpty());
        assertEquals(Optional.empty(), versions.getVaadinVersion());
        assertTrue(versions.getDependencies(false, false, keepEverything())
                .properties().isEmpty());
    }

    @Test
    void onlyCoreVersionsFile_onlyItsDependenciesArePinned()
            throws IOException {
        PinnedNpmVersions versions = createVersions(CORE_VERSIONS, null);

        ObjectNode dependencies = versions.getDependencies(false, false,
                keepEverything());
        assertTrue(dependencies.has("@vaadin/button"));
        assertFalse(dependencies.has("@vaadin/grid-pro"));
        assertEquals(Optional.of("25.1.0"), versions.getVaadinVersion());
    }

    @Test
    void bothVersionsFiles_dependenciesOfBothArePinned() throws IOException {
        PinnedNpmVersions versions = createVersions(CORE_VERSIONS,
                VAADIN_VERSIONS);

        ObjectNode dependencies = versions.getDependencies(false, false,
                keepEverything());
        assertTrue(dependencies.has("@vaadin/button"));
        assertTrue(dependencies.has("@vaadin/grid-pro"));
        assertEquals("25.1.0",
                versions.getAllDependencies().get("@vaadin/grid").asString());
    }

    @Test
    void exclusionsOfBothVersionsFilesAreCollected() throws IOException {
        PinnedNpmVersions versions = createVersions(CORE_VERSIONS,
                VAADIN_VERSIONS);

        assertEquals(Set.of("@vaadin/legacy-grid"),
                versions.getExclusions(false, false));
    }

    @Test
    void versionOfOneFileIsFilteredOut_theVersionOfTheOtherIsUsed()
            throws IOException {
        PinnedNpmVersions versions = createVersions(CORE_VERSIONS, """
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
        // version the other file declares with it
        assertEquals("25.1.0",
                versions.getDependencies(false, false, keepEverything())
                        .get("@vaadin/button").asString());
    }

    @Test
    void filterIsGivenTheFileTheVersionsCameFrom() throws IOException {
        PinnedNpmVersions versions = createVersions(CORE_VERSIONS,
                VAADIN_VERSIONS);
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

        versions.getDependencies(false, false, filter);

        assertEquals(List.of(Constants.VAADIN_CORE_VERSIONS_JSON,
                Constants.VAADIN_VERSIONS_JSON), origins);
    }

    private static VersionsJsonFilter keepEverything() {
        return new VersionsJsonFilter(JacksonUtils.createObjectNode(),
                NodeUpdater.DEPENDENCIES);
    }

    private PinnedNpmVersions createVersions(String coreVersions,
            String vaadinVersions) throws IOException {
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(writeVersionsFile(
                        Constants.VAADIN_CORE_VERSIONS_JSON, coreVersions));
        Mockito.when(finder.getResource(Constants.VAADIN_VERSIONS_JSON))
                .thenReturn(writeVersionsFile(Constants.VAADIN_VERSIONS_JSON,
                        vaadinVersions));
        return new PinnedNpmVersions(finder);
    }

    private URL writeVersionsFile(String name, String content)
            throws IOException {
        if (content == null) {
            return null;
        }
        File file = new File(temporaryFolder, name);
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file.toURI().toURL();
    }
}
