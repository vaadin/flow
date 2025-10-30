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
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.installer.Platform;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

/**
 * Build a <code>NodeExecutor</code> instance.
 */
public class Options implements Serializable {

    private String buildDirectoryName;

    private ClassFinder classFinder;

    private File frontendDirectory;

    private File webappResourcesDirectory = null;

    private File resourceOutputDirectory = null;

    private boolean enablePackagesUpdate = false;

    private boolean createMissingPackageJson = false;

    private boolean enableImportsUpdate = false;

    private boolean enableConfigUpdate = false;

    private boolean runNpmInstall = false;

    private boolean bundleBuild = false;

    private Set<File> jarFiles = null;

    private boolean generateEmbeddableWebComponents = true;

    private boolean cleanNpmFiles = false;

    private File jarFrontendResourcesFolder = null;

    private File localResourcesFolder = null;

    private boolean useByteCodeScanner = false;

    private JsonNode tokenFileData;

    private File tokenFile;

    private boolean enablePnpm = Constants.ENABLE_PNPM_DEFAULT;

    private boolean enableBun = Constants.ENABLE_BUN_DEFAULT;

    private boolean ciBuild;

    private boolean forceProductionBuild;

    private boolean useGlobalPnpm = false;

    private File frontendGeneratedFolder;

    private boolean requireHomeNodeExec;

    private boolean copyTemplates = false;

    private File npmFolder;

    private boolean skipDevBundle = false;

    private boolean compressBundle = true;

    private List<String> frontendExtraFileExtensions = null;

    private FrontendDependenciesScanner frontendDependenciesScanner;

