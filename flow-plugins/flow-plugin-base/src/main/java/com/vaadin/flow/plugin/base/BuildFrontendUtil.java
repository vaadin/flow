/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.plugin.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.CvdlProducts;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeTasks;
import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;
import com.vaadin.flow.utils.FlowFileUtils;
import com.vaadin.pro.licensechecker.BuildType;
import com.vaadin.pro.licensechecker.LicenseChecker;
import com.vaadin.pro.licensechecker.Product;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.server.Constants.CONNECT_APPLICATION_PROPERTIES_TOKEN;
import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.JAVA_RESOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.CONNECT_OPEN_API_FILE_TOKEN;
import static com.vaadin.flow.server.Constants.FRONTEND_TOKEN;
import static com.vaadin.flow.server.Constants.GENERATED_TOKEN;
import static com.vaadin.flow.server.Constants.NPM_TOKEN;
import static com.vaadin.flow.server.Constants.PROJECT_FRONTEND_GENERATED_DIR_TOKEN;
import static com.vaadin.flow.server.InitParameters.NODE_DOWNLOAD_ROOT;
import static com.vaadin.flow.server.InitParameters.NODE_VERSION;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_ENABLE_DEV_SERVER;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_INITIAL_UIDL;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_USE_V14_BOOTSTRAP;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;

/**
 * Util class provides all methods a Plugin will need.
 */
public class BuildFrontendUtil {

    /**
     * Hide public constructor.
     */
    private BuildFrontendUtil() {

    }

    /**
     * creates a {@link ClassFinder} from a List of classpathElements.
     *
     * @param classpathElements
     *            the list of classpath elements
     * @return the {@link ClassFinder} that uses the classpathElements
     */
    public static ClassFinder getClassFinder(List<String> classpathElements) {

        URL[] urls = classpathElements.stream().distinct().map(File::new)
                .map(FlowFileUtils::convertToUrl).toArray(URL[]::new);

        return new ReflectionsClassFinder(urls);
    }

    /**
     * Location of the Token File.
     *
     * @param adapter
     *            - the PluginAdapterBase.
     * @return {@link File} the Token File
     */
    public static File getTokenFile(PluginAdapterBase adapter) {

        return new File(adapter.servletResourceOutputDirectory(), TOKEN_FILE);
    }

    /**
     * Prepares the Frontend
     *
     * @param adapter
     *            - the PluginAdapterBase.
     * @throws IOException
     *             - Could not forceMkdir(adapter.generatedFolder());
     * @throws ExecutionFailedException
     *             - While NodeTasks.execute()
     * @throws URISyntaxException
     *             - Could not build an URI from nodeDownloadRoot().
     */
    public static void prepareFrontend(PluginAdapterBase adapter)
            throws IOException, ExecutionFailedException, URISyntaxException {

        final URI nodeDownloadRootURI = adapter.nodeDownloadRoot();

        FrontendToolsSettings settings = getFrontendToolsSettings(adapter);
        FrontendTools tools = new FrontendTools(settings);
        tools.validateNodeAndNpmVersion();

        try {
            FileUtils.forceMkdir(adapter.generatedFolder());
        } catch (IOException e) {
            throw new IOException(
                    "Failed to create folder '" + adapter.generatedFolder()
                            + "'. Verify that you may write to path.",
                    e);
        }
        ClassFinder classFinder = adapter.getClassFinder();
        Lookup lookup = adapter.createLookup(classFinder);

        Options options = new Options(lookup, adapter.npmFolder(),
                adapter.generatedFolder(), adapter.frontendDirectory(),
                adapter.buildFolder())
                .useV14Bootstrap(adapter.isUseDeprecatedV14Bootstrapping())
                .withJarFrontendResourcesFolder(
                        getJarFrontendResourcesFolder(adapter))
                .createMissingPackageJson(true).enableImportsUpdate(false)
                .enablePackagesUpdate(false).runNpmInstall(false)
                .withFrontendGeneratedFolder(adapter.generatedTsFolder())
                .withNodeVersion(adapter.nodeVersion())
                .withNodeDownloadRoot(nodeDownloadRootURI)
                .setNodeAutoUpdate(adapter.nodeAutoUpdate())
                .withHomeNodeExecRequired(adapter.requireHomeNodeExec())
                .setJavaResourceFolder(adapter.javaResourceFolder())
                .withProductionMode(adapter.productionMode());

        // Copy jar artifact contents in TaskCopyFrontendFiles
        options.copyResources(adapter.getJarFiles());

        try {
            new NodeTasks(options).execute();
        } catch (ExecutionFailedException exception) {
            throw exception;
        } catch (Throwable throwable) { // NOSONAR Intentionally throwable
            throw new ExecutionFailedException(
                    "Error occured during goal execution: "
                            + throwable.getMessage()
                            + "\n\nPlease run Maven with the -e switch (or Gradle with the --stacktrace switch), to learn the full stack trace.",
                    throwable);
        }

    }

