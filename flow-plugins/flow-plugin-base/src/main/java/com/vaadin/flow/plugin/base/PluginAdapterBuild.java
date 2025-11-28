/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.function.Consumer;

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

    /**
     * Checks if the artifact defined by given coordinates is a dependency of
     * the project, present at runtime.
     * <p>
     *
     * If the dependency is missing or in invalid scope, the method produces a
     * message containing the necessary instructions to fix the project and
     * notifies the caller by invoking the provided message consumer, if
     * present.
     *
     * @param groupId
     *            dependency groupId, not {@literal null}.
     * @param artifactId
     *            dependency artifactId, not {@literal null}.
     * @param missingDependencyMessageConsumer
     *            a consumer for missing dependency message produced by the
     *            adapter.
     * @return {@literal true} if the given coordinates identify a project
     *         dependency present at runtime, otherwise {@literal false}.
     */
    boolean checkRuntimeDependency(String groupId, String artifactId,
            Consumer<String> missingDependencyMessageConsumer);

    /**
     * The resources output directory for META-INF/resources in the classes
     * output directory.
     *
     * @return the META-INF/resources directory, usually
     *         {output}/classes/META-INF/resources
     */
    File resourcesOutputDirectory();
}
