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
import java.util.Objects;
import java.util.Optional;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.FrontendToolsLocator;

/**
 * The class to manage the runners for frontend tools – gulp, node and yarn. The
 * manager first attempts to use the local tools, if the paths are provided,
 * then it tries to find the locally installed tools (if configured to do so)
 * and then it downloads the missing tools.
 *
 * @since 1.2
 */
public class RunnerManager {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(RunnerManager.class);
    private final FrontendToolsLocator toolsLocator;
    private final YarnRunner yarnRunner;
    private final GulpRunner gulpRunner;

    private RunnerManager(Builder builder) {
        this.toolsLocator = builder.toolsLocator;

        Optional<File> localNodePath = getToolPath(builder.nodePath,
                builder.autodetectTools, "node");
        if (localNodePath.isPresent()) {
            this.gulpRunner = new DefaultGulpRunnerLocal(
                    new NodeExecutorConfigLocal(localNodePath.get(), null,
                            builder.workingDirectory, builder.workingDirectory))
                                    .getDefaultGulpRunner();
            this.yarnRunner = getToolPath(builder.yarnPath,
                    builder.autodetectTools, "yarn")
                            .map(localYarnPath -> new YarnConfigurationLocal(
                                    localNodePath.get(), localYarnPath,
                                    builder.workingDirectory))
                            .map(config -> new DefaultYarnRunnerLocal(config,
                                    builder.proxyConfig,
                                    validateUrl(builder.npmRegistryUrl)))
                            .map(DefaultYarnRunnerLocal::getDefaultYarnRunner)
                            .orElseGet(() -> downloadYarn(
                                    builder.workingDirectory,
                                    builder.proxyConfig, builder.npmRegistryUrl,
                                    builder.yarnVersion));
        } else {
            LOGGER.debug("Downloading the tools required into '{}' directory",
                    builder.workingDirectory);
            this.gulpRunner = downloadNode(builder.workingDirectory,
                    builder.proxyConfig, builder.nodeVersion);
            this.yarnRunner = downloadYarn(builder.workingDirectory,
                    builder.proxyConfig, builder.npmRegistryUrl,
                    builder.yarnVersion);
        }
    }

    /**
     * The builder for creating the {@link RunnerManager}.
     */
    public static class Builder {
        private String nodeVersion;
        private File nodePath;
        private String yarnVersion;
        private File yarnPath;
        private String npmRegistryUrl;
        private boolean autodetectTools;
        private FrontendToolsLocator toolsLocator = new FrontendToolsLocator();

        private final File workingDirectory;
        private final ProxyConfig proxyConfig;

        /**
         * Create the builder with common configuration for all the tools
         * provided.
         *
         * @param workingDirectory
         *            the directory to install and run the tools into
         * @param proxyConfig
         *            the configuration used when installing and running the
         *            tools
         */
        public Builder(File workingDirectory, ProxyConfig proxyConfig) {
            this.workingDirectory = workingDirectory;
            this.proxyConfig = proxyConfig;
        }

        /**
         * Sets the versions of the tools to download, if no local tools are
         * installed. The versions specified are used for downloading the tools
         * only.
         *
         * @param nodeVersion
         *            node version to download, if the local installation is
         *            missing
         * @param yarnVersion
         *            yarn version to download, if the local installation is
         *            missing
         * @return current builder instance to chain calls
         */
        public Builder versionsToDownload(String nodeVersion,
                String yarnVersion) {
            this.nodeVersion = nodeVersion;
            this.yarnVersion = yarnVersion;
            return this;
        }

        /**
         * Sets the paths to the local installation of the tools. If both tool
         * paths are specified and both tools work, no download attempts with
         * versions specified in
         * {@link Builder#versionsToDownload(String, String)}} are made.
         *
         * @param nodePath
         *            the path to locally installed node or {@code null} if the
         *            tool is absent
         * @param yarnPath
         *            the path to locally installed yarn or {@code null} if the
         *            tool is absent
         * @return current builder instance to chain calls
         */
        public Builder localInstallations(File nodePath, File yarnPath) {
            this.nodePath = nodePath;
            this.yarnPath = yarnPath;
            return this;
        }

