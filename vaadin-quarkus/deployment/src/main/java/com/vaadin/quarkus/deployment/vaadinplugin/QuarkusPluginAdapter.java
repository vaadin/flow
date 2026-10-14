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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.runtime.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.Platform;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.plugin.base.BuildFrontendUtil;
import com.vaadin.flow.plugin.base.PluginAdapterBuild;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;
import com.vaadin.flow.utils.FlowFileUtils;

/**
 * Quarkus implementation of Vaadin build plugin adapter.
 */
class QuarkusPluginAdapter implements PluginAdapterBuild {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(QuarkusPluginAdapter.class);

    private final VaadinBuildTimeConfig config;
    private final ApplicationModel model;
    private final WorkspaceModule appModule;
    private final SourceDir sourcesDir;
    private final SourceDir resourcesDir;

    /**
     * Creates a new instance of {@link QuarkusPluginAdapter} for the give build
     * configuration and application.
     *
     * @param config
     *            the Vaadin build configuration.
     * @param applicationModel
     *            the application model.
     */
    QuarkusPluginAdapter(VaadinBuildTimeConfig config,
            ApplicationModel applicationModel) {
        this(config, applicationModel, applicationModel.getApplicationModule());
    }

    /**
     * Creates a new instance of {@link QuarkusPluginAdapter} for the give build
     * configuration and application.
     *
     * @param config
     *            the Vaadin build configuration.
     * @param applicationModel
     *            the application model.
     * @param appModule
     *            the application module.
     */
    QuarkusPluginAdapter(VaadinBuildTimeConfig config,
            ApplicationModel applicationModel, WorkspaceModule appModule) {
        this.config = config;
        this.model = applicationModel;
        this.appModule = appModule;
        SourceDir assumedSources = SourceDir.of(
                appModule.getModuleDir().toPath()
                        .resolve(Paths.get("src", "main", "java")),
                appModule.getBuildDir().toPath().resolve("classes"));
        SourceDir assumedResources = SourceDir.of(
                appModule.getModuleDir().toPath()
                        .resolve(Paths.get("src", "main", "resources")),
                appModule.getBuildDir().toPath().resolve("classes"));
        if (appModule.hasMainSources()) {
            sourcesDir = appModule.getMainSources().getSourceDirs().stream()
                    .findFirst().orElse(assumedSources);
            resourcesDir = appModule.getMainSources().getResourceDirs().stream()
                    .findFirst().orElse(assumedResources);
        } else {
            sourcesDir = assumedSources;
            resourcesDir = assumedResources;
        }
    }

    @Override
    public File frontendResourcesDirectory() {
        return resolveProjectDirectory(config.frontendResourcesDirectory(),
                "vaadin.build.frontendResourcesDirectory");
    }

    @Override
    public boolean generateBundle() {
        return config.generateBundle();
    }

    @Override
    public boolean generateEmbeddableWebComponents() {
        return config.generateEmbeddableWebComponents();
    }

    @Override
    public boolean optimizeBundle() {
        return config.optimizeBundle();
    }

    @Override
    public boolean runNpmInstall() {
        return config.runNpmInstall();
    }

    @Override
    public boolean ciBuild() {
        return config.ciBuild();
    }

    @Override
    public boolean forceProductionBuild() {
        return config.forceProductionBuild();
    }

    @Override
    public boolean compressBundle() {
        return true;
    }

    @Override
    public boolean checkRuntimeDependency(String groupId, String artifactId,
            Consumer<String> missingDependencyMessageConsumer) {
        Objects.requireNonNull(groupId, "groupId cannot be null");
        Objects.requireNonNull(artifactId, "artifactId cannot be null");
        if (missingDependencyMessageConsumer == null) {
            missingDependencyMessageConsumer = text -> {
            };
        }
        // Unlike Maven, there is no need to inspect the dependency scope, since
        // the Quarkus application model already reports only dependencies that
        // are present at runtime.
        if (model.getRuntimeDependencies().stream()
                .noneMatch(dependency -> groupId.equals(dependency.getGroupId())
                        && artifactId.equals(dependency.getArtifactId()))) {
            missingDependencyMessageConsumer.accept(String.format(
                    """
                            The dependency %1$s:%2$s has not been found in the project configuration.
                            Please add the following dependency to your POM file:

                            <dependency>
                                <groupId>%1$s</groupId>
                                <artifactId>%2$s</artifactId>
                                <scope>runtime</scope>
                            </dependency>
                            """,
                    groupId, artifactId));
            return false;
        }
        return true;
    }

    @Override
    public File applicationProperties() {
        return config.applicationProperties();
    }

    @Override
    public boolean eagerServerLoad() {
        return config.eagerServerLoad();
    }

