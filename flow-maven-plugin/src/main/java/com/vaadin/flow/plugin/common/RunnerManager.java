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

package com.vaadin.flow.plugin.common;

import java.io.File;

import com.github.eirslett.maven.plugins.frontend.lib.DefaultGulpRunnerLocal;
import com.github.eirslett.maven.plugins.frontend.lib.DefaultYarnRunnerLocal;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.GulpRunner;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.NodeExecutorConfigLocal;
import com.github.eirslett.maven.plugins.frontend.lib.NodeInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.YarnConfigurationLocal;
import com.github.eirslett.maven.plugins.frontend.lib.YarnInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.YarnRunner;

/**
 * Creates and configures the runners used by the FrontendToolsManager,
 * providing an extra layer of abstraction.
 *
 * RunnerManager can be used to grab the local node and yarn from the local
 * environment or to download and install them.
 */
public class RunnerManager {

    /**
     * It is used in the installation of the dependencies.
     */
    private final YarnRunner yarnRunner;

    /**
     * It is used in the transpilation process.
     */
    private final GulpRunner gulpRunner;

    /**
     * Creates the yarn and gulp runners downloading it dependencies(node and
     * yarn).
     * 
     * @param workingDirectory
     *            working directory
     * @param proxyConfig
     *            proxy configuration
     * @param nodeVersion
     *            node version
     * @param yarnVersion
     *            yarn version
     */
    public RunnerManager(File workingDirectory, ProxyConfig proxyConfig,
            String nodeVersion, String yarnVersion) {
        FrontendPluginFactory factory = new FrontendPluginFactory(
                workingDirectory, workingDirectory);
        try {
            factory.getNodeInstaller(proxyConfig).setNodeVersion(nodeVersion)
                    .setNodeDownloadRoot(
                            NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT)
                    .install();
            factory.getYarnInstaller(proxyConfig).setYarnVersion(yarnVersion)
                    .setYarnDownloadRoot(
                            YarnInstaller.DEFAULT_YARN_DOWNLOAD_ROOT)
                    .install();
        } catch (InstallationException e) {
            throw new IllegalStateException(
                    "Failed to install required frontend dependencies", e);
        }
        yarnRunner = factory.getYarnRunner(proxyConfig, null);
        gulpRunner = factory.getGulpRunner();
    }

    /**
     * Creates the yarn runner and gulp runners with out downloading node and
     * yarn.
     * 
     * @param workingDirectory
     *            working directory
     * @param proxyConfig
     *            proxy configuration
     * @param nodePath
     *            file which contains node
     * @param yarnPath
     *            file which contains yarn
     */
    public RunnerManager(File workingDirectory, ProxyConfig proxyConfig,
            File nodePath, File yarnPath) {
        NodeExecutorConfigLocal nodeExecutorConfig = new NodeExecutorConfigLocal(
                nodePath, null, workingDirectory, workingDirectory);
        YarnConfigurationLocal yarnConfigurationLocal = new YarnConfigurationLocal(
                nodePath, yarnPath, workingDirectory);

        yarnRunner = new DefaultYarnRunnerLocal(yarnConfigurationLocal,
                proxyConfig, null).getDefaultYarnRunner();
        gulpRunner = new DefaultGulpRunnerLocal(nodeExecutorConfig)
                .getDefaultGulpRunner();
    }

    /**
     * Gets the {@link YarnRunner} that is used to install dependencies.
     * 
     * @return yarnRunner YarnRunner
     */
    public YarnRunner getYarnRunner() {
        return yarnRunner;
    }

    /**
     * Gets the {@link GulpRunner} that will be used in the transpilation.
     *
     * @return gulpRunner GulpRunner
     */
    public GulpRunner getGulpRunner() {
        return gulpRunner;
    }
}
