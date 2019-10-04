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

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;

/**
 * The base class of Flow Mojos in order to compute correctly the modes.
 *
 * @since 2.0
 */
public abstract class FlowModeAbstractMojo extends AbstractMojo {
    static final String VAADIN_COMPATIBILITY_MODE = "vaadin.compatibilityMode";

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

    abstract boolean isDefaultCompatibility();
}
