/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

/**
 * An executor that it's run when the servlet context is initialised in dev-mode
 * or when flow-maven-plugin goals are run. It can chain a set of task to run.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class NodeTasks implements FallibleCommand {

    // @formatter:off
    // This list keeps the tasks in order so that they are executed
    // without depending on when they are added.
    private static final List<Class<? extends FallibleCommand>> commandOrder =
        Collections.unmodifiableList(Arrays.asList(
            TaskGeneratePackageJson.class,
            TaskGenerateIndexHtml.class,
            TaskGenerateIndexTs.class,
            TaskUpdateOldIndexTs.class,
            TaskGenerateViteDevMode.class,
            TaskGenerateTsConfig.class,
            TaskGenerateTsDefinitions.class,
            TaskGenerateServiceWorker.class,
            TaskGenerateBootstrap.class,
            TaskGenerateWebComponentHtml.class,
            TaskGenerateWebComponentBootstrap.class,
            TaskGenerateFeatureFlags.class,
            TaskInstallFrontendBuildPlugins.class,
            TaskUpdatePackages.class,
            TaskRunNpmInstall.class,
            TaskGenerateOpenAPI.class,
            TaskGenerateEndpoint.class,
            TaskCopyFrontendFiles.class,
            TaskCopyLocalFrontendFiles.class,
            TaskUpdateSettingsFile.class,
            TaskUpdateVite.class,
            TaskUpdateImports.class,
            TaskUpdateThemeImport.class,
            TaskCopyTemplateFiles.class,
            TaskRunDevBundleBuild.class,
            TaskPrepareProdBundle.class,
            TaskCleanFrontendFiles.class
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
                options.getClassFinder());
        FrontendDependenciesScanner frontendDependencies = null;

        Set<String> webComponentTags = new HashSet<>();

        final FeatureFlags featureFlags = options.getFeatureFlags();

        if (options.isFrontendHotdeploy()) {
            UsageStatistics.markAsUsed("flow/hotdeploy", null);
        }

        if (options.isEnablePackagesUpdate() || options.isEnableImportsUpdate()
                || options.isEnableWebpackConfigUpdate()) {
            frontendDependencies = new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                    .createScanner(!options.isUseByteCodeScanner(), classFinder,
                            options.isGenerateEmbeddableWebComponents(),
                            featureFlags);

            if (options.isProductionMode()) {
                boolean needBuild = BundleValidationUtil.needsBuild(options,
                        frontendDependencies, classFinder,
                        Mode.PRODUCTION_PRECOMPILED_BUNDLE);
                options.withRunNpmInstall(needBuild);
                options.withBundleBuild(needBuild);
                if (!needBuild) {
                    commands.add(new TaskPrepareProdBundle(options));
                    UsageStatistics.markAsUsed("flow/prod-pre-compiled-bundle",
                            null);
                } else {
                    BundleUtils.copyPackageLockFromBundle(options);
                }
            } else if (options.isBundleBuild()) {
                // The dev bundle check needs the frontendDependencies to be
                // able to
                // determine if we need a rebuild as the check happens
                // immediately
                // and no update tasks are executed before it.
                if (BundleValidationUtil.needsBuild(options,
                        frontendDependencies, classFinder,
                        Mode.DEVELOPMENT_BUNDLE)) {
                    commands.add(
                            new TaskCleanFrontendFiles(options.getNpmFolder()));
                    options.withRunNpmInstall(true);
                    options.withCopyTemplates(true);
                    BundleUtils.copyPackageLockFromBundle(options);
                    UsageStatistics.markAsUsed("flow/app-dev-bundle", null);
                } else {
                    // A dev bundle build is not needed after all, skip it
                    options.withBundleBuild(false);
                    File devBundleFolder = DevBundleUtils
                            .getDevBundleFolder(options.getNpmFolder());
                    if (devBundleFolder.exists()) {
                        UsageStatistics.markAsUsed("flow/app-dev-bundle", null);
                    } else {
                        UsageStatistics.markAsUsed("flow/dev-bundle", null);
                    }
                }
            } else if (options.isFrontendHotdeploy()) {
                BundleUtils.copyPackageLockFromBundle(options);
            }

            if (options.isGenerateEmbeddableWebComponents()) {
                FrontendWebComponentGenerator generator = new FrontendWebComponentGenerator(
                        classFinder);
                Set<File> webComponents = generator.generateWebComponents(
                        FrontendUtils.getFlowGeneratedWebComponentsFolder(
                                options.getFrontendDirectory()),
                        frontendDependencies.getThemeDefinition());

                if (webComponents.size() > 0) {
                    commands.add(new TaskGenerateWebComponentHtml(options));
                    commands.add(
                            new TaskGenerateWebComponentBootstrap(options));
                    webComponentTags = webComponents.stream().map(
                            webComponentPath -> FilenameUtils.removeExtension(
                                    webComponentPath.getName()))
                            .collect(Collectors.toSet());
                }
            }

            TaskUpdatePackages packageUpdater = null;
            if (options.isEnablePackagesUpdate()
                    && options.getJarFrontendResourcesFolder() != null) {
                packageUpdater = new TaskUpdatePackages(classFinder,
                        frontendDependencies, options);
                commands.add(packageUpdater);
            }

            if (packageUpdater != null && options.isRunNpmInstall()) {
                commands.add(new TaskRunNpmInstall(packageUpdater, options));

                commands.add(new TaskInstallFrontendBuildPlugins(options));
            }

            if (packageUpdater != null && options.isDevBundleBuild()) {
                commands.add(new TaskRunDevBundleBuild(options));
            }

        }

        if (options.isCreateMissingPackageJson()) {
            TaskGeneratePackageJson packageCreator = new TaskGeneratePackageJson(
                    options);
            commands.add(packageCreator);
        }

        if (frontendDependencies != null) {
            addGenerateServiceWorkerTask(options,
                    frontendDependencies.getPwaConfiguration());

            if (options.isFrontendHotdeploy() || options.isBundleBuild()) {
                addGenerateTsConfigTask(options);
            }
        }

        addBootstrapTasks(options);

        // Add Hilla generator tasks (the called method will verify if Hilla is
        // available)
        addEndpointServicesTasks(options);

        commands.add(new TaskGenerateBootstrap(frontendDependencies, options));

        commands.add(new TaskGenerateFeatureFlags(options));

        if (options.getJarFiles() != null
                && options.getJarFrontendResourcesFolder() != null) {
            commands.add(new TaskCopyFrontendFiles(options));
        }

        if (options.getLocalResourcesFolder() != null
                && options.getJarFrontendResourcesFolder() != null) {
            commands.add(new TaskCopyLocalFrontendFiles(options));
        }

        String themeName = "";
        PwaConfiguration pwa;
        if (frontendDependencies != null) {
            if (frontendDependencies.getThemeDefinition() != null) {
                themeName = frontendDependencies.getThemeDefinition().getName();
            }
            pwa = frontendDependencies.getPwaConfiguration();
        } else {
            pwa = new PwaConfiguration();
        }
        commands.add(new TaskUpdateSettingsFile(options, themeName, pwa));
        if (options.isFrontendHotdeploy() || options.isBundleBuild()) {
            commands.add(new TaskUpdateVite(options, webComponentTags));
        }

        if (options.isEnableImportsUpdate()) {
            commands.add(new TaskUpdateImports(classFinder,
                    frontendDependencies, options));

            commands.add(new TaskUpdateThemeImport(
                    frontendDependencies.getThemeDefinition(), options));
        }

        if (options.isCopyTemplates()) {
            commands.add(new TaskCopyTemplateFiles(classFinder, options));

        }
    }

    private void addBootstrapTasks(Options options) {
        commands.add(new TaskGenerateIndexHtml(options));
        if (options.isProductionMode() || options.isFrontendHotdeploy()
                || options.isBundleBuild()) {
            commands.add(new TaskGenerateIndexTs(options));
            if (!options.isProductionMode()) {
                commands.add(new TaskGenerateViteDevMode(options));
            }
        }
        commands.add(new TaskUpdateOldIndexTs(options));
    }

    private void addGenerateTsConfigTask(Options options) {
        TaskGenerateTsConfig taskGenerateTsConfig = new TaskGenerateTsConfig(
                options);
        commands.add(taskGenerateTsConfig);

        TaskGenerateTsDefinitions taskGenerateTsDefinitions = new TaskGenerateTsDefinitions(
                options);
        commands.add(taskGenerateTsDefinitions);

    }

    private void addGenerateServiceWorkerTask(Options options,
            PwaConfiguration pwaConfiguration) {
        if (pwaConfiguration.isEnabled()) {
            commands.add(new TaskGenerateServiceWorker(options));
        }
    }

    private void addEndpointServicesTasks(Options options) {
        Lookup lookup = options.getLookup();
        EndpointGeneratorTaskFactory endpointGeneratorTaskFactory = lookup
                .lookup(EndpointGeneratorTaskFactory.class);

        if (endpointGeneratorTaskFactory != null) {
            TaskGenerateOpenAPI taskGenerateOpenAPI = endpointGeneratorTaskFactory
                    .createTaskGenerateOpenAPI(options);
            commands.add(taskGenerateOpenAPI);

            if (options.getFrontendGeneratedFolder() != null) {
                TaskGenerateEndpoint taskGenerateEndpoint = endpointGeneratorTaskFactory
                        .createTaskGenerateEndpoint(options);
                commands.add(taskGenerateEndpoint);
            }
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
