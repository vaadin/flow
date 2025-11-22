/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendVersion;

public class NodeInstallerTest {

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private String baseDir;

    @Before
    public void setup() {
        baseDir = tmpDir.getRoot().getAbsolutePath();
    }

    @Test
    public void installNodeFromFileSystem_NodeIsInstalledToTargetDirectory()
            throws IOException {
        Platform platform = Platform.guess();
        String nodeExec = platform.isWindows() ? "node.exe" : "node";
        String prefix = String.format("node-%s-%s",
                FrontendTools.DEFAULT_NODE_VERSION,
                platform.getNodeClassifier(new FrontendVersion(
                        FrontendTools.DEFAULT_NODE_VERSION)));

        File targetDir = new File(baseDir + "/installation");

        Assert.assertFalse(
                "Clean test should not contain a installation folder",
                targetDir.exists());
        File downloadDir = tmpDir.newFolder(FrontendTools.DEFAULT_NODE_VERSION);
        File archiveFile = new File(downloadDir,
                prefix + "." + platform.getArchiveExtension());
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
            }
        } else {
            try (OutputStream fo = Files.newOutputStream(tempArchive);
                    OutputStream gzo = new GzipCompressorOutputStream(fo);
                    ArchiveOutputStream o = new TarArchiveOutputStream(gzo)) {
                o.putArchiveEntry(o.createArchiveEntry(
                        new File(prefix + "/bin/" + nodeExec),
                        prefix + "/bin/" + nodeExec));
                o.closeArchiveEntry();
                o.putArchiveEntry(o.createArchiveEntry(
                        new File(prefix + "/bin/npm"), prefix + "/bin/npm"));
                o.closeArchiveEntry();
                o.putArchiveEntry(o.createArchiveEntry(
                        new File(prefix + "/lib/node_modules/npm/bin/npm"),
                        prefix + "/lib/node_modules/npm/bin/npm"));
                o.closeArchiveEntry();
                o.putArchiveEntry(o.createArchiveEntry(
                        new File(prefix + "/lib/node_modules/npm/bin/npm.cmd"),
                        prefix + "/lib/node_modules/npm/bin/npm.cmd"));
                o.closeArchiveEntry();
            }
        }

        // add a file to node-{version}/node_modules_npm that should be cleaned
        // out
        String versionedNodeDir = "node-" + FrontendTools.DEFAULT_NODE_VERSION;
        File nodeDirectory = new File(targetDir, versionedNodeDir);
        String nodeModulesPath = platform.isWindows() ? "node_modules"
                : "lib/node_modules";
        File nodeModulesDirectory = new File(nodeDirectory, nodeModulesPath);
        File npmDirectory = new File(nodeModulesDirectory, "npm");
        File garbage = new File(npmDirectory, "garbage");
        FileUtils.forceMkdir(npmDirectory);
        Assert.assertTrue("garbage file should be created",
                garbage.createNewFile());

        File oldNpm = new File(nodeDirectory, "node_modules/npm");
        File oldGarbage = new File(oldNpm, "oldGarbage");
        FileUtils.forceMkdir(oldNpm);
        Assert.assertTrue("oldGarbage file should be created",
                oldGarbage.createNewFile());

        NodeInstaller nodeInstaller = new NodeInstaller(targetDir,
                Collections.emptyList())
                .setNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                .setNodeDownloadRoot(new File(baseDir).toPath().toUri());

        try {
            nodeInstaller.install();
        } catch (InstallationException e) {
            throw new IllegalStateException("Failed to install Node", e);
        }

        Assert.assertTrue("node should have been installed",
                new File(targetDir, versionedNodeDir + "/" + nodeExec)
                        .exists());
        String npmInstallPath = platform.isWindows()
                ? versionedNodeDir + "/node_modules/npm/bin/npm"
                : versionedNodeDir + "/lib/node_modules/npm/bin/npm";
        Assert.assertTrue("npm should have been copied to node_modules",
                new File(targetDir, npmInstallPath).exists());
        Assert.assertFalse("old npm files should have been removed",
                garbage.exists());
        Assert.assertFalse(
                "old style node_modules files should have been removed",
                oldGarbage.exists());
    }

    @Test
    public void fallbackToExistingCompatibleVersion() throws IOException {
        Platform platform = Platform.guess();
        String nodeExec = platform.isWindows() ? "node.exe" : "node";

        File targetDir = new File(baseDir + "/installation");

        // Pre-install an older but compatible version (v24.8.0)
        String existingVersion = "v24.8.0";
        String existingVersionDir = "node-" + existingVersion;
        File existingNodeDir = new File(targetDir, existingVersionDir);
        File existingBinDir = platform.isWindows() ? existingNodeDir
                : new File(existingNodeDir, "bin");
        FileUtils.forceMkdir(existingBinDir);
        File existingNodeExec = new File(existingBinDir, nodeExec);
        Assert.assertTrue("Existing node executable should be created",
                existingNodeExec.createNewFile());

        // Request a newer version that doesn't exist
        String requestedVersion = "v24.99.0";

        NodeInstaller nodeInstaller = new NodeInstaller(targetDir,
                Collections.emptyList()).setNodeVersion(requestedVersion)
                .setNodeDownloadRoot(new File(baseDir).toPath().toUri());

        // The install should succeed by falling back to v24.8.0
        try {
            nodeInstaller.install();
        } catch (InstallationException e) {
            // Expected - download will fail, but fallback should work
            Assert.fail(
                    "Should have used fallback version instead of trying to download: "
                            + e.getMessage());
        }

        // Verify that the install directory points to the fallback version
        Assert.assertEquals(
                "Install directory should point to fallback version",
                new File(targetDir, existingVersionDir).getPath(),
                nodeInstaller.getInstallDirectory());
    }

    @Test
    public void fallbackSelectsNewestCompatibleVersion() throws IOException {
        Platform platform = Platform.guess();
        String nodeExec = platform.isWindows() ? "node.exe" : "node";

        File targetDir = new File(baseDir + "/installation");

        // Pre-install multiple versions
        for (String version : new String[] { "v24.5.0", "v24.8.0", "v24.6.0",
                "v23.0.0" }) {
            String versionDir = "node-" + version;
            File nodeDir = new File(targetDir, versionDir);
            File binDir = platform.isWindows() ? nodeDir
                    : new File(nodeDir, "bin");
            FileUtils.forceMkdir(binDir);
            File nodeExecFile = new File(binDir, nodeExec);
            nodeExecFile.createNewFile();
        }

        // Request a version that doesn't exist
        String requestedVersion = "v24.99.0";

        NodeInstaller nodeInstaller = new NodeInstaller(targetDir,
                Collections.emptyList()).setNodeVersion(requestedVersion)
                .setNodeDownloadRoot(new File(baseDir).toPath().toUri());

        try {
            nodeInstaller.install();
        } catch (InstallationException e) {
            Assert.fail(
                    "Should have used fallback version instead of trying to download: "
                            + e.getMessage());
        }

        // Verify that the newest compatible version (v24.8.0) was selected
        // Note: v23.0.0 should be skipped as it's below minimum supported
        // version
        Assert.assertEquals("Should select newest compatible version (v24.8.0)",
                new File(targetDir, "node-v24.8.0").getPath(),
                nodeInstaller.getInstallDirectory());
    }
}
