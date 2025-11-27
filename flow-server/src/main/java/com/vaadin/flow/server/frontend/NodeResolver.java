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
 * <li>Try to find and use node from global PATH if it meets version
 * requirements and has npm available</li>
 * <li>If no suitable global node found, use NodeInstaller to resolve or install
 * node in alternative directory (~/.vaadin)</li>
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
            ActiveNodeInstallation globalInstallation = tryUseGlobalNode();
            if (globalInstallation != null) {
                return globalInstallation;
            }
        }

        // Either forceAlternativeNode is true, or global node was unsuitable
        return resolveOrInstallAlternativeNode();
    }

    /**
     * Tries to use a globally installed node that meets version requirements.
     *
     * @return the active node installation, or null if global node not found or
     *         unsuitable
     */
    private ActiveNodeInstallation tryUseGlobalNode() {
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

            // Check that major version is within supported range
            if (installedNodeVersion
                    .getMajorVersion() > FrontendTools.MAX_SUPPORTED_NODE_MAJOR_VERSION) {
                getLogger().info(
                        "The globally installed Node.js version {}.x is newer than the maximum supported version {}.x and may not be compatible. Using Node.js from {}.",
                        installedNodeVersion.getMajorVersion(),
                        FrontendTools.MAX_SUPPORTED_NODE_MAJOR_VERSION,
                        alternativeDir);
                return null;
            }

            // Found suitable global node - now get npm information
            String npmCliScript = getGlobalNpmCliScript(nodeExecutable);
            if (npmCliScript == null) {
                getLogger().debug(
                        "npm-cli.js not found in global Node.js installation, will use alternative directory");
                return null;
            }

            String npmVersion;
            try {
                npmVersion = FrontendUtils
                        .getVersion("npm",
                                List.of(nodeExecutable.getAbsolutePath(),
                                        npmCliScript, "--version"))
                        .getFullVersion();
            } catch (UnknownVersionException e) {
                getLogger().debug(
                        "Could not determine npm version from global installation",
                        e);
                npmVersion = "unknown";
            }

            getLogger().info("Using globally installed Node.js version {}",
                    installedNodeVersion.getFullVersion());
            return new ActiveNodeInstallation(nodeExecutable.getAbsolutePath(),
                    installedNodeVersion.getFullVersion(), npmCliScript,
                    npmVersion);
        } catch (UnknownVersionException e) {
            getLogger().error("Failed to get version for installed node.", e);
            return null;
        }
    }

    /**
     * Tries to find npm-cli.js in a global Node.js installation.
     *
     * @param nodeExecutable
     *            the global node executable
     * @return path to npm-cli.js, or null if not found
     */
    private String getGlobalNpmCliScript(File nodeExecutable) {
        // Global npm is typically installed alongside node
        File nodeDir = nodeExecutable.getParentFile();
        boolean isWindows = FrontendUtils.isWindows();

        // Try common locations relative to node executable
        String[] possiblePaths = isWindows
                ? new String[] { "..\\node_modules\\npm\\bin\\npm-cli.js" }
                : new String[] { "../lib/node_modules/npm/bin/npm-cli.js" };

        for (String path : possiblePaths) {
            File npmCliScript = new File(nodeDir, path);
            if (npmCliScript.exists()) {
                return npmCliScript.getAbsolutePath();
            }
        }

        return null;
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
        File alternativeDirFile = new File(alternativeDir);
        NodeInstaller nodeInstaller = new NodeInstaller(alternativeDirFile,
                proxies);
        if (nodeDownloadRoot != null) {
            nodeInstaller.setNodeDownloadRoot(nodeDownloadRoot);
        }

        // First, check if the exact requested version is already installed
        String versionToUse = nodeVersion;
        File nodeExecutable = getNodeExecutableForVersion(alternativeDirFile,
                versionToUse);

        if (nodeExecutable.exists()) {
            try {
                String installedVersion = FrontendUtils
                        .getVersion("node", List.of(
                                nodeExecutable.getAbsolutePath(), "--version"))
                        .getFullVersion();

                // Normalize versions for comparison
                String normalizedInstalled = installedVersion.startsWith("v")
                        ? installedVersion.substring(1)
                        : installedVersion;
                String normalizedRequested = nodeVersion.startsWith("v")
                        ? nodeVersion.substring(1)
                        : nodeVersion;

                if (normalizedInstalled.equals(normalizedRequested)) {
                    getLogger().info("Node {} is already installed in {}",
                            nodeVersion, alternativeDir);
                    return createActiveInstallation(nodeExecutable,
                            versionToUse, alternativeDirFile);
                }
            } catch (UnknownVersionException e) {
                getLogger().debug(
                        "Could not verify version of existing node installation",
                        e);
            }
        }

        // Check if any other compatible version is available
        String fallbackVersion = findCompatibleInstalledVersion(
                alternativeDirFile);
        if (fallbackVersion != null) {
            getLogger().debug("Using existing Node {} instead of installing {}",
                    fallbackVersion, nodeVersion);
            versionToUse = fallbackVersion;
            nodeExecutable = getNodeExecutableForVersion(alternativeDirFile,
                    versionToUse);
            return createActiveInstallation(nodeExecutable, versionToUse,
                    alternativeDirFile);
        }

        // No suitable version found, install the requested version
        getLogger().info("Installing Node {} to {}", nodeVersion,
                alternativeDir);
        try {
            nodeInstaller.setNodeVersion(nodeVersion);
            nodeInstaller.install();
            nodeExecutable = getNodeExecutableForVersion(alternativeDirFile,
                    nodeVersion);
            return createActiveInstallation(nodeExecutable, nodeVersion,
                    alternativeDirFile);
        } catch (InstallationException e) {
            throw new IllegalStateException("Failed to install Node", e);
        }
    }

    private ActiveNodeInstallation createActiveInstallation(File nodeExecutable,
            String version, File installDir) {
        String nodePath = nodeExecutable.exists()
                ? nodeExecutable.getAbsolutePath()
                : null;
        if (nodePath == null) {
            throw new IllegalStateException(
                    "Node installation failed - executable not found at "
                            + nodeExecutable);
        }

        String npmCliScript = getNpmCliScriptPath(installDir, version);
        if (npmCliScript == null) {
            String versionedPath = "node-v"
                    + (version.startsWith("v") ? version.substring(1)
                            : version);
            boolean isWindows = FrontendUtils.isWindows();
            String expectedNpmPath = isWindows
                    ? versionedPath + "\\node_modules\\npm\\bin\\npm-cli.js"
                    : versionedPath + "/lib/node_modules/npm/bin/npm-cli.js";
            File expectedNpmFile = new File(installDir, expectedNpmPath);
            throw new IllegalStateException(
                    "npm-cli.js not found at expected location: "
                            + expectedNpmFile.getAbsolutePath());
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

        return new ActiveNodeInstallation(nodePath, version, npmCliScript,
                npmVersion);
    }

    /**
     * Scans the install directory for installed Node.js versions and returns
     * the newest one that is supported.
     *
     * @param installDir
     *            the installation directory to scan
     * @return the version string (e.g., "v24.10.0") of the best available
     *         version, or null if none found
     */
    private String findCompatibleInstalledVersion(File installDir) {
        if (!installDir.exists() || !installDir.isDirectory()) {
            return null;
        }

        File[] nodeDirs = installDir.listFiles(file -> file.isDirectory()
                && file.getName().startsWith("node-v"));

        if (nodeDirs == null || nodeDirs.length == 0) {
            return null;
        }

        FrontendVersion bestVersion = null;
        String bestVersionString = null;

        for (File nodeDir : nodeDirs) {
            String dirName = nodeDir.getName();
            // Extract version from directory name (node-v24.10.0 -> v24.10.0)
            String versionString = dirName.substring("node-".length());

            try {
                FrontendVersion version = new FrontendVersion(versionString);

                // Skip versions older than minimum supported
                if (version.isOlderThan(FrontendTools.SUPPORTED_NODE_VERSION)) {
                    getLogger().debug(
                            "Skipping {} - older than minimum supported {}",
                            versionString, FrontendTools.SUPPORTED_NODE_VERSION
                                    .getFullVersion());
                    continue;
                }

                // Skip versions with major version higher than maximum supported
                if (version
                        .getMajorVersion() > FrontendTools.MAX_SUPPORTED_NODE_MAJOR_VERSION) {
                    getLogger().debug(
                            "Skipping {} - major version {} is newer than maximum supported {}",
                            versionString, version.getMajorVersion(),
                            FrontendTools.MAX_SUPPORTED_NODE_MAJOR_VERSION);
                    continue;
                }

                // Verify the node executable actually exists
                File nodeExecutable = getNodeExecutableForVersion(installDir,
                        versionString);
                if (!nodeExecutable.exists()) {
                    getLogger().debug(
                            "Skipping {} - executable not found at {}",
                            versionString, nodeExecutable);
                    continue;
                }

                // Keep the newest version
                if (bestVersion == null || version.isNewerThan(bestVersion)) {
                    bestVersion = version;
                    bestVersionString = versionString;
                }
            } catch (NumberFormatException e) {
                getLogger().debug("Could not parse version from directory: {}",
                        dirName);
            }
        }

        return bestVersionString;
    }

    /**
     * Gets the node executable path for a specific version.
     *
     * @param installDir
     *            the installation directory
     * @param version
     *            the version string (e.g., "v24.10.0")
     * @return the File pointing to the node executable
     */
    private File getNodeExecutableForVersion(File installDir, String version) {
        String versionedPath = "node-v"
                + (version.startsWith("v") ? version.substring(1) : version);
        boolean isWindows = FrontendUtils.isWindows();
        String nodeExecutable = isWindows ? versionedPath + "\\node.exe"
                : versionedPath + "/bin/node";
        return new File(installDir, nodeExecutable);
    }

    /**
     * Gets the npm-cli.js script path for a specific version.
     *
     * @param installDir
     *            the installation directory
     * @param version
     *            the version string (e.g., "v24.10.0")
     * @return the absolute path to npm-cli.js, or null if not found
     */
    private String getNpmCliScriptPath(File installDir, String version) {
        String versionedPath = "node-v"
                + (version.startsWith("v") ? version.substring(1) : version);
        boolean isWindows = FrontendUtils.isWindows();
        String npmPath = isWindows
                ? versionedPath + "\\node_modules\\npm\\bin\\npm-cli.js"
                : versionedPath + "/lib/node_modules/npm/bin/npm-cli.js";
        File npmCliScript = new File(installDir, npmPath);
        return npmCliScript.exists() ? npmCliScript.getAbsolutePath() : null;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(NodeResolver.class);
    }
}
