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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.plugin.common.AnnotationValuesExtractor;
import com.vaadin.flow.plugin.common.FlowPluginFrontendUtils;
import com.vaadin.flow.plugin.common.WebComponentModulesGenerator;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeTasks;
import com.vaadin.flow.theme.Theme;

import static com.vaadin.flow.plugin.common.FlowPluginFrontendUtils.getClassFinder;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_GENERATED_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_IMPORTS_FILE;

/**
 * Goal that builds frontend bundle by:
 * <ul>
 * <li>Updating <code>package.json</code> file with the {@link NpmPackage}
 * annotations defined in the classpath,</li>
 * <li>Installing dependencies by running <code>npm install</code></li>
 * <li>Updating the {@link FrontendUtils#FLOW_IMPORTS_FILE} file imports with
 * the {@link JsModule} {@link Theme} and {@link JavaScript} annotations defined
 * in the classpath,</li>
 * <li>creating <code>webpack.config.js</code> if it does not exist yet, or
 * updating it otherwise</li>
 * </ul>
 */
@Mojo(name = "build-frontend", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class NodeBuildFrontendMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Enable or disable legacy components annotated only with
     * {@link HtmlImport}.
     */
    @Parameter(defaultValue = "true")
    private boolean convertHtml;

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File npmFolder;

    /**
     * The path to the {@literal node_modules} directory of the project.
     */
    @Parameter(defaultValue = "${project.basedir}/node_modules/")
    private File nodeModulesPath;

    /**
     * The JavaScript file used as entry point of the application, and which is
     * automatically updated by flow by reading java annotations.
     */
    @Parameter(defaultValue = "${project.build.directory}/" + FLOW_IMPORTS_FILE)
    private File generatedFlowImports;

    /**
     * A directory for generated frontend source files.
     */
    @Parameter(defaultValue = "${project.build.directory}/" + FLOW_GENERATED_FOLDER)
    private File generatedFrontendDirectory;

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
     * Whether to generate embeddable web components from
     * WebComponentExporter inheritors.
     */
    @Parameter(defaultValue = "true")
    private boolean generateEmbeddableWebComponents;

    @Override
    public void execute() {
        // Do nothing when bower mode
        if (FlowPluginFrontendUtils.isBowerMode()) {
            getLog().info("Skipped 'update-frontend' goal because 'vaadin.bowerMode' is set to true.");
            return;
        }

        long start = System.nanoTime();

        generateExportedWebComponents();

        runNodeUpdater();

        if (generateBundle) {
            runWebpack();
        }

        long ms = (System.nanoTime() - start) / 1000;
        getLog().info("update-frontend took " + ms + "ms.");
    }


    /**
     * Uses
     * {@link com.vaadin.flow.plugin.common.WebComponentModulesGenerator} to
     * generate JavaScript files from the {@code WebComponentExporters}
     * present in the code base. The generated JavaScript files are placed in
     * {@code ./target/frontend}.
     */
    private void generateExportedWebComponents() {
        if (!generateEmbeddableWebComponents) {
            return;
        }
        WebComponentModulesGenerator generator =
                new WebComponentModulesGenerator(new AnnotationValuesExtractor(
                        getClassFinder(project)), false);

        try {
            FileUtils.forceMkdir(generatedFrontendDirectory);
            generator.getExporters().forEach(exporter ->
                    generator.generateModuleFile(exporter, generatedFrontendDirectory));
        } catch (IOException e) {
            getLog().error("Failed to create a directory for generated web " +
                    "components", e);
        }
    }

    private void runNodeUpdater() {
        new NodeTasks.Builder(getClassFinder(project), frontendDirectory,
                generatedFrontendDirectory, generatedFlowImports, npmFolder,
                nodeModulesPath, convertHtml)
                .runNpmInstall(runNpmInstall)
                .enablePackagesUpdate(true)
                .enableImportsUpdate(true)
                .withEmbeddableWebComponents(generateEmbeddableWebComponents)
                .build().execute();
    }

    private void runWebpack() {
        String webpackCommand = "webpack/bin/webpack.js";
        File webpackExecutable = new File(nodeModulesPath, webpackCommand);
        if (!webpackExecutable.isFile()) {
            throw new IllegalStateException(String.format(
                    "Unable to locate webpack executable by path '%s'. Double" +
                            " check that the plugin is executed correctly",
                    webpackExecutable.getAbsolutePath()));
        }

        String nodePath  = FrontendUtils.getNodeExecutable();

        Process webpackLaunch = null;
        try {
            webpackLaunch =  new ProcessBuilder(nodePath,
                    webpackExecutable.getAbsolutePath()).directory(project.getBasedir())
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
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
                webpackLaunch::getErrorStream);
        throw new IllegalStateException(String.format(
                "Webpack process exited with non-zero exit code.%nStderr: '%s'",
                stderr));
    }

    private String readFullyAndClose(String readErrorMessage,
            Supplier<InputStream> inputStreamSupplier) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                inputStreamSupplier.get(), StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new UncheckedIOException(readErrorMessage, e);
        }
    }

}
