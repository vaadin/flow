/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

package com.github.eirslett.maven.plugins.frontend.lib;

/**
 * Gulp runner that uses an specific configuration(paths) of node, npm,
 * installation directory and working directory and platform.
 *
 * @since 1.2
 */
public class DefaultGulpRunnerLocal {
    private DefaultGulpRunner defaultGulpRunner;

    /**
     * Defines and configures a DefaultGulpRunner with a specific gulp
     * configuration.
     *
     * @param config
     *            Gulp configuration
     */
    public DefaultGulpRunnerLocal(NodeExecutorConfig config) {
        defaultGulpRunner = new DefaultGulpRunner(config);
    }

    /**
     * Gets the GulpRunner of the local default gulp runner.
     *
     * @return GulpRunner gulp node task
     */
    public GulpRunner getDefaultGulpRunner() {
        return defaultGulpRunner;
    }
}
