/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.utils.LookupImpl;

/**
 * Gives access to access to plugin-spectific implementations and
 * configurations.
 *
 */
public interface PluginAdapterBase {

    /**
     * Application properties file in Spring project.
     *
     * @return {@link File} applicationProperties
     */
    File applicationProperties();

    /**
     * Creates a {@link Lookup} for the {@link ClassFinder}.
     *
     * @param classFinder
     *            implementation that will be registered for the serviceTxpe
     *            `ClassFinder`.
     * @return {@link Lookup}
     */
    default Lookup createLookup(ClassFinder classFinder) {

        return Lookup.compose(Lookup.of(classFinder, ClassFinder.class),
                new LookupImpl(classFinder));
    }

    /**
     * Whether or not insert the initial Uidl object in the bootstrap
     * index.html.
     *
     * @return {@link boolean}
     */
    boolean eagerServerLoad();

    /**
     * A directory with project's frontend source files.
     *
     * @return {@link File}
     */
    File frontendDirectory();

    /**
     * The folder where flow will put generated files that will be used by
     * webpack.
     *
     * @return {@link File}
     */
    File generatedFolder();

    /**
     * The folder where flow will put TS API files for client projects.
     *
     * @return {@link File}
     */
    File generatedTsFolder();

    /**
     * The {@link ClassFinder} that should be used.
     *
     * @return {@link ClassFinder}
     */
    ClassFinder getClassFinder();

    /**
     * The Jar Files that would be searched.
     *
     * @return {@link Set} of {@link File}
     */
    Set<File> getJarFiles();

    /**
     * Indicates that it is a Jar Project.
     *
     * @return boolean - indicates that it is a Jar Project
     */
    boolean isJarProject();

    /**
     * Whether or not we are running in legacy V14 bootstrap mode. True if
     * defined or if it's set to true.
     *
     * @return {@link String}
     **/
    String getUseDeprecatedV14Bootstrapping();

    /**
     * Checks the debug Mode.
     *
     * @return boolean
     */
    boolean isDebugEnabled();

    /**
     * Check if the plugin is running in legacy V14 bootstrap mode or not.
     * Default: false.
     *
     * @return true if the `useDeprecatedV14Bootstrapping` is empty or true.
     */
    default boolean isUseDeprecatedV14Bootstrapping() {

        String useDeprecatedV14Bootstrapping = getUseDeprecatedV14Bootstrapping();
        if (useDeprecatedV14Bootstrapping == null) {
            return false;
        }
        if (useDeprecatedV14Bootstrapping.isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(useDeprecatedV14Bootstrapping);
    }

    /**
     * Java source folders for scanning.
     *
     * @return {@link File}
     */
    File javaSourceFolder();

    /**
     * Java resource folder.
     *
     * @return {@link File}
     */
    File javaResourceFolder();

    /**
     * Delegates a debug-Message to a logger.
     *
     * @param debugMessage
     *            to be logged.
     */
    void logDebug(CharSequence debugMessage);

    /**
     * Delegates a info-Message to a logger.
     *
     * @param infoMessage
     *            to be logged.
     */
    void logInfo(CharSequence infoMessage);

    /**
     * delegates a warning-Message to a logger.
     *
     * @param warningMessage
     *            to be logged.
     */

    void logWarn(CharSequence warningMessage);

    /**
     * Delegates a warning-Message to a logger.
     *
     * @param warningMessage
     *            to be logged.
     * @param throwable
     *            to be logged.
     */
    void logWarn(CharSequence warningMessage, Throwable throwable);

    /**
     * Delegates a error-Message to a logger.
     *
     * @param warning
     *            to be logged.
     * @param e
     *            to be logged.
     */
    void logError(CharSequence warning, Throwable e);

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to null which will cause the downloader to use
     * {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
     * <p>
     * Example: <code>"https://nodejs.org/dist/"</code>.
     *
     * @return nodeDownloadRoot
     * @throws URISyntaxException
     *             - Could not build an URI from nodeDownloadRoot().
     */
    URI nodeDownloadRoot() throws URISyntaxException;

    /**
     * Whether the alternative node may be autoupdated or not.
     *
     * @return {@code true} to update node if older than default
     */
    boolean nodeAutoUpdate();

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example `"v12.18.3"`. Defaults to null which uses the
     * Vaadin-default node version - see {@link FrontendTools} for details.
     *
     * @return node Verrsion as String
     */
    String nodeVersion();

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     *
     * @return boolean
     */
    File npmFolder();

    /**
     * Default generated path of the OpenAPI json.
     *
     * @return boolean
     */
    File openApiJsonFile();

    /**
     * Instructs to use pnpm for installing npm frontend resources.
     *
     * @return boolean
     */

    boolean pnpmEnable();

    /**
     * Instructs to use globally installed pnpm tool or the default supported
     * pnpm version.
     *
     * @return boolean
     */
    boolean useGlobalPnpm();

    /**
     * Whether or not we are running in productionMode.
     *
     * @return boolean
     */
    boolean productionMode();

    /**
     * The projects root Directory.
     *
     * @return {@link Path}
     */
    Path projectBaseDirectory();

    /**
     * Whether vaadin home node executable usage is forced. If it's set to
     * {@code true} then vaadin home 'node' is checked and installed if it's
     * absent. Then it will be used instead of globally 'node' or locally
     * installed installed 'node'.
     *
     * @return boolean
     */
    boolean requireHomeNodeExec();

    /**
     * Defines the output directory for generated non-served resources, such as
     * the token file.
     *
     * @return {@link File}
     */
    File servletResourceOutputDirectory();

    /**
     * The folder where webpack should output index.js and other generated
     * files.
     *
     * @return {@link File}
     */
    File webpackOutputDirectory();

    /**
     * The folder where everything is built into.
     *
     * @return build folder
     */
    String buildFolder();
}
