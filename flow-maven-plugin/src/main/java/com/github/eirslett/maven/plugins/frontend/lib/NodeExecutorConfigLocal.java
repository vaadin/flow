/*
 * Copyright 2000-2018 Vaadin Ltd.
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
