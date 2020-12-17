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

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

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
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_GENERATED_DIR;

/**
 * An executor that it's run when the servlet context is initialised in dev-mode
 * or when flow-maven-plugin goals are run. It can chain a set of task to run.
 *
 * @since 2.0
 */
public class NodeTasks implements FallibleCommand {

    /**
     * Build a <code>NodeExecutor</code> instance.
     */
    public static class Builder implements Serializable {

        private final ClassFinder classFinder;

        private final File frontendDirectory;

        private File webpackOutputDirectory = null;

        private File resourceOutputDirectory = null;

        private String webpackTemplate = null;

        private String webpackGeneratedTemplate = null;

        private String serviceWorkerTemplate = null;

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

        private File connectJavaSourceFolder;

        private File connectGeneratedOpenApiFile;

        private File connectApplicationProperties;

        private File connectClientTsApiFolder;

        private boolean requireHomeNodeExec;

        /**
         * Directory for for npm and folders and files.
         */
        public final File npmFolder;

        /**
         * Directory where generated files are written.
         */
        public final File generatedFolder;

        /**
         * Is in client-side bootstrapping mode.
         */
        private boolean useDeprecatedV14Bootstrapping;

        /**
         * The node.js version to be used when node.js is installed
         * automatically by Vaadin, for example <code>"v12.18.3"</code>.
         * Defaults to {@value FrontendTools#DEFAULT_NODE_VERSION}.
         */
        private String nodeVersion = FrontendTools.DEFAULT_NODE_VERSION;

        /**
         * Download node.js from this URL. Handy in heavily firewalled corporate
         * environments where the node.js download can be provided from an
         * intranet mirror. Defaults to {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
         */
        private URI nodeDownloadRoot = URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT);

        private Lookup lookup;