    private boolean copyAssets = true;

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example <code>"v16.0.0"</code>. Defaults to
     * {@value FrontendTools#DEFAULT_NODE_VERSION}.
     */
    private String nodeVersion = FrontendTools.DEFAULT_NODE_VERSION;

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
     */
    private URI nodeDownloadRoot = URI
            .create(Platform.guess().getNodeDownloadRoot());

    private boolean nodeAutoUpdate = false;

    private Lookup lookup;

    /**
     * Default is true here so we do not accidentally include development stuff
     * into production.
     */
    private boolean productionMode = true;

    /**
     * The resource folder for java resources.
     */
    private File javaResourceFolder;

    /**
     * Additional npm packages to run postinstall for.
     */
    private List<String> postinstallPackages = new ArrayList<>();

    private FeatureFlags featureFlags;

    private boolean frontendHotdeploy = false;

    private boolean reactEnable = true;

    private boolean npmExcludeWebComponents = false;

    /**
     * Removes generated files from a previous execution that are no more
     * created.
     */
    private boolean cleanOldGeneratedFiles = false;

    private boolean frontendIgnoreVersionChecks = false;

    private boolean commercialBannerEnabled = false;

    /**
     * Creates a new instance.
     *
     * @param lookup
     *            a {@link Lookup} to discover services used by Flow (SPI)
     * @param npmFolder
     *            a project's base folder
     */
    public Options(Lookup lookup, File npmFolder) {
        this(lookup, new ClassFinder.CachedClassFinder(
                lookup.lookup(ClassFinder.class)), npmFolder);
    }

    /**
     * Creates a new instance.
     *
     * @param lookup
     *            a {@link Lookup} to discover services used by Flow (SPI)
     * @param classFinder
     *            a class finder to use in node tasks
     * @param npmFolder
     *            a project's base folder
     */
    public Options(Lookup lookup, ClassFinder classFinder, File npmFolder) {
        this.lookup = lookup;
        this.classFinder = classFinder;
        this.npmFolder = npmFolder;
    }

    /**
     * Sets the directory containing the project's frontend files
     *
     * @param frontendDirectory
     *            a directory with project's frontend files
     * @return this
     */
    public Options withFrontendDirectory(File frontendDirectory) {
        this.frontendDirectory = frontendDirectory.isAbsolute()
                ? frontendDirectory
                : new File(npmFolder, frontendDirectory.getPath());
        return this;
    }

    /**
     * Use the specified directory name (inside the project folder) for the
     * build output.
     *
     * @param buildDirectory
     *            project build directory
     *
     * @return this builder
     */
    public Options withBuildDirectory(String buildDirectory) {
        this.buildDirectoryName = buildDirectory;
        return this;
    }

    /**
     * Sets the folders where frontend build results should be stored.
     *
     * @param webappResourcesDirectory
     *            the directory to set for build tool to output its build
     *            results, meant for serving from context root.
     * @param resourceOutputDirectory
     *            the directory to output generated non-served resources, such
     *            as the "config/stats.json" stats file, and the
     *            "config/flow-build-info.json" token file.
     * @return this builder
     */
    public Options withBuildResultFolders(File webappResourcesDirectory,
            File resourceOutputDirectory) {
        this.enableConfigUpdate = true;
        this.webappResourcesDirectory = webappResourcesDirectory;
        this.resourceOutputDirectory = resourceOutputDirectory;
        return this;
    }

    /**
     * Sets whether to enable packages and frontend file updates. Default is
     * <code>true</code>.
     *
     * @param enablePackagesUpdate
     *            <code>true</code> to enable packages and frontend update,
     *            otherwise <code>false</code>
     * @return this builder
     */
    public Options enablePackagesUpdate(boolean enablePackagesUpdate) {
        this.enablePackagesUpdate = enablePackagesUpdate;
        return this;
    }

    // Visible only for tests
    Options enableNpmFileCleaning(boolean forceClean) {
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
    public Options enableImportsUpdate(boolean enableImportsUpdate) {
        this.enableImportsUpdate = enableImportsUpdate;
        this.createMissingPackageJson = enableImportsUpdate
                || createMissingPackageJson;
        return this;
    }

    /**
     * Sets whether run <code>npm install</code> after updating dependencies.
     *
     * @param runNpmInstall
     *            run npm install. Default is <code>false</code>
     * @return the builder
     */
    public Options withRunNpmInstall(boolean runNpmInstall) {
        this.runNpmInstall = runNpmInstall;
        return this;
    }

    /**
     * Sets the appropriate npm package folder for copying flow resources in
     * jars.
     *
     * @param jarFrontendResourcesFolder
     *            target folder
     * @return the builder
     */
    public Options withJarFrontendResourcesFolder(
            File jarFrontendResourcesFolder) {
        this.jarFrontendResourcesFolder = jarFrontendResourcesFolder
                .isAbsolute() ? jarFrontendResourcesFolder
                        : new File(npmFolder,
                                jarFrontendResourcesFolder.getPath());
        return this;
    }

    /**
     * Sets whether copy resources from classpath to the appropriate npm package
     * folder so as they are available for frontend build.
     *
     * @param jars
     *            set of class nodes to be visited. Not {@code null}
     *
     * @return the builder
     */
    public Options copyResources(Set<File> jars) {
        Objects.requireNonNull(jars, "Parameter 'jars' must not be null!");
        this.jarFiles = jars;
        return this;
    }

    /**
     * Sets whether copy templates to {@code META-INF/VAADIN/config/templates}.
     *
     * @param copyTemplates
     *            whether to copy templates
     *
     * @return the builder
     */
    public Options withCopyTemplates(boolean copyTemplates) {
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
    public Options withEmbeddableWebComponents(
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
    public Options createMissingPackageJson(boolean create) {
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
    public Options copyLocalResources(File localResourcesFolder) {
        this.localResourcesFolder = localResourcesFolder;
        return this;
    }

    /**
     * Set the folder where frontend files should be generated.
     *
     * @param frontendGeneratedFolder
     *            folder to generate frontend files in.
     * @return the builder, for chaining
     */
    public Options withFrontendGeneratedFolder(File frontendGeneratedFolder) {
        this.frontendGeneratedFolder = frontendGeneratedFolder;
        return this;
    }

    /**
     * Gets the folder where frontend files should be generated.
     *
     * @return folder to generate frontend files in
     */
    public File getFrontendGeneratedFolder() {
        if (frontendGeneratedFolder == null) {
            return new File(getFrontendDirectory(), FrontendUtils.GENERATED);
        }
        return frontendGeneratedFolder;
    }

    /**
     * Sets frontend scanner strategy: byte code scanning strategy is used if
     * {@code byteCodeScanner} is {@code true}, full classpath scanner strategy
     * is used otherwise (by default).
     *
     * @param byteCodeScanner
     *            if {@code true} then byte code scanner is used, full scanner
     *            is used otherwise (by default).
     * @return the builder, for chaining
     */
    public Options useByteCodeScanner(boolean byteCodeScanner) {
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
    public Options populateTokenFileData(JsonNode object) {
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
    public Options withTokenFile(File tokenFile) {
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
    public Options withEnablePnpm(boolean enable) {
        enablePnpm = enable;
        return this;
    }

    /**
     * Enables bun tool.
     * <p>
     * "bun" will be used instead of "npm".
     *
     * @param enable
     *            enables bun.
     * @return the builder, for chaining
     */
    public Options withEnableBun(boolean enable) {
        enableBun = enable;
        return this;
    }

    /**
     * Enables ci build.
     * <p>
     * "npm ci" will be used instead of "npm install". "--frozen-lockfile" will
     * be used if pnpm is used instead of npm.
     *
     * @param ciBuild
     *            true to enable ci build
     * @return the builder, for chaining
     */
    public Options withCiBuild(boolean ciBuild) {
        this.ciBuild = ciBuild;
        return this;
    }

    /**
     * Setting this to {@code true} will force a build of the production build
     * even if there is a default production bundle that could be used.
     *
     * @param forceProductionBuild
     *            true to force production build
     * @return the builder, for chaining
     */
    public Options withForceProductionBuild(boolean forceProductionBuild) {
        this.forceProductionBuild = forceProductionBuild;
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
    public Options useGlobalPnpm(boolean useGlobalPnpm) {
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
    public Options withHomeNodeExecRequired(boolean requireHomeNodeExec) {
        this.requireHomeNodeExec = requireHomeNodeExec;
        return this;
    }

    /**
     * Sets the node.js version to be used when node.js is installed
     * automatically by Vaadin, for example <code>"v16.0.0"</code>. Defaults to
     * {@value FrontendTools#DEFAULT_NODE_VERSION}.
     *
     * @param nodeVersion
     *            the new node version to download, not null.
     * @return the builder, for chaining
     */
    public Options withNodeVersion(String nodeVersion) {
        this.nodeVersion = Objects.requireNonNull(nodeVersion);
        return this;
    }

    /**
     * Sets the download node.js URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
     *
     * @param nodeDownloadRoot
     *            the new download URL to set, not null.
     * @return the builder, for chaining
     */
    public Options withNodeDownloadRoot(URI nodeDownloadRoot) {
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
    public Options withProductionMode(boolean productionMode) {
        this.productionMode = productionMode;
        return this;
    }

    /**
     * Whether to run with a dev server (when not in production mode).
     *
     * @param frontendHotdeploy
     *            true to run with a dev server, false to run in development
     *            bundle mode
     * @return this builder
     */
    public Options withFrontendHotdeploy(boolean frontendHotdeploy) {
        this.frontendHotdeploy = frontendHotdeploy;
        return this;
    }

    /**
     * Whether to ignore node/npm tool version checks or not. Defaults to
     * {@code false}.
     *
     * @param frontendIgnoreVersionChecks
     *            {@code true} to ignore node/npm tool version checks
     * @return the builder, for chaining
     */
    public Options withFrontendIgnoreVersionChecks(
            boolean frontendIgnoreVersionChecks) {
        this.frontendIgnoreVersionChecks = frontendIgnoreVersionChecks;
        return this;
    }

    /**
     * Checks if running with a dev server (when not in production mode).
     *
     * @return true to run with a dev server, false to run in development bundle
     *         mode
     */
    public boolean isFrontendHotdeploy() {
        return frontendHotdeploy;
    }

    /**
     * Check if a dev mode bundle build should run.
     *
     * @return true to run the build, false otherwise
     */
    public boolean isDevBundleBuild() {
        return !isProductionMode() && isBundleBuild();
    }

    /**
     * Whether to run bundle build. Needed when not using a dev server or
     * running for production.
     *
     * @param bundleBuild
     *            true to run a bundle build
     * @return this builder
     */
    public Options withBundleBuild(boolean bundleBuild) {
        this.bundleBuild = bundleBuild;
        return this;
    }

    /**
     * Check if a bundle build should run.
     *
     * @return true to run the build, false otherwise
     */
    public boolean isBundleBuild() {
        return bundleBuild;
    }

    /**
     * Sets whether it is fine to automatically update the alternate node
     * installation if installed version is older than the current default.
     *
     * @param update
     *            true to update alternate node when used
     * @return the builder
     */
    public Options setNodeAutoUpdate(boolean update) {
        this.nodeAutoUpdate = update;
        return this;
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
    public Options setJavaResourceFolder(File javaResourceFolder) {
        this.javaResourceFolder = javaResourceFolder;
        return this;
    }

    /**
     * Sets the additional npm packages to run {@code postinstall} for.
     * <p>
     * By default, postinstall is only run for internal dependencies which rely
     * on post install scripts to work, e.g. esbuild
     *
     * @param postinstallPackages
     *            the additional npm packages to run postinstall for
     * @return the builder, for chaining
     */
    public Options withPostinstallPackages(List<String> postinstallPackages) {
        this.postinstallPackages = postinstallPackages;
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
     * Get the output directory for frontend build output.
     *
     * @return webappResourcesDirectory
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
        if (frontendDirectory == null && npmFolder != null) {
            // Use default if not specified
            return new File(npmFolder,
                    System.getProperty(FrontendUtils.PARAM_FRONTEND_DIR,
                            FrontendUtils.DEFAULT_FRONTEND_DIR));
        }
        return frontendDirectory;
    }

    /**
     * Get the name of the used build directory.
     * <p>
     * By default this will be {@code target} for maven and {@code build} for
     * gradle.
     *
     * @return The name of the build directory
     */
    public String getBuildDirectoryName() {
        return buildDirectoryName;
    }

    /**
     * Gets the directory used for the build output.
     *
     * @return the build directory
     */
    public File getBuildDirectory() {
        return new File(npmFolder, getBuildDirectoryName());
    }

    public Options withFeatureFlags(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
        return this;
    }

    /**
     * Get the available feature flags.
     *
     * @return FeatureFlags object
     */
    public FeatureFlags getFeatureFlags() {
        if (featureFlags == null) {
            featureFlags = new FeatureFlags(lookup);
            if (javaResourceFolder != null) {
                featureFlags.setPropertiesLocation(javaResourceFolder);
            }
        }
        return featureFlags;
    }

    /**
     * Gets the folder to which frontend resources from add-on jars are copied.
     *
     * @return the jar resource folder
     */
    public File getJarFrontendResourcesFolder() {
        return jarFrontendResourcesFolder;
    }

    /**
     * Gets the used class finder.
     *
     * @return the class finder
     */
    public ClassFinder getClassFinder() {
        return classFinder;
    }

    public File getNodeModulesFolder() {
        return new File(getNpmFolder(), FrontendUtils.NODE_MODULES);
    }

    public File getResourceOutputDirectory() {
        return resourceOutputDirectory;
    }

    public boolean isEnablePackagesUpdate() {
        return enablePackagesUpdate;
    }

    public boolean isCreateMissingPackageJson() {
        return createMissingPackageJson;
    }

    public boolean isEnableImportsUpdate() {
        return enableImportsUpdate;
    }

    public boolean isEnableConfigUpdate() {
        return enableConfigUpdate;
    }

    public boolean isRunNpmInstall() {
        return runNpmInstall;
    }

    public Set<File> getJarFiles() {
        return jarFiles;
    }

    public boolean isGenerateEmbeddableWebComponents() {
        return generateEmbeddableWebComponents;
    }

    /**
     * @deprecated used internally only for testing, to be removed without a
     *             replacement.
     * @return true if npm files should be cleaned, false otherwise
     */
    @Deprecated(since = "25.0", forRemoval = true)
    public boolean isCleanNpmFiles() {
        return cleanNpmFiles;
    }

    public File getLocalResourcesFolder() {
        return localResourcesFolder;
    }

    public boolean isUseByteCodeScanner() {
        return useByteCodeScanner;
    }

    public JsonNode getTokenFileData() {
        return tokenFileData;
    }

    public File getTokenFile() {
        return tokenFile;
    }

    public boolean isEnablePnpm() {
        return enablePnpm;
    }

    public boolean isEnableBun() {
        return enableBun;
    }

    public boolean isCiBuild() {
        return ciBuild;
    }

    public boolean isForceProductionBuild() {
        return forceProductionBuild;
    }

    public boolean isUseGlobalPnpm() {
        return useGlobalPnpm;
    }

    public boolean isRequireHomeNodeExec() {
        return requireHomeNodeExec;
    }

    public boolean isCopyTemplates() {
        return copyTemplates;
    }

    public String getNodeVersion() {
        return nodeVersion;
    }

    public URI getNodeDownloadRoot() {
        return nodeDownloadRoot;
    }

    public boolean isNodeAutoUpdate() {
        return nodeAutoUpdate;
    }

    /**
     * Gets the lookup instance to use for internal lookups.
     *
     * @return the lookup instance
     */
    public Lookup getLookup() {
        return lookup;
    }

    /**
     * Gets the production mode.
     *
     * @return <code>true</code> if production mode is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isProductionMode() {
        return productionMode;
    }

    /**
     * Gets the folder containing java resources, typically
     * {@literal src/main/resources} in a Maven project.
     *
     * @return the java resource folder
     */
    public File getJavaResourceFolder() {
        return javaResourceFolder;
    }

    public List<String> getPostinstallPackages() {
        return postinstallPackages;
    }

    /**
     * Set to true to skip dev bundle build in case a dev bundle exists.
     * <p>
     * Dev bundle build will not be skipped in case no dev bundle is found.
     *
     * @param skip
     *            {@code true} to skip rebuild of dev bundle
     * @return this builder
     */
    public Options skipDevBundleBuild(boolean skip) {
        skipDevBundle = skip;
        return this;
    }

    /**
     * Is dev bundle build skipped or not.
     *
     * @return {@code true} to skip dev bundle checks, {@code false} to run
     *         normally. Default is {@code false}
     */
    public boolean isSkipDevBundle() {
        return skipDevBundle;
    }

    /**
     * Set to {@code false} to skip frontend bundle compression.
     *
     * @param compressBundle
     *            {@code false} to not compress frontend bundles
     * @return this builder
     */
    public Options withCompressBundle(boolean compressBundle) {
        this.compressBundle = compressBundle;
        return this;
    }

    /**
     * Get if frontend bundle should be compressed or not.
     *
     * @return true to copress, false to skip compression
     */
    public boolean isCompressBundle() {
        return compressBundle;
    }

    public boolean isReactEnabled() {
        return reactEnable;
    }

    public Options withReact(boolean reactEnable) {
        this.reactEnable = reactEnable;
        if (reactEnable && !FrontendUtils
                .isReactRouterRequired(getFrontendDirectory())) {
            LoggerFactory.getLogger(Options.class).debug(
                    "Setting reactEnable to false as Vaadin Router is used!");
            this.reactEnable = false;
        }
        return this;
    }

    /**
     * Sets whether generated files from a previous execution that are no more
     * created should be removed.
     * <p>
     *
     * By default, the odl generated files are preserved.
     *
     * @param clean
     *            {@literal true} if old generated files should be removed,
     *            {@literal false} if they should be preserved.
     *
     * @return this builder
     */
    public Options withCleanOldGeneratedFiles(boolean clean) {
        this.cleanOldGeneratedFiles = clean;
        return this;
    }

    /**
     * Gets if generated files from a previous execution that are no more
     * created should be removed.
     *
     * @return {@literal true} if old generated files should be removed,
     *         otherwise {@literal false}.
     */
    public boolean isCleanOldGeneratedFiles() {
        return cleanOldGeneratedFiles;
    }

    /**
     * Sets the extra file extensions used in the project.
     *
     * @param frontendExtraFileExtensions
     *            the file extensions to add for the project
     * @return this builder
     */
    public Options withFrontendExtraFileExtensions(
            List<String> frontendExtraFileExtensions) {
        this.frontendExtraFileExtensions = frontendExtraFileExtensions;
        return this;
    }

    /**
     * Gets the project file extensions.
     *
     * @return the project file extensions
     */
    public List<String> getFrontendExtraFileExtensions() {
        return frontendExtraFileExtensions;
    }

    /**
     * Sets whether to exclude web component npm packages in packages.json.
     *
     * @return this builder
     */
    public boolean isNpmExcludeWebComponents() {
        return npmExcludeWebComponents;
    }

    /**
     * Sets whether to exclude web component npm packages in packages.json.
     *
     * @param exclude
     *            whether to exclude web component npm packages
     * @return this builder
     */
    public Options withNpmExcludeWebComponents(boolean exclude) {
        this.npmExcludeWebComponents = exclude;
        return this;
    }

    /**
     * Whether to ignore node/npm tool version checks or not.
     *
     * @return {@code true} to ignore node/npm tool version checks
     */
    public boolean isFrontendIgnoreVersionChecks() {
        return frontendIgnoreVersionChecks;
    }

    /**
     * Sets the frontend dependencies scanner to use.
     *
     * @param frontendDependenciesScanner
     *            frontend dependencies scanner
     * @return this builder
     */
    public Options withFrontendDependenciesScanner(
            FrontendDependenciesScanner frontendDependenciesScanner) {
        this.frontendDependenciesScanner = frontendDependenciesScanner;
        return this;
    }

    /**
     * Checks if the commercial banner is enabled for the build.
     *
     * @return {@code true} if the commercial banner is enabled, {@code false}
     *         otherwise
     */
    public boolean isCommercialBannerEnabled() {
        return commercialBannerEnabled;
    }

    /**
     * Sets whether the build could generate an application with a commercial
     * banner.
     *
     * @param enableCommercialBanner
     *            a boolean value indicating whether the built application could
     *            add a commercial banner.
     * @return this builder
     */
    public Options withCommercialBanner(boolean enableCommercialBanner) {
        this.commercialBannerEnabled = enableCommercialBanner;
        return this;
    }

    /**
     * Gets the frontend dependencies scanner to use. If not is not pre-set,
     * this initializes a new one based on the Options set.
     *
     * @return frontend dependencies scanner
     */
    public FrontendDependenciesScanner getFrontendDependenciesScanner() {
        if (frontendDependenciesScanner == null) {
            boolean reactEnabled = isReactEnabled() && FrontendUtils
                    .isReactRouterRequired(getFrontendDirectory());
            frontendDependenciesScanner = new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                    .createScanner(!isUseByteCodeScanner(), getClassFinder(),
                            isGenerateEmbeddableWebComponents(),
                            getFeatureFlags(), reactEnabled);
        }
        return frontendDependenciesScanner;
    }

    /**
     * Sets whether to copy npm assets or not. True by default.
     *
     * @param copyAssets
     *            boolean value indicating if npm assets should be copied.
     * @return this builder
     */
    public Options setCopyAssets(boolean copyAssets) {
        this.copyAssets = copyAssets;
        return this;
    }

    /**
     * Get if npm assets should be copied for this Options execution.
     * <p>
     * NOTE! For a devBundleBuild copy assets will always be true!
     *
     * @return {@code false} to skip copying except for devBundleBuild.
     */
    public boolean copyAssets() {
        if (isDevBundleBuild()) {
            return true;
        }
        return copyAssets;
    }
}
