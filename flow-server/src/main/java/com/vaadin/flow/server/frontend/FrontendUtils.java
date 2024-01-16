/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.FallbackChunk.CssImportData;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.STATISTICS_JSON_DEFAULT;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_STATISTICS_JSON;
import static com.vaadin.flow.server.frontend.FrontendTools.INSTALL_NODE_LOCALLY;

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
     * Default folder for copying front-end resources present in the classpath
     * jars.
     */
    public static final String DEAULT_FLOW_RESOURCES_FOLDER = TARGET
            + "flow-frontend";

    /**
     * Development mode gizmo JS module.
     */
    public static final String DEVMODE_GIZMO_MODULE = FLOW_NPM_PACKAGE_NAME
            + "VaadinDevmodeGizmo.js";

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

    private static String operatingSystem = null;

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
     * Get the first non null value from the given array.
     *
     * @param valueArray
     *            array of values to get non null from
     * @return first non null value or null if no values found
     */
    private static String getNonNull(String... valueArray) {
        for (String value : valueArray) {
            if (value != null) {
                return value;
            }
        }
        return null;
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
     * Gets the content of the <code>stats.json</code> file produced by webpack.
     *
     * Note: Caches the <code>stats.json</code> when external stats is enabled
     * or <code>stats.json</code> is provided from the class path. To clear the
     * cache use {@link #clearCachedStatsContent(VaadinService)}.
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

        try {
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
        } finally {
            IOUtils.closeQuietly(content);
        }
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

    private static InputStream getStatsFromClassPath(VaadinService service)
            throws IOException {
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
        URL statsUrl = resourceProvider
                .getApplicationResource(service.getContext(), stats);
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
         * Constructs an exception telling what code the command execution
         * process was exited with and the output that it produced.
         *
         * @param processExitCode
         *            process exit code
         * @param output
         *            the output from the command
         * @param errorOutput
         *            the error output from the command
         */
        public CommandExecutionException(int processExitCode, String output,
                String errorOutput) {
            super("Process execution failed with exit code " + processExitCode
                    + "\nOutput: " + output + "\nError output: " + errorOutput);
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
        try {
            String output = executeCommand(versionCommand);
            return new FrontendVersion(parseVersionString(output));
        } catch (IOException | CommandExecutionException e) {
            throw new UnknownVersionException(tool,
                    "Using command " + String.join(" ", versionCommand), e);
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
                throw new CommandExecutionException(exitCode,
                        streamToString(process.getInputStream()),
                        streamToString(process.getErrorStream()));
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
        } catch (ClassCastException classCastException) {
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
     *
     * /** Intentionally send to console instead to log, useful when executing
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
        System.out.print(String.format(format, message));
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

        deleteDirectory(nodeModules);
    }

    /**
     * Recursively delete given directory and contents.
     * <p>
     * Will not delete contents of symlink or junction directories, only the
     * link file.
     *
     * @param directory
     *            directory to delete
     * @throws IOException
     *             on failure to delete or read any one file
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        if (!(Files.isSymbolicLink(directory.toPath())
                || isJunction(directory.toPath()))) {
            cleanDirectory(directory);
        }

        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Check that directory is not a windows junction which is basically a
     * symlink.
     *
     * @param directory
     *            directory path to check
     * @return true if directory is a windows junction
     * @throws IOException
     *             if an I/O error occurs
     */
    private static boolean isJunction(Path directory) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase()
                .contains("windows");
        BasicFileAttributes attrs = Files.readAttributes(directory,
                BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return isWindows && attrs.isDirectory() && attrs.isOther();
    }

    private static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) { // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (File file : files) {
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (exception != null) {
            throw exception;
        }
    }

    private static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent) {
                    throw new FileNotFoundException(
                            "File does not exist: " + file);
                }
                String message = "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }
}
