package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.installer.Platform;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import elemental.json.JsonObject;

/**
 * Build a <code>NodeExecutor</code> instance.
 */
public class Options implements Serializable {

    private String buildDirectoryName;

    ClassFinder classFinder;

    private File frontendDirectory;

    File webappResourcesDirectory = null;

    File resourceOutputDirectory = null;

    boolean enablePackagesUpdate = false;

    boolean createMissingPackageJson = false;

    boolean enableImportsUpdate = false;

    boolean enableWebpackConfigUpdate = false;

    boolean runNpmInstall = false;

    Set<File> jarFiles = null;

    boolean generateEmbeddableWebComponents = true;

    boolean cleanNpmFiles = false;

    File jarFrontendResourcesFolder = null;

    File localResourcesFolder = null;

    boolean useByteCodeScanner = false;

    JsonObject tokenFileData;

    File tokenFile;

    boolean enablePnpm = Constants.ENABLE_PNPM_DEFAULT;

    boolean useGlobalPnpm = false;

    File endpointSourceFolder;

    File endpointGeneratedOpenAPIFile;

    File applicationProperties;

    File frontendGeneratedFolder;

    boolean requireHomeNodeExec;

    boolean copyTemplates = false;

    File npmFolder;

    private File generatedFolder;

    /**
     * Is in client-side bootstrapping mode.
     */
    boolean useLegacyV14Bootstrap;

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example <code>"v16.0.0"</code>. Defaults to
     * {@value FrontendTools#DEFAULT_NODE_VERSION}.
     */
    String nodeVersion = FrontendTools.DEFAULT_NODE_VERSION;

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
     */
    URI nodeDownloadRoot = URI.create(Platform.guess().getNodeDownloadRoot());

    boolean nodeAutoUpdate = false;

    Lookup lookup;

    /**
     * Default is true here so we do not accidentally include development stuff
     * into production.
     */
    boolean productionMode = true;

    /**
     * The resource folder for java resources.
     */
    File javaResourceFolder;

    /**
     * Additional npm packages to run postinstall for.
     */
    List<String> postinstallPackages = new ArrayList<>();

    private FeatureFlags featureFlags;

    /**
     * Creates a new instance.
     *
     * @param lookup
     *            a {@link Lookup} to discover services used by Flow (SPI)
     */
    public Options(Lookup lookup, File npmFolder) {
        this.lookup = lookup;
        this.classFinder = lookup.lookup(ClassFinder.class);
        this.npmFolder = npmFolder;
    }

    /**
     * Sets the irectory where generated files are written.
     *
     * @param generatedFolder
     *            folder where flow generated files will be placed
     * @return this
     */
    public Options withGeneratedFolder(File generatedFolder) {
        this.generatedFolder = generatedFolder;
        return this;
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
     * Sets the webpack related properties.
     *
     * @param webappResourcesDirectory
     *            the directory to set for webpack to output its build results,
     *            meant for serving from context root.
     * @param resourceOutputDirectory
     *            the directory to output generated non-served resources, such
     *            as the "config/stats.json" stats file, and the
     *            "config/flow-build-info.json" token file.
     * @return this builder
     */
    public Options withWebpack(File webappResourcesDirectory,
            File resourceOutputDirectory) {
        this.enableWebpackConfigUpdate = true;
        this.webappResourcesDirectory = webappResourcesDirectory;
        this.resourceOutputDirectory = resourceOutputDirectory;
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
    public Options enablePackagesUpdate(boolean enablePackagesUpdate) {
        this.enablePackagesUpdate = enablePackagesUpdate;
        return this;
    }

    /**
     * Sets whether to perform always perform clean up procedure. Default is
     * <code>false</code>. When the value is false, npm related files will only
     * be removed when a platform version update is detected.
     *
     * This method is only for tests.
     *
     * @param forceClean
     *            <code>true</code> to clean npm files always, otherwise
     *            <code>false</code>
     * @return this builder
     */
    @Deprecated
    public Options enableNpmFileCleaning(boolean forceClean) {
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
    public Options runNpmInstall(boolean runNpmInstall) {
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
     * folder so as they are available for webpack build.
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
    public Options copyTemplates(boolean copyTemplates) {
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
     * Use V14 bootstrapping that disables index.html entry point.
     *
     * @param useDeprecatedV14Bootstrapping
     *            <code>true</code> to use legacy V14 bootstrapping
     * @return the builder, for chaining
     */
    public Options useV14Bootstrap(boolean useDeprecatedV14Bootstrapping) {
        this.useLegacyV14Bootstrap = useDeprecatedV14Bootstrapping;
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
        return frontendGeneratedFolder;
    }

    /**
     * Set application properties file for Spring project.
     *
     * @param applicationProperties
     *            application properties file.
     * @return this builder, for chaining
     */
    public Options withApplicationProperties(File applicationProperties) {
        this.applicationProperties = applicationProperties;
        return this;
    }

    /**
     * Set output location for the generated OpenAPI file.
     *
     * @param endpointGeneratedOpenAPIFile
     *            the generated output file.
     * @return the builder, for chaining
     */
    public Options withEndpointGeneratedOpenAPIFile(
            File endpointGeneratedOpenAPIFile) {
        this.endpointGeneratedOpenAPIFile = endpointGeneratedOpenAPIFile;
        return this;
    }

    /**
     * Gets the output location for the generated OpenAPI file. .
     *
     * @return the generated output file
     */
    public File getEndpointGeneratedOpenAPIFile() {
        return endpointGeneratedOpenAPIFile;
    }

    /**
     * Set source paths that OpenAPI generator searches for endpoints.
     *
     * @param endpointSourceFolder
     *            java source folder
     * @return the builder, for chaining
     */
    public Options withEndpointSourceFolder(File endpointSourceFolder) {
        this.endpointSourceFolder = endpointSourceFolder;
        return this;
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
    public Options populateTokenFileData(JsonObject object) {
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
    public Options enablePnpm(boolean enable) {
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
     * Get the generated folder for this build, always an absolute path.
     *
     * @return the generated folder
     */
    public File getGeneratedFolder() {
        if (generatedFolder != null) {
            if (generatedFolder.isAbsolute()) {
                return generatedFolder;
            } else {
                return new File(npmFolder, generatedFolder.getPath());
            }
        }

        if (buildDirectoryName != null && npmFolder != null) {
            // Use default if not specified
            String generatedDir = System
                    .getProperty(FrontendUtils.PARAM_GENERATED_DIR,
                            Paths.get(buildDirectoryName,
                                    FrontendUtils.DEFAULT_GENERATED_DIR)
                                    .toString());
            return new File(npmFolder, generatedDir);

        }
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

    public File getBuildDirectory() {
        return new File(npmFolder, getBuildDirectoryName());
    }

    public Options withFeatureFlags(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
        return this;
    }

    protected FeatureFlags getFeatureFlags() {
        if (featureFlags != null) {
            return featureFlags;
        }

        final FeatureFlags flags = new FeatureFlags(lookup);
        if (javaResourceFolder != null) {
            flags.setPropertiesLocation(javaResourceFolder);
        }
        return flags;
    }

    public File getJarFrontendResourcesFolder() {
        return jarFrontendResourcesFolder;
    }

    public ClassFinder getClassFinder() {
        return classFinder;
    }
}