        /**
         * Create a builder instance given an specific npm folder.
         *
         * @param lookup
         *            a {@link Lookup} to discover services used by Flow (SPI)
         * @param npmFolder
         *            folder with the `package.json` file
         */
        public Builder(Lookup lookup, File npmFolder) {
            this(lookup, npmFolder, new File(npmFolder, System
                    .getProperty(PARAM_GENERATED_DIR, DEFAULT_GENERATED_DIR)));
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
         */
        public Builder(Lookup lookup, File npmFolder,
                File generatedPath) {
            this(lookup, npmFolder, generatedPath,
                    new File(npmFolder, System.getProperty(PARAM_FRONTEND_DIR,
                            DEFAULT_FRONTEND_DIR)));
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
         */
        public Builder(Lookup lookup, File npmFolder,
                File generatedPath, File frontendDirectory) {
            this.lookup = lookup;
            this.classFinder = lookup.lookup(ClassFinder.class);
            this.npmFolder = npmFolder;
            this.generatedFolder = generatedPath.isAbsolute() ? generatedPath
                    : new File(npmFolder, generatedPath.getPath());
            this.frontendDirectory = frontendDirectory.isAbsolute()
                    ? frontendDirectory
                    : new File(npmFolder, frontendDirectory.getPath());
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
         * @param webpackOutputDirectory
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
         * @param serviceWorkerTemplate
         *            name of the service worker resource to be used as template
         *            when creating the <code>sw.ts</code> file.
         * @return this builder
         */
        public Builder withWebpack(File webpackOutputDirectory,
                File resourceOutputDirectory, String webpackTemplate,
                String webpackGeneratedTemplate, String serviceWorkerTemplate) {
            this.webpackOutputDirectory = webpackOutputDirectory;
            this.resourceOutputDirectory = resourceOutputDirectory;
            this.webpackTemplate = webpackTemplate;
            this.webpackGeneratedTemplate = webpackGeneratedTemplate;
            this.serviceWorkerTemplate = serviceWorkerTemplate;
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
        public Builder enableNpmFileCleaning(boolean forceClean) {
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
         * @param connectClientTsApiFolder
         *            folder for Ts files in the frontend.
         * @return the builder, for chaining
         */
        public Builder withConnectClientTsApiFolder(
                File connectClientTsApiFolder) {
            this.connectClientTsApiFolder = connectClientTsApiFolder;
            return this;
        }

        /**
         * Set application properties file for Spring project.
         *
         * @param applicationProperties
         *            application properties file.
         * @return this builder, for chaining
         */
        public Builder withConnectApplicationProperties(
                File applicationProperties) {
            this.connectApplicationProperties = applicationProperties;
            return this;
        }

        /**
         * Set output location for the generated OpenAPI file.
         *
         * @param generatedOpenApiFile
         *            the generated output file.
         * @return the builder, for chaining
         */
        public Builder withConnectGeneratedOpenApiJson(
                File generatedOpenApiFile) {
            this.connectGeneratedOpenApiFile = generatedOpenApiFile;
            return this;
        }

        /**
         * Set source paths that OpenAPI generator searches for endpoints.
         *
         * @param connectJavaSourceFolder
         *            java source folder
         * @return the builder, for chaining
         */
        public Builder withConnectJavaSourceFolder(
                File connectJavaSourceFolder) {
            this.connectJavaSourceFolder = connectJavaSourceFolder;
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
         * automatically by Vaadin, for example <code>"v12.18.3"</code>.
         * Defaults to {@value FrontendTools#DEFAULT_NODE_VERSION}.
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
         * intranet mirror. Defaults to {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
         *
         * @param nodeDownloadRoot
         *            the new download URL to set, not null.
         * @return the builder, for chaining
         */
        public Builder withNodeDownloadRoot(URI nodeDownloadRoot) {
            this.nodeDownloadRoot = Objects.requireNonNull(nodeDownloadRoot);
            return this;
        }
    }

    private final Collection<FallibleCommand> commands = new ArrayList<>();

    private NodeTasks(Builder builder) {

        ClassFinder classFinder = new ClassFinder.CachedClassFinder(
                builder.classFinder);
        FrontendDependenciesScanner frontendDependencies = null;

        boolean enableWebpackConfigUpdate = builder.webpackTemplate != null
                && !builder.webpackTemplate.isEmpty();

        if (builder.enablePackagesUpdate || builder.enableImportsUpdate
                || enableWebpackConfigUpdate) {
            frontendDependencies = new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                    .createScanner(!builder.useByteCodeScanner, classFinder,
                            builder.generateEmbeddableWebComponents);

            if (builder.generateEmbeddableWebComponents) {
                FrontendWebComponentGenerator generator = new FrontendWebComponentGenerator(
                        classFinder);
                generator.generateWebComponents(builder.generatedFolder, frontendDependencies.getThemeDefinition());
            }
        }

        if (builder.createMissingPackageJson) {
            TaskGeneratePackageJson packageCreator = new TaskGeneratePackageJson(
                    builder.npmFolder, builder.generatedFolder,
                    builder.flowResourcesFolder);
            commands.add(packageCreator);
        }

        if (!builder.useDeprecatedV14Bootstrapping) {
            addBootstrapTasks(builder, frontendDependencies);

            if (builder.connectJavaSourceFolder != null
                    && builder.connectJavaSourceFolder.exists()
                    && builder.connectGeneratedOpenApiFile != null) {
                addConnectServicesTasks(builder);
            }
        }

        if (builder.enablePackagesUpdate) {
            TaskUpdatePackages packageUpdater = new TaskUpdatePackages(
                    classFinder, frontendDependencies, builder.npmFolder,
                    builder.generatedFolder, builder.flowResourcesFolder,
                    builder.cleanNpmFiles, builder.enablePnpm);
            commands.add(packageUpdater);

            if (builder.runNpmInstall) {
                commands.add(new TaskRunNpmInstall(
                        classFinder, packageUpdater,
                        builder.enablePnpm, builder.requireHomeNodeExec,
                        builder.nodeVersion, builder.nodeDownloadRoot));

                commands.add(new TaskInstallWebpackPlugins(
                    new File(builder.npmFolder, NODE_MODULES)));
            }
        }

        if (builder.jarFiles != null) {
            commands.add(new TaskCopyFrontendFiles(builder.flowResourcesFolder,
                    builder.jarFiles));

            if (builder.localResourcesFolder != null) {
                commands.add(new TaskCopyLocalFrontendFiles(
                        builder.flowResourcesFolder,
                        builder.localResourcesFolder));
            }
        }

        if (enableWebpackConfigUpdate) {
            PwaConfiguration pwaConfiguration = frontendDependencies
                    .getPwaConfiguration();
            commands.add(new TaskUpdateWebpack(builder.frontendDirectory,
                    builder.npmFolder, builder.webpackOutputDirectory,
                    builder.resourceOutputDirectory, builder.webpackTemplate,
                    builder.webpackGeneratedTemplate,
                    builder.serviceWorkerTemplate,
                    new File(builder.generatedFolder, IMPORTS_NAME),
                    builder.useDeprecatedV14Bootstrapping,
                    builder.flowResourcesFolder, pwaConfiguration));
        }

        if (builder.enableImportsUpdate) {
            commands.add(
                    new TaskUpdateImports(classFinder, frontendDependencies,
                            finder -> getFallbackScanner(builder, finder),
                            builder.npmFolder, builder.generatedFolder,
                            builder.frontendDirectory, builder.tokenFile,
                            builder.tokenFileData, builder.enablePnpm));

            commands.add(new TaskUpdateThemeImport(builder.npmFolder,
                frontendDependencies.getThemeDefinition()));
        }
    }

    private void addBootstrapTasks(Builder builder, FrontendDependenciesScanner frontendDependencies) {
        File outputDirectory = new File(builder.npmFolder,
                FrontendUtils.TARGET);
        TaskGenerateIndexHtml taskGenerateIndexHtml = new TaskGenerateIndexHtml(
                builder.frontendDirectory, outputDirectory);
        commands.add(taskGenerateIndexHtml);
        TaskGenerateIndexTs taskGenerateIndexTs = new TaskGenerateIndexTs(
                builder.frontendDirectory,
                new File(builder.generatedFolder, IMPORTS_NAME),
                outputDirectory);
        commands.add(taskGenerateIndexTs);

        TaskGenerateTsConfig taskGenerateTsConfig = new TaskGenerateTsConfig(
                builder.npmFolder);
        commands.add(taskGenerateTsConfig);

        TaskGenerateTsDefinitions taskGenerateTsDefinitions = new TaskGenerateTsDefinitions(
                builder.npmFolder);
        commands.add(taskGenerateTsDefinitions);

        if (frontendDependencies != null) {
            PwaConfiguration pwaConfiguration = frontendDependencies.getPwaConfiguration();
            if (pwaConfiguration.isEnabled()) {
                commands.add(new TaskGenerateServiceWorker(builder.frontendDirectory, outputDirectory));
            }
        }
    }

    private void addConnectServicesTasks(Builder builder) {
        Lookup lookup = builder.lookup;
        EndpointGeneratorTaskFactory endpointGeneratorTaskFactory = lookup.lookup(EndpointGeneratorTaskFactory.class);

        if (endpointGeneratorTaskFactory != null) {
            TaskGenerateOpenApi taskGenerateOpenApi = endpointGeneratorTaskFactory.createTaskGenerateOpenApi(
                    builder.connectApplicationProperties, builder.connectJavaSourceFolder,
                    builder.classFinder.getClassLoader(), builder.connectGeneratedOpenApiFile);
            commands.add(taskGenerateOpenApi);

            if (builder.connectClientTsApiFolder != null) {
                TaskGenerateConnect taskGenerateConnectTs = endpointGeneratorTaskFactory.createTaskGenerateConnect(
                        builder.connectApplicationProperties, builder.connectGeneratedOpenApiFile,
                        builder.connectClientTsApiFolder, builder.frontendDirectory);
                commands.add(taskGenerateConnectTs);
            }
        }
    }

    private FrontendDependenciesScanner getFallbackScanner(Builder builder,
            ClassFinder finder) {
        if (builder.useByteCodeScanner) {
            return new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                    .createScanner(true, finder,
                            builder.generateEmbeddableWebComponents);
        } else {
            return null;
        }
    }

    @Override
    public void execute() throws ExecutionFailedException {
        for (FallibleCommand command : commands) {
            command.execute();
        }
    }

}
