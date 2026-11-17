/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.quarkus.deployment.vaadinplugin;

import java.io.File;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.FrontendTools;

import static com.vaadin.flow.internal.FrontendUtils.FRONTEND;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;

/**
 * Build time configuration of the Vaadin Quarkus extension, read from the
 * {@code vaadin.build} properties.
 */
@ConfigMapping(prefix = "vaadin.build")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface VaadinBuildTimeConfig {

    /**
     * Gets if Vaadin Quarkus Plugins is enabled.
     *
     * @return {@code true} if the plugin is enabled
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Defines the project frontend directory from where resources should be
     * copied from for use with the frontend build tool.
     *
     * @return the frontend resources directory
     */
    @WithDefault(Constants.LOCAL_FRONTEND_RESOURCES_PATH)
    File frontendResourcesDirectory();

    /**
     * Whether to generate a bundle from the project frontend sources or not.
     * Defaults to {@literal true}
     *
     * @return {@code true} to generate the bundle
     */
    @WithDefault("true")
    boolean generateBundle();

    /**
     * Whether to generate embeddable web components from WebComponentExporter
     * inheritors.
     *
     * @return {@code true} to generate embeddable web components
     */
    @WithDefault("true")
    boolean generateEmbeddableWebComponents();

    /**
     * Whether to use byte code scanner strategy to discover frontend
     * components.
     *
     * @return {@code true} to use the byte code scanner
     */
    @WithDefault("true")
    boolean optimizeBundle();

    /**
     * Whether to run npm install after updating dependencies.
     *
     * @return {@code true} to run npm install
     */
    @WithDefault("true")
    boolean runNpmInstall();

    /**
     * Setting this to true will run npm ci instead of npm install when using
     * npm. If using pnpm, the install will be run with --frozen-lockfile
     * parameter. This makes sure that the versions in package lock file will
     * not be overwritten and production builds are reproducible.
     *
     * @return {@code true} to run a CI build
     */
    @WithDefault("false")
    boolean ciBuild();

    /**
     * Setting this to true will force a build of the production build even if
     * there is a default production bundle that could be used. Created
     * production bundle optimization is defined by optimizeBundle parameter.
     *
     * @return {@code true} to force the production build
     */
    @WithDefault("false")
    boolean forceProductionBuild();

    /**
     * Control cleaning of generated frontend files when executing
     * 'build-frontend'. Mainly this is wanted to be true which it is by
     * default.
     *
     * @return {@code true} to clean the generated frontend files
     */
    @WithDefault("true")
    boolean cleanFrontendFiles();

    /**
     * Application properties file in Quarkus project.
     *
     * @return the application properties file
     */
    @WithDefault("src/main/resources/application.properties")
    File applicationProperties();

    /**
     * Whether to insert the initial UIDL object in the bootstrap index.html
     *
     * @return {@code true} to insert the initial UIDL object
     */
    @WithDefault("false")
    boolean eagerServerLoad();

    /**
     * A directory with project's frontend source files.
     *
     * @return the frontend directory
     */
    @WithDefault("src/main/" + FRONTEND)
    File frontendDirectory();

    /**
     * The folder where flow will put TS API files for client projects.
     *
     * @return the folder for generated TypeScript files, if set
     */
    Optional<File> generatedTsFolder();

    /**
     * Download node. js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to null which will cause the downloader to use
     * NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT.
     *
     * Example: {@literal https://nodejs.org/dist/}
     *
     * @return the Node.js download root URL, if set
     */
    Optional<String> nodeDownloadRoot();

    /**
     * The node. js version to be used when node. js is installed automatically
     * by Vaadin, for example `"v16.0.0"`. Defaults to the Vaadin-default node
     * version - see FrontendTools for details.
     *
     * @return the Node.js version
     */
    @WithDefault(FrontendTools.DEFAULT_NODE_VERSION)
    String nodeVersion();

    /**
     * The folder containing the Node.js executable.
     * <p>
     * When set, Node.js will be exclusively used from this folder. If not
     * found, the build will fail. If not provided, Vaadin will try to detect an
     * existing compatible Node.js installation or install it in Vaadin home
     * directory.
     *
     * @return the Node.js folder, if set
     */
    Optional<File> nodeFolder();

    /**
     * The folder where `package. json` file is located. Default is project root
     * dir.
     *
     * @return the folder of {@code package.json}, if set
     */
    Optional<File> npmFolder();

    /**
     * Default generated path of the OpenAPI json.
     *
     * @return the OpenAPI json file
     */
    @WithDefault("generated-resources/openapi.json")
    File openApiJsonFile();

    /**
     * Instructs to use pnpm for installing npm frontend resources.
     *
     * @return {@code true} to use pnpm
     */
    @WithDefault("" + Constants.ENABLE_PNPM_DEFAULT)
    boolean pnpmEnable();

    /**
     * Instructs to use bun for installing npm frontend resources.
     *
     * @return {@code true} to use bun
     */
    @WithDefault("" + Constants.ENABLE_BUN_DEFAULT)
    boolean bunEnable();

    /**
     * Instructs to use globally installed pnpm tool or the default supported
     * pnpm version.
     *
     * @return {@code true} to use the globally installed pnpm
     */
    @WithDefault("" + Constants.GLOBAL_PNPM_DEFAULT)
    boolean useGlobalPnpm();

    /**
     * Whether vaadin home node executable usage is forced. If it's set to true
     * then vaadin home 'node' is checked and installed if it's absent. Then it
     * will be used instead of globally 'node' or locally installed 'node'.
     *
     * @return {@code true} to require the Node.js executable in Vaadin home
     */
    @WithDefault("" + Constants.DEFAULT_REQUIRE_HOME_NODE_EXECUTABLE)
    boolean requireHomeNodeExec();

    /**
     * Defines the output directory for generated non-served resources, such as
     * the token file.
     *
     * @return the output directory for generated non-served resources
     */
    @WithDefault(VAADIN_SERVLET_RESOURCES)
    File generatedResourceOutputDirectory();

    /**
     * The resources output directory for META-INF/resources in the classes
     * output directory.
     *
     * @return the META-INF/resources directory, usually
     *         {output}/classes/META-INF/resources
     */
    @WithDefault("META-INF/resources/")
    File resourcesOutputDirectory();

    /**
     * The folder where the frontend build tool should output index. js and
     * other generated files.
     *
     * @return the frontend build output directory, if set
     * @deprecated use {@link #frontendOutputDirectory()}
     */
    @Deprecated(since = "24.8", forRemoval = true)
    Optional<File> webpackOutputDirectory();

    /**
     * The folder where the frontend build tool should output index. js and
     * other generated files.
     *
     * @return the frontend build output directory
     */
    @WithDefault(Constants.VAADIN_WEBAPP_RESOURCES)
    File frontendOutputDirectory();

    /**
     * Additional npm packages to run post install scripts for.
     * <p>
     * Post install is automatically run for internal dependencies which rely on
     * post install scripts to work, e.g. esbuild.
     *
     * @return the additional packages to run post install scripts for, if set
     */
    Optional<List<String>> postinstallPackages();

    /**
     * Npm packages to exclude from running post install scripts.
     * <p>
     * Used to skip built-in entries (e.g. {@code esbuild}) when their
     * postinstall step is known to fail or is not needed.
     *
     * @return the packages to exclude from post install scripts, if set
     */
    Optional<List<String>> excludePostinstallPackages();

    /**
     * Whether to disable dev bundle rebuild.
     *
     * @return {@code true} to skip the dev bundle rebuild
     */
    @WithDefault("false")
    boolean skipDevBundleBuild();

    /**
     * Whether to enable react
     *
     * @return whether React is enabled, if set
     */
    Optional<Boolean> reactEnabled();

    /**
     * Identifier for the application. If not specified, defaults to the hashed
     * value of 'groupId:artifactId'.
     *
     * @return the application identifier, if set
     */
    Optional<String> applicationIdentifier();

    /**
     * Parameter for adding file extensions to handle when generating bundles.
     * Hashes are calculated for these files as part of detecting if a new
     * bundle should be generated. From the commandline use comma separated list
     * -Ddevmode.frontendExtraFileExtensions="svg,ico" In plugin configuration
     * use comma separated values svg,ico
     *
     * @return the additional file extensions, if set
     */
    Optional<List<String>> frontendExtraFileExtensions();

    /**
     * Whether to exclude npm packages for web components.
     *
     * @return {@code true} to exclude npm packages for web components
     */
    @WithDefault("false")
    boolean npmExcludeWebComponents();

    /**
     * Set to {@code true} to ignore node/npm tool version checks.
     * <p>
     * </p>
     * Note that disabling frontend tools version checking could cause failing
     * builds and other issues that are difficult to debug.
     *
     * @return {@code true} to ignore the version checks
     */
    @WithDefault("false")
    boolean frontendIgnoreVersionChecks();

    /**
     * Allows building a version of the application with a commercial banner
     * when commercial components are used without a license key.
     *
     * @return {@code true} to allow building with a commercial banner
     */
    @WithDefault("false")
    boolean commercialWithBanner();

    /**
     * Minimum age (in days) a frontend (npm) package version must have before
     * npm, pnpm or bun is allowed to install it. Mitigates supply-chain attacks
     * where a compromised version is briefly available on the registry. Set to
     * {@code 0} to disable. Requires pnpm &ge; 10.16.0 or bun &ge; 1.3.0 when
     * those tools are used.
     * <p>
     * When not set, the value configured for npm or pnpm itself ({@code .npmrc}
     * or {@code pnpm-workspace.yaml}) is used, so that a manually run
     * {@code npm install} behaves the same way. Only when there is no such
     * value does the check default to {@code 1} day. The configuration of bun
     * cannot be read, so the default always applies for it.
     *
     * @return the minimum package age in days, if set
     */
    Optional<Integer> minimumFrontendPackageAgeDays();
}
