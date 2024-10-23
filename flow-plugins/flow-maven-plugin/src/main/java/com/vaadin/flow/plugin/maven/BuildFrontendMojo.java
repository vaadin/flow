/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
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
 * <li>Update {@link FrontendUtils#VITE_CONFIG} file.</li>
 * </ul>
 *
 * @since 2.0
 */
@Mojo(name = "build-frontend", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class BuildFrontendMojo extends FlowModeAbstractMojo {

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
     * copied from for use with the frontend build tool.
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
     * Setting this to true will run {@code npm ci} instead of
     * {@code npm install} when using npm.
     *
     * If using pnpm, the install will be run with {@code --frozen-lockfile}
     * parameter.
     *
     * This makes sure that the versions in package lock file will not be
     * overwritten and production builds are reproducible.
     */
    @Parameter(property = InitParameters.CI_BUILD, defaultValue = "false")
    private boolean ciBuild;

    /**
     * Setting this to {@code true} will force a build of the production build
     * even if there is a default production bundle that could be used.
     *
     * Created production bundle optimization is defined by
     * {@link #optimizeBundle} parameter.
     */
    @Parameter(property = InitParameters.FORCE_PRODUCTION_BUILD, defaultValue = "false")
    private boolean forceProductionBuild;

    /**
     * Control cleaning of generated frontend files when executing
     * 'build-frontend'.
     *
     * Mainly this is wanted to be true which it is by default.
     */
    @Parameter(property = InitParameters.CLEAN_BUILD_FRONTEND_FILES, defaultValue = "true")
    private boolean cleanFrontendFiles;

    @Override
    protected Class<?> taskClass(Reflector reflector)
            throws ClassNotFoundException {
        return reflector.loadClass(BuildFrontendTask.class.getName());
    }
}
