/*
 * Copyright 2000-2021 Vaadin Ltd.
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

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import elemental.json.JsonObject;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_GENERATED_DIR;

/**
 * An executor that it's run when the servlet context is initialised in dev-mode
 * or when flow-maven-plugin goals are run. It can chain a set of task to run.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class NodeTasks implements FallibleCommand {

    /**
     * Build a <code>NodeExecutor</code> instance.
     */
    public static class Builder implements Serializable {

        private final String buildDirectory;

        private final ClassFinder classFinder;

        private final File frontendDirectory;

        private File webappResourcesDirectory = null;

        private File resourceOutputDirectory = null;

        private String webpackTemplate = null;

        private String webpackGeneratedTemplate = null;

        private boolean enablePackagesUpdate = false;

        private boolean createMissingPackageJson = false;

        private boolean enableImportsUpdate = false;

        private boolean runNpmInstall = false;

        private Set<File> jarFiles = null;

        private boolean generateEmbeddableWebComponents = true;

        private boolean cleanNpmFiles = false;

        private File flowResourcesFolder = null;

        private File localResourcesFolder = null;

        private boolean useByteCodeScanner = false;

        private JsonObject tokenFileData;

        private File tokenFile;

        private boolean enablePnpm;

        private boolean useGlobalPnpm = false;

        private File fusionJavaSourceFolder;

        private File fusionGeneratedOpenAPIFile;

        private File fusionApplicationProperties;

        private File fusionClientAPIFolder;

        private boolean requireHomeNodeExec;

        private boolean copyTemplates = false;

        /**
         * Directory for npm and folders and files.
         */
        private final File npmFolder;

        /**
         * Directory where generated files are written.
         */
        private final File generatedFolder;

        /**
         * Is in client-side bootstrapping mode.
         */
        private boolean useDeprecatedV14Bootstrapping;

        /**
         * The node.js version to be used when node.js is installed
         * automatically by Vaadin, for example <code>"v16.0.0"</code>. Defaults
         * to {@value FrontendTools#DEFAULT_NODE_VERSION}.
         */
        private String nodeVersion = FrontendTools.DEFAULT_NODE_VERSION;

        /**
         * Download node.js from this URL. Handy in heavily firewalled corporate
         * environments where the node.js download can be provided from an
         * intranet mirror. Defaults to
         * {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
         */
        private URI nodeDownloadRoot = URI
                .create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT);

        private boolean nodeAutoUpdate = false;

        private Lookup lookup;

        /**
         * Default is true here so we do not accidentally include development
         * stuff into production.
         */
        private boolean productionMode = true;

        /**
         * The resource folder for java resources.
         */
        private File javaResourceFolder;

        /**
         * Create a builder instance given an specific npm folder.
         *
         * @param lookup
         *            a {@link Lookup} to discover services used by Flow (SPI)
         * @param npmFolder
         *            folder with the `package.json` file
         * @param buildDirectory
         *            project build directory
         */
        public Builder(Lookup lookup, File npmFolder, String buildDirectory) {
            this(lookup, npmFolder,
                    new File(npmFolder,
                            System.getProperty(PARAM_GENERATED_DIR,
                                    Paths.get(buildDirectory,
                                            DEFAULT_GENERATED_DIR).toString())),
                    buildDirectory);
        }

        /**
         * Create a builder instance with custom npmFolder and generatedPath
         *
         * @param lookup
         *            a {@link Lookup} to discover services used by Flow (SPI)
         * @param npmFolder
         *            folder with the `package.json` file
         * @param generatedPath
         *            folder where flow generated files will be placed.
         * @param buildDirectory
         *            project build directory
         */
        public Builder(Lookup lookup, File npmFolder, File generatedPath,
                String buildDirectory) {
            this(lookup, npmFolder, generatedPath, new File(npmFolder, System
                    .getProperty(PARAM_FRONTEND_DIR, DEFAULT_FRONTEND_DIR)),
                    buildDirectory);
        }

        /**
         * Create a builder instance with all parameters.
         *
         * @param lookup
         *            a {@link Lookup} to discover services used by Flow (SPI)
         * @param npmFolder
         *            folder with the `package.json` file
         * @param generatedPath
         *            folder where flow generated files will be placed.
         * @param frontendDirectory
         *            a directory with project's frontend files
         * @param buildDirectory
         *            project build directory
         */
        public Builder(Lookup lookup, File npmFolder, File generatedPath,
                File frontendDirectory, String buildDirectory) {
            this.lookup = lookup;
            this.classFinder = lookup.lookup(ClassFinder.class);
            this.npmFolder = npmFolder;
            this.generatedFolder = generatedPath.isAbsolute() ? generatedPath
                    : new File(npmFolder, generatedPath.getPath());
            this.frontendDirectory = frontendDirectory.isAbsolute()
                    ? frontendDirectory
                    : new File(npmFolder, frontendDirectory.getPath());
            this.buildDirectory = buildDirectory;
        }

        /**
         * Creates a <code>NodeExecutor</code> using this configuration.
         *
         * @return a <code>NodeExecutor</code> instance
         */
        public NodeTasks build() {
            return new NodeTasks(this);
        }

        /**
         * Sets the webpack related properties.
         *
         * @param webappResourcesDirectory
         *            the directory to set for webpack to output its build
         *            results, meant for serving from context root.
         * @param resourceOutputDirectory
         *            the directory to output generated non-served resources,
         *            such as the "config/stats.json" stats file, and the
         *            "config/flow-build-info.json" token file.
         * @param webpackTemplate
         *            name of the webpack resource to be used as template when
         *            creating the <code>webpack.config.js</code> file.
         * @param webpackGeneratedTemplate
         *            name of the webpack resource to be used as template when
         *            creating the <code>webpack.generated.js</code> file.
         * @return this builder
         */
        public Builder withWebpack(File webappResourcesDirectory,
                File resourceOutputDirectory, String webpackTemplate,
                String webpackGeneratedTemplate) {
            this.webappResourcesDirectory = webappResourcesDirectory;
            this.resourceOutputDirectory = resourceOutputDirectory;
            this.webpackTemplate = webpackTemplate;
            this.webpackGeneratedTemplate = webpackGeneratedTemplate;
            return this;
        }

        /**
         * Sets whether to enable packages and webpack file updates. Default is
         * <code>true</code>.
         *
         * @param enablePackagesUpdate
         *            <code>true</code> to enable packages and webpack update,
         *            otherwise <code>false</code>
         * @return this builder
         */
        public Builder enablePackagesUpdate(boolean enablePackagesUpdate) {
            this.enablePackagesUpdate = enablePackagesUpdate;
            return this;
        }

        /**
         * Sets whether to perform always perform clean up procedure. Default is
         * <code>false</code>. When the value is false, npm related files will
         * only be removed when a platform version update is detected.
         *
         * @param forceClean
         *            <code>true</code> to clean npm files always, otherwise
         *            <code>false</code>
         * @return this builder
         */
        // This method is only used in tests ...
        Builder enableNpmFileCleaning(boolean forceClean) {
            this.cleanNpmFiles = forceClean;
            return this;
        }

        /**
         * Sets whether to enable imports file update. Default is
         * <code>false</code>. This will also enable creation of missing package
         * files if set to true.
         *
         * @param enableImportsUpdate
         *            <code>true</code> to enable imports file update, otherwise
         *            <code>false</code>
         * @return this builder
         */
        public Builder enableImportsUpdate(boolean enableImportsUpdate) {
            this.enableImportsUpdate = enableImportsUpdate;
            this.createMissingPackageJson = enableImportsUpdate
                    || createMissingPackageJson;
            return this;
        }

        /**
         * Sets whether run <code>npm install</code> after updating
         * dependencies.
         *
         * @param runNpmInstall
         *            run npm install. Default is <code>false</code>
         * @return the builder
         */
        public Builder runNpmInstall(boolean runNpmInstall) {
            this.runNpmInstall = runNpmInstall;
            return this;
        }

        /**
         * Sets the appropriate npm package folder for copying flow resources in
         * jars.
         *
         * @param flowResourcesFolder
         *            target folder
         * @return the builder
         */
        public Builder withFlowResourcesFolder(File flowResourcesFolder) {
            this.flowResourcesFolder = flowResourcesFolder.isAbsolute()
                    ? flowResourcesFolder
                    : new File(npmFolder, flowResourcesFolder.getPath());
            return this;
        }

        /**
         * Sets whether copy resources from classpath to the appropriate npm
         * package folder so as they are available for webpack build.
         *
         * @param jars
         *            set of class nodes to be visited. Not {@code null}
         *
         * @return the builder
         */
        public Builder copyResources(Set<File> jars) {
            Objects.requireNonNull(jars, "Parameter 'jars' must not be null!");
            this.jarFiles = jars;
            return this;
        }

        /**
         * Sets whether copy templates to
         * {@code META-INF/VAADIN/config/templates}.
         *
         * @param copyTemplates
         *            whether to copy templates
         *
         * @return the builder
         */
        public Builder copyTemplates(boolean copyTemplates) {
            this.copyTemplates = copyTemplates;
            return this;
        }

        /**
         * Sets whether to collect and package
         * {@link com.vaadin.flow.component.WebComponentExporter} dependencies.
         *
         * @param generateEmbeddableWebComponents
         *            collect dependencies. Default is {@code true}
         * @return the builder
         */
        public Builder withEmbeddableWebComponents(
                boolean generateEmbeddableWebComponents) {
            this.generateEmbeddableWebComponents = generateEmbeddableWebComponents;
            return this;
        }

        /**
         * Sets whether to create the package file if missing.
         *
         * @param create
         *            create the package
         * @return the builder
         */
        public Builder createMissingPackageJson(boolean create) {
            this.createMissingPackageJson = create;
            return this;
        }

        /**
         * Set local frontend files to be copied from given folder.
         *
         * @param localResourcesFolder
         *            folder to copy local frontend files from
         * @return the builder, for chaining
         */
        public Builder copyLocalResources(File localResourcesFolder) {
            this.localResourcesFolder = localResourcesFolder;
            return this;
        }

        /**
         * Use V14 bootstrapping that disables index.html entry point.
         *
         * @param useDeprecatedV14Bootstrapping
         *            <code>true</code> to use legacy V14 bootstrapping
         * @return the builder, for chaining
         */
        public Builder useV14Bootstrap(boolean useDeprecatedV14Bootstrapping) {
            this.useDeprecatedV14Bootstrapping = useDeprecatedV14Bootstrapping;
            return this;
        }

        /**
         * Set the folder where Ts files should be generated.
         *
         * @param fusionClientTsApiFolder
         *            folder for Ts files in the frontend.
         * @return the builder, for chaining
         */
        public Builder withFusionClientAPIFolder(File fusionClientTsApiFolder) {
            this.fusionClientAPIFolder = fusionClientTsApiFolder;
            return this;
        }

        /**
         * Set application properties file for Spring project.
         *
         * @param applicationProperties
         *            application properties file.
         * @return this builder, for chaining
         */
        public Builder withFusionApplicationProperties(
                File applicationProperties) {
            this.fusionApplicationProperties = applicationProperties;
            return this;
        }

        /**
         * Set output location for the generated OpenAPI file.
         *
         * @param generatedOpenAPIFile
         *            the generated output file.
         * @return the builder, for chaining
         */
        public Builder withFusionGeneratedOpenAPIJson(
                File generatedOpenAPIFile) {
            this.fusionGeneratedOpenAPIFile = generatedOpenAPIFile;
            return this;
        }

        /**
         * Set source paths that OpenAPI generator searches for endpoints.
         *
         * @param fusionJavaSourceFolder
         *            java source folder
         * @return the builder, for chaining
         */
        public Builder withFusionJavaSourceFolder(File fusionJavaSourceFolder) {
            this.fusionJavaSourceFolder = fusionJavaSourceFolder;
            return this;
        }

        /**
         * Sets frontend scanner strategy: byte code scanning strategy is used
         * if {@code byteCodeScanner} is {@code true}, full classpath scanner
         * strategy is used otherwise (by default).
         *
         * @param byteCodeScanner
         *            if {@code true} then byte code scanner is used, full
         *            scanner is used otherwise (by default).
         * @return the builder, for chaining
         */
        public Builder useByteCodeScanner(boolean byteCodeScanner) {
            this.useByteCodeScanner = byteCodeScanner;
            return this;
        }

        /**
         * Fill token file data into the provided {@code object}.
         *
         * @param object
         *            the object to fill with token file data
         * @return the builder, for chaining
         */
        public Builder populateTokenFileData(JsonObject object) {
            tokenFileData = object;
            return this;
        }

        /**
         * Sets the token file (flow-build-info.json) path.
         *
         * @param tokenFile
         *            token file path
         * @return the builder, for chaining
         */
        public Builder withTokenFile(File tokenFile) {
            this.tokenFile = tokenFile;
            return this;
        }

        /**
         * Enables pnpm tool.
         * <p>
         * "pnpm" will be used instead of "npm".
         *
         * @param enable
         *            enables pnpm.
         * @return the builder, for chaining
         */
        public Builder enablePnpm(boolean enable) {
            enablePnpm = enable;
            return this;
        }

        /**
         * Uses globally installed pnpm tool for frontend packages installation.
         * 
         * @param useGlobalPnpm
         *            uses globally installed pnpm instead of default one, see
         *            {@link FrontendTools#DEFAULT_PNPM_VERSION}.
         * @return the builder, for chaining
         */
        public Builder useGlobalPnpm(boolean useGlobalPnpm) {
            this.useGlobalPnpm = useGlobalPnpm;
            return this;
        }

        /**
         * Requires node executable to be installed in vaadin home folder.
         *
         * @param requireHomeNodeExec
         *            requires vaadin home node exec
         * @return the builder, for chaining
         */
        public Builder withHomeNodeExecRequired(boolean requireHomeNodeExec) {
            this.requireHomeNodeExec = requireHomeNodeExec;
            return this;
        }

        /**
         * Sets the node.js version to be used when node.js is installed
         * automatically by Vaadin, for example <code>"v16.0.0"</code>. Defaults
         * to {@value FrontendTools#DEFAULT_NODE_VERSION}.
         *
         * @param nodeVersion
         *            the new node version to download, not null.
         * @return the builder, for chaining
         */
        public Builder withNodeVersion(String nodeVersion) {
            this.nodeVersion = Objects.requireNonNull(nodeVersion);
            return this;
        }

        /**
         * Sets the download node.js URL. Handy in heavily firewalled corporate
         * environments where the node.js download can be provided from an
         * intranet mirror. Defaults to
         * {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
         *
         * @param nodeDownloadRoot
         *            the new download URL to set, not null.
         * @return the builder, for chaining
         */
        public Builder withNodeDownloadRoot(URI nodeDownloadRoot) {
            this.nodeDownloadRoot = Objects.requireNonNull(nodeDownloadRoot);
            return this;
        }

        /**
         * Sets the production mode.
         *
         * @param productionMode
         *            <code>true</code> to enable production mode, otherwise
         *            <code>false</code>
         * @return this builder
         */
        public Builder withProductionMode(boolean productionMode) {
            this.productionMode = productionMode;
            return this;
        }

        /**
         * Sets whether it is fine to automatically update the alternate node
         * installation if installed version is older than the current default.
         *
         * @param update
         *            true to update alternate node when used
         * @return the builder
         */
        public Builder setNodeAutoUpdate(boolean update) {
            this.nodeAutoUpdate = update;
            return this;
        }

        /**
         * Get the npm folder used for this build.
         *
         * @return npmFolder
         */
        public File getNpmFolder() {
            return npmFolder;
        }

        /**
         * Get the generated folder for this build.
         *
         * @return generatedFolder
         */
        public File getGeneratedFolder() {
            return generatedFolder;
        }

        /**
         * Get the output directory for webpack output.
         * 
         * @return webpackOutputDirectory
         */
        public File getWebappResourcesDirectory() {
            return webappResourcesDirectory;
        }

        /**
         * Get the defined frontend directory.
         * 
         * @return frontendDirectory
         */
        public File getFrontendDirectory() {
            return frontendDirectory;
        }

        /**
         * Get the name of the used build directory.
         * <p>
         * By default this will be {@code target} for maven and {@code build}
         * for gradle.
         *
         * @return buildDirectory
         */
        public String getBuildDirectory() {
            return buildDirectory;
        }

        /**
         * Set the java resources folder to be checked for feature file.
         * <p>
         * Needed for plugin execution.
         *
         * @param javaResourceFolder
         *            java resources folder
         * @return this builder
         */
        public Builder setJavaResourceFolder(File javaResourceFolder) {
            this.javaResourceFolder = javaResourceFolder;
            return this;
        }

        protected FeatureFlags getFeatureFlags() {
            final FeatureFlags featureFlags = new FeatureFlags(lookup);
            if (javaResourceFolder != null) {
                featureFlags.setPropertiesLocation(javaResourceFolder);
            }
            return featureFlags;
        }
    }

    // @formatter:off
    // This list keeps the tasks in order so that they are executed
    // without depending on when they are added.
    private static final List<Class<? extends FallibleCommand>> commandOrder =
        Collections.unmodifiableList(Arrays.asList(
            TaskGeneratePackageJson.class,
            TaskGenerateIndexHtml.class,
            TaskGenerateIndexTs.class,
            TaskGenerateViteDevMode.class,
            TaskGenerateTsConfig.class,
            TaskGenerateTsDefinitions.class,
            TaskGenerateServiceWorker.class,
            TaskGenerateOpenAPI.class,
            TaskGenerateFusion.class,
            TaskGenerateBootstrap.class,
            TaskInstallWebpackPlugins.class,
            TaskUpdatePackages.class,
            TaskRunNpmInstall.class,
            TaskCopyFrontendFiles.class,
            TaskCopyLocalFrontendFiles.class,
            TaskUpdateSettingsFile.class,
            TaskUpdateWebpack.class,
            TaskUpdateVite.class,
            TaskUpdateImports.class,
            TaskUpdateThemeImport.class,
            TaskCopyTemplateFiles.class
        ));
    // @formatter:on

    private final List<FallibleCommand> commands = new ArrayList<>();

    private NodeTasks(Builder builder) {

        ClassFinder classFinder = new ClassFinder.CachedClassFinder(
                builder.classFinder);
        FrontendDependenciesScanner frontendDependencies = null;

        boolean enableWebpackConfigUpdate = builder.webpackTemplate != null
                && !builder.webpackTemplate.isEmpty();

        final FeatureFlags featureFlags = builder.getFeatureFlags();

        if (builder.enablePackagesUpdate || builder.enableImportsUpdate
                || enableWebpackConfigUpdate) {
            frontendDependencies = new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                    .createScanner(!builder.useByteCodeScanner, classFinder,
                            builder.generateEmbeddableWebComponents,
                            builder.useDeprecatedV14Bootstrapping);

            if (builder.generateEmbeddableWebComponents) {
                FrontendWebComponentGenerator generator = new FrontendWebComponentGenerator(
                        classFinder);
                generator.generateWebComponents(builder.generatedFolder,
                        frontendDependencies.getThemeDefinition());
            }

            TaskUpdatePackages packageUpdater = null;
            if (builder.enablePackagesUpdate
                    && builder.flowResourcesFolder != null) {
                packageUpdater = new TaskUpdatePackages(classFinder,
                        frontendDependencies, builder.npmFolder,
                        builder.generatedFolder, builder.flowResourcesFolder,
                        builder.cleanNpmFiles, builder.enablePnpm,
                        builder.buildDirectory, featureFlags);
                commands.add(packageUpdater);

            }
            if (packageUpdater != null && builder.runNpmInstall) {
                commands.add(new TaskRunNpmInstall(classFinder, packageUpdater,
                        builder.enablePnpm, builder.requireHomeNodeExec,
                        builder.nodeVersion, builder.nodeDownloadRoot,
                        builder.useGlobalPnpm, builder.nodeAutoUpdate,
                        featureFlags.isEnabled(FeatureFlags.VITE)));

                commands.add(new TaskInstallWebpackPlugins(
                        new File(builder.npmFolder, builder.buildDirectory)));
            }

        }

        if (builder.createMissingPackageJson) {
            TaskGeneratePackageJson packageCreator = new TaskGeneratePackageJson(
                    builder.npmFolder, builder.generatedFolder,
                    builder.flowResourcesFolder, builder.buildDirectory,
                    featureFlags);
            commands.add(packageCreator);
        }

        if (frontendDependencies != null) {
            addGenerateServiceWorkerTask(builder,
                    frontendDependencies.getPwaConfiguration());
            addGenerateTsConfigTask(builder);
        }

        if (!builder.useDeprecatedV14Bootstrapping) {
            addBootstrapTasks(builder);

            if (builder.fusionJavaSourceFolder != null
                    && builder.fusionJavaSourceFolder.exists()
                    && builder.fusionGeneratedOpenAPIFile != null) {
                addFusionServicesTasks(builder);
            }

            commands.add(new TaskGenerateBootstrap(frontendDependencies,
                    builder.frontendDirectory, builder.productionMode));
        }

        if (builder.jarFiles != null && builder.flowResourcesFolder != null) {
            commands.add(new TaskCopyFrontendFiles(builder.flowResourcesFolder,
                    builder.jarFiles));
        }

        if (builder.localResourcesFolder != null
                && builder.flowResourcesFolder != null) {
            commands.add(new TaskCopyLocalFrontendFiles(
                    builder.flowResourcesFolder, builder.localResourcesFolder));
        }

        if (featureFlags.isEnabled(FeatureFlags.VITE)) {
            String themeName = "";
            PwaConfiguration pwa;
            if (frontendDependencies != null) {
                if (frontendDependencies.getThemeDefinition() != null) {
                    themeName = frontendDependencies.getThemeDefinition()
                            .getName();
                }
                pwa = frontendDependencies.getPwaConfiguration();
            } else {
                pwa = new PwaConfiguration();
            }
            commands.add(new TaskUpdateSettingsFile(builder, themeName, pwa));
            commands.add(new TaskUpdateVite(builder.npmFolder,
                    builder.buildDirectory));
        } else if (enableWebpackConfigUpdate) {
            PwaConfiguration pwaConfiguration = frontendDependencies
                    .getPwaConfiguration();
            commands.add(new TaskUpdateWebpack(builder.frontendDirectory,
                    builder.npmFolder, builder.webappResourcesDirectory,
                    builder.resourceOutputDirectory, builder.webpackTemplate,
                    builder.webpackGeneratedTemplate,
                    new File(builder.generatedFolder, IMPORTS_NAME),
                    builder.useDeprecatedV14Bootstrapping,
                    builder.flowResourcesFolder, pwaConfiguration,
                    builder.fusionClientAPIFolder, builder.buildDirectory));
        }

        if (builder.enableImportsUpdate) {
            commands.add(
                    new TaskUpdateImports(classFinder, frontendDependencies,
                            finder -> getFallbackScanner(builder, finder),
                            builder.npmFolder, builder.generatedFolder,
                            builder.frontendDirectory, builder.tokenFile,
                            builder.tokenFileData, builder.enablePnpm,
                            builder.buildDirectory, builder.productionMode,
                            featureFlags));

            commands.add(new TaskUpdateThemeImport(builder.npmFolder,
                    frontendDependencies.getThemeDefinition(),
                    builder.frontendDirectory, builder.fusionClientAPIFolder));
        }

        if (builder.copyTemplates) {
            commands.add(new TaskCopyTemplateFiles(classFinder,
                    builder.npmFolder, builder.resourceOutputDirectory));
        }
    }

    private void addBootstrapTasks(Builder builder) {
        TaskGenerateIndexHtml taskGenerateIndexHtml = new TaskGenerateIndexHtml(
                builder.frontendDirectory);
        commands.add(taskGenerateIndexHtml);
        File buildDirectory = new File(builder.npmFolder,
                builder.buildDirectory);
        TaskGenerateIndexTs taskGenerateIndexTs = new TaskGenerateIndexTs(
                builder.frontendDirectory,
                new File(builder.generatedFolder, IMPORTS_NAME),
                buildDirectory);
        commands.add(taskGenerateIndexTs);
        if (builder.getFeatureFlags().isEnabled(FeatureFlags.VITE)
                && !builder.productionMode) {
            commands.add(
                    new TaskGenerateViteDevMode(builder.frontendDirectory));
        }
    }

    private void addGenerateTsConfigTask(Builder builder) {
        TaskGenerateTsConfig taskGenerateTsConfig = new TaskGenerateTsConfig(
                builder.npmFolder);
        commands.add(taskGenerateTsConfig);

        TaskGenerateTsDefinitions taskGenerateTsDefinitions = new TaskGenerateTsDefinitions(
                builder.npmFolder);
        commands.add(taskGenerateTsDefinitions);

    }

    private void addGenerateServiceWorkerTask(Builder builder,
            PwaConfiguration pwaConfiguration) {
        File outputDirectory = new File(builder.npmFolder,
                builder.buildDirectory);
        if (pwaConfiguration.isEnabled()) {
            commands.add(new TaskGenerateServiceWorker(
                    builder.frontendDirectory, outputDirectory));
        }
    }

    private void addFusionServicesTasks(Builder builder) {
        Lookup lookup = builder.lookup;
        EndpointGeneratorTaskFactory endpointGeneratorTaskFactory = lookup
                .lookup(EndpointGeneratorTaskFactory.class);

        if (endpointGeneratorTaskFactory != null) {
            TaskGenerateOpenAPI taskGenerateOpenAPI = endpointGeneratorTaskFactory
                    .createTaskGenerateOpenAPI(
                            builder.fusionApplicationProperties,
                            builder.fusionJavaSourceFolder,
                            builder.classFinder.getClassLoader(),
                            builder.fusionGeneratedOpenAPIFile);
            commands.add(taskGenerateOpenAPI);

            if (builder.fusionClientAPIFolder != null) {
                TaskGenerateFusion taskGenerateFusion = endpointGeneratorTaskFactory
                        .createTaskGenerateFusion(
                                builder.fusionApplicationProperties,
                                builder.fusionGeneratedOpenAPIFile,
                                builder.fusionClientAPIFolder,
                                builder.frontendDirectory);
                commands.add(taskGenerateFusion);
            }
        }
    }

    private FrontendDependenciesScanner getFallbackScanner(Builder builder,
            ClassFinder finder) {
        if (builder.useByteCodeScanner) {
            return new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                    .createScanner(true, finder,
                            builder.generateEmbeddableWebComponents,
                            builder.useDeprecatedV14Bootstrapping);
        } else {
            return null;
        }
    }

    @Override
    public void execute() throws ExecutionFailedException {
        sortCommands(commands);

        for (FallibleCommand command : commands) {
            command.execute();
        }
    }

    /**
     * Sort command list so we always execute commands in a pre-defined order.
     *
     * @param commandList
     *            list of FallibleCommands to sort
     */
    private void sortCommands(List<FallibleCommand> commandList) {
        commandList.sort((c1, c2) -> {
            final int indexOf1 = getIndex(c1);
            final int indexOf2 = getIndex(c2);
            if (indexOf1 == -1 || indexOf2 == -1) {
                return 0;
            }
            return indexOf1 - indexOf2;
        });
    }

    /**
     * Find index of command for which it is assignable to.
     *
     * @param command
     *            command to find execution index for
     * @return index of command or -1 if not available
     */
    private int getIndex(FallibleCommand command) {
        return IntStream.range(0, commandOrder.size())
                .filter(i -> commandOrder.get(i)
                        .isAssignableFrom(command.getClass()))
                .findFirst()
                .orElseThrow(() -> new UnknownTaskException(command));
    }
}