    private static File getJarFrontendResourcesFolder(
            PluginAdapterBase adapter) {
        return new File(
                new File(adapter.frontendDirectory(), FrontendUtils.GENERATED),
                FrontendUtils.JAR_RESOURCES_FOLDER);
    }

    private static FrontendToolsSettings getFrontendToolsSettings(
            PluginAdapterBase adapter) throws URISyntaxException {
        FrontendToolsSettings settings = new FrontendToolsSettings(
                adapter.npmFolder().getAbsolutePath(),
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        settings.setNodeDownloadRoot(adapter.nodeDownloadRoot());
        settings.setNodeVersion(adapter.nodeVersion());
        settings.setAutoUpdate(adapter.nodeAutoUpdate());
        settings.setUseGlobalPnpm(adapter.useGlobalPnpm());
        settings.setForceAlternativeNode(adapter.requireHomeNodeExec());

        return settings;
    }

    /**
     * Propagates the BuildInfo.
     *
     * @param adapter
     *            - the PluginAdapterBase.
     * @return the Token {@link File}.
     */
    public static File propagateBuildInfo(PluginAdapterBase adapter) {

        // For forked processes not accessing to System.properties we leave a
        // token file with the information about the build
        File token = new File(adapter.servletResourceOutputDirectory(),
                TOKEN_FILE);
        JsonObject buildInfo = Json.createObject();
        buildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE,
                adapter.productionMode());
        buildInfo.put(SERVLET_PARAMETER_USE_V14_BOOTSTRAP,
                adapter.isUseDeprecatedV14Bootstrapping());
        buildInfo.put(SERVLET_PARAMETER_INITIAL_UIDL,
                adapter.eagerServerLoad());
        buildInfo.put(NPM_TOKEN, adapter.npmFolder().getAbsolutePath());
        buildInfo.put(NODE_VERSION, adapter.nodeVersion());
        try {
            buildInfo.put(NODE_DOWNLOAD_ROOT,
                    adapter.nodeDownloadRoot().toString());
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger("BuildInfo").error(
                    "Configuration 'nodeDownloadRoot'  (property 'node.download.root') is defined incorrectly",
                    e);
        }
        buildInfo.put(GENERATED_TOKEN,
                adapter.generatedFolder().getAbsolutePath());
        buildInfo.put(FRONTEND_TOKEN,
                adapter.frontendDirectory().getAbsolutePath());
        buildInfo.put(CONNECT_JAVA_SOURCE_FOLDER_TOKEN,
                adapter.javaSourceFolder().getAbsolutePath());
        buildInfo.put(JAVA_RESOURCE_FOLDER_TOKEN,
                adapter.javaResourceFolder().getAbsolutePath());
        buildInfo.put(CONNECT_APPLICATION_PROPERTIES_TOKEN,
                adapter.applicationProperties().getAbsolutePath());
        buildInfo.put(CONNECT_OPEN_API_FILE_TOKEN,
                adapter.openApiJsonFile().getAbsolutePath());
        buildInfo.put(PROJECT_FRONTEND_GENERATED_DIR_TOKEN,
                adapter.generatedTsFolder().getAbsolutePath());

        buildInfo.put(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                adapter.pnpmEnable());
        buildInfo.put(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE,
                adapter.requireHomeNodeExec());

        buildInfo.put(InitParameters.BUILD_FOLDER, adapter.buildFolder());

        try {
            FileUtils.forceMkdir(token.getParentFile());
            FileUtils.write(token, JsonUtil.stringify(buildInfo, 2) + "\n",
                    StandardCharsets.UTF_8.name());
            // Enable debug to find out problems related with flow modes

            if (adapter.isDebugEnabled()) {
                adapter.logDebug(String.format(
                        "%n>>> Running prepare-frontend%nSystem"
                                + ".properties:%n productionMode: %s%n"
                                + " webpackPort: %s%n "
                                + "project.basedir: %s%nGoal parameters:%n "
                                + "productionMode: %s%n "
                                + "npmFolder: %s%nToken file: " + "%s%n"
                                + "Token content: %s%n",
                        adapter.productionMode(),
                        System.getProperty(
                                "vaadin.devmode.webpack.running-port"),
                        adapter.projectBaseDirectory(),
                        adapter.productionMode(), adapter.npmFolder(),
                        token.getAbsolutePath(), buildInfo.toJson()));
            }
            return token;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * runs the node-Updater.
     *
     * @param adapter
     *            - the PluginAdapterBase.
     * @throws ExecutionFailedException
     *             - a ExecutionFailedException.
     * @throws URISyntaxException
     *             - - Could not build an URI from nodeDownloadRoot().
     */
    public static void runNodeUpdater(PluginAdapterBuild adapter)
            throws ExecutionFailedException, URISyntaxException {

        Set<File> jarFiles = adapter.getJarFiles();
        final URI nodeDownloadRootURI;

        nodeDownloadRootURI = adapter.nodeDownloadRoot();

        ClassFinder classFinder = adapter.getClassFinder();

        Lookup lookup = adapter.createLookup(classFinder);

        try {
            Options options = new Options(lookup, adapter.npmFolder(),
                    adapter.generatedFolder(), adapter.frontendDirectory(),
                    adapter.buildFolder())
                    .runNpmInstall(adapter.runNpmInstall())
                    .withWebpack(adapter.webpackOutputDirectory(),
                            adapter.servletResourceOutputDirectory())
                    .useV14Bootstrap(adapter.isUseDeprecatedV14Bootstrapping())
                    .enablePackagesUpdate(true)
                    .useByteCodeScanner(adapter.optimizeBundle())
                    .withJarFrontendResourcesFolder(
                            getJarFrontendResourcesFolder(adapter))
                    .copyResources(jarFiles).copyTemplates(true)
                    .copyLocalResources(adapter.frontendResourcesDirectory())
                    .enableImportsUpdate(true)
                    .withEmbeddableWebComponents(
                            adapter.generateEmbeddableWebComponents())
                    .withTokenFile(BuildFrontendUtil.getTokenFile(adapter))
                    .enablePnpm(adapter.pnpmEnable())
                    .useGlobalPnpm(adapter.useGlobalPnpm())
                    .withApplicationProperties(adapter.applicationProperties())
                    .withEndpointSourceFolder(adapter.javaSourceFolder())
                    .withEndpointGeneratedOpenAPIFile(adapter.openApiJsonFile())
                    .withFrontendGeneratedFolder(adapter.generatedTsFolder())
                    .withHomeNodeExecRequired(adapter.requireHomeNodeExec())
                    .withNodeVersion(adapter.nodeVersion())
                    .withNodeDownloadRoot(nodeDownloadRootURI)
                    .setNodeAutoUpdate(adapter.nodeAutoUpdate())
                    .setJavaResourceFolder(adapter.javaResourceFolder())
                    .withPostinstallPackages(adapter.postinstallPackages())
                    .withCiBuild(adapter.ciBuild());
            new NodeTasks(options).execute();
        } catch (ExecutionFailedException exception) {
            throw exception;
        } catch (Throwable throwable) { // NOSONAR Intentionally throwable
            throw new ExecutionFailedException(
                    "Error occured during goal execution: "
                            + throwable.getMessage()
                            + "Please run Maven with the -e switch (or Gradle with the --stacktrace switch), to learn the full stack trace.",
                    throwable);
        }
    }

    /**
     * Execute the frontend build with the wanted build system.
     *
     * @param adapter
     *            - the PluginAdapterBase.
     * @throws TimeoutException
     *             - while running build system
     * @throws URISyntaxException
     *             - while parsing nodeDownloadRoot()) to URI
     */
    public static void runFrontendBuild(PluginAdapterBase adapter)
            throws TimeoutException, URISyntaxException {
        FeatureFlags featureFlags = getFeatureFlags(adapter);

        LicenseChecker.setStrictOffline(
                !featureFlags.isEnabled(FeatureFlags.OLD_LICENSE_CHECKER));

        FrontendToolsSettings settings = getFrontendToolsSettings(adapter);
        FrontendTools tools = new FrontendTools(settings);
        tools.validateNodeAndNpmVersion();
        if (featureFlags.isEnabled(FeatureFlags.WEBPACK)) {
            BuildFrontendUtil.runWebpack(adapter, tools);
        } else {
            BuildFrontendUtil.runVite(adapter, tools);
        }
    }

    /**
     * Runs the Webpack build
     *
     * @param adapter
     *            - the PluginAdapterBase.
     * @param frontendTools
     *            - frontend tools access object
     * @throws TimeoutException
     *             - while run webpack
     * @throws URISyntaxException
     *             - while parsing nodeDownloadRoot()) to URI
     */
    public static void runWebpack(PluginAdapterBase adapter,
            FrontendTools frontendTools)
            throws TimeoutException, URISyntaxException {
        runFrontendBuildTool(adapter, frontendTools, "Webpack",
                "webpack/bin/webpack.js",
                frontendTools.getWebpackNodeEnvironment());
    }

    /**
     * Runs the Vite build
     *
     * @param adapter
     *            - the PluginAdapterBase.
     * @param frontendTools
     *            - frontend tools access object
     * @throws TimeoutException
     *             - while running vite
     * @throws URISyntaxException
     *             - while parsing nodeDownloadRoot()) to URI
     */
    public static void runVite(PluginAdapterBase adapter,
            FrontendTools frontendTools)
            throws TimeoutException, URISyntaxException {
        runFrontendBuildTool(adapter, frontendTools, "Vite", "vite/bin/vite.js",
                Collections.emptyMap(), "build");
    }

    private static void runFrontendBuildTool(PluginAdapterBase adapter,
            FrontendTools frontendTools, String toolName, String executable,
            Map<String, String> environment, String... params)
            throws TimeoutException, URISyntaxException {

        File buildExecutable = new File(adapter.npmFolder(),
                NODE_MODULES + executable);
        if (!buildExecutable.isFile()) {
            throw new IllegalStateException(String.format(
                    "Unable to locate %s executable by path '%s'. Double"
                            + " check that the plugin is executed correctly",
                    toolName, buildExecutable.getAbsolutePath()));
        }

        String nodePath;
        if (adapter.requireHomeNodeExec()) {
            nodePath = frontendTools.forceAlternativeNodeExecutable();
        } else {
            nodePath = frontendTools.getNodeExecutable();
        }

        List<String> command = new ArrayList<>();
        command.add(nodePath);
        command.add(buildExecutable.getAbsolutePath());
        command.addAll(Arrays.asList(params));

        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);

        ProcessExecutor processExecutor = new ProcessExecutor()
                .command(builder.command()).environment(builder.environment())
                .environment(environment)
                .directory(adapter.projectBaseDirectory().toFile());

        adapter.logInfo("Running " + toolName + " ...");
        if (adapter.isDebugEnabled()) {
            adapter.logDebug(FrontendUtils.commandToString(
                    adapter.npmFolder().getAbsolutePath(), command));
        }
        try {
            processExecutor.exitValueNormal().readOutput(true).destroyOnExit()
                    .execute();
        } catch (InvalidExitValueException e) {
            throw new IllegalStateException(String.format(
                    "%s process exited with non-zero exit code.%nStderr: '%s'",
                    toolName, e.getResult().outputUTF8()), e);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(
                    String.format("Failed to run %s due to an error", toolName),
                    e);
        }

        // Check License
        validateLicenses(adapter);
    }

    private static void validateLicenses(PluginAdapterBase adapter) {
        File nodeModulesFolder = new File(adapter.npmFolder(),
                FrontendUtils.NODE_MODULES);

        File outputFolder = adapter.webpackOutputDirectory();
        File statsFile = new File(adapter.servletResourceOutputDirectory(),
                Constants.VAADIN_CONFIGURATION + "/stats.json");

        if (!statsFile.exists()) {
            throw new RuntimeException(
                    "Stats file " + statsFile + " does not exist");
        }
        List<Product> commercialComponents = findCommercialFrontendComponents(
                nodeModulesFolder, statsFile);
        commercialComponents.addAll(findCommercialJavaComponents(adapter));

        for (Product component : commercialComponents) {
            try {
                LicenseChecker.checkLicense(component.getName(),
                        component.getVersion(), BuildType.PRODUCTION);
            } catch (Exception e) {
                try {
                    getLogger().debug(
                            "License check for {} failed. Invalidating output",
                            component);

                    FileUtils.deleteDirectory(outputFolder);
                } catch (IOException e1) {
                    getLogger().debug("Failed to remove {}", outputFolder);
                }
                throw e;
            }
        }

    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BuildFrontendUtil.class);
    }

