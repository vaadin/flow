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
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.plugin.base.BuildFrontendUtil;
import com.vaadin.flow.plugin.base.PluginAdapterBuild;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.theme.Theme;

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
public class BuildFrontendMojo extends FlowModeAbstractMojo
        implements PluginAdapterBuild {

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
    private File frontendResourcesDirectory;

    /**
     * Whether to use byte code scanner strategy to discover frontend
     * components.
     */
    @Parameter(defaultValue = "true")
    private boolean optimizeBundle;

    /**
     * Copy the `webpack.config.js` from the specified URL if missing. Default
     * is the template provided by this plugin. Set it to empty string to
     * disable the feature.
     */
    @Parameter(defaultValue = FrontendUtils.WEBPACK_CONFIG)
    private String webpackTemplate;

    /**
     * Copy the `webpack.generated.js` from the specified URL. Default is the
     * template provided by this plugin. Set it to empty string to disable the
     * feature.
     */
    @Parameter(defaultValue = FrontendUtils.WEBPACK_GENERATED)
    private String webpackGeneratedTemplate;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        long start = System.nanoTime();

        try {
            BuildFrontendUtil.runNodeUpdater(this);
        } catch (ExecutionFailedException | URISyntaxException exception) {
            throw new MojoFailureException(
                    "Could not execute build-frontend goal", exception);
        }

        if (generateBundle()) {
            try {
                BuildFrontendUtil.runFrontendBuild(this);
            } catch (URISyntaxException | TimeoutException exception) {
                throw new MojoExecutionException(exception.getMessage(),
                        exception);
            }
        }

        BuildFrontendUtil.updateBuildFile(this);

        long ms = (System.nanoTime() - start) / 1000000;
        getLog().info("update-frontend took " + ms + "ms.");
    }

    @Override
    public File frontendResourcesDirectory() {

        return frontendResourcesDirectory;
    }

    @Override
    public boolean generateBundle() {

        return generateBundle;
    }

    @Override
    public boolean generateEmbeddableWebComponents() {

        return generateEmbeddableWebComponents;
    }

    @Override
    public boolean optimizeBundle() {

        return optimizeBundle;
    }

    @Override
    public boolean runNpmInstall() {

        return runNpmInstall;
    }

    @Override
    public String webpackGeneratedTemplate() {

        return webpackGeneratedTemplate;
    }

    @Override
    public String webpackTemplate() {

        return webpackTemplate;
    }

}
