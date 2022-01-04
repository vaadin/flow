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
package com.vaadin.flow.plugin.base;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeTasks;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;
import com.vaadin.flow.utils.FlowFileUtils;

import elemental.json.Json;
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
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_ENABLE_DEV_SERVER;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_INITIAL_UIDL;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_USE_V14_BOOTSTRAP;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER;
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
        File flowResourcesFolder = new File(adapter.npmFolder(),
                Paths.get(adapter.buildFolder(), DEFAULT_FLOW_RESOURCES_FOLDER)
                        .toString());
        ClassFinder classFinder = adapter.getClassFinder();
        Lookup lookup = adapter.createLookup(classFinder);

        NodeTasks.Builder builder = new NodeTasks.Builder(lookup,
                adapter.npmFolder(), adapter.generatedFolder(),
                adapter.frontendDirectory(), adapter.buildFolder())
                        .useV14Bootstrap(
                                adapter.isUseDeprecatedV14Bootstrapping())
                        .withFlowResourcesFolder(flowResourcesFolder)
                        .createMissingPackageJson(true)
                        .enableImportsUpdate(false).enablePackagesUpdate(false)
                        .runNpmInstall(false)
                        .withNodeVersion(adapter.nodeVersion())
                        .withNodeDownloadRoot(nodeDownloadRootURI)
                        .setNodeAutoUpdate(adapter.nodeAutoUpdate())
                        .withHomeNodeExecRequired(adapter.requireHomeNodeExec())
                        .setJavaResourceFolder(adapter.javaResourceFolder())
                        .withProductionMode(adapter.productionMode());

        // Copy jar artifact contents in TaskCopyFrontendFiles
        builder.copyResources(adapter.getJarFiles());

        try {
            builder.build().execute();
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
        File flowResourcesFolder = new File(adapter.npmFolder(),
                Paths.get(adapter.buildFolder(), DEFAULT_FLOW_RESOURCES_FOLDER)
                        .toString());
        final URI nodeDownloadRootURI;

        nodeDownloadRootURI = adapter.nodeDownloadRoot();

        ClassFinder classFinder = adapter.getClassFinder();

        Lookup lookup = adapter.createLookup(classFinder);

        try {
            new NodeTasks.Builder(lookup, adapter.npmFolder(),
                    adapter.generatedFolder(), adapter.frontendDirectory(),
                    adapter.buildFolder())
                            .runNpmInstall(adapter.runNpmInstall())
                            .withWebpack(adapter.webpackOutputDirectory(),
                                    adapter.servletResourceOutputDirectory(),
                                    adapter.webpackTemplate(),
                                    adapter.webpackGeneratedTemplate())
                            .useV14Bootstrap(
                                    adapter.isUseDeprecatedV14Bootstrapping())
                            .enablePackagesUpdate(true)
                            .useByteCodeScanner(adapter.optimizeBundle())
                            .withFlowResourcesFolder(flowResourcesFolder)
                            .copyResources(jarFiles).copyTemplates(true)
                            .copyLocalResources(
                                    adapter.frontendResourcesDirectory())
                            .enableImportsUpdate(true)
                            .withEmbeddableWebComponents(
                                    adapter.generateEmbeddableWebComponents())
                            .withTokenFile(
                                    BuildFrontendUtil.getTokenFile(adapter))
                            .enablePnpm(adapter.pnpmEnable())
                            .useGlobalPnpm(adapter.useGlobalPnpm())
                            .withHillaApplicationProperties(
                                    adapter.applicationProperties())
                            .withHillaJavaSourceFolder(
                                    adapter.javaSourceFolder())
                            .withHillaGeneratedOpenAPIJson(
                                    adapter.openApiJsonFile())
                            .withHillaClientAPIFolder(
                                    adapter.generatedTsFolder())
                            .withHomeNodeExecRequired(
                                    adapter.requireHomeNodeExec())
                            .withNodeVersion(adapter.nodeVersion())
                            .withNodeDownloadRoot(nodeDownloadRootURI)
                            .setNodeAutoUpdate(adapter.nodeAutoUpdate())
                            .setJavaResourceFolder(adapter.javaResourceFolder())
                            .withPostinstallPackages(
                                    adapter.postinstallPackages())
                            .build().execute();
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
        ClassFinder classFinder = adapter.getClassFinder();

        Lookup lookup = adapter.createLookup(classFinder);

        final FeatureFlags featureFlags = new FeatureFlags(lookup);
        featureFlags.setPropertiesLocation(adapter.javaResourceFolder());
        if (featureFlags.isEnabled(FeatureFlags.VITE)) {
            BuildFrontendUtil.runVite(adapter);
        } else {
            BuildFrontendUtil.runWebpack(adapter);
        }
    }

    /**
     * Runs the Webpack build
     *
     * @param adapter
     *            - the PluginAdapterBase.
     * @throws TimeoutException
     *             - while run webpack
     * @throws URISyntaxException
     *             - while parsing nodeDownloadRoot()) to URI
     */
    public static void runWebpack(PluginAdapterBase adapter)
            throws TimeoutException, URISyntaxException {
        runFrontendBuildTool(adapter, "Webpack", "webpack/bin/webpack.js");
    }

    /**
     * Runs the Vite build
     *
     * @param adapter
     *            - the PluginAdapterBase.
     * @throws TimeoutException
     *             - while running vite
     * @throws URISyntaxException
     *             - while parsing nodeDownloadRoot()) to URI
     */
    public static void runVite(PluginAdapterBase adapter)
            throws TimeoutException, URISyntaxException {
        runFrontendBuildTool(adapter, "Vite", "vite/bin/vite.js", "build");
    }

    private static void runFrontendBuildTool(PluginAdapterBase adapter,
            String toolName, String executable, String... params)
            throws TimeoutException, URISyntaxException {

        File buildExecutable = new File(adapter.npmFolder(),
                NODE_MODULES + executable);
        if (!buildExecutable.isFile()) {
            throw new IllegalStateException(String.format(
                    "Unable to locate webpack executable by path '%s'. Double"
                            + " check that the plugin is executed correctly",
                    buildExecutable.getAbsolutePath()));
        }

        String nodePath;
        FrontendToolsSettings settings = getFrontendToolsSettings(adapter);
        FrontendTools tools = new FrontendTools(settings);
        if (adapter.requireHomeNodeExec()) {
            nodePath = tools.forceAlternativeNodeExecutable();
        } else {
            nodePath = tools.getNodeExecutable();
        }

        List<String> command = new ArrayList<>();
        command.add(nodePath);
        command.add(buildExecutable.getAbsolutePath());
        command.addAll(Arrays.asList(params));

        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);

        ProcessExecutor processExecutor = new ProcessExecutor()
                .command(builder.command()).environment(builder.environment())
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
                    "Failed to run webpack due to an error", e);
        }
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
            buildInfo.remove(GENERATED_TOKEN);
            buildInfo.remove(FRONTEND_TOKEN);
            buildInfo.remove(Constants.SERVLET_PARAMETER_ENABLE_PNPM);
            buildInfo.remove(Constants.REQUIRE_HOME_NODE_EXECUTABLE);
            buildInfo.remove(
                    Constants.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE);
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