    private static List<Product> findCommercialFrontendComponents(
            File nodeModulesFolder, File statsFile) {
        List<Product> components = new ArrayList<>();
        try (InputStream in = new FileInputStream(statsFile)) {
            String contents = IOUtils.toString(in, StandardCharsets.UTF_8);
            JsonArray npmModules = Json.parse(contents).getArray("npmModules");
            for (int i = 0; i < npmModules.length(); i++) {
                String npmModule = npmModules.getString(i);
                Product product = CvdlProducts
                        .getProductIfCvdl(nodeModulesFolder, npmModule);
                if (product != null) {
                    components.add(product);
                }
            }
            return components;
        } catch (Exception e) {
            throw new RuntimeException("Error reading file " + statsFile, e);
        }
    }

    private static List<Product> findCommercialJavaComponents(
            PluginAdapterBase adapter) {
        List<Product> components = new ArrayList<>();

        for (File f : adapter.getJarFiles()) {
            try (JarFile jarFile = new JarFile(f)) {
                Manifest manifest = jarFile.getManifest();
                if (manifest == null) {
                    continue;
                }
                Attributes attributes = manifest.getMainAttributes();
                if (attributes == null) {
                    continue;
                }
                String cvdlName = attributes.getValue("CvdlName");
                if (cvdlName != null) {
                    String version = attributes
                            .getValue("Implementation-Version");
                    if (version == null) {
                        version = attributes.getValue("Bundle-Version");
                    }
                    Product p = new Product(cvdlName, version);
                    components.add(p);
                }
            } catch (IOException e) {
                getLogger().debug("Error reading manifest for jar " + f, e);
            }
        }

        return components;
    }

