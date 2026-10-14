/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.plugin.base;

import java.io.File;

import com.vaadin.flow.server.InitParameters;

/**
 * Gives access to plugin-specific implementations and configurations.
 *
 */
public interface PluginAdapterBuild extends PluginAdapterBase {

    /**
     * Defines the project frontend directory from where resources should be
     * copied from for use with webpack.
     *
     * @return {@link File}
     */

    File frontendResourcesDirectory();

    /**
     * Whether to generate a bundle from the project frontend sources or not.
     *
     * @return boolean
     */

    boolean generateBundle();

    /**
     * Whether to generate embeddable web components from WebComponentExporter
     * inheritors.
     *
     * @return boolean
     */

    boolean generateEmbeddableWebComponents();

    /**
     * Whether to use byte code scanner strategy to discover frontend
     * components.
     *
     * @return boolean
     */
    boolean optimizeBundle();

    /**
     * Whether to run <code>npm install</code> after updating dependencies.
     *
     * @return boolean
     */
    boolean runNpmInstall();

    /**
     * Setting this to true will run {@code npm ci} instead of
     * {@code npm install} when using npm.
     *
     * If using pnpm or bun, the installation will be run with
     * {@code --frozen-lockfile} parameter.
     *
     * This makes sure that the package lock file will not be overwritten.
     *
     * @return true if ci build should be enabled
     */
    boolean ciBuild();

    /**
     * Setting this to {@code true} will force a build of the production build
     * even if there is a default production bundle that could be used.
     *
     * Created production bundle optimization is defined by
     * {@link #optimizeBundle} parameter.
     */
    boolean forceProductionBuild();

    /**
     * Setting this to {@code false} will skip compression of the bundle.
     * {@code true} by default.
     *
     * @return {@code true} if the bundle should be compressed
     */
    boolean compressBundle();
}
