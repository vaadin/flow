/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

package com.github.eirslett.maven.plugins.frontend.lib;

/**
 * Yarn runner that uses an specific configuration(paths) of node, yarn, working
 * directory and platform.
 *
 * @since 1.2
 */
public class DefaultYarnRunnerLocal {

    /**
     * yarn runner for executing yarn.
     */
    private DefaultYarnRunner defaultYarnRunner;

    /**
     * Creates a DefaultYarnRunner with an specific configuration.
     *
     * @param config
     *            yarn configuration
     * @param proxyConfig
     *            proxy configuration
     * @param npmRegistryURL
     *            npm registry website
     */
    public DefaultYarnRunnerLocal(YarnExecutorConfig config,
            ProxyConfig proxyConfig, String npmRegistryURL) {
        defaultYarnRunner = new DefaultYarnRunner(config, proxyConfig,
                npmRegistryURL);
    }

    /**
     * Gets the yarnRunner.
     *
     * @return defaultYarnRunner yarnRunner
     */
    public YarnRunner getDefaultYarnRunner() {
        return defaultYarnRunner;
    }
}