    private static FeatureFlags getFeatureFlags(PluginAdapterBase adapter) {
        ClassFinder classFinder = adapter.getClassFinder();

        Lookup lookup = adapter.createLookup(classFinder);

        final FeatureFlags featureFlags = new FeatureFlags(lookup);
        featureFlags.setPropertiesLocation(adapter.javaResourceFolder());
        return featureFlags;
    }

    /**
     * Add the devMode token to build token file so we don't try to start the
     * dev server. Remove the abstract folder paths as they should not be used
     * for prebuilt bundles.
     *
     * @param adapter
     *            - the PluginAdapterBase.
     *
     */
    public static void updateBuildFile(PluginAdapterBuild adapter) {

        File tokenFile = getTokenFile(adapter);
        if (!tokenFile.exists()) {
            adapter.logWarn(
                    "Couldn't update devMode token due to missing token file.");
            return;
        }
        try {
            String json = FileUtils.readFileToString(tokenFile,
                    StandardCharsets.UTF_8.name());
            JsonObject buildInfo = JsonUtil.parse(json);

            buildInfo.remove(NPM_TOKEN);
            buildInfo.remove(NODE_VERSION);
            buildInfo.remove(NODE_DOWNLOAD_ROOT);
            buildInfo.remove(GENERATED_TOKEN);
            buildInfo.remove(FRONTEND_TOKEN);
            buildInfo.remove(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM);
            buildInfo.remove(InitParameters.CI_BUILD);
            buildInfo.remove(InitParameters.REQUIRE_HOME_NODE_EXECUTABLE);
            buildInfo.remove(
                    InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE);
            buildInfo.remove(Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN);
            buildInfo.remove(Constants.JAVA_RESOURCE_FOLDER_TOKEN);
            buildInfo.remove(Constants.CONNECT_APPLICATION_PROPERTIES_TOKEN);
            buildInfo.remove(Constants.CONNECT_OPEN_API_FILE_TOKEN);
            buildInfo.remove(Constants.PROJECT_FRONTEND_GENERATED_DIR_TOKEN);
            buildInfo.remove(InitParameters.BUILD_FOLDER);

            buildInfo.put(SERVLET_PARAMETER_ENABLE_DEV_SERVER, false);
            FileUtils.write(tokenFile, JsonUtil.stringify(buildInfo, 2) + "\n",
                    StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            adapter.logWarn("Unable to read token file", e);
        }
    }
}
