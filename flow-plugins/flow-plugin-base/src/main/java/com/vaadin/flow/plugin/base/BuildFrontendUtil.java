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
package com.vaadin.flow.plugin.base;

import static com.vaadin.flow.server.Constants.CONNECT_APPLICATION_PROPERTIES_TOKEN;
import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.CONNECT_OPEN_API_FILE_TOKEN;
import static com.vaadin.flow.server.Constants.DISABLE_PREPARE_FRONTEND_CACHE;
import static com.vaadin.flow.server.Constants.FRONTEND_TOKEN;
import static com.vaadin.flow.server.Constants.JAVA_RESOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.NPM_TOKEN;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.PROJECT_FRONTEND_GENERATED_DIR_TOKEN;
import static com.vaadin.flow.server.InitParameters.FRONTEND_HOTDEPLOY;
import static com.vaadin.flow.server.InitParameters.NODE_DOWNLOAD_ROOT;
import static com.vaadin.flow.server.InitParameters.NODE_VERSION;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_INITIAL_UIDL;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.BundleValidationUtil;
import com.vaadin.flow.server.frontend.CvdlProducts;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeTasks;
import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;
import com.vaadin.flow.utils.FlowFileUtils;
import com.vaadin.pro.licensechecker.BuildType;
import com.vaadin.pro.licensechecker.LicenseChecker;
import com.vaadin.pro.licensechecker.Product;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

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

        ClassFinder classFinder = adapter.getClassFinder();
        Lookup lookup = adapter.createLookup(classFinder);

        Options options = new Options(lookup, adapter.npmFolder())
                .withFrontendDirectory(adapter.frontendDirectory())
                .withBuildDirectory(adapter.buildFolder())
                .withJarFrontendResourcesFolder(
                        getJarFrontendResourcesFolder(adapter))
                .createMissingPackageJson(
                        new File(adapter.npmFolder(), PACKAGE_JSON).exists())
                .enableImportsUpdate(false).enablePackagesUpdate(false)
                .withRunNpmInstall(false)
                .withFrontendGeneratedFolder(adapter.generatedTsFolder())
                .withNodeVersion(adapter.nodeVersion())
                .withNodeDownloadRoot(nodeDownloadRootURI)
                .setNodeAutoUpdate(adapter.nodeAutoUpdate())
                .withHomeNodeExecRequired(adapter.requireHomeNodeExec())
                .setJavaResourceFolder(adapter.javaResourceFolder())
                .withProductionMode(false);

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
        buildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE, false);
        buildInfo.put(SERVLET_PARAMETER_INITIAL_UIDL,
                adapter.eagerServerLoad());
        buildInfo.put(NPM_TOKEN, adapter.npmFolder().getAbsolutePath());
        buildInfo.put(NODE_VERSION, adapter.nodeVersion());
        if (adapter.isFrontendHotdeploy()) {
            buildInfo.put(FRONTEND_HOTDEPLOY, adapter.isFrontendHotdeploy());
        }
        try {
            buildInfo.put(NODE_DOWNLOAD_ROOT,
                    adapter.nodeDownloadRoot().toString());
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger("BuildInfo").error(
                    "Configuration 'nodeDownloadRoot'  (property 'node.download.root') is defined incorrectly",
                    e);
        }
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

        if (adapter.isPrepareFrontendCacheDisabled()) {
            buildInfo.put(DISABLE_PREPARE_FRONTEND_CACHE, true);
        }

        try {
            FileUtils.forceMkdir(token.getParentFile());
            FileUtils.write(token, JsonUtil.stringify(buildInfo, 2) + "\n",
                    StandardCharsets.UTF_8.name());
            // Enable debug to find out problems related with flow modes

            if (adapter.isDebugEnabled()) {
                adapter.logDebug(String.format(
                        "%n>>> Running prepare-frontend%nSystem"
                                + ".properties:%n "
                                + "project.basedir: %s%nGoal parameters:%n "
                                + "npmFolder: %s%nToken file: " + "%s%n"
                                + "Token content: %s%n",
                        adapter.projectBaseDirectory(), adapter.npmFolder(),
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
            Options options = new com.vaadin.flow.server.frontend.Options(
                    lookup, adapter.npmFolder())
                    .withFrontendDirectory(adapter.frontendDirectory())
                    .withBuildDirectory(adapter.buildFolder())
                    .withRunNpmInstall(adapter.runNpmInstall())
                    .withWebpack(adapter.webpackOutputDirectory(),
                            adapter.servletResourceOutputDirectory())
                    .enablePackagesUpdate(true)
                    .useByteCodeScanner(adapter.optimizeBundle())
                    .withJarFrontendResourcesFolder(
                            getJarFrontendResourcesFolder(adapter))
                    .copyResources(jarFiles).withCopyTemplates(true)
                    .copyLocalResources(adapter.frontendResourcesDirectory())
                    .enableImportsUpdate(true)
                    .withEmbeddableWebComponents(
                            adapter.generateEmbeddableWebComponents())
                    .withTokenFile(BuildFrontendUtil.getTokenFile(adapter))
                    .withEnablePnpm(adapter.pnpmEnable())
                    .useGlobalPnpm(adapter.useGlobalPnpm())
                    .withFrontendGeneratedFolder(adapter.generatedTsFolder())
                    .withHomeNodeExecRequired(adapter.requireHomeNodeExec())
                    .withNodeVersion(adapter.nodeVersion())
                    .withNodeDownloadRoot(nodeDownloadRootURI)
                    .setNodeAutoUpdate(adapter.nodeAutoUpdate())
                    .setJavaResourceFolder(adapter.javaResourceFolder())
                    .withPostinstallPackages(adapter.postinstallPackages())
                    .withCiBuild(adapter.ciBuild())
                    .withForceProductionBuild(adapter.forceProductionBuild());
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
     * Run a dev-bundle build.
     *
     * @param adapter
     *            - the PluginAdapterBase.
     * @throws ExecutionFailedException
     *             - a ExecutionFailedException.
     * @throws URISyntaxException
     *             - - Could not build an URI from nodeDownloadRoot().
     */
    public static void runDevBuildNodeUpdater(PluginAdapterBuild adapter)
            throws ExecutionFailedException, URISyntaxException, IOException {

        Set<File> jarFiles = adapter.getJarFiles();
        final URI nodeDownloadRootURI;

        nodeDownloadRootURI = adapter.nodeDownloadRoot();

        ClassFinder classFinder = adapter.getClassFinder();

        Lookup lookup = adapter.createLookup(classFinder);

        try {
            Options options = new com.vaadin.flow.server.frontend.Options(
                    lookup, adapter.npmFolder()).withProductionMode(false)
                    .withFrontendDirectory(adapter.frontendDirectory())
                    .withBuildDirectory(adapter.buildFolder())
                    .withRunNpmInstall(adapter.runNpmInstall())
                    .withWebpack(adapter.webpackOutputDirectory(),
                            adapter.servletResourceOutputDirectory())
                    .enablePackagesUpdate(true).useByteCodeScanner(false)
                    .withJarFrontendResourcesFolder(
                            getJarFrontendResourcesFolder(adapter))
                    .copyResources(jarFiles).withCopyTemplates(true)
                    .copyLocalResources(adapter.frontendResourcesDirectory())
                    .enableImportsUpdate(true)
                    .withEmbeddableWebComponents(
                            adapter.generateEmbeddableWebComponents())
                    .withTokenFile(BuildFrontendUtil.getTokenFile(adapter))
                    .withEnablePnpm(adapter.pnpmEnable())
                    .useGlobalPnpm(adapter.useGlobalPnpm())
                    .withFrontendGeneratedFolder(adapter.generatedTsFolder())
                    .withHomeNodeExecRequired(adapter.requireHomeNodeExec())
                    .withNodeVersion(adapter.nodeVersion())
                    .withNodeDownloadRoot(nodeDownloadRootURI)
                    .setNodeAutoUpdate(adapter.nodeAutoUpdate())
                    .setJavaResourceFolder(adapter.javaResourceFolder())
                    .withPostinstallPackages(adapter.postinstallPackages())
                    .withBundleBuild(true)
                    .skipDevBundleBuild(adapter.skipDevBundleBuild());
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
        LicenseChecker.setStrictOffline(true);

        FrontendToolsSettings settings = getFrontendToolsSettings(adapter);
        FrontendTools tools = new FrontendTools(settings);
        tools.validateNodeAndNpmVersion();
        BuildFrontendUtil.runVite(adapter, tools);
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
     */
    public static void runVite(PluginAdapterBase adapter,
            FrontendTools frontendTools) throws TimeoutException {
        runFrontendBuildTool(adapter, frontendTools, "Vite", "vite/bin/vite.js",
                Collections.emptyMap(), "build");
    }

    private static void runFrontendBuildTool(PluginAdapterBase adapter,
            FrontendTools frontendTools, String toolName, String executable,
            Map<String, String> environment, String... params)
            throws TimeoutException {

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
    }

    /**
     * Validate pro component licenses.
     *
     * @param adapter
     *            the PluginAdapterBase
     */
    public static void validateLicenses(PluginAdapterBase adapter) {
        File outputFolder = adapter.webpackOutputDirectory();

        String statsJsonContent = null;
        try {
            // First check for compiled bundle
            File statsFile = new File(adapter.servletResourceOutputDirectory(),
                    Constants.VAADIN_CONFIGURATION + "/stats.json");
            if (!statsFile.exists()) {
                // If no compiled bundle available check for jar-bundle
                statsJsonContent = BundleValidationUtil
                        .findProdBundleStatsJson(adapter.getClassFinder());
            } else {
                statsJsonContent = IOUtils.toString(statsFile.toURI().toURL(),
                        StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (statsJsonContent == null) {
            // without stats.json in bundle we can not say if it is up-to-date
            getLogger().debug(
                    "No production bundle stats.json available for licenses validation.");
            statsJsonContent = "{}";
        }

        FrontendDependenciesScanner scanner = new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                .createScanner(false, adapter.getClassFinder(), true, null);
        List<Product> commercialComponents = findCommercialFrontendComponents(
                scanner, statsJsonContent);
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

    static List<Product> findCommercialFrontendComponents(
            FrontendDependenciesScanner scanner, String statsJsonContent) {
        List<Product> components = new ArrayList<>();

        final JsonObject statsJson = Json.parse(statsJsonContent);
        Set<String> usedPackages = getUsedPackages(scanner);
        if (statsJson.hasKey("cvdlModules")) {
            final JsonObject cvdlModules = statsJson.getObject("cvdlModules");
            for (String key : cvdlModules.keys()) {
                if (!usedPackages.contains(key)) {
                    // If product is not used do not collect it.
                    continue;
                }
                final JsonObject cvdlModule = cvdlModules.getObject(key);
                components.add(new Product(cvdlModule.getString("name"),
                        cvdlModule.getString("version")));
            }
        }
        return components;
    }

    private static Set<String> getUsedPackages(
            FrontendDependenciesScanner scanner) {
        Set<String> usedPackages = new HashSet<>();
        Set<String> npmPackages = scanner.getPackages().keySet();
        Set<String> jsAndCssImports = new HashSet<String>();
        for (List<String> modules : scanner.getModules().values()) {
            jsAndCssImports.addAll(modules);
        }
        for (List<String> scripts : scanner.getScripts().values()) {
            jsAndCssImports.addAll(scripts);
        }
        for (String importPath : jsAndCssImports) {
            if (importPath.startsWith(".")) {
                continue;
            }
            if (importPath.contains("/")) {
                String[] parts = importPath.split("/");
                String potentialBasicPackage = parts[0];
                String potentialOrgPackage = parts[0] + "/" + parts[1];
                if (npmPackages.contains(potentialOrgPackage)) {
                    usedPackages.add(potentialOrgPackage);
                } else if (npmPackages.contains(potentialBasicPackage)) {
                    usedPackages.add(potentialBasicPackage);
                } else {
                    getLogger().debug(
                            "Import from an unknown package: " + importPath);
                }
            }
        }
        return usedPackages;

    }

    static List<Product> findCommercialJavaComponents(
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

    /**
     * Updates the build info after the bundle has been built by build-frontend.
     * <p>
     * Removes the abstract folder paths as they should not be used for prebuilt
     * bundles and ensures production mode is set to true.
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
            buildInfo.remove(FRONTEND_TOKEN);
            buildInfo.remove(FRONTEND_HOTDEPLOY);
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
            buildInfo.put(SERVLET_PARAMETER_PRODUCTION_MODE, true);

            FileUtils.write(tokenFile, JsonUtil.stringify(buildInfo, 2) + "\n",
                    StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            adapter.logWarn("Unable to read token file", e);
        }
    }

    /**
     * Delete the build token file. This is used with dev-bundle build as token
     * file should never be added to the package.
     *
     * @param adapter
     *            used plugin adapter implementation
     */
    public static void removeBuildFile(PluginAdapterBuild adapter)
            throws IOException {
        File tokenFile = getTokenFile(adapter);
        if (tokenFile.exists()) {
            FileUtils.delete(tokenFile);
        }
    }
}
