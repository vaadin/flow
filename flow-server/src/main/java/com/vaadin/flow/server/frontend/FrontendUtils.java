/*
 * Copyright 2000-2021 Vaadin Ltd.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.FallbackChunk.CssImportData;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.STATISTICS_JSON_DEFAULT;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_STATISTICS_JSON;
import static com.vaadin.flow.server.frontend.FrontendTools.INSTALL_NODE_LOCALLY;
import static java.lang.String.format;

/**
 * A class for static methods and definitions that might be used in different
 * locations.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class FrontendUtils {

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
     * Default folder for client-side generated files inside the project root
     * frontend folder.
     */
    public static final String GENERATED = "generated/";

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
     * The name of the vite configuration file.
     */
    public static final String VITE_CONFIG = "vite.config.ts";

    /**
     * The name of the generated vite configuration file.
     */
    public static final String VITE_GENERATED_CONFIG = "vite.generated.ts";

    /**
     * The name of the webpack generated configuration file.
     */
    public static final String WEBPACK_GENERATED = "webpack.generated.js";

    /**
     * The name of the service worker source file for InjectManifest method of
     * workbox-webpack-plugin.
     */
    public static final String SERVICE_WORKER_SRC = "sw.ts";

    /**
     * The JavaScript version of the service worker file, for checking if a user
     * has a JavaScript version of a custom service worker file already.
     */
    public static final String SERVICE_WORKER_SRC_JS = "sw.js";

    /**
     * The NPM package name that will be used for the javascript files present
     * in jar resources that will to be copied to the npm folder so as they are
     * accessible to webpack.
     */
    public static final String FLOW_NPM_PACKAGE_NAME = NodeUpdater.DEP_NAME_FLOW_JARS
            + "/";

    /**
     * Default folder where front-end resources present in the classpath jars
     * are copied to. Relative to the
     * {@link com.vaadin.flow.server.InitParameters#BUILD_FOLDER}.
     */
    public static final String DEFAULT_FLOW_RESOURCES_FOLDER = "flow-frontend";

    /**
     * Default folder under build folder for copying front-end resources present
     * in the classpath jars.
     *
     * @deprecated This is deprecated due to a typo. Use
     *             DEFAULT_FLOW_RESOURCES_FOLDER instead.
     * @see #DEFAULT_FLOW_RESOURCES_FOLDER
     */
    @Deprecated
    public static final String DEAULT_FLOW_RESOURCES_FOLDER = DEFAULT_FLOW_RESOURCES_FOLDER;

    /**
     * Default folder name for flow generated stuff relative to the
     * {@link com.vaadin.flow.server.InitParameters#BUILD_FOLDER}.
     */
    public static final String DEFAULT_GENERATED_DIR = FRONTEND;

    /**
     * Name of the file that contains application imports, javascript, theme and
     * style annotations. It is also the entry-point for webpack. It is always
     * generated in the {@link FrontendUtils#DEFAULT_GENERATED_DIR} folder.
     */
    public static final String IMPORTS_NAME = "generated-flow-imports.js";

    /**
     * The TypeScript definitions for the {@link FrontendUtils#IMPORTS_NAME}
     * file.
     */
    public static final String IMPORTS_D_TS_NAME = "generated-flow-imports.d.ts";

    public static final String THEME_IMPORTS_D_TS_NAME = "theme.d.ts";
    public static final String THEME_IMPORTS_NAME = "theme.js";

    /**
     * File name of the bootstrap file that is generated in frontend
     * {@link #GENERATED} folder. The bootstrap file is always executed in a
     * Vaadin app.
     */
    public static final String BOOTSTRAP_FILE_NAME = "vaadin.ts";

    /**
     * File name of the index.html in client side.
     */
    public static final String INDEX_HTML = "index.html";

    /**
     * File name of the index.ts in client side.
     */
    public static final String INDEX_TS = "index.ts";

    /**
     * File name of the index.js in client side.
     */
    public static final String INDEX_JS = "index.js";

    /**
     * File name of Vite helper used in development mode.
     */
    public static final String VITE_DEVMODE_TS = "vite-devmode.ts";

    /**
     * Default Java source folder for OpenAPI generator.
     */
    public static final String DEFAULT_CONNECT_JAVA_SOURCE_FOLDER = "src/main/java";

    /**
     * Default application properties file path in Vaadin project.
     */
    public static final String DEFAULT_CONNECT_APPLICATION_PROPERTIES = "src/main/resources/application.properties";

    /**
     * Default generated path for OpenAPI spec file.
     */
    public static final String DEFAULT_CONNECT_OPENAPI_JSON_FILE = "generated-resources/openapi.json";

    /**
     * Default generated path for generated frontend files.
     */
    public static final String DEFAULT_PROJECT_FRONTEND_GENERATED_DIR = DEFAULT_FRONTEND_DIR
            + GENERATED;

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
     * The entry-point key used for the exported bundle.
     */
    public static final String EXPORT_CHUNK = "export";

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

    public static final String DISABLE_CHECK = "%nYou can disable the version check using -D%s=true";

    private static final String NO_CONNECTION = "Webpack-dev-server couldn't be reached for %s.%n"
            + "Check the startup logs for exceptions in running webpack-dev-server.%n"
            + "If server should be running in production mode check that production mode flag is set correctly.";

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

    // Proxy config properties keys (for both system properties and environment
    // variables) can be either fully upper case or fully lower case
    static final String SYSTEM_NOPROXY_PROPERTY_KEY = "NOPROXY";
    static final String SYSTEM_HTTPS_PROXY_PROPERTY_KEY = "HTTPS_PROXY";
    static final String SYSTEM_HTTP_PROXY_PROPERTY_KEY = "HTTP_PROXY";

    public static final String YELLOW = "\u001b[38;5;111m%s\u001b[0m";

    public static final String RED = "\u001b[38;5;196m%s\u001b[0m";

    public static final String GREEN = "\u001b[38;5;35m%s\u001b[0m";

    public static final String BRIGHT_BLUE = "\u001b[94m%s\u001b[0m";

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
        return System.getProperty("os.name");
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
     * Read a stream and copy the content into a String using system line
     * separators for all 'carriage return' characters.
     *
     * @param inputStream
     *            the input stream
     * @return the string
     */
    public static String streamToString(InputStream inputStream) {
        String ret = "";
        try (InputStream handledStream = inputStream) {
            return IOUtils.toString(handledStream, StandardCharsets.UTF_8)
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

            String pathEnvVar;
            if (isWindows()) {
                /*
                 * Determine the name of the PATH environment variable on
                 * Windows, as variables names are not case-sensitive (the
                 * common name is "Path").
                 */
                pathEnvVar = environment.keySet().stream()
                        .filter("PATH"::equalsIgnoreCase).findFirst()
                        .orElse("Path");
            } else {
                pathEnvVar = "PATH";
            }

            String path = environment.get(pathEnvVar);
            if (path == null || path.isEmpty()) {
                path = commandPath;
            } else if (!path.contains(commandPath)) {
                path += File.pathSeparatorChar + commandPath;
            }
            environment.put(pathEnvVar, path);
        }

        return processBuilder;
    }

    /**
     * Clears the <code>stats.json</code> cache within this
     * {@link VaadinContext}.
     *
     * @param service
     *            the vaadin service.
     */
    public static void clearCachedStatsContent(VaadinService service) {
        service.getContext().removeAttribute(Stats.class);
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
        return getFileContent(service, INDEX_HTML);
    }

    private static String getFileContent(VaadinService service, String path)
            throws IOException {
        DeploymentConfiguration config = service.getDeploymentConfiguration();
        InputStream content = null;

        try {
            Optional<DevModeHandler> devModeHandler = DevModeHandlerManager
                    .getDevModeHandler(service);
            if (!config.isProductionMode() && config.enableDevServer()
                    && devModeHandler.isPresent()) {
                content = getFileFromDevModeHandler(devModeHandler.get(), path);
            }

            if (content == null) {
                content = getFileFromClassPath(service, path);
            }
            return content != null ? streamToString(content) : null;
        } finally {
            IOUtils.closeQuietly(content);
        }
    }

    private static InputStream getFileFromClassPath(VaadinService service,
            String filePath) {
        final URL resource = service.getContext().getAttribute(Lookup.class)
                .lookup(ResourceProvider.class)
                .getApplicationResource(VAADIN_WEBAPP_RESOURCES + filePath);
        if (resource == null) {
            getLogger().error("Cannot get the '{}' from the classpath",
                    filePath);
            return null;
        }
        try {
            return resource.openStream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream getResourceFromWebpack(
            DevModeHandler devModeHandler, String resource,
            String exceptionMessage) throws IOException {
        HttpURLConnection statsConnection = devModeHandler
                .prepareConnection(resource, "GET");
        if (statsConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new WebpackConnectionException(
                    String.format(NO_CONNECTION, exceptionMessage));
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
                if (statistics == null || modified.isAfter(statistics
                        .getLastModified().orElse(LocalDateTime.MIN))) {
                    byte[] buffer = IOUtils
                            .toByteArray(connection.getInputStream());
                    statistics = new Stats(buffer, lastModified);
                    context.setAttribute(statistics);
                }
                return new ByteArrayInputStream(statistics.statsJson);
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
        Stats statistics = service.getContext().getAttribute(Stats.class);

        if (statistics != null) {
            return new ByteArrayInputStream(statistics.statsJson);
        }

        String stats = service.getDeploymentConfiguration()
                .getStringProperty(SERVLET_PARAMETER_STATISTICS_JSON,
                        VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT)
                // Remove absolute
                .replaceFirst("^/", "");
        ResourceProvider resourceProvider = service.getContext()
                .getAttribute(Lookup.class).lookup(ResourceProvider.class);
        URL statsUrl = resourceProvider.getApplicationResource(stats);
        InputStream stream = null;
        if (statsUrl != null) {
            try (InputStream statsStream = statsUrl.openStream()) {
                byte[] buffer = IOUtils.toByteArray(statsStream);
                statistics = new Stats(buffer, null);
                service.getContext().setAttribute(statistics);
                stream = new ByteArrayInputStream(buffer);
            } catch (IOException exception) {
                getLogger().warn("Couldn't read content of stats file {}",
                        stats, exception);
                stream = null;
            }
        }
        if (stream == null) {
            getLogger().error(
                    "Cannot get the 'stats.json' from the classpath '{}'",
                    stats);
        }
        return stream;
    }

    private static InputStream getFileFromDevModeHandler(
            DevModeHandler devModeHandler, String filePath) throws IOException {
        return devModeHandler.prepareConnection("/" + filePath, "GET")
                .getInputStream();
    }

    /**
     * Get the contents of a frontend file from the running dev server.
     *
     * @param service
     *            the Vaadin service.
     * @param path
     *            the file path.
     * @return an input stream for reading the file contents; null if there is
     *         no such file or the dev server is not running.
     */
    public static InputStream getFrontendFileFromDevModeHandler(
            VaadinService service, String path) {
        Optional<DevModeHandler> devModeHandler = DevModeHandlerManager
                .getDevModeHandler(service);
        if (devModeHandler.isPresent()) {
            try {
                File frontendFile = resolveFrontendPath(
                        devModeHandler.get().getProjectRoot(), path);
                return frontendFile == null ? null
                        : new FileInputStream(frontendFile);
            } catch (IOException e) {
                throw new UncheckedIOException("Error reading file " + path, e);
            }
        }
        return null;
    }

    /**
     * Looks up the front file at the given path. If the path starts with
     * {@code ./}, first look in {@code projectRoot/frontend}, then in
     * {@code projectRoot/node_modules/@vaadin/flow-frontend}. If the path does
     * not start with {@code ./}, look in {@code node_modules} instead.
     *
     * @param projectRoot
     *            the project root folder.
     * @param path
     *            the file path.
     * @return an existing {@link File} , or null if the file doesn't exist.
     */
    public static File resolveFrontendPath(File projectRoot, String path) {
        File localFrontendFolder = new File(projectRoot,
                FrontendUtils.FRONTEND);
        File nodeModulesFolder = new File(projectRoot, NODE_MODULES);
        File flowFrontendFolder = new File(nodeModulesFolder,
                "@vaadin/" + DEFAULT_FLOW_RESOURCES_FOLDER);
        List<File> candidateParents = path.startsWith("./")
                ? Arrays.asList(localFrontendFolder, flowFrontendFolder)
                : Arrays.asList(nodeModulesFolder, localFrontendFolder,
                        flowFrontendFolder);
        return candidateParents.stream().map(parent -> new File(parent, path))
                .filter(File::exists).findFirst().orElse(null);
    }

    /**
     * Load the asset chunks from <code>stats.json</code>. We will only read the
     * file until we have reached the assetsByChunkName json and return that as
     * a json object string.
     *
     * Note: The <code>stats.json</code> is cached when external stats is
     * enabled or <code>stats.json</code> is provided from the class path. To
     * clear the cache use {@link #clearCachedStatsContent(VaadinService)}.
     *
     * @param service
     *            the Vaadin service.
     * @return json for assetsByChunkName object in stats.json or {@code null}
     *         if stats.json not found or content not found.
     * @throws IOException
     *             if an I/O error occurs while creating the input stream.
     */
    public static String getStatsAssetsByChunkName(VaadinService service)
            throws IOException {
        DeploymentConfiguration config = service.getDeploymentConfiguration();
        Optional<DevModeHandler> devModeHandler = DevModeHandlerManager
                .getDevModeHandler(service);
        if (!config.isProductionMode() && config.enableDevServer()
                && devModeHandler.isPresent()) {
            return streamToString(getResourceFromWebpack(devModeHandler.get(),
                    "/assetsByChunkName", "getting assets by chunk name."));
        }
        InputStream resourceAsStream;
        if (config.isStatsExternal()) {
            resourceAsStream = getStatsFromExternalUrl(
                    config.getExternalStatsUrl(), service.getContext());
        } else {
            resourceAsStream = getStatsFromClassPath(service);
        }
        if (resourceAsStream == null) {
            return null;
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
            getLogger()
                    .error("Could not parse assetsByChunkName from stats.json");
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
        return configuration.getStringProperty(PARAM_FRONTEND_DIR,
                DEFAULT_FRONTEND_DIR);
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

    /**
     * Thrown when the command execution fails.
     */
    public static class CommandExecutionException extends Exception {
        /**
         * Constructs an exception telling what code the command execution
         * process was exited with.
         *
         * @param processExitCode
         *            process exit code
         */
        public CommandExecutionException(int processExitCode) {
            super("Process execution failed with exit code " + processExitCode);
        }

        /**
         * Constructs an exception telling what was the original exception the
         * command execution process failed with.
         *
         * @param cause
         *            the cause exception of process failure.
         */
        public CommandExecutionException(Throwable cause) {
            super("Process execution failed", cause);
        }
    }

    protected static FrontendVersion getVersion(String tool,
            List<String> versionCommand) throws UnknownVersionException {
        String output;
        try {
            output = executeCommand(versionCommand);
        } catch (CommandExecutionException e) {
            throw new UnknownVersionException(tool,
                    "Using command " + String.join(" ", versionCommand), e);
        }

        try {
            return new FrontendVersion(parseVersionString(output));
        } catch (IOException e) {
            throw new UnknownVersionException(tool,
                    "Expected a version number as output but got '" + output
                            + "'" + " when using command "
                            + String.join(" ", versionCommand),
                    e);
        }
    }

    /**
     * Executes a given command as a native process.
     *
     * @param command
     *            the command to be executed and it's arguments.
     * @return process output string.
     * @throws CommandExecutionException
     *             if the process completes exceptionally.
     */
    public static String executeCommand(List<String> command)
            throws CommandExecutionException {
        try {
            Process process = FrontendUtils.createProcessBuilder(command)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new CommandExecutionException(exitCode);
            }
            return streamToString(process.getInputStream());
        } catch (IOException | InterruptedException e) {
            throw new CommandExecutionException(e);
        }
    }

    /**
     * Parse the version number of node/npm from version output string.
     *
     * @param versionString
     *            string containing version output, typically produced by
     *            <code>tool --version</code>
     * @return FrontendVersion of versionString
     * @throws IOException
     *             if parsing fails
     */
    public static FrontendVersion parseFrontendVersion(String versionString)
            throws IOException {
        return new FrontendVersion((parseVersionString(versionString)));
    }

    /**
     * Gets vaadin home directory ({@code ".vaadin"} folder in the user home
     * dir).
     * <p>
     * The directory is created if it's doesn't exist.
     *
     * @return a vaadin home directory
     */
    public static File getVaadinHomeDirectory() {
        File home = FileUtils.getUserDirectory();
        if (!home.exists()) {
            throw new IllegalStateException("The user directory '"
                    + home.getAbsolutePath() + "' doesn't exist");
        }
        if (!home.isDirectory()) {
            throw new IllegalStateException("The path '"
                    + home.getAbsolutePath() + "' is not a directory");
        }
        File vaadinFolder = new File(home, ".vaadin");
        if (vaadinFolder.exists()) {
            if (vaadinFolder.isDirectory()) {
                return vaadinFolder;
            } else {
                throw new IllegalStateException("The path '"
                        + vaadinFolder.getAbsolutePath()
                        + "' is not a directory. "
                        + "This path is used to store vaadin related data. "
                        + "Please either remove the file or create a directory");
            }
        }
        try {
            FileUtils.forceMkdir(vaadinFolder);
            return vaadinFolder;
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Couldn't create '.vaadin' folder inside home directory '"
                            + home.getAbsolutePath() + "'",
                    exception);
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

    /**
     * Container class for caching the external stats.json contents.
     */
    private static class Stats implements Serializable {
        private final String lastModified;
        protected final byte[] statsJson;

        /**
         * Create a new container for stats.json caching.
         *
         * @param statsJson
         *            the gotten stats.json as a string
         * @param lastModified
         *            last modification timestamp for stats.json in RFC-1123
         *            date-time format, such as 'Tue, 3 Jun 2008 11:05:30 GMT'
         */
        public Stats(byte[] statsJson, String lastModified) {
            this.statsJson = statsJson;
            this.lastModified = lastModified;
        }

        /**
         * Return last modified timestamp for contained stats.json.
         *
         * @return timestamp as LocalDateTime
         */
        public Optional<LocalDateTime> getLastModified() {
            if (lastModified == null) {
                return Optional.empty();
            }
            return Optional.of(ZonedDateTime
                    .parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toLocalDateTime());
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
        StringBuilder retval = new StringBuilder("\n");
        StringBuilder curLine = new StringBuilder();
        for (String fragment : command) {
            if (curLine.length() + fragment.length() > 55) {
                retval.append(curLine.toString());
                retval.append("\\ \n");
                curLine = new StringBuilder("    ");
            }
            curLine.append(fragment.replace(baseDir, "."));
            curLine.append(" ");
        }
        retval.append(curLine.toString());
        retval.append("\n");
        return retval.toString();
    }

    /**
     * Tries to parse the given package's frontend version or if it doesn't
     * exist, returns {@code null}. In case the value cannot be parsed, logs an
     * error and returns {@code null}.
     *
     * @param sourceJson
     *            json object that has the package
     * @param pkg
     *            the package name
     * @param versionOrigin
     *            origin of the version (like a file), used in error message
     * @return the frontend version the package or {@code null}
     */
    public static FrontendVersion getPackageVersionFromJson(
            JsonObject sourceJson, String pkg, String versionOrigin) {
        if (!sourceJson.hasKey(pkg)) {
            return null;
        }
        try {
            final String versionString = sourceJson.getString(pkg);
            return new FrontendVersion(pkg, versionString);
        } catch (ClassCastException classCastException) { // NOSONAR
            LoggerFactory.getLogger(FrontendVersion.class).warn(
                    "Ignoring error while parsing frontend dependency version for package '{}' in '{}'",
                    pkg, versionOrigin);
        } catch (NumberFormatException nfe) {
            // intentionally not failing the build at this point
            LoggerFactory.getLogger(FrontendVersion.class).warn(
                    "Ignoring error while parsing frontend dependency version in {}: {}",
                    versionOrigin, nfe.getMessage());
        }
        return null;
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
    public static void console(String format, Object message) {
        System.out.print(format(format, message));
    }

    /**
     * Try to remove the {@code node_modules} directory, if it exists inside the
     * given base directory. Note that pnpm uses symlinks internally, so delete
     * utilities that follow symlinks when deleting and/or modifying permissions
     * may not work as intended.
     *
     * @param nodeModules
     *            the {@code node_modules} directory
     * @throws IOException
     *             on failure to delete any one file, or if the directory name
     *             is not {@code node_modules}
     */
    public static void deleteNodeModules(File nodeModules) throws IOException {
        if (!nodeModules.exists()) {
            return;
        }

        if (!nodeModules.isDirectory()
                || !nodeModules.getName().equals("node_modules")) {
            throw new IOException(nodeModules.getAbsolutePath()
                    + " does not look like a node_modules directory");
        }

        Path nodeModulesPath = nodeModules.toPath();
        try (Stream<Path> walk = Files.walk(nodeModulesPath)) {
            String undeletable = walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile).filter(file -> !file.delete())
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(", "));

            if (!undeletable.isEmpty()) {
                throw new IOException("Unable to delete files: " + undeletable);
            }
        }
    }
}
