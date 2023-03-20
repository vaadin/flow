/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.plugin.base;

import java.io.File;

/**
 * Gives access to access to plugin-spectific implementations and
 * configurations.
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
     * Copy the `webpack.generated.js` from the specified URL. Default is the
     * template provided by this plugin. Set it to empty string to disable the
     * feature.
     *
     * @return webpackGeneratedTemplate
     */
    String webpackGeneratedTemplate();

    /**
     * Copy the `webpack.config.js` from the specified URL if missing. Default
     * is the template provided by this plugin. Set it to empty string to
     * disable the feature.
     *
     * @return webpackTemplate
     */
    String webpackTemplate();

}
