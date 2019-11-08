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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeTasks;
import com.vaadin.flow.theme.Theme;

import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.plugin.common.FlowPluginFrontendUtils.getClassFinder;
import static com.vaadin.flow.server.Constants.FRONTEND_TOKEN;
import static com.vaadin.flow.server.Constants.GENERATED_TOKEN;
import static com.vaadin.flow.server.Constants.NPM_TOKEN;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_ENABLE_DEV_SERVER;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;

/**
 * Goal that builds the frontend bundle.
 *
 * It performs the following actions when creating a package:
 * <ul>
 * <li>Update {@link Constants#PACKAGE_JSON} file with the {@link NpmPackage}
 * annotations defined in the classpath,</li>
 * <li>Copy resource files used by flow from `.jar` files to the `node_modules`
 * folder</li>
 * <li>Install dependencies by running <code>npm install</code></li>
 * <li>Update the {@link FrontendUtils#IMPORTS_NAME} file imports with the
 * {@link JsModule} {@link Theme} and {@link JavaScript} annotations defined in
 * the classpath,</li>
 * <li>Update {@link FrontendUtils#WEBPACK_CONFIG} file.</li>
 * </ul>
 *
 * @since 2.0
 */
@Mojo(name = "build-frontend", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class BuildFrontendMojo extends FlowModeAbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File npmFolder;

    /**
     * The JavaScript file used as entry point of the application, and which is
     * automatically updated by flow by reading java annotations.
     */
    @Parameter(defaultValue = "${project.build.directory}/" + FRONTEND)
    private File generatedFolder;

    /**
     * A directory with project's frontend source files.
     */
    @Parameter(defaultValue = "${project.basedir}/frontend")
    private File frontendDirectory;

    /**
     * Whether to generate a bundle from the project frontend sources or not.
     */
    @Parameter(defaultValue = "true")
    private boolean generateBundle;

    /**
     * Whether to run <code>npm install</code> after updating dependencies.
     */
    @Parameter(defaultValue = "true")
    private boolean runNpmInstall;

    /**
     * Whether to generate embeddable web components from WebComponentExporter
     * inheritors.
     */
    @Parameter(defaultValue = "true")
    private boolean generateEmbeddableWebComponents;

    /**
     * Defines the project frontend directory from where resources should be
     * copied from for use with webpack.
     */
    @Parameter(defaultValue = "${project.basedir}/"
            + Constants.LOCAL_FRONTEND_RESOURCES_PATH)
    protected File frontendResourcesDirectory;

    @Parameter
    private String polymerVersion;

    /**
     * Whether to use byte code scanner strategy to discover frontend
     * components.
     */
    @Parameter(defaultValue = "true")
    private boolean optimizeBundle;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        // Do nothing when compatibility mode
        if (compatibility) {
            getLog().info(
                    "Skipped 'build-frontend' goal because compatibility mode is set to true.");
            return;
        }

        updateBuildFile();

        long start = System.nanoTime();

        try {
            runNodeUpdater();
        } catch (ExecutionFailedException exception) {
            throw new MojoFailureException(
                    "Could not execute build-frontend goal", exception);
        }

        if (generateBundle) {
            try {
                runWebpack();
            } catch (IllegalStateException exception) {
                throw new MojoExecutionException(exception.getMessage(),
                        exception);
            }
        }

        long ms = (System.nanoTime() - start) / 1000000;
        getLog().info("update-frontend took " + ms + "ms.");
    }

    private void runNodeUpdater() throws ExecutionFailedException {
        Set<File> jarFiles = project.getArtifacts().stream()
                .filter(artifact -> "jar".equals(artifact.getType()))
                .map(Artifact::getFile).collect(Collectors.toSet());

        new NodeTasks.Builder(getClassFinder(project), npmFolder,
                generatedFolder, frontendDirectory).runNpmInstall(runNpmInstall)
                        .enablePackagesUpdate(true)
                        .useByteCodeScanner(optimizeBundle)
                        .copyResources(jarFiles)
                        .copyLocalResources(frontendResourcesDirectory)
                        .enableImportsUpdate(true)
                        .withEmbeddableWebComponents(
                                generateEmbeddableWebComponents)
                        .withTokenFile(getTokenFile())
                        .withPolymerVersion(polymerVersion).build().execute();
    }

    private void runWebpack() {
        String webpackCommand = "webpack/bin/webpack.js";
        File webpackExecutable = new File(npmFolder,
                NODE_MODULES + webpackCommand);
        if (!webpackExecutable.isFile()) {
            throw new IllegalStateException(String.format(
                    "Unable to locate webpack executable by path '%s'. Double"
                            + " check that the plugin is executed correctly",
                    webpackExecutable.getAbsolutePath()));
        }

        String nodePath = FrontendUtils
                .getNodeExecutable(npmFolder.getAbsolutePath());

        Process webpackLaunch = null;
        try {
            getLog().info("Running webpack ...");
            webpackLaunch = new ProcessBuilder(nodePath,
                    webpackExecutable.getAbsolutePath())
                            .directory(project.getBasedir())
                            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                            .start();
            int errorCode = webpackLaunch.waitFor();
            if (errorCode != 0) {
                readDetailsAndThrowException(webpackLaunch);
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(
                    "Failed to run webpack due to an error", e);
        } finally {
            if (webpackLaunch != null) {
                webpackLaunch.destroyForcibly();
            }
        }
    }

    private void readDetailsAndThrowException(Process webpackLaunch) {
        String stderr = readFullyAndClose(
                "Failed to read webpack process stderr",
                webpackLaunch.getErrorStream());
        throw new IllegalStateException(String.format(
                "Webpack process exited with non-zero exit code.%nStderr: '%s'",
                stderr));
    }

    private String readFullyAndClose(String readErrorMessage,
            InputStream inputStreamSupplier) {
        try {
            return IOUtils.toString(inputStreamSupplier, StandardCharsets.UTF_8)
                    .replaceAll("\\R", System.lineSeparator());
        } catch (IOException e) {
            throw new UncheckedIOException(readErrorMessage, e);
        }
    }

    /**
     * Add the devMode token to build token file so we don't try to start the
     * dev server. Remove the abstract folder paths as they should not be used
     * for prebuilt bundles.
     */
    private void updateBuildFile() {
        File tokenFile = getTokenFile();
        if (!tokenFile.exists()) {
            getLog().warn(
                    "Couldn't update devMode token due to missing token file.");
            return;
        }
        try {
            String json = FileUtils.readFileToString(tokenFile,
                    StandardCharsets.UTF_8.name());
            JsonObject buildInfo = JsonUtil.parse(json);

            buildInfo.remove(NPM_TOKEN);
            buildInfo.remove(GENERATED_TOKEN);
            buildInfo.remove(FRONTEND_TOKEN);

            buildInfo.put(SERVLET_PARAMETER_ENABLE_DEV_SERVER, false);
            FileUtils.write(tokenFile, JsonUtil.stringify(buildInfo, 2) + "\n",
                    StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            getLog().warn("Unable to read token file", e);
        }
    }

    @Override
    boolean isDefaultCompatibility() {
        File tokenFile = getTokenFile();
        if (!tokenFile.exists()) {
            getLog().warn("'build-frontend' goal was called without previously "
                    + "calling 'prepare-frontend'");
            return true;
        }
        try {
            String json = FileUtils.readFileToString(tokenFile,
                    StandardCharsets.UTF_8.name());
            JsonObject buildInfo = JsonUtil.parse(json);
            return buildInfo.hasKey(SERVLET_PARAMETER_COMPATIBILITY_MODE)
                    ? buildInfo.getBoolean(SERVLET_PARAMETER_COMPATIBILITY_MODE)
                    : true;
        } catch (IOException e) {
            getLog().warn("Unable to read token file", e);
            return true;
        }
    }

    private File getTokenFile() {
        return new File(webpackOutputDirectory, TOKEN_FILE);
    }
}
