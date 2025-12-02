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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.net.URI;

import com.vaadin.flow.server.frontend.installer.InstallationException;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.installer.ProxyConfig;

/**
 * Test helper utilities for FrontendTools tests.
 */
public class FrontendToolsTestHelper {

    /**
     * Install node and npm to a specific directory without using NodeResolver.
     * This is a test utility that unconditionally installs the specified
     * version.
     *
     * @param installDir
     *            directory where node should be installed
     * @param proxies
     *            list of proxy configurations
     * @param nodeVersion
     *            node version to install
     * @param downloadRoot
     *            optional download root for downloading node. May be a
     *            filesystem file or a URL see
     *            {@link NodeInstaller#setNodeDownloadRoot(URI)}.
     * @return node installation path, or null if installation failed
     */
    public static String installNode(File installDir,
            java.util.List<ProxyConfig.Proxy> proxies, String nodeVersion,
            URI downloadRoot) {
        NodeInstaller nodeInstaller = new NodeInstaller(installDir, proxies)
                .setNodeVersion(nodeVersion);
        if (downloadRoot != null) {
            nodeInstaller.setNodeDownloadRoot(downloadRoot);
        }

        try {
            nodeInstaller.install();
        } catch (InstallationException e) {
            throw new IllegalStateException("Failed to install Node", e);
        }

        // Compute the path to the installed node executable
        String normalizedVersion = nodeVersion.startsWith("v")
                ? nodeVersion.substring(1)
                : nodeVersion;
        String versionedPath = "node-v" + normalizedVersion;
        String nodeBin = FrontendUtils.isWindows()
                ? versionedPath + "\\node.exe"
                : versionedPath + "/bin/node";
        File nodeExecutable = new File(installDir, nodeBin);
        return nodeExecutable.exists() ? nodeExecutable.getAbsolutePath()
                : null;
    }
}
