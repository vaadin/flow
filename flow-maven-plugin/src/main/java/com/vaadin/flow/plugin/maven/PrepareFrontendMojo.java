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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
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

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeTasks;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.plugin.common.FlowPluginFrontendUtils.getClassFinder;
import static com.vaadin.flow.server.Constants.CONNECT_APPLICATION_PROPERTIES_TOKEN;
import static com.vaadin.flow.server.Constants.CONNECT_GENERATED_TS_DIR_TOKEN;
import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.CONNECT_OPEN_API_FILE_TOKEN;
import static com.vaadin.flow.server.Constants.FRONTEND_TOKEN;
import static com.vaadin.flow.server.Constants.GENERATED_TOKEN;
import static com.vaadin.flow.server.Constants.NPM_TOKEN;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_INITIAL_UIDL;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_USE_V14_BOOTSTRAP;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEAULT_FLOW_RESOURCES_FOLDER;
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

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext; // m2eclipse integration

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // propagate info via System properties and token file
        propagateBuildInfo();

        final URI nodeDownloadRootURI;
        try {
            nodeDownloadRootURI = new URI(nodeDownloadRoot);
        } catch (URISyntaxException e) {
            throw new MojoExecutionException("Failed to parse " + nodeDownloadRoot, e);
        }
        try {
            FrontendTools tools = new FrontendTools(npmFolder.getAbsolutePath(),
                    () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath(),
                    nodeVersion, nodeDownloadRootURI);
            tools.validateNodeAndNpmVersion();
        } catch (IllegalStateException exception) {
            throw new MojoExecutionException(exception.getMessage(), exception);
        }
        try {
            FileUtils.forceMkdir(generatedFolder);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to create folder '"
                    + generatedFolder + "'. Verify that you may write to path.",
                    e);
        }
        try {
            File flowResourcesFolder = new File(npmFolder,
                    DEAULT_FLOW_RESOURCES_FOLDER);
            NodeTasks.Builder builder = new NodeTasks.Builder(
                    getClassFinder(project), npmFolder, generatedFolder,
                    frontendDirectory)
                            .useV14Bootstrap(useDeprecatedV14Bootstrapping())
                            .withFlowResourcesFolder(flowResourcesFolder)
                            .createMissingPackageJson(true)
                            .enableImportsUpdate(false)
                            .enablePackagesUpdate(false)
                            .runNpmInstall(false)
                            .withNodeVersion(nodeVersion)
                            .withNodeDownloadRoot(nodeDownloadRootURI)
                            .withHomeNodeExecRequired(requireHomeNodeExec);
            // If building a jar project copy jar artifact contents now as we
            // might
            // not be able to read files from jar path.
            if ("jar".equals(project.getPackaging())) {
                Set<File> jarFiles = project.getArtifacts().stream()
                        .filter(artifact -> "jar".equals(artifact.getType()))
                        .map(Artifact::getFile).collect(Collectors.toSet());
                builder.copyResources(jarFiles);
            }

            builder.build().execute();
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
        buildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE, productionMode);
        buildInfo.put(SERVLET_PARAMETER_USE_V14_BOOTSTRAP,
                useDeprecatedV14Bootstrapping());
        buildInfo.put(SERVLET_PARAMETER_INITIAL_UIDL, eagerServerLoad);
        buildInfo.put(NPM_TOKEN, npmFolder.getAbsolutePath());
        buildInfo.put(GENERATED_TOKEN, generatedFolder.getAbsolutePath());
        buildInfo.put(FRONTEND_TOKEN, frontendDirectory.getAbsolutePath());
        buildInfo.put(CONNECT_JAVA_SOURCE_FOLDER_TOKEN,
                javaSourceFolder.getAbsolutePath());
        buildInfo.put(CONNECT_APPLICATION_PROPERTIES_TOKEN,
                applicationProperties.getAbsolutePath());
        buildInfo.put(CONNECT_OPEN_API_FILE_TOKEN,
                openApiJsonFile.getAbsolutePath());
        buildInfo.put(CONNECT_GENERATED_TS_DIR_TOKEN,
                generatedTsFolder.getAbsolutePath());

        buildInfo.put(Constants.SERVLET_PARAMETER_ENABLE_PNPM, pnpmEnable);
        buildInfo.put(Constants.REQUIRE_HOME_NODE_EXECUTABLE,
                requireHomeNodeExec);

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
                            + ".properties:%n productionMode: %s%n"
                            + " webpackPort: %s%n "
                            + "project.basedir: %s%nGoal parameters:%n "
                            + "productionMode: %s%n "
                            + "npmFolder: %s%nToken file: " + "%s%n"
                            + "Token content: %s%n",
                    project.getName(),
                    System.getProperty("vaadin.productionMode"),
                    System.getProperty("vaadin.devmode.webpack.running-port"),
                    System.getProperty("project.basedir"), productionMode,
                    npmFolder, token.getAbsolutePath(), buildInfo.toJson()));
        }
    }

}
