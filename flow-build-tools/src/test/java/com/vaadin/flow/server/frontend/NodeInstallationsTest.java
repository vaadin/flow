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

import static com.vaadin.flow.server.frontend.NodeInstallation.LAST_USED_FILE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeInstallationsTest {

    @TempDir
    File vaadinHome;

    private static final Instant LONG_AGO = Instant.now()
            .minus(Duration.ofDays(400));
    private static final Instant RECENTLY = Instant.now()
            .minus(Duration.ofDays(3));

    @Test
    void removeUnused_removesOnlyLongUnusedInstallations() throws IOException {
        NodeInstallation inUse = installation("node-v24.19.0", LONG_AGO);
        NodeInstallation stale = installation("node-v20.0.0", LONG_AGO);
        NodeInstallation recent = installation("node-v22.0.0", RECENTLY);

        // The remains of an earlier removal, and neighbours in ~/.vaadin that
        // are none of our business
        File remains = new File(vaadinHome, ".removed-node-v18.0.0");
        assertTrue(new File(remains, "bin").mkdirs());
        File legacyNodeFolder = new File(vaadinHome, "node");
        assertTrue(new File(legacyNodeFolder, "bin").mkdirs());
        File pnpmCache = new File(vaadinHome, "pnpm");
        assertTrue(pnpmCache.mkdirs());
        File offlineKey = new File(vaadinHome, "offlineKey");
        assertTrue(offlineKey.createNewFile());

        NodeInstallations.removeUnused(vaadinHome, inUse);

        assertFalse(exists(stale),
                "An installation unused for over 6 months should be removed");
        assertTrue(exists(inUse),
                "The installation in use should be kept even when stale");
        assertTrue(exists(recent),
                "A recently used installation should be kept");
        assertFalse(remains.exists(),
                "The remains of an earlier removal should be cleaned up");
        assertTrue(legacyNodeFolder.isDirectory(),
                "The legacy unversioned node folder should not be touched");
        assertTrue(pnpmCache.isDirectory(),
                "Unrelated folders should not be touched");
        assertTrue(offlineKey.exists(),
                "Unrelated files should not be touched");
    }

    @Test
    void removeUnused_theLastInstallationIsKept() throws IOException {
        NodeInstallation only = installation("node-v20.0.0", LONG_AGO);

        NodeInstallations.removeUnused(vaadinHome, null);

        assertTrue(exists(only),
                "The last version in the folder should never be removed");
    }

    @Test
    void removeUnused_unknownLastUse_isKeptAndItsRetentionStartsNow()
            throws IOException {
        NodeInstallation inUse = installation("node-v24.19.0", Instant.now());
        NodeInstallation legacy = installation("node-v20.0.0", null);
        assertTrue(
                legacy.getDirectory().setLastModified(LONG_AGO.toEpochMilli()),
                "Test setup should be able to backdate the directory");

        NodeInstallations.removeUnused(vaadinHome, inUse);

        assertTrue(exists(legacy),
                "An installation installed before the marker existed may still be in daily use and must be kept");
        assertTrue(legacy.getLastUsed().orElseThrow().isAfter(RECENTLY),
                "The installation should have been marked so its retention period starts now");
    }

    @Test
    void removeUnused_removesTheLeftoverArchivesOfInstalledVersions()
            throws IOException {
        NodeInstallation inUse = installation("node-v24.19.0", Instant.now());
        installation("node-v20.0.0", LONG_AGO);

        File inUseArchive = archive("node-v24.19.0-linux-x64.tar.gz", LONG_AGO);
        File removedArchive = archive("node-v20.0.0-darwin-arm64.tar.xz",
                LONG_AGO);
        File notInstalled = archive("node-v26.0.0-linux-x64.tar.gz", LONG_AGO);
        File downloading = archive("node-v24.20.0-linux-x64.tar.gz",
                Instant.now());
        File notAnArchive = archive("node-SHASUMS256.txt", LONG_AGO);

        NodeInstallations.removeUnused(vaadinHome, inUse);

        assertFalse(inUseArchive.exists(),
                "An unpacked archive is dead weight even when its installation is kept");
        assertFalse(removedArchive.exists(),
                "The archive of a removed installation should be removed too");
        assertTrue(notInstalled.exists(),
                "An archive of a version that is not installed may have been put there to install from");
        assertTrue(downloading.exists(),
                "An archive that another process may still be downloading should be kept");
        assertTrue(notAnArchive.exists(),
                "Files that are not Node.js archives should not be touched");
    }

    /**
     * Creates an installation directory, marked as last used at the given time
     * or without a marker at all when {@code lastUsed} is {@code null}.
     */
    private NodeInstallation installation(String name, Instant lastUsed)
            throws IOException {
        File directory = new File(vaadinHome, name);
        assertTrue(new File(directory, "bin").mkdirs(),
                "Test setup should be able to create " + name);
        if (lastUsed != null) {
            Files.writeString(new File(directory, LAST_USED_FILE).toPath(),
                    lastUsed.toString(), StandardCharsets.UTF_8);
        }
        return new NodeInstallation(directory);
    }

    private File archive(String name, Instant modified) throws IOException {
        File archive = new File(vaadinHome, name);
        assertTrue(archive.createNewFile(),
                "Test setup should be able to create " + name);
        assertTrue(archive.setLastModified(modified.toEpochMilli()),
                "Test setup should be able to set the age of " + name);
        return archive;
    }

    private static boolean exists(NodeInstallation installation) {
        return installation.getDirectory().isDirectory();
    }
}
