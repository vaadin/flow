/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.FallbackChunk.CssImportData;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_STATISTICS_JSON;
import static com.vaadin.flow.server.Constants.STATISTICS_JSON_DEFAULT;
import static com.vaadin.flow.server.Constants.VAADIN_MAPPING;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.YELLOW;
import static com.vaadin.flow.server.frontend.FrontendUtils.commandToString;
import static com.vaadin.flow.server.frontend.FrontendUtils.console;
import static java.lang.String.format;

/**
 * A class for static methods and definitions that might be used in different
 * locations.
 *
 * @since 2.0
 */
public class FrontendUtils {

    private static final String PNMP_INSTALLED_BY_NPM_FOLDER = "node_modules/pnpm/";

    private static final String PNMP_INSTALLED_BY_NPM = PNMP_INSTALLED_BY_NPM_FOLDER
            + "bin/pnpm.js";

    public static final String PROJECT_BASEDIR = "project.basedir";

    /**
     * Default folder for the node related content. It's the base directory for
     * {@link Constants#PACKAGE_JSON}, {@link FrontendUtils#WEBPACK_CONFIG},
     * {@link FrontendUtils#NODE_MODULES}.
     *
     * By default it's the project root folder.
     */
    public static final String DEFAULT_NODE_DIR = "./";

    /**
     * Location for the installed node packages. This folder is always
     * considered by node, even though we define extra folders with the
     * <code>NODE_PATH</code>.
     */
    public static final String NODE_MODULES = "node_modules/";

    /**
     * Default folder used for source and generated folders.
     */
    public static final String FRONTEND = "frontend/";

    /**
     * Path of the folder containing application frontend source files, it needs
     * to be relative to the {@link FrontendUtils#DEFAULT_NODE_DIR}
     *
     * By default it is <code>/frontend</code> in the project folder.
     */
    public static final String DEFAULT_FRONTEND_DIR = DEFAULT_NODE_DIR
            + FRONTEND;

    /**
     * The name of the webpack configuration file.
     */
    public static final String WEBPACK_CONFIG = "webpack.config.js";
    /**
     * The name of the webpack generated configuration file.
     */
    public static final String WEBPACK_GENERATED = "webpack.generated.js";

    /**
     * Default target folder for the java project.
     */
    public static final String TARGET = "target/";

    /**
     * The NPM package name that will be used for the javascript files present
     * in jar resources that will to be copied to the npm folder so as they are
     * accessible to webpack.
     */
    public static final String FLOW_NPM_PACKAGE_NAME = "@vaadin/flow-frontend/";


    /**
     * Default folder for copying front-end resources present in the classpath jars.
     */
    public static final String DEAULT_FLOW_RESOURCES_FOLDER = TARGET + "flow-frontend";

    /**
     * Default folder name for flow generated stuff relative to the
     * {@link FrontendUtils#TARGET}.
     */
    public static final String DEFAULT_GENERATED_DIR = TARGET + FRONTEND;

    /**
     * Name of the file that contains application imports, javascript, theme and
     * style annotations. It is also the entry-point for webpack. It is always
     * generated in the {@link FrontendUtils#DEFAULT_GENERATED_DIR} folder.
     */
    public static final String IMPORTS_NAME = "generated-flow-imports.js";

    /**
     * The TypeScript definitions for the {@link FrontendUtils#IMPORTS_NAME} file.
     */
    public static final String IMPORTS_D_TS_NAME = "generated-flow-imports.d.ts";

    /**
     * File name of the index.html in clientSideMode.
     */
    public static final String INDEX_HTML = "index.html";

    /**
     * File name of the index.ts in clientSideMode.
     */
    public static final String INDEX_TS = "index.ts";

    /**
     * File name of the index.js in clientSideMode.
     */
    public static final String INDEX_JS = "index.js";

    /**
     * Default Java source folder for OpenAPI generator.
     */
    public static final String DEFAULT_CONNECT_JAVA_SOURCE_FOLDER = "src/main/java";

    /**
     * Default application properties file path in Connect project.
     */
    public static final String DEFAULT_CONNECT_APPLICATION_PROPERTIES = "src/main/resources/application.properties";

    /**
     * Default generated path for OpenAPI spec file.
     */
    public static final String DEFAULT_CONNECT_OPENAPI_JSON_FILE = TARGET
            + "generated-resources/openapi.json";

    /**
     * Default generated path for generated TS files.
     */
    public static final String DEFAULT_CONNECT_GENERATED_TS_DIR = DEFAULT_FRONTEND_DIR + "generated/";

    /**
     * Name of the file that contains all application imports, javascript, theme
     * and style annotations which are not discovered by the current scanning
     * strategy (but they are in the project classpath). This file is
     * dynamically imported by the {@link FrontendUtils#IMPORTS_NAME} file. It
     * is always generated in the {@link FrontendUtils#DEFAULT_GENERATED_DIR}
     * folder.
     */
    public static final String FALLBACK_IMPORTS_NAME = "generated-flow-imports-fallback.js";

    /**
     * A parameter for overriding the
     * {@link FrontendUtils#DEFAULT_GENERATED_DIR} folder.
     */
    public static final String PARAM_GENERATED_DIR = "vaadin.frontend.generated.folder";

