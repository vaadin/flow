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
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.vaadin.flow.server.frontend.NodeInstallations.LAST_USED_FILE;
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
    void markUsed_writesParseableTimestamp() throws IOException {
        File installation = installation("node-v24.10.0");

        Instant before = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        NodeInstallations.markUsed(installation);

        File marker = new File(installation, LAST_USED_FILE);
        assertTrue(marker.isFile(), "last-used should have been written");
        Instant written = Instant.parse(
                Files.readString(marker.toPath(), StandardCharsets.UTF_8));
        assertFalse(written.isBefore(before),
                "last-used should hold the current time");
    }

    @Test
    void markUsed_overwritesPreviousTimestamp() throws IOException {
        File installation = installation("node-v24.10.0");
        markUsed(installation, LONG_AGO);

        NodeInstallations.markUsed(installation);

        assertTrue(readLastUsed(installation).isAfter(LONG_AGO),
                "last-used should have been refreshed");
    }

    @Test
    void markUsed_missingDirectory_doesNotFail() {
        File installation = new File(vaadinHome, "node-v24.10.0");
        assertDoesNotThrow(() -> NodeInstallations.markUsed(installation));
        assertFalse(new File(installation, LAST_USED_FILE).exists());
    }

    @Test
    void removeUnusedInstallations_removesOnlyLongUnusedOnes()
            throws IOException {
        File inUse = installation("node-v24.19.0");
        markUsed(inUse, Instant.now());
        File stale = installation("node-v20.0.0");
        markUsed(stale, LONG_AGO);
        File recent = installation("node-v22.0.0");
        markUsed(recent, RECENTLY);

        NodeInstallations.removeUnusedInstallations(vaadinHome, inUse);

        assertTrue(inUse.isDirectory(),
                "The installation in use should be kept");
        assertTrue(recent.isDirectory(),
                "A recently used installation should be kept");
        assertFalse(stale.exists(),
                "An installation unused for over 6 months should be removed");
    }

    @Test
    void removeUnusedInstallations_neverRemovesTheInstallationInUse()
            throws IOException {
        File inUse = installation("node-v24.19.0");
        markUsed(inUse, LONG_AGO);
        File stale = installation("node-v20.0.0");
        markUsed(stale, LONG_AGO);

        NodeInstallations.removeUnusedInstallations(vaadinHome, inUse);

        assertTrue(inUse.isDirectory(),
                "The installation in use should be kept even when stale");
        assertFalse(stale.exists());
    }

    @Test
    void removeUnusedInstallations_singleStaleInstallation_isKept()
            throws IOException {
        File only = installation("node-v20.0.0");
        markUsed(only, LONG_AGO);

        NodeInstallations.removeUnusedInstallations(vaadinHome, null);

        assertTrue(only.isDirectory(),
                "The last version in the folder should never be removed");
    }

    @Test
    void removeUnusedInstallations_allStaleAndNoneInUse_keepsMostRecentlyUsed()
            throws IOException {
        File older = installation("node-v20.0.0");
        markUsed(older, LONG_AGO);
        File newer = installation("node-v22.0.0");
        markUsed(newer, LONG_AGO.plus(Duration.ofDays(10)));

        NodeInstallations.removeUnusedInstallations(vaadinHome, null);

        assertFalse(older.exists());
        assertTrue(newer.isDirectory(),
                "The last version in the folder should never be removed");
    }

    @Test
    void removeUnusedInstallations_noMarker_isKeptAndItsRetentionStartsNow()
            throws IOException {
        File inUse = installation("node-v24.19.0");
        File legacy = installation("node-v20.0.0");
        assertTrue(legacy.setLastModified(LONG_AGO.toEpochMilli()),
                "Test setup should be able to backdate the directory");

        NodeInstallations.removeUnusedInstallations(vaadinHome, inUse);

        assertTrue(legacy.isDirectory(),
                "An installation installed before the marker existed may still be in daily use and must be kept");
        assertTrue(readLastUsed(legacy).isAfter(RECENTLY),
                "The installation should have been marked so its retention period starts now");
    }

    @Test
    void removeUnusedInstallations_unparseableMarker_isKeptAndRewritten()
            throws IOException {
        File inUse = installation("node-v24.19.0");
        File broken = installation("node-v20.0.0");
        Files.writeString(new File(broken, LAST_USED_FILE).toPath(),
                "not a timestamp", StandardCharsets.UTF_8);

        NodeInstallations.removeUnusedInstallations(vaadinHome, inUse);

        assertTrue(broken.isDirectory(),
                "A broken marker should not make an installation look stale");
        assertTrue(readLastUsed(broken).isAfter(RECENTLY),
                "The broken marker should have been rewritten");
    }

    @Test
    void removeUnusedInstallations_removesLeftoverArchivesOfEveryVersion()
            throws IOException {
        File inUse = installation("node-v24.19.0");
        markUsed(inUse, Instant.now());
        File recent = installation("node-v22.0.0");
        markUsed(recent, RECENTLY);
        File stale = installation("node-v20.0.0");
        markUsed(stale, LONG_AGO);

        File inUseArchive = archive("node-v24.19.0-linux-x64.tar.gz", LONG_AGO);
        File recentArchive = archive("node-v22.0.0-win-x64.zip", LONG_AGO);
        File staleArchive = archive("node-v20.0.0-darwin-arm64.tar.xz",
                LONG_AGO);

        NodeInstallations.removeUnusedInstallations(vaadinHome, inUse);

        assertFalse(inUseArchive.exists(),
                "An unpacked archive is dead weight even when its installation is kept");
        assertFalse(recentArchive.exists(),
                "An unpacked archive is dead weight even when its installation is kept");
        assertFalse(staleArchive.exists(),
                "The archive of a removed installation should be removed too");
    }

    @Test
    void removeUnusedInstallations_freshArchive_isKept() throws IOException {
        File inUse = installation("node-v24.19.0");
        installation("node-v22.0.0");
        File downloading = archive("node-v24.19.0-linux-x64.tar.gz",
                Instant.now());

        NodeInstallations.removeUnusedInstallations(vaadinHome, inUse);

        assertTrue(downloading.exists(),
                "An archive that another process may still be downloading should be kept");
    }

    @Test
    void removeUnusedInstallations_unrelatedFilesAndFolders_areNotTouched()
            throws IOException {
        File inUse = installation("node-v24.19.0");
        markUsed(inUse, Instant.now());
        File stale = installation("node-v20.0.0");
        markUsed(stale, LONG_AGO);

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
        File shaSums = new File(vaadinHome, "node-SHASUMS256.txt");
        assertTrue(shaSums.createNewFile());
        shaSums.setLastModified(LONG_AGO.toEpochMilli());
        File notAnArchive = new File(vaadinHome, "node-v20.0.0-notes.txt");
        assertTrue(notAnArchive.createNewFile());
        notAnArchive.setLastModified(LONG_AGO.toEpochMilli());

        NodeInstallations.removeUnusedInstallations(vaadinHome, inUse);

        assertFalse(stale.exists(), "The stale installation should be removed");
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
    void removeUnusedInstallations_emptyDirectory_doesNotFail() {
        assertDoesNotThrow(() -> NodeInstallations
                .removeUnusedInstallations(vaadinHome, null));
        assertEquals(0, vaadinHome.listFiles().length);
    }

    private File installation(String name) {
        File installation = new File(vaadinHome, name);
        assertTrue(new File(installation, "bin").mkdirs(),
                "Test setup should be able to create " + name);
        return installation;
    }

    private File archive(String name, Instant modified) throws IOException {
        File archive = new File(vaadinHome, name);
        assertTrue(archive.createNewFile(),
                "Test setup should be able to create " + name);
        assertTrue(archive.setLastModified(modified.toEpochMilli()),
                "Test setup should be able to set the age of " + name);
        return archive;
    }

    private static void markUsed(File installation, Instant when)
            throws IOException {
        Files.writeString(new File(installation, LAST_USED_FILE).toPath(),
                when.toString(), StandardCharsets.UTF_8);
    }

    private static Instant readLastUsed(File installation) throws IOException {
        return Instant.parse(Files.readString(
                new File(installation, LAST_USED_FILE).toPath(),
                StandardCharsets.UTF_8));
    }
}
