/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.plugin.maven;

import java.io.File;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import com.vaadin.flow.server.Constants;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.InitParameters.NODE_DOWNLOAD_ROOT;
import static com.vaadin.flow.server.InitParameters.NODE_VERSION;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;

/**
 * The base class of Flow Mojos in order to compute correctly the modes.
 *
 * @since 2.0
 */
public abstract class FlowModeAbstractMojo extends AbstractMojo {

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

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example `"v14.15.4"`. Defaults to null which uses the
     * Vaadin-default node version - see {@link FrontendTools} for details.
     */
    @Parameter(property = NODE_VERSION, defaultValue = FrontendTools.DEFAULT_NODE_VERSION)
    protected String nodeVersion;

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to null which will cause the downloader to use
     * {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
     * <p>
     * </p>
     * Example: <code>"https://nodejs.org/dist/"</code>.
     */
    @Parameter(property = NODE_DOWNLOAD_ROOT)
    protected String nodeDownloadRoot;

    /**
     * A directory with project's frontend source files.
     */
    @Parameter(defaultValue = "${project.basedir}/" + FRONTEND)
    protected File frontendDirectory;

    /**
     * Setting this to true will run {@code npm ci} instead of
     * {@code npm install} when using npm.
     *
     * If using pnpm, the install will be run with {@code --frozen-lockfile}
     * parameter.
     *
     * This makes sure that the versions in package lock file will not be
     * overwritten and production builds are reproducible.
     */
    @Parameter(property = InitParameters.CI_BUILD, defaultValue = "false")
    protected boolean ciBuild;
}