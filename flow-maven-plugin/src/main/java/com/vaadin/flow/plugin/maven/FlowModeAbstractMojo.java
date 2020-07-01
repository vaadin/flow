/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.plugin.maven;

import java.io.File;

import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import com.vaadin.flow.server.Constants;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;

/**
 * The base class of Flow Mojos in order to compute correctly the modes.
 *
 * @since 2.0
 */
public abstract class FlowModeAbstractMojo extends AbstractMojo {
    static final String VAADIN_COMPATIBILITY_MODE = "vaadin.compatibilityMode";

    /**
     * Whether or not we are running in compatibility mode.
     */
    @Parameter(defaultValue = "${vaadin.bowerMode}", alias = "bowerMode")
    public String compatibilityMode;

    /**
     * Whether or not we are running in productionMode.
     */
    @Parameter(defaultValue = "${vaadin.productionMode}")
    public boolean productionMode;

    /**
     * The folder where webpack should output index.js and other generated
     * files.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/"
            + VAADIN_SERVLET_RESOURCES)
    protected File webpackOutputDirectory;

    /**
     * The actual compatibility mode boolean.
     */
    protected boolean compatibility;

    /**
     * Instructs to use pnpm for installing npm frontend resources.
     */
    @Parameter(property = Constants.SERVLET_PARAMETER_ENABLE_PNPM, defaultValue = "false")
    protected boolean pnpmEnable;

    /**
     * Whether vaadin home node executable usage is forced. If it's set to
     * {@code true} then vaadin home 'node' is checked and installed if it's
     * absent. Then it will be used instead of globally 'node' or locally
     * installed installed 'node'.
     */
    @Parameter(property = Constants.REQUIRE_HOME_NODE_EXECUTABLE, defaultValue = "false")
    protected boolean requireHomeNodeExec;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (compatibilityMode == null) {
            compatibilityMode = System.getProperty(VAADIN_COMPATIBILITY_MODE);
        }
        // Default mode for V14 is bower true
        compatibility = compatibilityMode != null
                ? Boolean.valueOf(compatibilityMode)
                : isDefaultCompatibility();
    }

    abstract boolean isDefaultCompatibility();

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example `"v12.16.0"`. Defaults to null which uses the
     * Vaadin-default node version - see {@link FrontendTools} for details.
     */
    @Parameter(property = "node.version", defaultValue = FrontendTools.DEFAULT_NODE_VERSION)
    protected String nodeVersion;

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to null which will cause the downloader to use
     * {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
     * <p></p>
     * Example: <code>"https://nodejs.org/dist/"</code>.
     */
    @Parameter(property = "node.download.root", defaultValue = NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT)
    protected String nodeDownloadRoot;
}
