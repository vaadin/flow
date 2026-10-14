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
package com.vaadin.flow.server.frontend.installer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.internal.FrontendVersion;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.internal.Platform;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.frontendtools.installer.DefaultArchiveExtractor;

import static com.vaadin.flow.internal.Platform.ALPINE_RELEASE_FILE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeInstallerTest {

    @TempDir
    File tmpDir;

    private String baseDir;

    @BeforeEach
    void setup() {
        baseDir = tmpDir.getAbsolutePath();
    }

    @Test
    void installNodeFromFileSystem_NodeIsInstalledToTargetDirectory()
            throws IOException {
        Platform platform = Platform.guess();
        String nodeExec = platform.isWindows() ? "node.exe" : "node";
        String prefix = nodeArchivePrefix(platform);

        File targetDir = new File(baseDir + "/installation");

        assertFalse(targetDir.exists(),
                "Clean test should not contain a installation folder");
        createDownloadableNodeArchive(platform, prefix);

        // Note: Previous test versions created garbage files to verify cleanup.
        // With the new approach of replacing the entire installation directory,
        // this is implicitly tested.

        NodeInstaller nodeInstaller = new NodeInstaller(targetDir,
                Collections.emptyList())
                .setNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                .setNodeDownloadRoot(new File(baseDir).toPath().toUri());

        try {
            nodeInstaller.install();
        } catch (InstallationException e) {
            throw new IllegalStateException("Failed to install Node", e);
        }

        String versionedNodeDir = "node-" + FrontendTools.DEFAULT_NODE_VERSION;
        String nodeInstallPath = platform.isWindows()
                ? versionedNodeDir + "/" + nodeExec
                : versionedNodeDir + "/bin/" + nodeExec;
        assertTrue(new File(targetDir, nodeInstallPath).exists(),
                "node should have been installed");
        String npmInstallPath = platform.isWindows()
                ? versionedNodeDir + "/node_modules/npm/bin/npm"
                : versionedNodeDir + "/lib/node_modules/npm/bin/npm";
        assertTrue(new File(targetDir, npmInstallPath).exists(),
                "npm should have been copied to node_modules");
        String npmBinPath = platform.isWindows() ? versionedNodeDir + "/npm.cmd"
                : versionedNodeDir + "/bin/npm";
        assertTrue(new File(targetDir, npmBinPath).exists(),
                "npm should be available in bin");
        // Note: old installation cleanup is verified by the fact that the
        // entire
        // installation directory is deleted and replaced with the new
        // distribution
    }

    @Test
    void installNodeFromFileSystem_unpackedArchiveIsRemoved()
            throws IOException {
        Platform platform = Platform.guess();
        String prefix = nodeArchivePrefix(platform);
        String archiveName = prefix + "." + platform.getArchiveExtension();

        File targetDir = new File(baseDir + "/installation");
        File sourceArchive = createDownloadableNodeArchive(platform, prefix);

        NodeInstaller nodeInstaller = new NodeInstaller(targetDir,
                Collections.emptyList())
                .setNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                .setNodeDownloadRoot(new File(baseDir).toPath().toUri());

        try {
            nodeInstaller.install();
        } catch (InstallationException e) {
            throw new IllegalStateException("Failed to install Node", e);
        }

        assertFalse(new File(targetDir, archiveName).exists(),
                "The downloaded archive should have been removed after it was unpacked");
        assertTrue(sourceArchive.exists(),
                "Only the downloaded copy should be removed, not the download source");
    }

    /**
     * Installing twice in a row has to work even though the first install
     * removed the downloaded archive.
     */
    @Test
    void installNodeTwice_archiveIsDownloadedAgainAndRemovedAgain()
            throws IOException {
        Platform platform = Platform.guess();
        String prefix = nodeArchivePrefix(platform);
        String archiveName = prefix + "." + platform.getArchiveExtension();

        File targetDir = new File(baseDir + "/installation");
        createDownloadableNodeArchive(platform, prefix);

        NodeInstaller nodeInstaller = new NodeInstaller(targetDir,
                Collections.emptyList())
                .setNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                .setNodeDownloadRoot(new File(baseDir).toPath().toUri());

        try {
            nodeInstaller.install();
            nodeInstaller.install();
        } catch (InstallationException e) {
            throw new IllegalStateException("Failed to install Node", e);
        }

        assertFalse(new File(targetDir, archiveName).exists(),
                "The downloaded archive should have been removed after it was unpacked");
    }

    @Test
    void defaultNodeChecksums_coverDefaultVersionOnSupportedPlatforms() {
        String version = FrontendTools.DEFAULT_NODE_VERSION;
        for (String archive : List.of("darwin-arm64.tar.gz",
                "darwin-x64.tar.gz", "linux-arm64.tar.gz", "linux-x64.tar.gz",
                "linux-arm64-musl.tar.gz", "linux-x64-musl.tar.gz",
                "win-arm64.zip", "win-x64.zip")) {
            String name = "node-" + version + "-" + archive;
            assertTrue(
                    NodeInstaller.DefaultNodeChecksums.CHECKSUMS
                            .containsKey(name),
                    "No checksum for " + name
                            + ". Run scripts/updateNodeChecksums.sh after changing DEFAULT_NODE_VERSION");
        }
    }

    @Test
    void installDefaultVersionFromMirror_archiveNotMatchingShippedChecksum_installFails()
            throws IOException {
        Platform platform = Platform.guess();
        String prefix = nodeArchivePrefix(platform);
        File archive = createDownloadableNodeArchive(platform, prefix);
        // A mirror serving a different archive can also serve a checksum
        // file that matches it
        writeShaSums(FrontendTools.DEFAULT_NODE_VERSION, archive);
        File targetDir = new File(baseDir, "installation");
        List<URI> downloads = new ArrayList<>();

        NodeInstaller nodeInstaller = mirrorInstaller(platform, targetDir,
                downloads).setNodeVersion(FrontendTools.DEFAULT_NODE_VERSION);

        InstallationException exception = assertThrows(
                InstallationException.class, nodeInstaller::install);
        assertTrue(
                exception.getMessage()
                        .contains(NodeInstaller.USE_DOWNLOAD_ROOT_SHA),
                "The failure should point to the opt-out property: "
                        + exception.getMessage());
        assertFalse(new File(targetDir, archive.getName()).exists(),
                "The rejected archive should have been removed");
        assertEquals(
                List.of(URI.create("https://mirror.example/dist/"
                        + FrontendTools.DEFAULT_NODE_VERSION + "/"
                        + archive.getName())),
                downloads,
                "The archive should be downloaded once without retries and the checksums of the default version should not be downloaded");
    }

    @Test
    void installDefaultVersionFromMirror_useDownloadRootSha_archiveMatchesDownloadedChecksum_nodeIsInstalled()
            throws IOException, InstallationException {
        Platform platform = Platform.guess();
        File archive = createDownloadableNodeArchive(platform,
                nodeArchivePrefix(platform));
        writeShaSums(FrontendTools.DEFAULT_NODE_VERSION, archive);
        File targetDir = new File(baseDir, "installation");

        System.setProperty(NodeInstaller.USE_DOWNLOAD_ROOT_SHA, "true");
        try {
            mirrorInstaller(platform, targetDir, new ArrayList<>())
                    .setNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                    .install();
        } finally {
            System.clearProperty(NodeInstaller.USE_DOWNLOAD_ROOT_SHA);
        }

        String nodeExec = platform.isWindows() ? "node.exe" : "bin/node";
        assertTrue(
                new File(targetDir,
                        "node-" + FrontendTools.DEFAULT_NODE_VERSION + "/"
                                + nodeExec)
                        .exists(),
                "node should have been installed");
    }

    @Test
    void installOtherVersionFromMirror_archiveMatchesDownloadedChecksum_nodeIsInstalled()
            throws IOException, InstallationException {
        String version = "v26.0.0";
        Platform platform = Platform.guess();
        File archive = createDownloadableNodeArchive(platform,
                nodeArchivePrefix(platform, version), version);
        writeShaSums(version, archive);
        File targetDir = new File(baseDir, "installation");

        mirrorInstaller(platform, targetDir, new ArrayList<>())
                .setNodeVersion(version).install();

        String nodeExec = platform.isWindows() ? "node.exe" : "bin/node";
        assertTrue(new File(targetDir, "node-" + version + "/" + nodeExec)
                .exists(), "node should have been installed");
    }

    /**
     * Writes a {@value NodeInstaller#SHA_SUMS_FILE} next to the archive. The
     * entry for a longer file name ending with the archive name comes first, so
     * only an exact file name match finds the right checksum.
     */
    private void writeShaSums(String version, File archive) throws IOException {
        String sha = MessageDigestUtil
                .sha256Hex(Files.readAllBytes(archive.toPath()));
        Files.write(
                new File(tmpDir, version + "/" + NodeInstaller.SHA_SUMS_FILE)
                        .toPath(),
                List.of("0".repeat(64) + "  other-" + archive.getName(),
                        sha + "  " + archive.getName()));
    }

    /**
     * Creates an installer downloading from a remote mirror that serves the
     * files written under the temporary directory.
     */
    private NodeInstaller mirrorInstaller(Platform platform, File targetDir,
            List<URI> downloads) {
        URI mirror = URI.create("https://mirror.example/dist/");
        FileDownloader downloader = (uri, destination, userName, password,
                progressListener) -> {
            downloads.add(uri);
            Path source = tmpDir.toPath()
                    .resolve(mirror.relativize(uri).getPath());
            try {
                Files.copy(source, destination.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new DownloadException("Could not download " + uri, e);
            }
        };
        return new NodeInstaller(targetDir, platform,
                new DefaultArchiveExtractor(), downloader)
                .setNodeDownloadRoot(mirror);
    }

    private static String nodeArchivePrefix(Platform platform) {
        return nodeArchivePrefix(platform, FrontendTools.DEFAULT_NODE_VERSION);
    }

    private static String nodeArchivePrefix(Platform platform, String version) {
        return String.format("node-%s-%s", version,
                platform.getNodeClassifier(new FrontendVersion(version)));
    }

    /**
     * Creates a minimal Node.js distribution archive in the layout the
     * installer expects to find under a download root.
     *
     * @return the created archive
     */
    private File createDownloadableNodeArchive(Platform platform, String prefix)
            throws IOException {
        return createDownloadableNodeArchive(platform, prefix,
                FrontendTools.DEFAULT_NODE_VERSION);
    }

    private File createDownloadableNodeArchive(Platform platform, String prefix,
            String version) throws IOException {
        String nodeExec = platform.isWindows() ? "node.exe" : "node";
        File downloadDir = new File(tmpDir, version);
        downloadDir.mkdirs();
        File archiveFile = new File(downloadDir,
                prefix + "." + platform.getArchiveExtension());
        if (archiveFile.exists()) {
            return archiveFile;
        }
        archiveFile.createNewFile();
        Path tempArchive = archiveFile.toPath();

        if (platform.getArchiveExtension().equals("zip")) {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                    Files.newOutputStream(tempArchive))) {
                zipOutputStream
                        .putNextEntry(new ZipEntry(prefix + "/" + nodeExec));
                zipOutputStream.closeEntry();
                zipOutputStream.putNextEntry(
                        new ZipEntry(prefix + "/node_modules/npm/bin/npm"));
                zipOutputStream.closeEntry();
                zipOutputStream.putNextEntry(
                        new ZipEntry(prefix + "/node_modules/npm/bin/npm.cmd"));
                zipOutputStream.closeEntry();
                zipOutputStream.putNextEntry(new ZipEntry(prefix + "/npm.cmd"));
                zipOutputStream.closeEntry();
            }
        } else {
            // Create actual temp directory structure to create proper archive
            // entries
            File tempDir = new File(tmpDir, "archiveContent");
            tempDir.mkdirs();
            File nodeDir = new File(tempDir, prefix);
            File binDir = new File(nodeDir, "bin");
            File libDir = new File(nodeDir, "lib");
            File libNodeModulesDir = new File(libDir, "node_modules");
            File npmDir = new File(libNodeModulesDir, "npm");
            File npmBinDir = new File(npmDir, "bin");

            binDir.mkdirs();
            npmBinDir.mkdirs();

            // Create empty files
            new File(binDir, nodeExec).createNewFile();
            new File(binDir, "npm").createNewFile();
            new File(binDir, "npx").createNewFile();
            new File(npmBinDir, "npm").createNewFile();
            new File(npmBinDir, "npm.cmd").createNewFile();

            try (OutputStream fo = Files.newOutputStream(tempArchive);
                    OutputStream gzo = new GzipCompressorOutputStream(fo);
                    TarArchiveOutputStream o = new TarArchiveOutputStream(
                            gzo)) {
                // Add root directory entry (matches real Node.js archives)
                TarArchiveEntry dirEntry = new TarArchiveEntry(prefix + "/");
                o.putArchiveEntry(dirEntry);
                o.closeArchiveEntry();

                // Create file entries - directories will be created
                // automatically by the extractor
                o.putArchiveEntry(
                        o.createArchiveEntry(new File(binDir, nodeExec),
                                prefix + "/bin/" + nodeExec));
                o.closeArchiveEntry();
                o.putArchiveEntry(o.createArchiveEntry(new File(binDir, "npm"),
                        prefix + "/bin/npm"));
                o.closeArchiveEntry();
                o.putArchiveEntry(o.createArchiveEntry(new File(binDir, "npx"),
                        prefix + "/bin/npx"));
                o.closeArchiveEntry();
                o.putArchiveEntry(
                        o.createArchiveEntry(new File(npmBinDir, "npm"),
                                prefix + "/lib/node_modules/npm/bin/npm"));
                o.closeArchiveEntry();
                o.putArchiveEntry(
                        o.createArchiveEntry(new File(npmBinDir, "npm.cmd"),
                                prefix + "/lib/node_modules/npm/bin/npm.cmd"));
                o.closeArchiveEntry();
            }
        }

        return archiveFile;
    }

    @Test
    void testGuess_whenOsIsLinuxAndAlpineReleaseFileExists_unofficialNodeDownloadPathReturned() {
        try (MockedStatic<Platform.OS> os = Mockito
                .mockStatic(Platform.OS.class);
                MockedStatic<Paths> paths = Mockito.mockStatic(Paths.class);
                MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {

            os.when(Platform.OS::guess).thenReturn(Platform.OS.LINUX);
            Path alpineReleaseFilePath = Paths.get(ALPINE_RELEASE_FILE_PATH);
            paths.when(() -> Paths.get(ALPINE_RELEASE_FILE_PATH))
                    .thenReturn(alpineReleaseFilePath);
            files.when(() -> Files.exists(alpineReleaseFilePath))
                    .thenReturn(true);

            Platform platform = Platform.guess();
            assertEquals(NodeInstaller.UNOFFICIAL_NODEJS_DOWNLOAD_ROOT,
                    NodeInstaller.getDownloadRoot(platform));

            FrontendVersion frontendVersion = Mockito
                    .mock(FrontendVersion.class);
            assertTrue(platform.getNodeClassifier(frontendVersion)
                    .contains("-musl"));
        }
    }

    @Test
    void testGuess_whenOsIsLinuxAndAlpineReleaseFileDoesNotExist_officialNodeDownloadPathReturned() {
        try (MockedStatic<Platform.OS> os = Mockito
                .mockStatic(Platform.OS.class)) {

            os.when(Platform.OS::guess).thenReturn(Platform.OS.LINUX);

            Platform platform = Platform.guess();
            assertEquals(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT,
                    NodeInstaller.getDownloadRoot(platform));

            FrontendVersion frontendVersion = Mockito
                    .mock(FrontendVersion.class);
            assertFalse(platform.getNodeClassifier(frontendVersion)
                    .contains("-musl"));
        }
    }

    @Test
    void testGuess_whenOsIsAnythingOtherThanLinuxAlpineRelease_officialNodeDownloadPathReturned() {
        try (MockedStatic<Platform.OS> os = Mockito
                .mockStatic(Platform.OS.class)) {

            os.when(Platform.OS::guess).thenReturn(Platform.OS.WINDOWS);

            Platform platform = Platform.guess();
            assertEquals(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT,
                    NodeInstaller.getDownloadRoot(platform));

            os.when(Platform.OS::guess).thenReturn(Platform.OS.MAC);

            platform = Platform.guess();
            assertEquals(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT,
                    NodeInstaller.getDownloadRoot(platform));

            os.when(Platform.OS::guess).thenReturn(Platform.OS.SUN_OS);

            platform = Platform.guess();
            assertEquals(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT,
                    NodeInstaller.getDownloadRoot(platform));
        }
    }
}
