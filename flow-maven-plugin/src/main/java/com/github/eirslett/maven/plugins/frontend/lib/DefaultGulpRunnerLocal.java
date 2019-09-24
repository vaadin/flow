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
