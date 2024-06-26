/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

/**
 * Contains the configuration for running gulp {@link DefaultGulpRunnerLocal}.
 * It specifies where node and npm are installed and the installation and
 * working directory.
 *
 * @since 1.2
 */
public class NodeExecutorConfigLocal implements NodeExecutorConfig {

    /**
     * Where Node is installed.
     */
    private File nodePath;

    /**
     * Where npm is installed.
     */
    private File npmPath;

    /**
     * Installation directory.
     */
    private File installDirectory;

    /**
     * Working directory.
     */
    private File workingDirectory;

    /**
     * Creates the configuration for a {@link NodeExecutor}.
     *
     * @param nodePath
     *            file where node is located
     * @param npmPath
     *            file where npm is located
     * @param installDirectory
     *            installation directory
     * @param workingDirectory
     *            working directory
     */
    public NodeExecutorConfigLocal(File nodePath, File npmPath,
            File installDirectory, File workingDirectory) {
        this.nodePath = nodePath;
        this.npmPath = npmPath;
        this.installDirectory = installDirectory;
        this.workingDirectory = workingDirectory;
    }

    /**
     * Gets the file where node is installed.
     *
     * @return nodePath File in which node is installed.
     */
    @Override
    public File getNodePath() {
        return nodePath;
    }

    /**
     * Gets the filed in which npm is installed.
     *
     * @return File in which npm is installed.
     */
    @Override
    public File getNpmPath() {
        return npmPath;
    }

    /**
     * Gets the installation directory.
     *
     * @return installDirectory Installation directory
     */
    @Override
    public File getInstallDirectory() {
        return installDirectory;
    }

    /**
     * Gets the working directory.
     *
     * @return workingDirectory the working directory
     */
    @Override
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Get the platform in which the plugin is executed.
     *
     * @return Platform platform
     */
    @Override
    public Platform getPlatform() {
        return Platform.guess();
    }
}