    /**
     * A parameter for overriding the {@link FrontendUtils#DEFAULT_FRONTEND_DIR}
     * folder.
     */
    public static final String PARAM_FRONTEND_DIR = "vaadin.frontend.frontend.folder";

    /**
     * Set to {@code true} to ignore node/npm tool version checks.
     */
    public static final String PARAM_IGNORE_VERSION_CHECKS = "vaadin.ignoreVersionChecks";

    /**
     * A special prefix used by webpack to map imports placed in the
     * {@link FrontendUtils#DEFAULT_FRONTEND_DIR}. e.g.
     * <code>import 'Frontend/foo.js';</code> references the
     * file<code>frontend/foo.js</code>.
     */
    public static final String WEBPACK_PREFIX_ALIAS = "Frontend/";

    /**
     * File used to enable npm mode.
     */
    public static final String TOKEN_FILE = Constants.VAADIN_CONFIGURATION
            + "flow-build-info.json";

    /**
     * A key in a Json object for chunks list.
     */
    public static final String CHUNKS = "chunks";

    /**
     * A key in a Json object for fallback chunk.
     */
    public static final String FALLBACK = "fallback";

    /**
     * A key in a Json object for css imports data.
     */
    public static final String CSS_IMPORTS = "cssImports";

    /**
     * A key in a Json object for js modules data.
     */
    public static final String JS_MODULES = "jsModules";

    /**
     * A parameter informing about the location of the
     * {@link FrontendUtils#TOKEN_FILE}.
     */
    public static final String PARAM_TOKEN_FILE = "vaadin.frontend.token.file";

    public static final String INSTALL_NODE_LOCALLY = "%n  $ mvn com.github.eirslett:frontend-maven-plugin:1.7.6:install-node-and-npm -DnodeVersion=\"v12.13.0\" ";
    public static final String DISABLE_CHECK = "%nYou can disable the version check using -D%s=true";

    private static final String NO_CONNECTION = "Webpack-dev-server couldn't be reached for %s.%n"
            + "Check the startup logs for exceptions in running webpack-dev-server.%n"
            + "If server should be running in production mode check that production mode flag is set correctly.";

    private static final String NOT_FOUND = "%n%n======================================================================================================"
            + "%nFailed to determine '%s' tool." + "%nPlease install it either:"
            + "%n  - by following the https://nodejs.org/en/download/ guide to install it globally"
            + "%n  - or by running the frontend-maven-plugin goal to install it in this project:"
            + INSTALL_NODE_LOCALLY
            + "%n======================================================================================================%n";

    private static final String SHOULD_WORK = "%n%n======================================================================================================"
            + "%nYour installed '%s' version (%s) is not supported but should still work. Supported versions are %d.%d+" //
            + "%nYou can install a new one:"
            + "%n  - by following the https://nodejs.org/en/download/ guide to install it globally"
            + "%n  - or by running the frontend-maven-plugin goal to install it in this project:"
            + INSTALL_NODE_LOCALLY + "%n" //
            + DISABLE_CHECK //
            + "%n======================================================================================================%n";

    private static final String TOO_OLD = "%n%n======================================================================================================"
            + "%nYour installed '%s' version (%s) is too old. Supported versions are %d.%d+" //
            + "%nPlease install a new one either:"
            + "%n  - by following the https://nodejs.org/en/download/ guide to install it globally"
            + "%n  - or by running the frontend-maven-plugin goal to install it in this project:"
            + INSTALL_NODE_LOCALLY + "%n" //
            + DISABLE_CHECK //
            + "%n======================================================================================================%n";

    private static final String BAD_VERSION = "%n%n======================================================================================================"
            + "%nYour installed '%s' version (%s) is known to have problems." //
            + "%nPlease update to a new one either:"
            + "%n  - by following the https://nodejs.org/en/download/ guide to install it globally"
            + "%s"
            + "%n  - or by running the frontend-maven-plugin goal to install it in this project:"
            + INSTALL_NODE_LOCALLY + "%n" //
            + DISABLE_CHECK //
            + "%n======================================================================================================%n";

    private static final List<FrontendVersion> NPM_BLACKLISTED_VERSIONS = Arrays
            .asList(new FrontendVersion("6.11.0"),
                    new FrontendVersion("6.11.1"),
                    new FrontendVersion("6.11.2"));

    private static final FrontendVersion SUPPORTED_NODE_VERSION = new FrontendVersion(
            Constants.SUPPORTED_NODE_MAJOR_VERSION,
            Constants.SUPPORTED_NODE_MINOR_VERSION);
    private static final FrontendVersion SHOULD_WORK_NODE_VERSION = new FrontendVersion(
            Constants.SHOULD_WORK_NODE_MAJOR_VERSION,
            Constants.SHOULD_WORK_NODE_MINOR_VERSION);

    private static final FrontendVersion SUPPORTED_NPM_VERSION = new FrontendVersion(
            Constants.SUPPORTED_NPM_MAJOR_VERSION,
            Constants.SUPPORTED_NPM_MINOR_VERSION);
    private static final FrontendVersion SHOULD_WORK_NPM_VERSION = new FrontendVersion(
            Constants.SHOULD_WORK_NPM_MAJOR_VERSION,
            Constants.SHOULD_WORK_NPM_MINOR_VERSION);