        /**
         * Sets the custom registry url to be used when running yarn.
         *
         * @param npmRegistryUrl
         *            the custom URL to NPM registry or null for default
         *            registry
         * @return current builder instance to chain calls
         */
        public Builder npmRegistryUrl(String npmRegistryUrl) {
            this.npmRegistryUrl = npmRegistryUrl;
            return this;
        }

        /**
         * Sets the custom {@link FrontendToolsLocator} to look for locally
         * installed tools and verify that they are working.
         *
         * @param toolsLocator
         *            local frontend tools locator, not {@code null}
         * @return current builder instance to chain calls
         */
        public Builder frontendToolsLocator(FrontendToolsLocator toolsLocator) {
            this.toolsLocator = Objects.requireNonNull(toolsLocator);
            return this;
        }

        /**
         * Enables or disables local tool detection, the default value is
         * {@code false}.
         *
         * @param autodetectTools
         *            when {@code true}, the manager searches the current system
         *            for installed tools
         * @return current builder instance to chain calls
         */
        public Builder autodetectTools(boolean autodetectTools) {
            this.autodetectTools = autodetectTools;
            return this;
        }

        /**
         * Creates the {@link RunnerManager} instance. If there is no data on
         * any tool specified (no local path, no version to download and
         * automatic detection is turned off), an exception is thrown.
         *
         * @return the {@link RunnerManager} instance
         * @throws IllegalStateException
         *             if there is no data on any tool (node, yarn) is specified
         *             that allows to locate it
         */
        public RunnerManager build() {
            if (nodePath == null && nodeVersion == null && !autodetectTools) {
                throw new IllegalStateException(
                        "There is no way to use node in the system: automatic detection is disabled, no path to a local installation or version to download is specified");
            }
            if (yarnPath == null && yarnVersion == null && !autodetectTools) {
                throw new IllegalStateException(
                        "There is no way to use yarn in the system: automatic detection is disabled, no path to a local installation or version to download is specified");
            }

            return new RunnerManager(this);
        }
    }

    private GulpRunner downloadNode(File workingDirectory,
            ProxyConfig proxyConfig, String nodeVersion) {
        FrontendPluginFactory factory = new FrontendPluginFactory(
                workingDirectory, workingDirectory);
        try {
            factory.getNodeInstaller(proxyConfig).setNodeVersion(nodeVersion)
                    .setNodeDownloadRoot(
                            NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT)
                    .install();
        } catch (InstallationException e) {
            throw new IllegalStateException("Failed to download node", e);
        }
        return factory.getGulpRunner();
    }

    private YarnRunner downloadYarn(File workingDirectory,
            ProxyConfig proxyConfig, String npmRegistryUrl,
            String yarnVersion) {
        FrontendPluginFactory factory = new FrontendPluginFactory(
                workingDirectory, workingDirectory);
        try {
            factory.getYarnInstaller(proxyConfig).setYarnVersion(yarnVersion)
                    .setYarnDownloadRoot(
                            YarnInstaller.DEFAULT_YARN_DOWNLOAD_ROOT)
                    .install();
        } catch (InstallationException e) {
            throw new IllegalStateException("Failed to download yarn", e);
        }
        return factory.getYarnRunner(proxyConfig, validateUrl(npmRegistryUrl));
    }

    private Optional<File> getToolPath(File toolPath, boolean autodetect,
            String toolName) {
        if (toolPath != null && toolsLocator.verifyTool(toolPath)) {
            LOGGER.debug("Using {} at path '{}'", toolName, toolPath);
            return Optional.of(toolPath);
        } else if (autodetect) {
            LOGGER.debug(
                    "Autodetect is enabled, attempting to locate {} installation",
                    toolName);
            Optional<File> autodetectedPath = toolsLocator
                    .tryLocateTool(toolName);
            autodetectedPath.ifPresent(path -> LOGGER.debug(
                    "Using automatically detected {} at path '{}'", toolName,
                    path));
            return autodetectedPath;
        }
        LOGGER.debug("Found no local installation for {}", toolName);
        return Optional.empty();
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

    private String validateUrl(String url) {
        if (URLValidator.isValid(url)) {
            return url;
        } else if (url != null) {
            LOGGER.warn(
                    "Provided npmRegistryURL '{}' is not valid. Ignoring customized NPM registry URL. Default NPM registry URL will be used instead.",
                    url);
        }
        return null;
    }
}
