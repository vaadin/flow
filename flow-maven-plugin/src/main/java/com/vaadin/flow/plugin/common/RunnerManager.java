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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.helger.commons.url.URLValidator;

/**
 * Creates and configures the runners used by the {@link FrontendToolsManager},
 * providing an extra layer of abstraction.
 *
 * RunnerManager can be used to grab the local node and yarn from the local
 * environment or to download and install them.
 */
public class RunnerManager {
  
    private static final Logger LOGGER = LoggerFactory.getLogger(RunnerManager.class);
    private final YarnRunner yarnRunner;
    private final GulpRunner gulpRunner;

    /**
     * Initializes the manager, downloading node and yarn of the versions
     * specified.
     * 
     * @param workingDirectory
     *            the directory to install and run the tools into
     * @param proxyConfig
     *            the configuration used when installing and running the tools
     * @param nodeVersion
     *            node version to install
     * @param yarnVersion
     *            yarn version to install
     */
    public RunnerManager(File workingDirectory, ProxyConfig proxyConfig,
            String nodeVersion, String yarnVersion, String npmRegistryURL) {
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
        final String finalNpmRegistryUrl = npmRegistryUrl(npmRegistryURL);
        yarnRunner = factory.getYarnRunner(proxyConfig, finalNpmRegistryUrl);
        gulpRunner = factory.getGulpRunner();
    }

    /**
     * Initializes the manager using the paths to locally installed node and
     * yarn.
     * 
     * @param workingDirectory
     *            the directory to run the tools into
     * @param proxyConfig
     *            the configuration used when running the tools
     * @param nodePath
     *            the path to locally installed node
     * @param yarnPath
     *            the path to locally installed yarn
     */
    public RunnerManager(File workingDirectory, ProxyConfig proxyConfig,
            File nodePath, File yarnPath, String npmRegistryURL) {
        NodeExecutorConfigLocal nodeExecutorConfig = new NodeExecutorConfigLocal(
                nodePath, null, workingDirectory, workingDirectory);
        YarnConfigurationLocal yarnConfigurationLocal = new YarnConfigurationLocal(
                nodePath, yarnPath, workingDirectory);

        final String finalNpmRegistryUrl = npmRegistryUrl(npmRegistryURL);
        yarnRunner = new DefaultYarnRunnerLocal(yarnConfigurationLocal,
            proxyConfig, finalNpmRegistryUrl).getDefaultYarnRunner();
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
    
    private String npmRegistryUrl(final String customizedNpmRegistryURL) {
      if (URLValidator.isValid(customizedNpmRegistryURL)) {
        return customizedNpmRegistryURL;
      }
      LOGGER.warn("Provided npmRegistryURL {} is not valid. Ignoring custoized Npm registry URL. ", customizedNpmRegistryURL);
      return null;
    }
}
