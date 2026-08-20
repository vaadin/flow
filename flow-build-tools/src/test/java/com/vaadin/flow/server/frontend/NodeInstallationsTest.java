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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeInstallationsTest {

    @TempDir
    File vaadinHome;

    @Test
    void findAll_onlyVersionedInstallationDirectories() throws IOException {
        installation("node-v24.19.0");
        installation("node-v20.0.0");
        // The legacy unversioned installation folder and other neighbours
        assertTrue(new File(vaadinHome, "node/bin").mkdirs());
        assertTrue(new File(vaadinHome, "pnpm").mkdirs());
        assertTrue(new File(vaadinHome, "node-v22.0.0-linux-x64.tar.gz")
                .createNewFile());
        assertTrue(new File(vaadinHome, "offlineKey").createNewFile());

        assertEquals(List.of("node-v20.0.0", "node-v24.19.0"),
                NodeInstallations.findAll(vaadinHome).stream()
                        .map(found -> found.getDirectory().getName()).sorted()
                        .toList());
    }

    @Test
    void findAll_emptyDirectory_isEmpty() {
        assertTrue(NodeInstallations.findAll(vaadinHome).isEmpty());
    }

    private void installation(String name) {
        assertTrue(new File(new File(vaadinHome, name), "bin").mkdirs(),
                "Test setup should be able to create " + name);
    }
}
