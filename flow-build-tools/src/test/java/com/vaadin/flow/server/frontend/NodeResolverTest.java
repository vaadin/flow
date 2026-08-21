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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.FrontendVersion;
import com.vaadin.flow.internal.Platform;
import com.vaadin.flow.server.frontend.NodeResolver.ActiveNodeInstallation;
import com.vaadin.flow.testutil.FrontendStubs;

import static com.vaadin.flow.server.frontend.NodeInstallation.LAST_USED_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Covers how {@link NodeResolver} treats the installations in
 * {@code ~/.vaadin}, both the one it takes into use and the ones it cleans up.
 */
class NodeResolverTest {

    private static final String VERSION = "v24.19.0";

    private static final Instant LONG_AGO = Instant.now()
            .minus(Duration.ofDays(400));
    private static final Instant RECENTLY = Instant.now()
            .minus(Duration.ofDays(3));

    @TempDir
    File vaadinHome;

    @TempDir
    File downloadRoot;

    @BeforeEach
    void requireShellStubs() {
        assumeFalse(FrontendUtils.isWindows(),
                "The node stub is a shell script, so it cannot be executed on Windows");
    }

    @Test
    void resolve_existingInstallation_isUsedWithoutInstalling()
            throws IOException {
        NodeInstallation installation = stubInstallation(VERSION);

        ActiveNodeInstallation active = resolve(VERSION);

        assertEquals(installation.getNodeExecutable().getAbsolutePath(),
                active.nodeExecutable());
        assertEquals(installation.getNpmCliScript().getAbsolutePath(),
                active.npmCliScript());
        assertEquals("24.19.0", active.nodeVersion());
    }

    @Test
    void resolve_existingInstallation_lastUsedMarkerIsRefreshed()
            throws IOException {
        NodeInstallation installation = stubInstallation(VERSION);
        Files.writeString(
                new File(installation.getDirectory(), LAST_USED_FILE).toPath(),
                LONG_AGO.toString(), StandardCharsets.UTF_8);

        resolve(VERSION);

        assertTrue(
                installation.getLastUsed().orElseThrow()
                        .isAfter(Instant.now().minus(Duration.ofMinutes(5))),
                "Taking an existing installation into use should refresh its last-used marker");
    }

    @Test
    void resolve_newVersionInstalled_installationsUnusedForOverSixMonthsAreRemoved()
            throws IOException {
        NodeInstallation stale = pruningCandidate("node-v20.0.0", LONG_AGO);
        NodeInstallation recent = pruningCandidate("node-v22.0.0", RECENTLY);
        prepareDownloadableNode(VERSION);

        ActiveNodeInstallation active = resolve(VERSION);

        NodeInstallation installed = NodeInstallation.forVersion(vaadinHome,
                VERSION);
        assertEquals(installed.getNodeExecutable().getAbsolutePath(),
                active.nodeExecutable());
        assertTrue(installed.getLastUsed().isPresent(),
                "A freshly installed version should be marked as used");
        assertFalse(stale.getDirectory().exists(),
                "An installation unused for over 6 months should be removed once a new version is installed");
        assertTrue(recent.getDirectory().isDirectory(),
                "A recently used installation should be kept");
    }

    private ActiveNodeInstallation resolve(String nodeVersion) {
        return new NodeResolver(vaadinHome.getAbsolutePath(), nodeVersion,
                downloadRoot.toURI(), true, List.of(), null).resolve();
    }

    /**
     * Creates an installation that is only there to be considered for removal,
     * of a version too old to be picked as a fallback.
     */
    private NodeInstallation pruningCandidate(String name, Instant lastUsed)
            throws IOException {
        File directory = new File(vaadinHome, name);
        assertTrue(new File(directory, "bin").mkdirs());
        Files.writeString(new File(directory, LAST_USED_FILE).toPath(),
                lastUsed.toString(), StandardCharsets.UTF_8);
        return new NodeInstallation(directory);
    }

    /**
     * Writes an archive holding a minimal Node.js distribution into the
     * download root, in the layout the installer downloads from.
     */
    private void prepareDownloadableNode(String version) throws IOException {
        Platform platform = Platform.guess();
        String prefix = "node-" + version + "-"
                + platform.getNodeClassifier(new FrontendVersion(version));
        File versionDirectory = new File(downloadRoot, version);
        assertTrue(versionDirectory.mkdirs());
        File archive = new File(versionDirectory,
                prefix + "." + platform.getArchiveExtension());

        String nodeScript = FrontendStubs.ToolStubInfo
                .builder(FrontendStubs.Tool.NODE)
                .withVersion(NodeInstallation.normalizeVersion(version)).build()
                .getScript();

        try (OutputStream out = Files.newOutputStream(archive.toPath());
                OutputStream gzip = new GzipCompressorOutputStream(out);
                TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)) {
            writeEntry(tar, prefix + "/bin/node", nodeScript);
            writeEntry(tar, prefix + "/lib/node_modules/npm/bin/npm-cli.js",
                    "");
        }
    }

    private static void writeEntry(TarArchiveOutputStream tar, String name,
            String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(bytes.length);
        tar.putArchiveEntry(entry);
        tar.write(bytes);
        tar.closeArchiveEntry();
    }

    private NodeInstallation stubInstallation(String version)
            throws IOException {
        return new NodeInstallation(FrontendStubs.createStubVersionedNode(
                version, vaadinHome.getAbsolutePath()));
    }
}
