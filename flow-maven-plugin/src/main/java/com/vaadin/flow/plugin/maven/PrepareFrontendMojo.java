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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.plugin.common.ArtifactData;
import com.vaadin.flow.plugin.common.JarContentsManager;
import com.vaadin.flow.plugin.production.ProductionModeCopyStep;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeTasks;

import static com.vaadin.flow.plugin.common.FlowPluginFrontendUtils.getClassFinder;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;

/**
 * This goal checks that node and npm tools are installed, copies frontend
 * resources available inside `.jar` dependencies to `node_modules`, and creates
 * or updates `package.json` and `webpack.config.json` files.
 */
@Mojo(name = "prepare-frontend", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class PrepareFrontendMojo extends AbstractMojo {

    /**
     * This goal checks that node and npm tools are installed, copies frontend
     * resources available inside `.jar` dependencies to `node_modules`, and creates
     * or updates `package.json` and `webpack.config.json` files.
     * 
     * @deprecated use {@link PrepareFrontendMojo} instead
     */
    @Deprecated
    @Mojo(name = "validate", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
    public static class VaildateMojo extends PrepareFrontendMojo {
        @Override
        public void execute() {
            getLog().warn("\n\n   You are using the 'validate' goal which has been renamed to 'prepare-frontend', please update your 'pom.xml'.\n");
            super.execute();
        }
    }

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/META-INF/resources/frontend")
    private File frontendResourcesDirectory;

    /**
     * Comma separated values for the paths that should be analyzed in every
     * project dependency jar and, if files suitable for copying present in
     * those paths, those should be copied.
     */
    @Parameter(defaultValue = RESOURCES_FRONTEND_DEFAULT)
    private String jarResourcePathsToCopy;

    /**
     * Comma separated wildcards for files and directories that should be
     * copied. Default is only .js and .css files.
     */
    @Parameter(defaultValue = "**/*.js,**/*.css", required = true)
    private String includes;


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
     * Whether or not we are running in bowerMode.
     */
    @Parameter(defaultValue = "${vaadin.bowerMode}")
    private boolean bowerMode;

    /**
     * Whether or not we are running in productionMode.
     */
    @Parameter(defaultValue = "${vaadin.productionMode}")
    private boolean productionMode;

    /**
     * The folder where flow will put generated files that will be used by webpack.
     */
    @Parameter(defaultValue = "${project.build.directory}/" + FRONTEND)
    private File generatedPath;

    /**
     * The folder where webpack should output index.js and other generated files.
     * By default the output folder is decided depending on project package type.
     */
    @Parameter
    private File webpackOutputDirectory;

    @Override
    public void execute() {
        // propagate to anything run after this the bower mode flag
        // It allows that we can use `vaadin.bowerMode` flag as a regular maven
        // property in the pom instead of having to use
        // `properties-maven-plugin` to propagate it to other plugins like
        // the `jetty-maven-plugin`
        System.setProperty("vaadin.bowerMode", "" + bowerMode);
        System.setProperty("vaadin.productionMode", "" + productionMode);

        // In the case of running npm dev-mode in a maven multi-module projects,
        // inform dev-mode server and updaters about the project folder, otherwise
        // it tries to run in the module parent folder.
        System.setProperty("project.basedir", npmFolder.getAbsolutePath());

        // Do nothing when bower mode
        if (bowerMode) {
            getLog().debug(
                    "Skipped 'validate' goal because `vaadin.bowerMode` is set.");
            return;
        }

        FrontendUtils.getNodeExecutable();
        FrontendUtils.getNpmExecutable();

        new NodeTasks.Builder(getClassFinder(project), npmFolder, generatedPath)
                .withWebpack(getWebpackOutputDirectory(), webpackTemplate)
                .createMissingPackageJson(true)
                .enableImportsUpdate(false)
                .enablePackagesUpdate(false)
                .runNpmInstall(false)
                .build().execute();

        File flowNodeDirectory = new File(npmFolder,
                NODE_MODULES + FLOW_NPM_PACKAGE_NAME);
        copyFlowModuleDependencies(flowNodeDirectory);
        copyProjectFrontendResources(flowNodeDirectory);
    }

    private void copyFlowModuleDependencies(File flowNodeDirectory) {
        List<ArtifactData> projectArtifacts = project.getArtifacts().stream()
                .filter(artifact -> "jar".equals(artifact.getType()))
                .map(artifact -> new ArtifactData(artifact.getFile(),
                        artifact.getArtifactId(), artifact.getVersion()))
                .collect(Collectors.toList());

        ProductionModeCopyStep copyHelper = new ProductionModeCopyStep(
                new JarContentsManager(), projectArtifacts);
        for (String path : jarResourcePathsToCopy.split(",")) {
            copyHelper.copyFrontendJavaScriptFiles(flowNodeDirectory, includes,
                    path);
        }
    }

    private void copyProjectFrontendResources(File flowNodeDirectory) {
        final List<File> projectFrontendDirectories = Stream.of(
                new File(project.getBasedir(), "src/main/webapp/frontend"),
                new File(project.getBasedir(), "src/main/resources/META-INF/resources/frontend"),
                new File(project.getBasedir(), "src/main/resources/public/frontend"),
                new File(project.getBasedir(), "src/main/resources/static/frontend"),
                new File(project.getBasedir(), "src/main/resources/resources/frontend"),
                frontendResourcesDirectory)
            .distinct().filter(File::isDirectory).collect(Collectors.toList());

        if (projectFrontendDirectories.isEmpty()) {
            getLog().debug("Found no local frontend resources for the project");
        } else {
            for (File frontendDirectory : projectFrontendDirectories) {
                try {
                    FileUtils.copyDirectory(frontendDirectory,
                        flowNodeDirectory);
                } catch (IOException e) {
                    throw new UncheckedIOException(String.format(
                        "Failed to copy project frontend resources from '%s' to '%s'",
                        frontendDirectory, flowNodeDirectory), e);
                }
            }
        }
    }

    private File getWebpackOutputDirectory() {
        if(webpackOutputDirectory != null) {
            return webpackOutputDirectory;
        }

        Build buildInformation = project.getBuild();
        switch (project.getPackaging()) {
            case "jar":
                return new File(buildInformation.getOutputDirectory(),
                        "META-INF/resources");
            case "war":
                return new File(buildInformation.getDirectory(),
                        buildInformation.getFinalName());
            default:
                throw new IllegalStateException(String.format(
                        "Unsupported packaging '%s'", project.getPackaging()));
        }
    }
}
