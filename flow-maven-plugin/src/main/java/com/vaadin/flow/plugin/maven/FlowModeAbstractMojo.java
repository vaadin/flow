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
package com.vaadin.flow.plugin.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import com.vaadin.flow.server.Constants;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEAULT_FLOW_RESOURCES_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;

/**
 * The base class of Flow Mojos in order to compute correctly the modes.
 *
 * @since 2.0
 */
public abstract class FlowModeAbstractMojo extends AbstractMojo {
    static final String VAADIN_COMPATIBILITY_MODE = "vaadin.compatibilityMode";

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
     * The directory where flow resources from jars will be copied to.
     */
    @Parameter(defaultValue = "${project.basedir}/" + DEAULT_FLOW_RESOURCES_FOLDER)
    public File flowResourcesFolder;

    /**
     * Whether or not we are running in compatibility mode.
     */
    @Parameter(defaultValue = "${vaadin.bowerMode}", alias = "bowerMode")
    public String compatibilityMode;

    /**
     * Whether or not we are running in productionMode.
     */
    @Parameter(defaultValue = "${vaadin.productionMode}")
    public boolean productionMode;

    /**
     * Whether or not we are running in client-side bootstrap mode (CCDM). True
     * if not defined.
     */
    @Parameter(defaultValue = "${vaadin.clientSideMode}")
    private String clientSideMode;

    /**
     * Whether or not insert the initial Uidl object in the bootstrap index.html
     */
    @Parameter(defaultValue = "${vaadin." + Constants.SERVLET_PARAMETER_INITIAL_UIDL + "}")
    public boolean eagerServerLoad;

    /**
     * The folder where webpack should output index.js and other generated
     * files.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/"
            + VAADIN_SERVLET_RESOURCES)
    protected File webpackOutputDirectory;

    /**
     * The actual compatibility mode boolean.
     */
    protected boolean compatibility;

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
     * Java source folders for connect scanning.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/java")
    protected File javaSourceFolder;

    /**
     * The folder where flow will put TS API files for client projects.
     */
    @Parameter(defaultValue = "${project.basedir}/" + FRONTEND + "/generated")
    protected File generatedTsFolder;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (compatibilityMode == null) {
            compatibilityMode = System.getProperty(VAADIN_COMPATIBILITY_MODE);
        }
        // Default mode for V14 is bower true
        compatibility = compatibilityMode != null
                ? Boolean.valueOf(compatibilityMode)
                : isDefaultCompatibility();
    }

    /**
     * Check if the plugin is running in `clientSideMode` or not. Default: true.
     *
     * @return true if the `clientSideMode` property is not defined or empty.
     */
    public boolean isClientSideMode() {
        if (clientSideMode == null || clientSideMode.isEmpty()) {
            return true;
        }
        return Boolean.parseBoolean(clientSideMode);
    }

    abstract boolean isDefaultCompatibility();
}
