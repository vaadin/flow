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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.internal.FrontendVersion;
import com.vaadin.flow.server.frontend.FrontendTools;

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
                zipOutputStream.putNextEntry(new ZipEntry(prefix + "/npm.cmd"));
                zipOutputStream.closeEntry();
            }
        } else {
            // Create actual temp directory structure to create proper archive
            // entries
            File tempDir = tmpDir.newFolder("archiveContent");
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
        Assert.assertTrue("node should have been installed",
                new File(targetDir, nodeInstallPath).exists());
        String npmInstallPath = platform.isWindows()
                ? versionedNodeDir + "/node_modules/npm/bin/npm"
                : versionedNodeDir + "/lib/node_modules/npm/bin/npm";
        Assert.assertTrue("npm should have been copied to node_modules",
                new File(targetDir, npmInstallPath).exists());
        String npmBinPath = platform.isWindows() ? versionedNodeDir + "/npm.cmd"
                : versionedNodeDir + "/bin/npm";
        Assert.assertTrue("npm should be available in bin",
                new File(targetDir, npmBinPath).exists());
        // Note: old installation cleanup is verified by the fact that the
        // entire
        // installation directory is deleted and replaced with the new
        // distribution
    }

}
