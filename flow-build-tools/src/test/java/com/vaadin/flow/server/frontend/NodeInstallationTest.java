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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.internal.FrontendUtils;

import static com.vaadin.flow.server.frontend.NodeInstallation.LAST_USED_FILE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeInstallationTest {

    private static final Instant LONG_AGO = Instant.now()
            .minus(Duration.ofDays(400));

    @TempDir
    File vaadinHome;

    @Test
    void forVersion_versionWithAndWithoutPrefix_sameDirectory() {
        assertEquals(new File(vaadinHome, "node-v24.10.0").getAbsolutePath(),
                NodeInstallation.forVersion(vaadinHome, "v24.10.0")
                        .getDirectory().getAbsolutePath());
        assertEquals(new File(vaadinHome, "node-v24.10.0").getAbsolutePath(),
                NodeInstallation.forVersion(vaadinHome, "24.10.0")
                        .getDirectory().getAbsolutePath());
    }

    @Test
    void getVersion_readFromDirectoryName() {
        assertEquals("v24.10.0",
                new NodeInstallation(new File(vaadinHome, "node-v24.10.0"))
                        .getVersion());
    }

    @Test
    void getExecutables_pointInsideTheInstallation() {
        NodeInstallation installation = NodeInstallation.forVersion(vaadinHome,
                "v24.10.0");
        File directory = installation.getDirectory();

        String nodePath = FrontendUtils.isWindows() ? "node.exe" : "bin/node";
        String npmPath = FrontendUtils.isWindows()
                ? "node_modules/npm/bin/npm-cli.js"
                : "lib/node_modules/npm/bin/npm-cli.js";

        assertEquals(new File(directory, nodePath),
                installation.getNodeExecutable());
        assertEquals(new File(directory, npmPath),
                installation.getNpmCliScript());
    }

    @Test
    void hasNodeExecutable_reflectsTheFileSystem() throws IOException {
        NodeInstallation installation = NodeInstallation.forVersion(vaadinHome,
                "v24.10.0");
        assertFalse(installation.hasNodeExecutable());

        File nodeExecutable = installation.getNodeExecutable();
        assertTrue(nodeExecutable.getParentFile().mkdirs());
        assertTrue(nodeExecutable.createNewFile());

        assertTrue(installation.hasNodeExecutable());
    }

    @Test
    void markUsed_writesParseableTimestamp() throws IOException {
        NodeInstallation installation = create("node-v24.10.0");

        Instant before = Instant.now().minus(Duration.ofSeconds(1));
        installation.markUsed();

        assertTrue(installation.getLastUsed().isPresent());
        assertFalse(installation.getLastUsed().get().isBefore(before),
                "last-used should hold the current time");
    }

    @Test
    void markUsed_overwritesPreviousTimestamp() throws IOException {
        NodeInstallation installation = create("node-v24.10.0");
        write(installation, LONG_AGO.toString());

        installation.markUsed();

        assertTrue(installation.getLastUsed().orElseThrow().isAfter(LONG_AGO),
                "last-used should have been refreshed");
    }

    @Test
    void markUsed_missingDirectory_doesNotFail() {
        NodeInstallation installation = NodeInstallation.forVersion(vaadinHome,
                "v24.10.0");

        assertDoesNotThrow(installation::markUsed);

        assertTrue(installation.getLastUsed().isEmpty());
    }

    @Test
    void getLastUsed_noMarker_isEmpty() throws IOException {
        assertTrue(create("node-v24.10.0").getLastUsed().isEmpty());
    }

    @Test
    void getLastUsed_unparseableMarker_isEmpty() throws IOException {
        NodeInstallation installation = create("node-v24.10.0");
        write(installation, "not a timestamp");

        assertTrue(installation.getLastUsed().isEmpty());
    }

    @Test
    void equals_sameDirectory_isEqual() throws IOException {
        NodeInstallation installation = create("node-v24.10.0");
        NodeInstallation other = create("node-v22.0.0");
        NodeInstallation same = NodeInstallation.forVersion(vaadinHome,
                "24.10.0");

        assertEquals(installation, same);
        assertEquals(installation.hashCode(), same.hashCode());
        assertNotEquals(installation, other);
        assertNotEquals(installation, null);
    }

    @Test
    void remove_deletesTheWholeInstallation() throws IOException {
        NodeInstallation installation = create("node-v24.10.0");
        File nodeExecutable = installation.getNodeExecutable();
        assertTrue(nodeExecutable.getParentFile().mkdirs());
        assertTrue(nodeExecutable.createNewFile());

        installation.remove();

        assertFalse(installation.getDirectory().exists());
    }

    private NodeInstallation create(String directoryName) throws IOException {
        File directory = new File(vaadinHome, directoryName);
        assertTrue(directory.mkdirs(),
                "Test setup should be able to create " + directoryName);
        return new NodeInstallation(directory);
    }

    private static void write(NodeInstallation installation, String content)
            throws IOException {
        Files.writeString(
                new File(installation.getDirectory(), LAST_USED_FILE).toPath(),
                content, StandardCharsets.UTF_8);
    }
}
