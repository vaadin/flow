/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FileIOUtils;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Constants;
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
            TaskGenerateReactFiles.class,
            TaskGenerateTailwindCss.class,
            TaskGenerateTailwindJs.class,
            TaskUpdateOldIndexTs.class,
            TaskGenerateViteDevMode.class,
            TaskGenerateCommercialBanner.class,
            TaskGenerateTsConfig.class,
            TaskGenerateTsDefinitions.class,
            TaskGenerateServiceWorker.class,
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
            TaskCopyNpmAssetsFiles.class,
            TaskGeneratePWAIcons.class,
            TaskUpdateSettingsFile.class,
            TaskUpdateVite.class,
            TaskUpdateImports.class,
            TaskUpdateThemeImport.class,
            TaskCopyTemplateFiles.class,
            TaskGenerateBootstrap.class,
            TaskRunDevBundleBuild.class,
            TaskPrepareProdBundle.class,
            TaskProcessStylesheetCss.class,
            TaskCleanFrontendFiles.class,
            TaskRemoveOldFrontendGeneratedFiles.class
        ));
    // @formatter:on

    private final List<FallibleCommand> commands = new ArrayList<>();

    private Path lockFile;

    /**
     * Initialize tasks with the given options.
     *
     * @param options
     *            the options
     */
    public NodeTasks(Options options) {
        FrontendDependenciesScanner frontendDependencies = options
                .getFrontendDependenciesScanner();

        // Lock file is created in the project root folder and not in target/ so
        // that Maven does not remove it
        lockFile = new File(options.getNpmFolder(), ".vaadin-node-tasks.lock")
                .toPath();

        ClassFinder classFinder = options.getClassFinder();

        Set<String> webComponentTags = new HashSet<>();

        if (options.isFrontendHotdeploy()) {
            UsageStatistics.markAsUsed("flow/hotdeploy", null);
        }

        if (options.isEnablePackagesUpdate() || options.isEnableImportsUpdate()
                || options.isEnableConfigUpdate()) {
            if (options.isProductionMode()) {
                boolean needBuild = BundleValidationUtil.needsBuild(options,
                        frontendDependencies,
                        Mode.PRODUCTION_PRECOMPILED_BUNDLE);
                options.withRunNpmInstall(needBuild);
                options.withBundleBuild(needBuild);
                if (!needBuild) {
                    commands.add(new TaskPrepareProdBundle(options));
                    File prodBundle = ProdBundleUtils
                            .getProdBundle(options.getNpmFolder());
                    if (prodBundle.exists()) {
                        UsageStatistics.markAsUsed("flow/app-prod-bundle",
                                null);
                    } else {
                        UsageStatistics.markAsUsed(
                                "flow/prod-pre-compiled-bundle", null);
                    }
                } else {
                    commands.add(new TaskGenerateCommercialBanner(options));
                    BundleUtils.copyPackageLockFromBundle(options);
                }
                // Process @StyleSheet CSS files (minify and inline @imports)
                commands.add(new TaskProcessStylesheetCss(options));
            } else if (options.isBundleBuild()) {
                // The dev bundle check needs the frontendDependencies to be
                // able to
                // determine if we need a rebuild as the check happens
                // immediately
                // and no update tasks are executed before it.
                if (BundleValidationUtil.needsBuild(options,
                        frontendDependencies, Mode.DEVELOPMENT_BUNDLE)) {
                    commands.add(new TaskCleanFrontendFiles(options));
                    options.withRunNpmInstall(true);
                    options.withCopyTemplates(true);
                    BundleUtils.copyPackageLockFromBundle(options);
                    UsageStatistics.markAsUsed("flow/app-dev-bundle", null);
                } else {
                    // A dev bundle build is not needed after all, skip it
                    options.withBundleBuild(false);
                    File devBundleFolder = DevBundleUtils.getDevBundleFolder(
                            options.getNpmFolder(),
                            options.getBuildDirectoryName());
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
                            webComponentPath -> FileIOUtils.removeExtension(
                                    webComponentPath.getName()))
                            .collect(Collectors.toSet());
                    UsageStatistics.markAsUsed(
                            Constants.STATISTIC_HAS_EXPORTED_WC, null);
                }
            }

            TaskUpdatePackages packageUpdater = null;
            if (options.isEnablePackagesUpdate()
                    && options.getJarFrontendResourcesFolder() != null) {
                packageUpdater = new TaskUpdatePackages(frontendDependencies,
                        options);
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

        if (commands.stream()
                .noneMatch(TaskRunDevBundleBuild.class::isInstance)) {
            commands.add(new TaskCopyNpmAssetsFiles(options));
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
        if (options.isProductionMode() && pwa.isEnabled()) {
            commands.add(new TaskGeneratePWAIcons(options, pwa));
        }
        commands.add(new TaskUpdateSettingsFile(options, themeName, pwa));
        if (options.isFrontendHotdeploy() || options.isBundleBuild()) {
            commands.add(new TaskUpdateVite(options, webComponentTags));
        }

        if (options.isEnableImportsUpdate()) {
            commands.add(new TaskUpdateImports(frontendDependencies, options));

            commands.add(new TaskUpdateThemeImport(
                    frontendDependencies.getThemeDefinition(), options));
        }

        if (options.isCopyTemplates()) {
            commands.add(new TaskCopyTemplateFiles(classFinder, options));
        }

        if (options.isCleanOldGeneratedFiles()) {
            commands.add(new TaskRemoveOldFrontendGeneratedFiles(options));
        }
    }

    private void addBootstrapTasks(Options options) {
        commands.add(new TaskGenerateIndexHtml(options));
        if (options.isProductionMode() || options.isFrontendHotdeploy()
                || options.isBundleBuild()) {
            commands.add(new TaskGenerateIndexTs(options));
            commands.add(new TaskGenerateReactFiles(options));
            if (FrontendUtils.isTailwindCssEnabled(options)) {
                commands.add(new TaskGenerateTailwindCss(options));
                commands.add(new TaskGenerateTailwindJs(options));
            }
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
        if (!FrontendUtils.isHillaUsed(options.getFrontendDirectory(),
                options.getClassFinder())) {
            return;
        }
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
        getLock();
        try {
            sortCommands(commands);
            GeneratedFilesSupport generatedFilesSupport = new GeneratedFilesSupport();
            for (FallibleCommand command : commands) {
                long startTime = System.nanoTime();
                command.setGeneratedFileSupport(generatedFilesSupport);
                command.execute();
                Duration durationInNs = Duration
                        .ofNanos(System.nanoTime() - startTime);
                getLogger().debug("Task [ {} ] completed in {} ms",
                        command.getClass().getSimpleName(),
                        durationInNs.toMillis());
            }
        } finally {
            releaseLock();
        }
    }

    private void getLock() {
        boolean loggedWaiting = false;

        while (lockFile.toFile().exists()) {
            NodeTasksLockInfo lockInfo;
            try {
                lockInfo = readLockFile();
            } catch (Exception e) {
                getLogger().error("Error waiting for another "
                        + getClass().getSimpleName() + " process to finish", e);
                break;
            }

            try {
                Optional<ProcessHandle> processHandle = ProcessHandle
                        .of(lockInfo.pid());

                if (processHandle.isPresent()
                        && normalizeCommandLine(processHandle.get().info())
                                .equals(lockInfo.commandLine())) {
                    if (!loggedWaiting) {
                        getLogger().info("Waiting for a previous instance of "
                                + getClass().getSimpleName() + " (pid: "
                                + lockInfo.pid() + ") to finish...");
                        loggedWaiting = true;
                    }
                    Thread.sleep(500);
                } else {
                    // The process has died without removing the lock file
                    lockFile.toFile().delete();
                }
            } catch (InterruptedException e) {
                // Restore interrupted state
                Thread.currentThread().interrupt();

                throw new RuntimeException(
                        "Interrupted while waiting for another "
                                + getClass().getSimpleName() + " process (pid: "
                                + lockInfo.pid() + ") to finish",
                        e);
            } catch (Exception e) {
                getLogger().error("Error waiting for another "
                        + getClass().getSimpleName() + " process (pid: "
                        + lockInfo.pid() + ") to finish", e);
            }
        }

        try {
            writeLockFile();
        } catch (IOException e) {
            getLogger().error("Error writing lock file ({})",
                    lockFile.toFile().getAbsolutePath(), e);
        }
    }

    private void releaseLock() {
        if (!lockFile.toFile().exists()) {
            getLogger().warn("Somebody else has removed the lock file ({})",
                    lockFile.toFile().getAbsolutePath());
            return;
        }

        try {
            long pid = readLockFile().pid();
            if (pid != ProcessHandle.current().pid()) {
                getLogger().warn(
                        "Another process ({}) has overwritten the lock file ({})",
                        pid, lockFile.toFile().getAbsolutePath());
                return;
            }
            lockFile.toFile().delete();
        } catch (Exception e) {
            getLogger().error("Error releasing lock file ({})",
                    lockFile.toFile().getAbsolutePath());
        }
    }

    public record NodeTasksLockInfo(long pid,
            String commandLine) implements Serializable {
    }

    private NodeTasksLockInfo readLockFile()
            throws NumberFormatException, IOException {
        List<String> lines = Files.readAllLines(lockFile,
                StandardCharsets.UTF_8);
        if (lines.size() != 2) {
            throw new IllegalStateException(
                    "Invalid lock file. It should contain 2 rows but contains "
                            + lines);
        }
        return new NodeTasksLockInfo(Long.parseLong(lines.get(0)),
                lines.get(1));
    }

    private void writeLockFile() throws IOException {
        ProcessHandle currentProcess = ProcessHandle.current();
        long myPid = currentProcess.pid();
        String commandLine = normalizeCommandLine(currentProcess.info());
        List<String> lines = List.of(Long.toString(myPid), commandLine);
        Files.write(lockFile, lines, StandardCharsets.UTF_8);
    }

    private String normalizeCommandLine(ProcessHandle.Info processInfo) {
        return processInfo.commandLine()
                .map(line -> line.replaceAll("\\r?\\n", " \\\\n")).orElse("");
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
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
    int getIndex(FallibleCommand command) {
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
