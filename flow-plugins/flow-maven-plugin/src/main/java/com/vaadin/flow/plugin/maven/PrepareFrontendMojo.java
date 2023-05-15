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
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.build.BuildContext;

import com.vaadin.flow.plugin.base.BuildFrontendUtil;

/**
 * This goal checks that node and npm tools are installed and creates or updates
 * `package.json` and the frontend build tool configuration files.
 * <p>
 * Copies frontend resources available inside `.jar` dependencies to
 * `node_modules` when building a jar package.
 *
 * @since 2.0
 */
@Mojo(name = "prepare-frontend", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class PrepareFrontendMojo extends FlowModeAbstractMojo {

    @Component
    private BuildContext buildContext; // m2eclipse integration

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (productionMode != null) {
            logWarn("The <productionMode>" + productionMode
                    + "</productionMode> Maven parameter no longer has any effect and can be removed. Production mode is automatically enabled when you run the build-frontend target.");
        }

        try {
            FileUtils.deleteDirectory(generatedTsFolder());
            logDebug("Cleaned the generated folder '"
                    + generatedTsFolder().getPath() + "'");
        } catch (IOException e) {
            throw new MojoFailureException("Failed to clean generated folder "
                    + generatedTsFolder().getPath(), e);
        }

        // propagate info via System properties and token file
        File tokenFile = BuildFrontendUtil.propagateBuildInfo(this);

        // Inform m2eclipse that the directory containing the token file has
        // been updated in order to trigger server re-deployment (#6103)
        if (buildContext != null) {
            buildContext.refresh(tokenFile.getParentFile());
        }

        try {
            BuildFrontendUtil.prepareFrontend(this);
        } catch (Exception exception) {
            throw new MojoFailureException(
                    "Could not execute prepare-frontend goal.", exception);
        }

    }

}
