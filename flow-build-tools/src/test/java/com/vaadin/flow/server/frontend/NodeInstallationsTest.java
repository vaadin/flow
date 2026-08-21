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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.vaadin.flow.server.frontend.NodeInstallation.LAST_USED_FILE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void findAll_onlyVersionedInstallationDirectories() throws IOException {
        installation("node-v24.19.0", RECENTLY);
        installation("node-v20.0.0", RECENTLY);
        // The legacy unversioned installation folder and other neighbours
        assertTrue(new File(vaadinHome, "node/bin").mkdirs());
        assertTrue(new File(vaadinHome, "pnpm").mkdirs());
        assertTrue(new File(vaadinHome, "node-v22.0.0-linux-x64.tar.gz")
                .createNewFile());
        // What a removal that could not delete every file left behind
        assertTrue(new File(vaadinHome, ".removed-node-v18.0.0/bin").mkdirs());

        assertEquals(List.of("node-v20.0.0", "node-v24.19.0"),
                NodeInstallations.findAll(vaadinHome).stream()
                        .map(found -> found.getDirectory().getName()).sorted()
                        .toList());
    }

    @Test
    void findAll_emptyDirectory_isEmpty() {
        assertTrue(NodeInstallations.findAll(vaadinHome).isEmpty());
    }

    @Test
    void findLeftoverArchives_onlyUnpackedOldNodeArchives() throws IOException {
        installation("node-v20.0.0", RECENTLY);
        installation("node-v22.0.0", RECENTLY);
        installation("node-v24.19.0", RECENTLY);
        archive("node-v20.0.0-darwin-arm64.tar.xz", LONG_AGO);
        archive("node-v22.0.0-win-x64.zip", LONG_AGO);
        archive("node-v24.19.0-linux-x64.tar.gz", LONG_AGO);
        // A download that another process may still be writing
        archive("node-v24.20.0-linux-x64.tar.gz", Instant.now());
        // Neighbours that are not Node.js archives
        archive("node-SHASUMS256.txt", LONG_AGO);
        archive("node-v20.0.0-notes.txt", LONG_AGO);
        assertTrue(new File(vaadinHome, "offlineKey").createNewFile());

        assertEquals(List.of("node-v20.0.0-darwin-arm64.tar.xz",
                "node-v22.0.0-win-x64.zip", "node-v24.19.0-linux-x64.tar.gz"),
                NodeInstallations.findLeftoverArchives(vaadinHome).stream()
                        .map(File::getName).sorted().toList());
    }

    @Test
    void findLeftoverArchives_versionThatIsNotInstalled_isKept()
            throws IOException {
        installation("node-v24.19.0", RECENTLY);
        archive("node-v26.0.0-linux-x64.tar.gz", LONG_AGO);

        assertTrue(NodeInstallations.findLeftoverArchives(vaadinHome).isEmpty(),
                "An archive of a version that is not installed may have been put there to install from");
    }

    @Test
    void removeUnused_removesOnlyLongUnusedInstallations() throws IOException {
        NodeInstallation inUse = installation("node-v24.19.0", Instant.now());
        NodeInstallation stale = installation("node-v20.0.0", LONG_AGO);
        NodeInstallation recent = installation("node-v22.0.0", RECENTLY);

        NodeInstallations.removeUnused(vaadinHome, inUse);

        assertTrue(exists(inUse), "The installation in use should be kept");
        assertTrue(exists(recent),
                "A recently used installation should be kept");
        assertFalse(exists(stale),
                "An installation unused for over 6 months should be removed");
    }

    @Test
    void removeUnused_neverRemovesTheInstallationInUse() throws IOException {
        NodeInstallation inUse = installation("node-v24.19.0", LONG_AGO);
        NodeInstallation stale = installation("node-v20.0.0", LONG_AGO);

        NodeInstallations.removeUnused(vaadinHome, inUse);

        assertTrue(exists(inUse),
                "The installation in use should be kept even when stale");
        assertFalse(exists(stale));
    }

    @Test
    void removeUnused_singleStaleInstallation_isKept() throws IOException {
        NodeInstallation only = installation("node-v20.0.0", LONG_AGO);

        NodeInstallations.removeUnused(vaadinHome, null);

        assertTrue(exists(only),
                "The last version in the folder should never be removed");
    }

    @Test
    void removeUnused_allStaleAndNoneInUse_keepsMostRecentlyUsed()
            throws IOException {
        NodeInstallation older = installation("node-v20.0.0", LONG_AGO);
        NodeInstallation newer = installation("node-v22.0.0",
                LONG_AGO.plus(Duration.ofDays(10)));

        NodeInstallations.removeUnused(vaadinHome, null);

        assertFalse(exists(older));
        assertTrue(exists(newer),
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
    void removeUnused_removesLeftoverArchivesOfEveryVersion()
            throws IOException {
        NodeInstallation inUse = installation("node-v24.19.0", Instant.now());
        installation("node-v22.0.0", RECENTLY);
        NodeInstallation stale = installation("node-v20.0.0", LONG_AGO);

        File inUseArchive = archive("node-v24.19.0-linux-x64.tar.gz", LONG_AGO);
        File recentArchive = archive("node-v22.0.0-win-x64.zip", LONG_AGO);
        File staleArchive = archive("node-v20.0.0-darwin-arm64.tar.xz",
                LONG_AGO);
        File downloading = archive("node-v24.20.0-linux-x64.tar.gz",
                Instant.now());

        NodeInstallations.removeUnused(vaadinHome, inUse);

        assertFalse(inUseArchive.exists(),
                "An unpacked archive is dead weight even when its installation is kept");
        assertFalse(recentArchive.exists(),
                "An unpacked archive is dead weight even when its installation is kept");
        assertFalse(staleArchive.exists(),
                "The archive of a removed installation should be removed too");
        assertFalse(exists(stale));
        assertTrue(downloading.exists(),
                "An archive that another process may still be downloading should be kept");
    }

    @Test
    void removeUnused_remainsOfAnInterruptedRemoval_areDeleted()
            throws IOException {
        NodeInstallation inUse = installation("node-v24.19.0", Instant.now());
        File remains = new File(vaadinHome, ".removed-node-v18.0.0");
        assertTrue(new File(remains, "bin").mkdirs());

        NodeInstallations.removeUnused(vaadinHome, inUse);

        assertFalse(remains.exists(),
                "The remains of an earlier removal should be cleaned up");
    }

    @Test
    void removeUnused_unrelatedFilesAndFolders_areNotTouched()
            throws IOException {
        NodeInstallation inUse = installation("node-v24.19.0", Instant.now());
        NodeInstallation stale = installation("node-v20.0.0", LONG_AGO);

        // The legacy unversioned installation folder and other caches living
        // next to the versioned installations in ~/.vaadin
        File legacyNodeFolder = new File(vaadinHome, "node");
        assertTrue(new File(legacyNodeFolder, "bin").mkdirs());
        legacyNodeFolder.setLastModified(LONG_AGO.toEpochMilli());
        File pnpmCache = new File(vaadinHome, "pnpm");
        assertTrue(pnpmCache.mkdirs());
        pnpmCache.setLastModified(LONG_AGO.toEpochMilli());
        File offlineKey = new File(vaadinHome, "offlineKey");
        assertTrue(offlineKey.createNewFile());
        File shaSums = archive("node-SHASUMS256.txt", LONG_AGO);
        File notAnArchive = archive("node-v20.0.0-notes.txt", LONG_AGO);

        NodeInstallations.removeUnused(vaadinHome, inUse);

        assertFalse(exists(stale), "The stale installation should be removed");
        assertTrue(legacyNodeFolder.isDirectory(),
                "The legacy unversioned node folder should not be touched");
        assertTrue(pnpmCache.isDirectory(),
                "Unrelated folders should not be touched");
        assertTrue(offlineKey.exists(),
                "Unrelated files should not be touched");
        assertTrue(shaSums.exists(),
                "Files that are not Node.js archives should not be touched");
        assertTrue(notAnArchive.exists(),
                "Files that are not Node.js archives should not be touched");
    }

    @Test
    void removeUnused_emptyDirectory_doesNotFail() {
        assertDoesNotThrow(
                () -> NodeInstallations.removeUnused(vaadinHome, null));
        assertEquals(0, vaadinHome.listFiles().length);
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
