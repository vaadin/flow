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
 * Yarn configuration needed for its running. It specifies where node and yarn
 * are installed and where is the working directory.
 *
 * @since 1.2
 */
public class YarnConfigurationLocal implements YarnExecutorConfig {

    /**
     * Where Node is installed.
     */
    private File nodePath;

    /**
     * Where Yarn is installed.
     */
    private File yarnPath;

    /**
     * Working directory.
     */
    private File workingDirectory;

    /**
     * Creates the configuration for a {@link YarnExecutor}.
     *
     * @param nodePath
     *            file where node is located
     * @param yarnPath
     *            file where yarn is located
     * @param workingDirectory
     *            working directory
     */
    public YarnConfigurationLocal(File nodePath, File yarnPath,
            File workingDirectory) {
        this.nodePath = nodePath;
        this.yarnPath = yarnPath;
        this.workingDirectory = workingDirectory;
    }

    /**
     * Gets the file where node is installed.
     *
     * @return nodePath File in which node is installed
     */
    @Override
    public File getNodePath() {
        return nodePath;
    }

    /**
     * Gets the file where yarn is installed.
     *
     * @return nodePath File in which yarn is installed
     */
    @Override
    public File getYarnPath() {
        return yarnPath;
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
