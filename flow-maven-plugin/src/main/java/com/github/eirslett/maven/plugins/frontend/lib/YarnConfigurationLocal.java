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