    @Override
    public File frontendDirectory() {
        return resolveProjectDirectory(config.frontendDirectory(),
                "vaadin.build.frontendDirectory");
    }

    @Override
    public File resourcesOutputDirectory() {
        return resolveBuildDirectory(config.resourcesOutputDirectory(),
                "vaadin.build.resourcesOutputDirectory");
    }

    private File resolveProjectDirectory(File directory, String name) {
        return resolveDirectory(projectBaseDirectory().toFile(), directory,
                name);
    }

    private File resolveBuildDirectory(File directory, String name) {
        return resolveDirectory(resourcesDir.getOutputDir().toFile(), directory,
                name);
    }

    private File resolveDirectory(File base, File directory, String key) {
        if (directory.isAbsolute() && directory.isDirectory()) {
            return directory;
        }
        directory = base.toPath().resolve(directory.toPath()).toFile();
        if (directory.exists() && !directory.isDirectory()) {
            throw new ConfigurationException(
                    key + " must be a directory: " + directory, Set.of(key));
        }
        return directory;
    }

    @Override
    public File generatedTsFolder() {
        return config.generatedTsFolder().orElseGet(() -> frontendDirectory()
                .toPath().resolve(FrontendUtils.GENERATED).toFile());
    }

    private ClassFinder classFinder;

    @Override
    public ClassFinder getClassFinder() {
        if (classFinder == null) {
            URL[] urls = buildClasspath().map(Path::toFile)
                    .map(FlowFileUtils::convertToUrl).toArray(URL[]::new);
            URLClassLoader classLoader = new URLClassLoader(urls,
                    Thread.currentThread().getContextClassLoader());
            classFinder = new ReflectionsClassFinder(classLoader, urls);
        }
        return classFinder;
    }

    @Override
    public Set<File> getJarFiles() {
        return model.getRuntimeDependencies().stream()
                .flatMap(dep -> dep.getResolvedPaths().stream())
                .map(Path::toFile).filter(file -> !file.isDirectory())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isJarProject() {
        return true;
    }

    @Override
    public boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }

    @Override
    public File javaSourceFolder() {
        return sourcesDir.getDir().toFile();
    }

    @Override
    public File javaResourceFolder() {
        return resourcesDir.getDir().toFile();
    }

    @Override
    public boolean isFrontendIgnoreVersionChecks() {
        return config.frontendIgnoreVersionChecks();
    }

    @Override
    public void logDebug(CharSequence charSequence) {
        LOGGER.debug(charSequence.toString());
    }

    @Override
    public void logDebug(CharSequence charSequence, Throwable throwable) {
        LOGGER.debug(charSequence.toString(), throwable);
    }

    @Override
    public void logInfo(CharSequence charSequence) {
        LOGGER.info(charSequence.toString());
    }

    @Override
    public void logWarn(CharSequence charSequence) {
        LOGGER.warn(charSequence.toString());
    }

    @Override
    public void logError(CharSequence charSequence) {
        LOGGER.error(charSequence.toString());
    }

    @Override
    public void logWarn(CharSequence charSequence, Throwable throwable) {
        LOGGER.warn(charSequence.toString(), throwable);
    }

    @Override
    public void logError(CharSequence charSequence, Throwable throwable) {
        LOGGER.error(charSequence.toString(), throwable);
    }

    @Override
    public URI nodeDownloadRoot() throws URISyntaxException {
        String nodeDownloadRoot = config.nodeDownloadRoot().orElseGet(
                () -> NodeInstaller.getDownloadRoot(Platform.guess()));
        try {
            return new URI(nodeDownloadRoot);
        } catch (URISyntaxException e) {
            logError("Failed to parse nodeDownloadRoot uri", e);
            throw new URISyntaxException(nodeDownloadRoot,
                    "Failed to parse nodeDownloadRoot uri");
        }
    }

    @Override
    public String nodeVersion() {
        return config.nodeVersion();
    }

    @Override
    public String nodeFolder() {
        return config.nodeFolder().map(File::getAbsolutePath).orElse(null);
    }

    @Override
    public File npmFolder() {
        return config.npmFolder()
                .map(dir -> resolveProjectDirectory(dir, "npmFolder"))
                .orElseGet(() -> projectBaseDirectory().toFile());
    }

    @Override
    public File openApiJsonFile() {
        return config.openApiJsonFile();
    }

    @Override
    public boolean pnpmEnable() {
        return config.pnpmEnable();
    }

    @Override
    public boolean bunEnable() {
        return config.bunEnable();
    }

    @Override
    public boolean useGlobalPnpm() {
        return config.useGlobalPnpm();
    }

    @Override
    public Path projectBaseDirectory() {
        return appModule.getModuleDir().toPath();
    }

    @Override
    public boolean requireHomeNodeExec() {
        return config.requireHomeNodeExec();
    }

