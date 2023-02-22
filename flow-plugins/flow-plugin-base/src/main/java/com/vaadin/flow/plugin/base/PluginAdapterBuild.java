/*
 * Copyright 2000-2023 Vaadin Ltd.
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
     * Setting this to true will run {@code npm ci} instead of
     * {@code npm install} when using npm.
     *
     * If using pnpm, the install will be run with {@code --frozen-lockfile}
     * parameter.
     *
     * This makes sure that the package lock file will not be overwritten.
     *
     * @return true if ci build should be enabled
     */
    boolean ciBuild();
}
