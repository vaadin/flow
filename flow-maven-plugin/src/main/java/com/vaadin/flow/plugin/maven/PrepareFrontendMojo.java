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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeTasks;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.plugin.common.FlowPluginFrontendUtils.getClassFinder;
import static com.vaadin.flow.server.Constants.FRONTEND_TOKEN;
import static com.vaadin.flow.server.Constants.GENERATED_TOKEN;
import static com.vaadin.flow.server.Constants.NPM_TOKEN;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;

/**
 * This goal checks that node and npm tools are installed, copies frontend
 * resources available inside `.jar` dependencies to `node_modules`, and creates
 * or updates `package.json` and `webpack.config.json` files.
 *
 * @since 2.0
 */
@Mojo(name = "prepare-frontend", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class PrepareFrontendMojo extends FlowModeAbstractMojo {

    /**
     * This goal checks that node and npm tools are installed, copies frontend
     * resources available inside `.jar` dependencies to `node_modules`, and
     * creates or updates `package.json` and `webpack.config.json` files.
     *
     * @deprecated use {@link PrepareFrontendMojo} instead
     */
    @Deprecated
    @Mojo(name = "validate", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
    public static class VaildateMojo extends PrepareFrontendMojo {
        @Override
        public void execute()
                throws MojoExecutionException, MojoFailureException {
            getLog().warn(
                    "\n\n   You are using the 'validate' goal which has been renamed to 'prepare-frontend', please update your 'pom.xml'.\n");
            super.execute();
        }
    }

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File npmFolder;

    /**
     * Copy the `webapp.config.js` from the specified URL if missing. Default is
     * the template provided by this plugin. Set it to empty string to disable
     * the feature.
     */
    @Parameter(defaultValue = FrontendUtils.WEBPACK_CONFIG)
    private String webpackTemplate;

    /**
     * Copy the `webapp.generated.js` from the specified URL. Default is the
     * template provided by this plugin. Set it to empty string to disable the
     * feature.
     */
    @Parameter(defaultValue = FrontendUtils.WEBPACK_GENERATED)
    private String webpackGeneratedTemplate;

    /**
     * The folder where flow will put generated files that will be used by
     * webpack.
     */
    @Parameter(defaultValue = "${project.build.directory}/" + FRONTEND)
    private File generatedFolder;

    @Component
    private BuildContext buildContext; // m2eclipse integration

    /**
     * A directory with project's frontend source files.
     */
    @Parameter(defaultValue = "${project.basedir}/frontend")
    private File frontendDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        // propagate info via System properties and token file
        propagateBuildInfo();

        // Do nothing when compatibility mode
        if (compatibility) {
            getLog().debug(
                    "Skipped 'prepare-frontend' goal because compatibility mode is set.");
            return;
        }

        try {
            FrontendUtils.getNodeExecutable(npmFolder.getAbsolutePath());
            FrontendUtils.getNpmExecutable(npmFolder.getAbsolutePath());
            FrontendUtils
                    .validateNodeAndNpmVersion(npmFolder.getAbsolutePath());
        } catch (IllegalStateException exception) {
            throw new MojoExecutionException(exception.getMessage(), exception);
        }

        try {
            new NodeTasks.Builder(getClassFinder(project), npmFolder,
                    generatedFolder, frontendDirectory)
                            .withWebpack(webpackOutputDirectory,
                                    webpackTemplate, webpackGeneratedTemplate)
                            .createMissingPackageJson(true)
                            .enableImportsUpdate(false)
                            .enablePackagesUpdate(false).runNpmInstall(false)
                            .build().execute();
        } catch (ExecutionFailedException exception) {
            throw new MojoFailureException(
                    "Could not execute prepare-frontend goal.", exception);
        }

    }

    private void propagateBuildInfo() {
        // For forked processes not accessing to System.properties we leave a
        // token file with the information about the build
        File token = new File(webpackOutputDirectory, TOKEN_FILE);
        JsonObject buildInfo = Json.createObject();
        buildInfo.put(SERVLET_PARAMETER_COMPATIBILITY_MODE, compatibility);
        buildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE, productionMode);
        buildInfo.put(NPM_TOKEN, npmFolder.getAbsolutePath());
        buildInfo.put(GENERATED_TOKEN, generatedFolder.getAbsolutePath());
        buildInfo.put(FRONTEND_TOKEN, frontendDirectory.getAbsolutePath());
        try {
            FileUtils.forceMkdir(token.getParentFile());
            FileUtils.write(token, JsonUtil.stringify(buildInfo, 2) + "\n",
                    StandardCharsets.UTF_8.name());

            // Inform m2eclipse that the directory containing the token file has
            // been updated in order to trigger server re-deployment (#6103)
            if (buildContext != null) {
                buildContext.refresh(token.getParentFile());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Enable debug to find out problems related with flow modes
        Log log = getLog();
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "%n>>> Running prepare-frontend in %s project%nSystem"
                            + ".properties:%n productionMode: %s%n bowerMode:"
                            + " %s%n compatibilityMode: %s%n webpackPort: %s%n "
                            + "project.basedir: %s%nGoal parameters:%n "
                            + "productionMode: %s%n compatibilityMode: %s%n "
                            + "compatibility: %b%n npmFolder: %s%nToken file: "
                            + "%s%n" + "Token content: %s%n",
                    project.getName(),
                    System.getProperty("vaadin.productionMode"),
                    System.getProperty("vaadin.bowerMode"),
                    System.getProperty("vaadin.compatibiityMode"),
                    System.getProperty("vaadin.devmode.webpack.running-port"),
                    System.getProperty("project.basedir"), productionMode,
                    compatibilityMode, compatibility, npmFolder,
                    token.getAbsolutePath(), buildInfo.toJson()));
        }
    }

    @Override
    boolean isDefaultCompatibility() {
        return false;
    }
}
