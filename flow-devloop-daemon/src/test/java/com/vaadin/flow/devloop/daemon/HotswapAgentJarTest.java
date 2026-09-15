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
package com.vaadin.flow.devloop.daemon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provisioning has to be offline-clean: the build installs the jar once, while
 * the machine still has network access, and every later {@code start} - of this
 * application or any other on the machine - has to be satisfied by what is
 * already there rather than reaching for the network again.
 */
class HotswapAgentJarTest {

    private final List<String> progress = new ArrayList<>();

    @AfterEach
    void clearOverride() {
        System.clearProperty(HotswapAgentJar.OVERRIDE_PROPERTY);
    }

    @Test
    void cacheDir_isMachineLevelRatherThanPerProject() {
        // One download per machine, and nothing written into a project - which
        // also means mvn clean does not throw it away.
        assertEquals(
                Path.of(System.getProperty("user.home"), ".vaadin", "devloop"),
                HotswapAgentJar.cacheDir());
        assertEquals(
                HotswapAgentJar.cacheDir().resolve(
                        "hotswap-agent-" + HotswapAgentJar.VERSION + ".jar"),
                HotswapAgentJar.cachedJar());
    }

    @Test
    void provision_honoursAnOverrideWithoutTouchingTheNetwork(@TempDir Path dir)
            throws IOException {
        Path own = Files.writeString(dir.resolve("ha.jar"), "not really a jar");
        System.setProperty(HotswapAgentJar.OVERRIDE_PROPERTY, own.toString());

        // No checksum on an override: the point of it is a jar the developer
        // vouches for, which is not required to be the pinned release asset.
        assertEquals(own, HotswapAgentJar.provision(progress::add));
        assertTrue(progress.isEmpty(),
                "nothing was downloaded, so nothing should be reported");
    }

    @Test
    void provision_failsLoudlyWhenTheOverrideIsMissing(@TempDir Path dir) {
        Path absent = dir.resolve("absent.jar");
        System.setProperty(HotswapAgentJar.OVERRIDE_PROPERTY,
                absent.toString());

        IOException e = assertThrows(IOException.class,
                () -> HotswapAgentJar.provision(progress::add));
        assertTrue(e.getMessage().contains(absent.toString()), e.getMessage());
    }
}
