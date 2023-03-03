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

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;

/**
 * An executor that it's run when the servlet context is initialised in dev-mode
 * or when flow-maven-plugin goals are run. It can chain a set of task to run.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class NodeTasks implements FallibleCommand {

    //@formatter:off
    private static final String V14_BOOTSTRAPPING_VITE_ERROR_MESSAGE =
            "\n\n************************************************************************************"
            + "\n*  Vite build tool is not supported when 'useDeprecatedV14Bootstrapping' is used.  *"
            + "\n*  Please fallback to Webpack build tool via setting the                           *"
            + "\n*  'com.vaadin.experimental.webpackForFrontendBuild=true' feature flag             *"
            + "\n*  in [project-root]/src/main/resources/vaadin-featureflags.properties             *"
            + "\n*  (you may create the file if not exists) and restart the application.            *"
            + "\n************************************************************************************\n\n";
    //@formatter:on

    // @formatter:off
    // This list keeps the tasks in order so that they are executed
    // without depending on when they are added.
    private static final List<Class<? extends FallibleCommand>> commandOrder =
        Collections.unmodifiableList(Arrays.asList(
            TaskNotifyWebpackConfExistenceWhileUsingVite.class,
            TaskGeneratePackageJson.class,
            TaskGenerateIndexHtml.class,
            TaskGenerateIndexTs.class,
            TaskGenerateViteDevMode.class,
            TaskGenerateTsConfig.class,
            TaskGenerateTsDefinitions.class,
            TaskGenerateServiceWorker.class,
            TaskGenerateOpenAPI.class,
            TaskGenerateEndpoint.class,
            TaskGenerateBootstrap.class,
            TaskGenerateWebComponentHtml.class,
            TaskGenerateWebComponentBootstrap.class,
            TaskGenerateFeatureFlags.class,
            TaskInstallWebpackPlugins.class,
            TaskUpdatePackages.class,
            TaskRunNpmInstall.class,
            TaskGenerateHilla.class,
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

    /**
     * Initialize tasks with the given options.
     *
     * @param options
     *            the options
     */
    public NodeTasks(Options options) {

        ClassFinder classFinder = new ClassFinder.CachedClassFinder(
                options.classFinder);
        FrontendDependenciesScanner frontendDependencies = null;

        final FeatureFlags featureFlags = options.getFeatureFlags();

        if (options.enablePackagesUpdate || options.enableImportsUpdate
                || options.enableWebpackConfigUpdate) {
            frontendDependencies = new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                    .createScanner(!options.useByteCodeScanner, classFinder,
                            options.generateEmbeddableWebComponents,
                            options.useLegacyV14Bootstrap, featureFlags);

            if (options.generateEmbeddableWebComponents) {
                FrontendWebComponentGenerator generator = new FrontendWebComponentGenerator(
                        classFinder);
                Set<File> webComponents = generator.generateWebComponents(
                        options.generatedFolder,
                        frontendDependencies.getThemeDefinition());

                if (webComponents.size() > 0) {
                    commands.add(new TaskGenerateWebComponentHtml(
                            options.frontendDirectory));
                    commands.add(new TaskGenerateWebComponentBootstrap(
                            options.frontendDirectory,
                            new File(options.generatedFolder, IMPORTS_NAME)));
                }
            }

            TaskUpdatePackages packageUpdater = null;
            if (options.enablePackagesUpdate
                    && options.jarFrontendResourcesFolder != null) {
                packageUpdater = new TaskUpdatePackages(classFinder,
                        frontendDependencies, options.npmFolder,
                        options.generatedFolder,
                        options.jarFrontendResourcesFolder,
                        options.cleanNpmFiles, options.enablePnpm,
                        options.buildDirectory, featureFlags);
                commands.add(packageUpdater);

            }
            if (packageUpdater != null && options.runNpmInstall) {
                commands.add(new TaskRunNpmInstall(packageUpdater,
                        options.enablePnpm, options.requireHomeNodeExec,
                        options.nodeVersion, options.nodeDownloadRoot,
                        options.useGlobalPnpm, options.nodeAutoUpdate,
                        options.postinstallPackages, options.isCiBuild()));

                commands.add(new TaskInstallWebpackPlugins(
                        new File(options.npmFolder, options.buildDirectory)));
            }

        }

        if (options.createMissingPackageJson) {
            TaskGeneratePackageJson packageCreator = new TaskGeneratePackageJson(
                    options.npmFolder, options.generatedFolder,
                    options.buildDirectory, featureFlags);
            commands.add(packageCreator);
        }

        if (frontendDependencies != null) {
            addGenerateServiceWorkerTask(options,
                    frontendDependencies.getPwaConfiguration());
            addGenerateTsConfigTask(options);
        }

        if (options.useLegacyV14Bootstrap) {
            if (!featureFlags.isEnabled(FeatureFlags.WEBPACK)) {
                throw new IllegalStateException(
                        V14_BOOTSTRAPPING_VITE_ERROR_MESSAGE);
            }
        } else {
            addBootstrapTasks(options);

            // use the new Hilla generator if enabled, otherwise use the old
            // generator.
            TaskGenerateHilla hillaTask;
            if (options.endpointGeneratedOpenAPIFile != null
                    && featureFlags.isEnabled(FeatureFlags.HILLA_ENGINE)
                    && (hillaTask = options.lookup
                            .lookup(TaskGenerateHilla.class)) != null) {
                hillaTask.configure(options.getNpmFolder(),
                        options.getBuildDirectory());
                commands.add(hillaTask);
            } else if (options.endpointGeneratedOpenAPIFile != null
                    && options.endpointSourceFolder != null
                    && options.endpointSourceFolder.exists()) {
                addEndpointServicesTasks(options);
            }

            commands.add(new TaskGenerateBootstrap(frontendDependencies,
                    options.frontendDirectory, options.productionMode));

            commands.add(new TaskGenerateFeatureFlags(options.frontendDirectory,
                    featureFlags));
        }

        if (options.jarFiles != null
                && options.jarFrontendResourcesFolder != null) {
            commands.add(new TaskCopyFrontendFiles(
                    options.jarFrontendResourcesFolder, options.jarFiles));
        }

        if (options.localResourcesFolder != null
                && options.jarFrontendResourcesFolder != null) {
            commands.add(new TaskCopyLocalFrontendFiles(
                    options.jarFrontendResourcesFolder,
                    options.localResourcesFolder));
        }

        if (!featureFlags.isEnabled(FeatureFlags.WEBPACK)) {
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
            commands.add(new TaskNotifyWebpackConfExistenceWhileUsingVite(
                    options.npmFolder));
            commands.add(new TaskUpdateSettingsFile(options, themeName, pwa));
            commands.add(new TaskUpdateVite(options.npmFolder,
                    options.buildDirectory));
        } else if (options.enableWebpackConfigUpdate) {
            PwaConfiguration pwaConfiguration = frontendDependencies
                    .getPwaConfiguration();
            commands.add(new TaskUpdateWebpack(options.frontendDirectory,
                    options.npmFolder, options.webappResourcesDirectory,
                    options.resourceOutputDirectory,
                    new File(options.generatedFolder, IMPORTS_NAME),
                    options.useLegacyV14Bootstrap, pwaConfiguration,
                    options.buildDirectory));
        }

        if (options.enableImportsUpdate) {
            commands.add(new TaskUpdateImports(classFinder,
                    frontendDependencies,
                    finder -> getFallbackScanner(options, finder, featureFlags),
                    options.npmFolder, options.generatedFolder,
                    options.frontendDirectory, options.tokenFile,
                    options.tokenFileData, options.enablePnpm,
                    options.buildDirectory, options.productionMode,
                    options.useLegacyV14Bootstrap, featureFlags));

            commands.add(new TaskUpdateThemeImport(options.npmFolder,
                    frontendDependencies.getThemeDefinition(),
                    options.frontendDirectory));
        }

        if (options.copyTemplates) {
            commands.add(new TaskCopyTemplateFiles(classFinder,
                    options.npmFolder, options.resourceOutputDirectory,
                    options.frontendDirectory));
        }
    }

    private void addBootstrapTasks(Options builder) {
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
        if (!builder.getFeatureFlags().isEnabled(FeatureFlags.WEBPACK)
                && !builder.productionMode) {
            commands.add(
                    new TaskGenerateViteDevMode(builder.frontendDirectory));
        }
    }

    private void addGenerateTsConfigTask(Options builder) {
        TaskGenerateTsConfig taskGenerateTsConfig = new TaskGenerateTsConfig(
                builder.npmFolder, builder.getFeatureFlags());
        commands.add(taskGenerateTsConfig);

        TaskGenerateTsDefinitions taskGenerateTsDefinitions = new TaskGenerateTsDefinitions(
                builder.npmFolder);
        commands.add(taskGenerateTsDefinitions);

    }

    private void addGenerateServiceWorkerTask(Options builder,
            PwaConfiguration pwaConfiguration) {
        File outputDirectory = new File(builder.npmFolder,
                builder.buildDirectory);
        if (pwaConfiguration.isEnabled()) {
            commands.add(new TaskGenerateServiceWorker(
                    builder.frontendDirectory, outputDirectory));
        }
    }

    private void addEndpointServicesTasks(Options builder) {
        Lookup lookup = builder.lookup;
        EndpointGeneratorTaskFactory endpointGeneratorTaskFactory = lookup
                .lookup(EndpointGeneratorTaskFactory.class);

        if (endpointGeneratorTaskFactory != null) {
            TaskGenerateOpenAPI taskGenerateOpenAPI = endpointGeneratorTaskFactory
                    .createTaskGenerateOpenAPI(builder.applicationProperties,
                            builder.endpointSourceFolder,
                            builder.classFinder.getClassLoader(),
                            builder.endpointGeneratedOpenAPIFile);
            commands.add(taskGenerateOpenAPI);

            if (builder.frontendGeneratedFolder != null) {
                TaskGenerateEndpoint taskGenerateEndpoint = endpointGeneratorTaskFactory
                        .createTaskGenerateEndpoint(
                                builder.applicationProperties,
                                builder.endpointGeneratedOpenAPIFile,
                                builder.frontendGeneratedFolder,
                                builder.frontendDirectory);
                commands.add(taskGenerateEndpoint);
            }
        }
    }

    private FrontendDependenciesScanner getFallbackScanner(Options builder,
            ClassFinder finder, FeatureFlags featureFlags) {
        if (builder.useByteCodeScanner) {
            return new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                    .createScanner(true, finder,
                            builder.generateEmbeddableWebComponents,
                            builder.useLegacyV14Bootstrap, featureFlags, true);
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
        int index = commandOrder.indexOf(command.getClass());
        if (index != -1) {
            return index;
        }
        for (int i = 0; i < commandOrder.size(); i++) {
            if (commandOrder.get(i).isAssignableFrom(command.getClass())) {
                return i;
            }
        }
        throw new UnknownTaskException(command);
    }
}
