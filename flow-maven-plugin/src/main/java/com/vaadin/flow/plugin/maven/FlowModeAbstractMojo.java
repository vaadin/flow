/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.plugin.maven;

import java.io.File;

import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.utils.LookupImpl;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;

/**
 * The base class of Flow Mojos in order to compute correctly the modes.
 *
 * @since 2.0
 */
public abstract class FlowModeAbstractMojo extends AbstractMojo {
    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    public File npmFolder;

    /**
     * The folder where flow will put generated files that will be used by
     * webpack.
     */
    @Parameter(defaultValue = "${project.build.directory}/" + FRONTEND)
    public File generatedFolder;

    /**
     * A directory with project's frontend source files.
     */
    @Parameter(defaultValue = "${project.basedir}/" + FRONTEND)
    public File frontendDirectory;

    /**
     * Whether or not we are running in productionMode.
     */
    @Parameter(defaultValue = "${vaadin.productionMode}")
    public boolean productionMode;

    /**
     * Whether or not insert the initial Uidl object in the bootstrap index.html
     */
    @Parameter(defaultValue = "${vaadin."
            + Constants.SERVLET_PARAMETER_INITIAL_UIDL + "}")
    public boolean eagerServerLoad;

    /**
     * The folder where webpack should output index.js and other generated
     * files.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/"
            + VAADIN_WEBAPP_RESOURCES)
    protected File webpackOutputDirectory;

    /**
     * Application properties file in Spring project.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/application.properties")
    protected File applicationProperties;

    /**
     * Default generated path of the OpenAPI json.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-resources/openapi.json")
    protected File openApiJsonFile;

    /**
     * Java source folders for scanning.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/java")
    protected File javaSourceFolder;

    /**
     * The folder where flow will put TS API files for client projects.
     */
    @Parameter(defaultValue = "${project.basedir}/" + FRONTEND + "/generated")
    protected File generatedTsFolder;

    /**
     * Defines the output directory for generated non-served resources, such as
     * the token file.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/"
            + VAADIN_SERVLET_RESOURCES)
    protected File resourceOutputDirectory;

    /**
     * Instructs to use pnpm for installing npm frontend resources.
     */
    @Parameter(property = Constants.SERVLET_PARAMETER_ENABLE_PNPM, defaultValue = Constants.ENABLE_PNPM_DEFAULT_STRING)
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
     * Vaadin, for example `"v12.18.3"`. Defaults to null which uses the
     * Vaadin-default node version - see {@link FrontendTools} for details.
     */
    @Parameter(property = "node.version", defaultValue = FrontendTools.DEFAULT_NODE_VERSION)
    protected String nodeVersion;

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to null which will cause the downloader to use
     * {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
     * <p></p>
     * Example: <code>"https://nodejs.org/dist/"</code>.
     */
    @Parameter(property = "node.download.root", defaultValue = NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT)
    protected String nodeDownloadRoot;

    /**
     * Whether or not we are running in legacy V14 bootstrap mode. True if
     * defined or if it's set to true.
     */
    @Parameter(defaultValue = "${vaadin.useDeprecatedV14Bootstrapping}")
    private String useDeprecatedV14Bootstrapping;

    /**
     * Check if the plugin is running in legacy V14 bootstrap mode or not.
     * Default: false.
     *
     * @return true if the `useDeprecatedV14Bootstrapping` is empty or true.
     */
    public boolean useDeprecatedV14Bootstrapping() {
        if (useDeprecatedV14Bootstrapping == null) {
            return false;
        }
        if (useDeprecatedV14Bootstrapping.isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(useDeprecatedV14Bootstrapping);
    }

    protected Lookup createLookup(ClassFinder classFinder){
        return Lookup.compose(Lookup.of(classFinder, ClassFinder.class), new LookupImpl(classFinder)) ;
    }
}