    private static FrontendToolsLocator frontendToolsLocator = new FrontendToolsLocator();

    private static String operatingSystem = null;

    public static final String YELLOW = "\u001b[38;5;111m%s\u001b[0m";

    public static final String RED = "\u001b[38;5;196m%s\u001b[0m";

    public static final String GREEN = "\u001b[38;5;35m%s\u001b[0m";

    /**
     * Only static stuff here.
     */
    private FrontendUtils() {
    }

    /**
     * Get the Operating System name from the {@code os.name} system property.
     *
     * @return operating system name
     */
    public static String getOsName() {
        if (operatingSystem == null) {
            operatingSystem = System.getProperty("os.name");
        }
        return operatingSystem;
    }

    /**
     * Check if the current os is Windows.
     *
     * @return true if windows
     */
    public static boolean isWindows() {
        return getOsName().startsWith("Windows");
    }

    /**
     * Locate <code>node</code> executable.
     *
     * @param baseDir
     *            project root folder.
     *
     * @return the full path to the executable
     */
    public static String getNodeExecutable(String baseDir) {
        String command = isWindows() ? "node.exe" : "node";
        String defaultNode = FrontendUtils.isWindows() ? "node/node.exe"
                : "node/node";
        return getExecutable(baseDir, command, defaultNode).getAbsolutePath();
    }

    /**
     * Locate <code>npm</code> executable.
     *
     * @param baseDir
     *            project root folder.
     *
     * @return the list of all commands in sequence that need to be executed to
     *         have npm running
     */
    public static List<String> getNpmExecutable(String baseDir) {
        // If `node` is not found in PATH, `node/node_modules/npm/bin/npm` will
        // not work because it's a shell or windows script that looks for node
        // and will fail. Thus we look for the `npm-cli` node script instead
        File file = new File(baseDir, "node/node_modules/npm/bin/npm-cli.js");
        List<String> returnCommand = new ArrayList<>();
        if (file.canRead()) {
            // We return a two element list with node binary and npm-cli script
            returnCommand.add(getNodeExecutable(baseDir));
            returnCommand.add(file.getAbsolutePath());
        } else {
            // Otherwise look for regulag `npm`
            String command = isWindows() ? "npm.cmd" : "npm";
            returnCommand.add(
                    getExecutable(baseDir, command, null).getAbsolutePath());
        }
        returnCommand.add("--no-update-notifier");
        return returnCommand;
    }

    /**
     * Locate <code>pnpm</code> executable.
     * <p>
     * In case pnpm is not available it will be installed.
     *
     * @param baseDir
     *            project root folder.
     *
     * @return the list of all commands in sequence that need to be executed to
     *         have pnpm running
     * @see #getPnpmExecutable(String, boolean)
     */
    public static List<String> getPnpmExecutable(String baseDir) {
        ensurePnpm(baseDir, true);
        return getPnpmExecutable(baseDir, true);
    }

    /**
     * Locate <code>pnpm</code> executable if it's possible.
     * <p>
     * In case the tool is not found either {@link IllegalStateException} is
     * thrown or an empty list is returned depending on {@code failOnAbsence}
     * value.
     *
     * @param baseDir
     *            project root folder.
     * @param failOnAbsence
     *            if {@code true} throws IllegalStateException if tool is not
     *            found, if {@code false} return an empty list if tool is not
     *            found
     *
     * @return the list of all commands in sequence that need to be executed to
     *         have pnpm running
     */
    public static List<String> getPnpmExecutable(String baseDir,
            boolean failOnAbsence) {
        // First try local pnpm JS script if it exists
        List<String> returnCommand = new ArrayList<>();
        Optional<File> localPnpmScript = getLocalPnpmScript(baseDir);
        if (localPnpmScript.isPresent()) {
            returnCommand.add(getNodeExecutable(baseDir));
            returnCommand.add(localPnpmScript.get().getAbsolutePath());
        } else {
            // Otherwise look for regular `pnpm`
            String command = isWindows() ? "pnpm.cmd" : "pnpm";
            if (failOnAbsence) {
                returnCommand.add(getExecutable(baseDir, command, null)
                        .getAbsolutePath());
            } else {
                returnCommand.addAll(frontendToolsLocator.tryLocateTool(command)
                        .map(File::getPath).map(Collections::singletonList)
                        .orElse(Collections.emptyList()));
            }
        }
        if (!returnCommand.isEmpty()) {
            returnCommand.add("--shamefully-hoist=true");
        }
        return returnCommand;
    }

    /**
     * Locate <code>bower</code> executable.
     * <p>
     * An empty list is returned if bower is not found
     *
     * @param baseDir
     *            project root folder.
     *
     * @return the list of all commands in sequence that need to be executed to
     *         have bower running, an empty list if bower is not found
     */
    public static List<String> getBowerExecutable(String baseDir) {
        File file = new File(baseDir, "node_modules/bower/bin/bower");
        if (file.canRead()) {
            // We return a two element list with node binary and bower script
            return Arrays.asList(getNodeExecutable(baseDir),
                    file.getAbsolutePath());
        }
        // Otherwise look for a regular `bower`
        String command = isWindows() ? "bower.cmd" : "bower";
        return frontendToolsLocator.tryLocateTool(command).map(File::getPath)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }

