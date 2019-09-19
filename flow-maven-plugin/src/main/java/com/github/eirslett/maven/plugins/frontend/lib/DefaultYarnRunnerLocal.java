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
