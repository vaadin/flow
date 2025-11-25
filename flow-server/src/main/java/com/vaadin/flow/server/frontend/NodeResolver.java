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
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.FrontendUtils.UnknownVersionException;
import com.vaadin.flow.server.frontend.installer.InstallationException;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.installer.ProxyConfig;

/**
 * Handles the one-time resolution of which Node.js installation to use.
 * Performs the following steps in order:
 * <ol>
 * <li>If forceAlternativeNode is true, skip to step 3</li>
 * <li>Try to find node in global PATH and verify it meets version
 * requirements</li>
 * <li>If no suitable global node found, use NodeInstaller to resolve or install
 * node in alternative directory</li>
 * </ol>
 * <p>
 * Once resolved, the result is cached in an {@link ActiveNodeInstallation}
 * record.
 * <p>
 * This class is not serializable as it is only used for resolution, not
 * storage. The result ({@link ActiveNodeInstallation}) is serializable and can
 * be cached.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
class NodeResolver implements java.io.Serializable {

    /**
     * Information about the active node/npm installation that will be used. All
     * fields are required and non-null.
     *
     * @param nodeExecutable
     *            path to node binary
     * @param nodeVersion
     *            node version string (e.g., "24.10.0")
     * @param npmCliScript
     *            path to npm-cli.js script
     * @param npmVersion
     *            npm version string (e.g., "11.3.0")
     */
    record ActiveNodeInstallation(String nodeExecutable, String nodeVersion,
            String npmCliScript, String npmVersion) implements Serializable {
        ActiveNodeInstallation {
            Objects.requireNonNull(nodeExecutable);
            Objects.requireNonNull(nodeVersion);
            Objects.requireNonNull(npmCliScript);
            Objects.requireNonNull(npmVersion);
        }
    }

    private final FrontendToolsLocator frontendToolsLocator = new FrontendToolsLocator();

    private final String alternativeDir;
    private final String nodeVersion;
    private final URI nodeDownloadRoot;
    private final boolean forceAlternativeNode;
    private final List<ProxyConfig.Proxy> proxies;

    /**
     * Creates a resolver with the given configuration.
     *
     * @param alternativeDir
     *            directory where node should be installed if not found globally
     * @param nodeVersion
     *            the node version to use/install (e.g., "v24.10.0")
     * @param nodeDownloadRoot
     *            URI to download node from
     * @param forceAlternativeNode
     *            if true, skip global node lookup and go straight to
     *            alternative directory
     * @param proxies
     *            list of proxy configurations
     */
    NodeResolver(String alternativeDir, String nodeVersion,
            URI nodeDownloadRoot, boolean forceAlternativeNode,
            List<ProxyConfig.Proxy> proxies) {
        this.alternativeDir = Objects.requireNonNull(alternativeDir);
        this.nodeVersion = Objects.requireNonNull(nodeVersion);
        this.nodeDownloadRoot = Objects.requireNonNull(nodeDownloadRoot);
        this.forceAlternativeNode = forceAlternativeNode;
        this.proxies = Objects.requireNonNull(proxies);
    }

    /**
     * Resolves which node installation to use. This method should be called
     * once and the result cached.
     *
     * @return the active node installation information
     * @throws IllegalStateException
     *             if node cannot be found or installed
     */
    ActiveNodeInstallation resolve() {
        // If forceAlternativeNode is set, skip global lookup
        if (!forceAlternativeNode) {
            File globalNode = tryFindSuitableGlobalNode();
            if (globalNode != null) {
                // We found a suitable global node, but we can't cache it
                // because we don't have reliable npm info without installing
                // to alternative dir
                getLogger().debug(
                        "Found suitable global node at {}, but will use alternative dir for consistent npm access",
                        globalNode);
            }
        }

        // Either forceAlternativeNode is true, or global node was unsuitable
        return resolveOrInstallAlternativeNode();
    }

    /**
     * Tries to find a globally installed node that meets version requirements.
     *
     * @return the node executable file, or null if not found or unsuitable
     */
    private File tryFindSuitableGlobalNode() {
        String nodeCommand = FrontendUtils.isWindows() ? "node.exe" : "node";
        File nodeExecutable = frontendToolsLocator.tryLocateTool(nodeCommand)
                .orElse(null);

        if (nodeExecutable == null) {
            return null;
        }

        // Check if version is acceptable
        try {
            List<String> versionCommand = new ArrayList<>();
            versionCommand.add(nodeExecutable.getAbsolutePath());
            versionCommand.add("--version");
            FrontendVersion installedNodeVersion = FrontendUtils
                    .getVersion("node", versionCommand);

            if (installedNodeVersion
                    .isOlderThan(FrontendTools.SUPPORTED_NODE_VERSION)) {
                getLogger().info(
                        "The globally installed Node.js version {} is older than the required minimum version {}. Using Node.js from {}.",
                        installedNodeVersion.getFullVersion(),
                        FrontendTools.SUPPORTED_NODE_VERSION.getFullVersion(),
                        alternativeDir);
                return null;
            }

            return nodeExecutable;
        } catch (UnknownVersionException e) {
            getLogger().error("Failed to get version for installed node.", e);
            return null;
        }
    }

    /**
     * Resolves an existing compatible node installation in the alternative
     * directory, or installs a new one.
     *
     * @return the active node installation information
     * @throws IllegalStateException
     *             if installation fails
     */
    private ActiveNodeInstallation resolveOrInstallAlternativeNode() {
        NodeInstaller nodeInstaller = new NodeInstaller(
                new File(alternativeDir), proxies).setNodeVersion(nodeVersion);
        if (nodeDownloadRoot != null) {
            nodeInstaller.setNodeDownloadRoot(nodeDownloadRoot);
        }

        try {
            nodeInstaller.install();
            String nodePath = nodeInstaller.getNodeExecutablePath();
            if (nodePath == null) {
                throw new IllegalStateException(
                        "Node installation failed - executable not found");
            }
            String resolvedNodeVersion = nodeInstaller.getActiveNodeVersion();
            String npmCliScript = nodeInstaller.getNpmCliScriptPath();
            if (npmCliScript == null) {
                throw new IllegalStateException(
                        "npm not found in node installation");
            }
            String npmVersion;
            try {
                npmVersion = FrontendUtils
                        .getVersion("npm",
                                List.of(nodePath, npmCliScript, "--version"))
                        .getFullVersion();
            } catch (UnknownVersionException e) {
                getLogger().debug("Could not determine npm version", e);
                npmVersion = "unknown";
            }
            return new ActiveNodeInstallation(nodePath, resolvedNodeVersion,
                    npmCliScript, npmVersion);
        } catch (InstallationException e) {
            throw new IllegalStateException("Failed to install Node", e);
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(NodeResolver.class);
    }
}