    private static File getExecutable(String baseDir, String cmd,
            String defaultLocation) {
        File file = null;
        try {
            file = defaultLocation == null
                    ? frontendToolsLocator.tryLocateTool(cmd).orElse(null)
                    : Optional.of(new File(baseDir, defaultLocation))
                            .filter(frontendToolsLocator::verifyTool)
                            .orElseGet(() -> frontendToolsLocator
                                    .tryLocateTool(cmd).orElse(null));
        } catch (Exception e) { // NOSONAR
            // There are IOException coming from process fork
        }
        if (file == null) {
            throw new IllegalStateException(String.format(NOT_FOUND, cmd));
        }
        return file;
    }

    /**
     * Read a stream and copy the content in a String.
     *
     * @param inputStream
     *            the input stream
     * @return the string
     */
    public static String streamToString(InputStream inputStream) {
        String ret = "";
        try {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8)
                    .replaceAll("\\R", System.lineSeparator());
        } catch (IOException exception) {
            // ignore exception on close()
            LoggerFactory.getLogger(FrontendUtils.class)
                    .warn("Couldn't close template input stream", exception);
        }
        return ret;
    }

    /**
     * Creates a process builder for the given list of program and arguments. If
     * the program is defined as an absolute path, then the directory that
     * contains the program is also appended to PATH so that the it can locate
     * related tools.
     *
     * @param command
     *            a list with the program and arguments
     * @return a configured process builder
     */
    public static ProcessBuilder createProcessBuilder(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        /*
         * Ensure the location of the command to run is in PATH. This is in some
         * cases needed by npm to locate a node binary.
         */
        File commandFile = new File(command.get(0));
        if (commandFile.isAbsolute()) {
            String commandPath = commandFile.getParent();

            Map<String, String> environment = processBuilder.environment();
            String path = environment.get("PATH");
            if (path == null || path.isEmpty()) {
                path = commandPath;
            } else if (!path.contains(commandPath)) {
                path += File.pathSeparatorChar + commandPath;
            }
            environment.put("PATH", path);
        }

        return processBuilder;
    }

    /**
     * Gets the content of the <code>stats.json</code> file produced by webpack.
     *
     * @param service
     *            the vaadin service.
     * @return the content of the file as a string, null if not found.
     * @throws IOException
     *             on error reading stats file.
     */
    public static String getStatsContent(VaadinService service)
            throws IOException {
        DeploymentConfiguration config = service.getDeploymentConfiguration();
        InputStream content = null;

        if (!config.isProductionMode() && config.enableDevServer()) {
            content = getStatsFromWebpack();
        }

        if (config.isStatsExternal()) {
            content = getStatsFromExternalUrl(config.getExternalStatsUrl(),
                    service.getContext());
        }

        if (content == null) {
            content = getStatsFromClassPath(service);
        }
        return content != null
                ? IOUtils.toString(content, StandardCharsets.UTF_8)
                : null;
    }

    /**
     * Gets the content of the <code>frontend/index.html</code> file which is
     * served by webpack-dev-server in dev-mode and read from classpath in
     * production mode. NOTE: In dev mode, the file content file is fetched via
     * webpack http request. So that we don't need to have a separate
     * index.html's content watcher, auto-reloading will work automatically,
     * like other files managed by webpack in `frontend/` folder.
     *
     * @param service
     *            the vaadin service
     * @return the content of the index html file as a string, null if not
     *         found.
     * @throws IOException
     *             on error when reading file
     *
     */
    public static String getIndexHtmlContent(VaadinService service)
            throws IOException {
        String indexHtmlPathInDevMode = "/" + VAADIN_MAPPING + INDEX_HTML;
        String indexHtmlPathInProductionMode = VAADIN_SERVLET_RESOURCES
                + INDEX_HTML;
        return getFileContent(service, indexHtmlPathInDevMode,
                indexHtmlPathInProductionMode);
    }

    private static String getFileContent(VaadinService service,
            String pathInDevMode, String pathInProductionMode)
            throws IOException {
        DeploymentConfiguration config = service.getDeploymentConfiguration();
        InputStream content = null;

        if (!config.isProductionMode() && config.enableDevServer()) {
            content = getFileFromWebpack(pathInDevMode);
        }

        if (content == null) {
            content = getFileFromClassPath(service, pathInProductionMode);
        }
        return content != null ? streamToString(content) : null;
    }

    private static InputStream getFileFromClassPath(VaadinService service,
            String filePath) {
        InputStream stream = service.getClassLoader()
                .getResourceAsStream(filePath);
        if (stream == null) {
            getLogger().error("Cannot get the '{}' from the classpath",
                    filePath);
        }
        return stream;
    }

    /**
     * Get the latest has for the stats file in development mode. This is
     * requested from the webpack-dev-server.
     * <p>
     * In production mode and disabled dev server mode an empty string is
     * returned.
     *
     * @param service
     *            the Vaadin service.
     * @return hash string for the stats.json file, empty string if none found
     * @throws IOException
     *             if an I/O error occurs while creating the input stream.
     */
    public static String getStatsHash(VaadinService service)
            throws IOException {
        DeploymentConfiguration config = service.getDeploymentConfiguration();
        if (!config.isProductionMode() && config.enableDevServer()) {
            DevModeHandler handler = DevModeHandler.getDevModeHandler();
            HttpURLConnection statsConnection = handler
                    .prepareConnection("/stats.hash", "GET");
            if (statsConnection
                    .getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new WebpackConnectionException(String.format(
                        NO_CONNECTION, "getting the stats content hash."));
            }
            return streamToString(statsConnection.getInputStream())
                    .replaceAll("\"", "");
        }

        return "";
    }

    private static InputStream getStatsFromWebpack() throws IOException {
        DevModeHandler handler = DevModeHandler.getDevModeHandler();
        HttpURLConnection statsConnection = handler
                .prepareConnection("/stats.json", "GET");
        if (statsConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new WebpackConnectionException(
                    String.format(NO_CONNECTION, "downloading stats.json"));
        }
        return statsConnection.getInputStream();
    }

    private static InputStream getStatsFromExternalUrl(String externalStatsUrl,
            VaadinContext context) {
        String url;
        // If url is relative try to get host from request
        // else fallback on 127.0.0.1:8080
        if (externalStatsUrl.startsWith("/")) {
            VaadinRequest request = VaadinRequest.getCurrent();
            url = getHostString(request) + externalStatsUrl;
        } else {
            url = externalStatsUrl;
        }
        try {
            URL uri = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) uri
                    .openConnection();
            connection.setRequestMethod("GET");
            // one minute timeout should be enough
            connection.setReadTimeout(60000);
            connection.setConnectTimeout(60000);
            String lastModified = connection.getHeaderField("last-modified");
            if (lastModified != null) {
                LocalDateTime modified = ZonedDateTime
                        .parse(lastModified,
                                DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toLocalDateTime();
                Stats statistics = context.getAttribute(Stats.class);
                if (statistics == null
                        || modified.isAfter(statistics.getLastModified())) {
                    statistics = new Stats(
                            streamToString(connection.getInputStream()),
                            lastModified);
                    context.setAttribute(statistics);
                }
                return new ByteArrayInputStream(
                        statistics.statsJson.getBytes(StandardCharsets.UTF_8));
            }
            return connection.getInputStream();
        } catch (IOException e) {
            getLogger().error("Failed to retrieve stats.json from the url {}.",
                    url, e);
        }
        return null;
    }

    private static String getHostString(VaadinRequest request) {
        String host = request.getHeader("host");
        if (host == null) {
            host = "http://127.0.0.1:8080";
        } else if (!host.contains("://")) {
            String scheme = request.getHeader("scheme");
            if (scheme == null) {
                scheme = "http";
            }
            host = scheme + "://" + host;
        }
        return host;
    }

    private static InputStream getStatsFromClassPath(VaadinService service) {
        String stats = service.getDeploymentConfiguration()
                .getStringProperty(SERVLET_PARAMETER_STATISTICS_JSON,
                        VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT)
                // Remove absolute
                .replaceFirst("^/", "");
        InputStream stream = service.getClassLoader()
                .getResourceAsStream(stats);
        if (stream == null) {
            getLogger().error(
                    "Cannot get the 'stats.json' from the classpath '{}'",
                    stats);
        }
        return stream;
    }

    private static InputStream getFileFromWebpack(String filePath)
            throws IOException {
        DevModeHandler handler = DevModeHandler.getDevModeHandler();
        return handler.prepareConnection(filePath, "GET").getInputStream();
    }

    /**
     * Load the asset chunks from stats.json. We will only read the file until
     * we have reached the assetsByChunkName json and return that as a json
     * object string.
     *
     * @param service
     *            the Vaadin service.
     * @return json for assetsByChunkName object in stats.json
     * @throws IOException
     *             if an I/O error occurs while creating the input stream.
     */
    public static String getStatsAssetsByChunkName(VaadinService service)
            throws IOException {
        DeploymentConfiguration config = service.getDeploymentConfiguration();
        if (!config.isProductionMode() && config.enableDevServer()) {
            DevModeHandler handler = DevModeHandler.getDevModeHandler();
            HttpURLConnection assetsConnection = handler
                    .prepareConnection("/assetsByChunkName", "GET");
            if (assetsConnection
                    .getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new WebpackConnectionException(String.format(
                        NO_CONNECTION, "getting assets by chunk name."));
            }
            return streamToString(assetsConnection.getInputStream());
        }
        InputStream resourceAsStream;
        if (config.isStatsExternal()) {
            resourceAsStream = getStatsFromExternalUrl(
                    config.getExternalStatsUrl(), service.getContext());
        } else {
            String stats = config
                    .getStringProperty(SERVLET_PARAMETER_STATISTICS_JSON,
                            VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT)
                    // Remove absolute
                    .replaceFirst("^/", "");
            resourceAsStream = service.getClassLoader()
                    .getResourceAsStream(stats);
        }
        try (Scanner scan = new Scanner(resourceAsStream,
                StandardCharsets.UTF_8.name())) {
            StringBuilder assets = new StringBuilder();
            assets.append("{");
            // Scan until we reach the assetsByChunkName object line
            scanToAssetChunkStart(scan, assets);
            // Add lines until we reach the first } breaking the object
            while (scan.hasNextLine()) {
                String line = scan.nextLine().trim();
                if ("}".equals(line) || "},".equals(line)) {
                    // Encountering } or }, means end of asset chunk
                    return assets.append("}").toString();
                } else if (line.endsWith("}") || line.endsWith("},")) {
                    return assets
                            .append(line.substring(0, line.indexOf('}')).trim())
                            .append("}").toString();
                } else if (line.contains("{")) {
                    // Encountering { means something is wrong as the assets
                    // should only contain key-value pairs.
                    break;
                }
                assets.append(line);
            }
        }
        return null;
    }

    /**
     * Scan until we reach the assetsByChunkName json object start. If faulty
     * format add first jsonObject to assets builder.
     *
     * @param scan
     *            Scanner used to scan data
     * @param assets
     *            assets builder
     */
    private static void scanToAssetChunkStart(Scanner scan,
            StringBuilder assets) {
        do {
            String line = scan.nextLine().trim();
            // Walk file until we get to the assetsByChunkName object.
            if (line.startsWith("\"assetsByChunkName\"")) {
                if (!line.endsWith("{")) {
                    assets.append(line.substring(line.indexOf('{') + 1).trim());
                }
                break;
            }
        } while (scan.hasNextLine());
    }

    /**
     * Validate that the found node and npm versions are new enough. Throws an
     * exception with a descriptive message if a version is too old.
     *
     * @param baseDir
     *            project root folder.
     */
    public static void validateNodeAndNpmVersion(String baseDir) {
        try {
            List<String> nodeVersionCommand = new ArrayList<>();
            nodeVersionCommand.add(FrontendUtils.getNodeExecutable(baseDir));
            nodeVersionCommand.add("--version");
            FrontendVersion nodeVersion = getVersion("node",
                    nodeVersionCommand);
            validateToolVersion("node", nodeVersion, SUPPORTED_NODE_VERSION,
                    SHOULD_WORK_NODE_VERSION);
        } catch (UnknownVersionException e) {
            getLogger().warn("Error checking if node is new enough", e);
        }

        try {
            List<String> npmVersionCommand = new ArrayList<>(
                    FrontendUtils.getNpmExecutable(baseDir));
            npmVersionCommand.add("--version");
            FrontendVersion npmVersion = getVersion("npm", npmVersionCommand);
            validateToolVersion("npm", npmVersion, SUPPORTED_NPM_VERSION,
                    SHOULD_WORK_NPM_VERSION);
            checkForFaultyNpmVersion(npmVersion);
        } catch (UnknownVersionException e) {
            getLogger().warn("Error checking if npm is new enough", e);
        }

    }

    /**
     * Ensure that pnpm tool is available and install it if it's not.
     *
     * @param baseDir
     *            project root folder.
     * @param ensure
     *            whether pnpm tool should be installed if it's absent
     */
    public static void ensurePnpm(String baseDir, boolean ensure) {
        if (ensure && getPnpmExecutable(baseDir, false).isEmpty()) {
            // copy the current content of package.json file to a temporary
            // location
            File packageJson = new File(baseDir, "package.json");
            File tempFile = null;
            boolean packageJsonExists = packageJson.canRead();
            if (packageJsonExists) {
                try {
                    tempFile = File.createTempFile("package", "json");
                    FileUtils.copyFile(packageJson, tempFile);
                } catch (IOException exception) {
                    throw new IllegalStateException(
                            "Couldn't make a copy of package.json file",
                            exception);
                }
                packageJson.delete();
            }
            // install pnpm locally using npm
            installPnpm(baseDir, getNpmExecutable(baseDir));

            // remove package-lock.json which contains pnpm as a dependency.
            new File(baseDir, "package-lock.json").delete();

            if (packageJsonExists && tempFile != null) {
                // return back the original package.json
                try {
                    FileUtils.copyFile(tempFile, packageJson);
                } catch (IOException exception) {
                    throw new IllegalStateException(
                            "Couldn't restore package.json file back",
                            exception);
                }
                tempFile.delete();
            }
        }
    }

    static void checkForFaultyNpmVersion(FrontendVersion npmVersion) {
        if (NPM_BLACKLISTED_VERSIONS.contains(npmVersion)) {
            String badNpmVersion = buildBadVersionString("npm",
                    npmVersion.getFullVersion(),
                    "by updating your global npm installation with `npm install -g npm@latest`");
            throw new IllegalStateException(badNpmVersion);
        }
    }

    private static void installPnpm(String baseDir,
            List<String> installCommand) {
        List<String> command = new ArrayList<>();
        command.addAll(installCommand);
        command.add("install");
        command.add("pnpm@4.5.0");

        console(YELLOW, commandToString(baseDir, command));

        ProcessBuilder builder = createProcessBuilder(command);
        builder.environment().put("ADBLOCK", "1");
        builder.directory(new File(baseDir));

        Process process = null;
        try {
            process = builder.inheritIO().start();
            int errorCode = process.waitFor();
            if (errorCode != 0) {
                getLogger().error("Couldn't install 'pnpm'");
            } else {
                getLogger().debug("Pnpm is successfully installed");
            }
        } catch (InterruptedException | IOException e) {
            getLogger().error("Error when running `npm install`", e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    private static String buildTooOldString(String tool, String version,
            int supportedMajor, int supportedMinor) {
        return String.format(TOO_OLD, tool, version, supportedMajor,
                supportedMinor, PARAM_IGNORE_VERSION_CHECKS);
    }

    private static String buildShouldWorkString(String tool, String version,
            int supportedMajor, int supportedMinor) {
        return String.format(SHOULD_WORK, tool, version, supportedMajor,
                supportedMinor, PARAM_IGNORE_VERSION_CHECKS);
    }

    private static String buildBadVersionString(String tool, String version,
            String... extraUpdateInstructions) {
        StringBuilder extraInstructions = new StringBuilder();
        for (String instruction : extraUpdateInstructions) {
            extraInstructions.append("%n  - or ").append(instruction);
        }
        return String.format(BAD_VERSION, tool, version,
                extraInstructions.toString(), PARAM_IGNORE_VERSION_CHECKS);
    }

    /**
     * Get directory where project's frontend files are located.
     *
     * @param configuration
     *            the current deployment configuration
     *
     * @return {@link #DEFAULT_FRONTEND_DIR} or value of
     *         {@link #PARAM_FRONTEND_DIR} if it is set.
     */
    public static String getProjectFrontendDir(
            DeploymentConfiguration configuration) {
        return configuration
                .getStringProperty(PARAM_FRONTEND_DIR, DEFAULT_FRONTEND_DIR);
    }

    /**
     * Checks whether the {@code file} is a webpack configuration file with the
     * expected content (includes a configuration generated by Flow).
     *
     * @param file
     *            a file to check
     * @return {@code true} iff the file exists and includes a generated
     *         configuration
     * @throws IOException
     *             if an I/O error occurs while reading the file
     */
    public static boolean isWebpackConfigFile(File file) throws IOException {
        return file.exists()
                && FileUtils.readFileToString(file, StandardCharsets.UTF_8)
                        .contains("./webpack.generated.js");
    }

    /**
     * Get relative path from a source path to a target path in Unix form. All
     * the Windows' path separator will be replaced.
     *
     * @param source
     *            the source path
     * @param target
     *            the target path
     * @return unix relative path from source to target
     */
    public static String getUnixRelativePath(Path source, Path target) {
        return getUnixPath(source.relativize(target));
    }

    /**
     * Get path as a String in Unix form.
     *
     * @param source
     *            path to get
     * @return path as a String in Unix form.
     */
    public static String getUnixPath(Path source) {
        return source.toString().replaceAll("\\\\", "/");
    }

    /**
     * Read fallback chunk data from a json object.
     *
     * @param object
     *            json object to read fallback chunk data
     * @return a fallback chunk data
     */
    public static FallbackChunk readFallbackChunk(JsonObject object) {
        if (!object.hasKey(CHUNKS)) {
            return null;
        }
        JsonObject obj = object.getObject(CHUNKS);
        if (!obj.hasKey(FALLBACK)) {
            return null;
        }
        obj = obj.getObject(FALLBACK);
        List<String> fallbackModles = new ArrayList<>();
        JsonArray modules = obj.getArray(JS_MODULES);
        for (int i = 0; i < modules.length(); i++) {
            fallbackModles.add(modules.getString(i));
        }
        List<CssImportData> fallbackCss = new ArrayList<>();
        JsonArray css = obj.getArray(CSS_IMPORTS);
        for (int i = 0; i < css.length(); i++) {
            fallbackCss.add(createCssData(css.getObject(i)));
        }
        return new FallbackChunk(fallbackModles, fallbackCss);
    }

    private static CssImportData createCssData(JsonObject object) {
        String value = null;
        String id = null;
        String include = null;
        String themeFor = null;
        if (object.hasKey("value")) {
            value = object.getString("value");
        }
        if (object.hasKey("id")) {
            id = object.getString("id");
        }
        if (object.hasKey("include")) {
            include = object.getString("include");
        }
        if (object.hasKey("themeFor")) {
            themeFor = object.getString("themeFor");
        }
        return new CssImportData(value, id, include, themeFor);
    }

    static void validateToolVersion(String tool, FrontendVersion toolVersion,
            FrontendVersion supported, FrontendVersion shouldWork) {
        if ("true".equalsIgnoreCase(
                System.getProperty(PARAM_IGNORE_VERSION_CHECKS))) {
            return;
        }

        if (isVersionAtLeast(toolVersion, supported)) {
            return;
        }
        if (isVersionAtLeast(toolVersion, shouldWork)) {
            getLogger().warn(buildShouldWorkString(tool,
                    toolVersion.getFullVersion(), supported.getMajorVersion(),
                    supported.getMinorVersion()));
            return;
        }

        throw new IllegalStateException(buildTooOldString(tool,
                toolVersion.getFullVersion(), supported.getMajorVersion(),
                supported.getMinorVersion()));
    }

    static boolean isVersionAtLeast(FrontendVersion toolVersion,
            FrontendVersion required) {
        int major = toolVersion.getMajorVersion();
        int minor = toolVersion.getMinorVersion();
        return (major > required.getMajorVersion()
                || (major == required.getMajorVersion()
                        && minor >= required.getMinorVersion()));
    }

    /**
     * Thrown when detecting the version of a tool fails.
     */
    public static class UnknownVersionException extends Exception {

        /**
         * Constructs an exception telling which tool was being detected and
         * using what command.
         *
         * @param tool
         *            the tool being detected
         * @param extraInfo
         *            extra information which might be helpful to the end user
         */
        public UnknownVersionException(String tool, String extraInfo) {
            super("Unable to detect version of " + tool + ". " + extraInfo);
        }

        /**
         * Constructs an exception telling which tool was being detected and
         * using what command, and the exception causing the failure.
         *
         * @param tool
         *            the tool being detected
         * @param extraInfo
         *            extra information which might be helpful to the end user
         * @param cause
         *            the exception causing the failure
         */
        public UnknownVersionException(String tool, String extraInfo,
                Exception cause) {
            super("Unable to detect version of " + tool + ". " + extraInfo,
                    cause);
        }
    }

    private static FrontendVersion getVersion(String tool,
            List<String> versionCommand) throws UnknownVersionException {
        try {
            Process process = FrontendUtils.createProcessBuilder(versionCommand)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new UnknownVersionException(tool,
                        "Using command " + String.join(" ", versionCommand));
            }
            String output = streamToString(process.getInputStream());
            return new FrontendVersion(parseVersionString(output));
        } catch (InterruptedException | IOException e) {
            throw new UnknownVersionException(tool,
                    "Using command " + String.join(" ", versionCommand), e);
        }
    }

    /**
     * Parse the version number of node/npm from the given output.
     *
     * @param output
     *            The output, typically produced by <code>tool --version</code>
     * @return the parsed version as an array with 3-4 elements
     * @throws IOException
     *             if parsing fails
     */
    static String parseVersionString(String output) throws IOException {
        Optional<String> lastOuput = Stream.of(output.split("\n"))
                .filter(line -> !line.matches("^[ ]*$"))
                .reduce((first, second) -> second);
        return lastOuput.map(line -> line.replaceFirst("^v", ""))
                .orElseThrow(() -> new IOException("No output"));
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(FrontendUtils.class);
    }

    private static Optional<File> getLocalPnpmScript(String baseDir) {
        File npmInstalled = new File(baseDir, PNMP_INSTALLED_BY_NPM);
        if (npmInstalled.canRead()) {
            return Optional.of(npmInstalled);
        }

        // For version 4.3.3 check ".ignored" folders
        File movedPnpmScript = new File(baseDir,
                "node_modules/.ignored_pnpm/bin/pnpm.js");
        if (movedPnpmScript.canRead()) {
            return Optional.of(movedPnpmScript);
        }

        movedPnpmScript = new File(baseDir,
                "node_modules/.ignored/pnpm/bin/pnpm.js");
        if (movedPnpmScript.canRead()) {
            return Optional.of(movedPnpmScript);
        }
        return Optional.empty();
    }

    /**
     * Container class for caching the external stats.json contents.
     */
    private static class Stats implements Serializable {
        private final String lastModified;
        protected final String statsJson;

        /**
         * Create a new container for stats.json caching.
         *
         * @param statsJson
         *            the gotten stats.json as a string
         * @param lastModified
         *            last modification timestamp for stats.json in RFC-1123
         *            date-time format, such as 'Tue, 3 Jun 2008 11:05:30 GMT'
         */
        public Stats(String statsJson, String lastModified) {
            this.statsJson = statsJson;
            this.lastModified = lastModified;
        }

        /**
         * Return last modified timestamp for contained stats.json.
         *
         * @return timestamp as LocalDateTime
         */
        public LocalDateTime getLastModified() {
            return ZonedDateTime
                    .parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toLocalDateTime();
        }
    }

    /**
     * Pretty prints a command line order. It split in lines adapting to 80
     * columns, and allowing copy and paste in console. It also removes the
     * current directory to avoid security issues in log files.
     *
     * @param baseDir
     *            the current directory
     * @param command
     *            the command and it's arguments
     * @return the string for printing in logs
     */
    public static String commandToString(String baseDir, List<String> command) {
        return "\n" + WordUtils
                .wrap(String.join(" ", command).replace(baseDir, "."), 50)
                .replace("\n", " \\ \n    ") + "\n";
    }

    /**
     * Intentionally send to console instead to log, useful when executing
     * external processes.
     *
     * @param format
     *            Format of the line to send to console, it must contain a `%s`
     *            outlet for the message
     * @param message
     *            the string to show
     */
    @SuppressWarnings("squid:S106")
    public
    static void console(String format, Object message) {
        System.out.print(format(format, message));
    }
}