    @Override
    public File servletResourceOutputDirectory() {
        return resolveBuildDirectory(config.generatedResourceOutputDirectory(),
                "resourceOutputDirectory");
    }

    @Deprecated(since = "24.8", forRemoval = true)
    @Override
    public File webpackOutputDirectory() {
        return frontendOutputDirectory();
    }

    @Override
    public File frontendOutputDirectory() {
        File outputDir = resolveBuildDirectory(config.frontendOutputDirectory(),
                "frontendOutputDirectory");
        config.webpackOutputDirectory()
                .map(f -> resolveBuildDirectory(f, "webpackOutputDirectory"))
                .filter(f -> !f.equals(outputDir))
                .ifPresent(deprecatedOutputDir -> logWarn(
                        "Both 'frontendOutputDirectory' and 'webpackOutputDirectory' are set. "
                                + "'webpackOutputDirectory' property will be removed in future releases and will be ignored. "
                                + "Please use only 'frontendOutputDirectory'."));
        return outputDir;
    }

    @Override
    public String buildFolder() {
        Path buildDir = appModule.getBuildDir().toPath();
        // buildFolder() is consumed as a path relative to the project folder
        // (new File(npmFolder, buildFolder)), so always return it relative to
        // the project basedir. A build dir outside basedir then yields a "../"
        // path. Returning the absolute path would instead append it to the
        // project folder and point outside the build dir.
        if (!buildDir.isAbsolute()) {
            return buildDir.toString();
        }
        // relativize() requires both paths to be absolute; the module dir is
        // always absolute in a real build, toAbsolutePath() only guards exotic
        // setups.
        try {
            return projectBaseDirectory().toAbsolutePath().relativize(buildDir)
                    .toString();
        } catch (IllegalArgumentException e) {
            // Different filesystem roots (e.g. on Windows): cannot relativize.
            return buildDir.toString();
        }
    }

    @Override
    public List<String> postinstallPackages() {
        return config.postinstallPackages().orElseGet(List::of);
    }

    @Override
    public List<String> excludePostinstallPackages() {
        return config.excludePostinstallPackages().orElseGet(List::of);
    }

    @Override
    public boolean isFrontendHotdeploy() {
        return true;
    }

    @Override
    public boolean skipDevBundleBuild() {
        return config.skipDevBundleBuild();
    }

    @Override
    public boolean isPrepareFrontendCacheDisabled() {
        return false;
    }

    @Override
    public boolean isReactEnabled() {
        return config.reactEnabled()
                .orElseGet(() -> FrontendUtils.isReactRouterRequired(
                        BuildFrontendUtil.getFrontendDirectory(this)));
    }

    @Override
    public String applicationIdentifier() {
        return config.applicationIdentifier().filter(id -> !id.isBlank())
                .orElseGet(
                        () -> "app-" + StringUtil.getHash(
                                model.getAppArtifact().getGroupId() + ":"
                                        + model.getAppArtifact()
                                                .getArtifactId(),
                                StandardCharsets.UTF_8));
    }

    @Override
    public List<String> frontendExtraFileExtensions() {
        return config.frontendExtraFileExtensions().orElseGet(List::of);
    }

    @Override
    public boolean isNpmExcludeWebComponents() {
        return config.npmExcludeWebComponents();
    }

    @Override
    public boolean isCommercialBannerEnabled() {
        return config.commercialWithBanner();
    }

    @Override
    public Integer minimumFrontendPackageAgeDays() {
        return config.minimumFrontendPackageAgeDays().orElse(null);
    }

    /**
     * Gets whether to cleans generated frontend files after the execution of
     * the frontend build. This is generally enabled by default to ensure a
     * clean state for after the build completes.
     *
     * @return {@code true} if the frontend files will be cleaned, {@code false}
     *         otherwise
     */
    public boolean cleanFrontendFiles() {
        return config.cleanFrontendFiles();
    }

    /**
     * Resolves and returns the build output directory.
     *
     * @return the path representing the build output directory.
     */
    Path buildDir() {
        return resourcesDir.getOutputDir();
    }

    /**
     * Collects the path of the artifacts that compose the application
     * classpath.
     *
     * @return the path of the artifacts that compose the application classpath.
     */
    private Stream<Path> buildClasspath() {
        // getMainSources() is null for a module that declares none, which the
        // constructor above already handles by assuming the standard layout;
        // buildDir() is where that layout puts the compiled classes.
        Stream<Path> outputs = appModule.hasMainSources()
                ? appModule.getMainSources().getOutputTree().getRoots().stream()
                : Stream.of(buildDir());
        return Stream.concat(outputs, model.getRuntimeDependencies().stream()
                .flatMap(dep -> dep.getResolvedPaths().stream()));
    }
}
