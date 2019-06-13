/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_STATISTICS_JSON;
import static com.vaadin.flow.server.Constants.STATISTICS_JSON_DEFAULT;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinService;

/**
 * A class for static methods and definitions that might be used in different
 * locations.
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
     * The NPM package name that will be used for the javascript files present
     * in jar resources that will to be copied to the npm folder so as they are
     * accessible to webpack.
     */
    public static final String FLOW_NPM_PACKAGE_NAME = "@vaadin/flow-frontend/";

    /**
     * Default target folder for the java project.
     */
    public static final String TARGET = "target/";

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
    public static final String TOKEN_FILE = Constants.VAADIN_CONFIGURATION + "flow-build-info.json";

    /**
     * A parameter informing about the location of the
     * {@link FrontendUtils#TOKEN_FILE}.
     */
    public static final String PARAM_TOKEN_FILE = "vaadin.frontend.token.file";

    private static final String NOT_FOUND = "%n%n======================================================================================================"
            + "%nFailed to determine '%s' tool." + "%nPlease install it either:"
            + "%n  - by following the https://nodejs.org/en/download/ guide to install it globally"
            + "%n  - or by running the frontend-maven-plugin goal to install it in this project:"
            + "%n  $ mvn com.github.eirslett:frontend-maven-plugin:1.7.6:install-node-and-npm -DnodeVersion=\"v10.16.0\" "
            + "%n======================================================================================================%n";

    private static final String SHOULD_WORK = "%n%n======================================================================================================"
            + "%nYour installed '%s' version (%s) is not supported but should still work. Supported versions are %d.%d+" //
            + "%nYou can install a new one:"
            + "%n  - by following the https://nodejs.org/en/download/ guide to install it globally"
            + "%n  - or by running the frontend-maven-plugin goal to install it in this project:"
            + "%n  $ mvn com.github.eirslett:frontend-maven-plugin:1.7.6:install-node-and-npm -DnodeVersion=\"v10.16.0\" "
            + "%n" //
            + "%nYou can disable the version check using -D%s=true" //
            + "%n======================================================================================================%n";

    private static final String TOO_OLD = "%n%n======================================================================================================"
            + "%nYour installed '%s' version (%s) is too old. Supported versions are %d.%d+" //
            + "%nPlease install a new one either:"
            + "%n  - by following the https://nodejs.org/en/download/ guide to install it globally"
            + "%n  - or by running the frontend-maven-plugin goal to install it in this project:"
            + "%n  $ mvn com.github.eirslett:frontend-maven-plugin:1.7.6:install-node-and-npm -DnodeVersion=\"v11.6.0\" "
            + "%n" //
            + "%nYou can disable the version check using -D%s=true" //
            + "%n======================================================================================================%n";

    private static FrontendToolsLocator frontendToolsLocator = new FrontendToolsLocator();

    private static String operatingSystem = null;

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
     * Computes the project root folder. This is useful in case build is
     * executed from a different working dir or when we want to change it for
     * testing purposes.
     *
     * @return folder location
     */
    public static String getBaseDir() {
        return System.getProperty(PROJECT_BASEDIR,
                System.getProperty("user.dir", "."));
    }

    /**
     * Locate <code>node</code> executable.
     *
     * @return the full path to the executable
     */
    public static String getNodeExecutable() {
        String command = isWindows() ? "node.exe" : "node";
        String defaultNode = FrontendUtils.isWindows() ? "node/node.exe"
                : "node/node";
        return getExecutable(command, defaultNode).getAbsolutePath();
    }

    /**
     * Locate <code>npm</code> executable.
     *
     * @return the a list of all commands in sequence that need to be executed
     *         to have npm running
     */
    public static List<String> getNpmExecutable() {
        // If `node` is not found in PATH, `node/node_modules/npm/bin/npm` will
        // not work
        // because it's a shell or windows script that looks for node and will
        // fail.
        // Thus we look for the `mpn-cli` node script instead
        File file = new File(getBaseDir(),
                "node/node_modules/npm/bin/npm-cli.js");
        if (file.canRead()) {
            // We return a two element list with node binary and npm-cli script
            return Arrays.asList(getNodeExecutable(), file.getAbsolutePath());
        }
        // Otherwise look for regulan `npm`
        String command = isWindows() ? "npm.cmd" : "npm";
        return Arrays.asList(getExecutable(command, null).getAbsolutePath());
    }

    private static File getExecutable(String cmd, String defaultLocation) {
        File file = null;
        try {
            file = defaultLocation == null
                    ? frontendToolsLocator.tryLocateTool(cmd).orElse(null)
                    : Optional.of(new File(getBaseDir(), defaultLocation))
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
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                inputStream, StandardCharsets.UTF_8.name()))) {

            ret = br.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
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
        String stats = config
                .getStringProperty(SERVLET_PARAMETER_STATISTICS_JSON,
                        VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT)
                // Remove absolute
                .replaceFirst("^/", "");

        // Try stats as a resource from the class path
        InputStream content = service.getClassLoader()
                .getResourceAsStream(stats);
        if (content != null) {
            getLogger().debug("Found stats file as a resource file '{}'.",
                    stats);
        } else {
            URL statsUrl = null;
            if (config.isProductionMode()) {
                statsUrl = service.getStaticResource("/" + stats);
            } else {
                statsUrl = getStatsFromWebpack(service, stats, statsUrl);
            }
            if (statsUrl != null) {
                getLogger().debug("Found stats file at url '{}'", statsUrl);
                content = statsUrl.openStream();
            }
        }
        return content != null ? streamToString(content) : null;
    }

    private static URL getStatsFromWebpack(VaadinService service, String stats,
            URL statsUrl) throws MalformedURLException {
        WebpackDevServerPort port = service.getContext()
                .getAttribute(WebpackDevServerPort.class);
        if (port != null) {
            statsUrl = new URL("http://localhost:" + port + "/" + stats);
        }
        if (statsUrl == null) {
            statsUrl = service.getStaticResource("/" + stats);
            if (statsUrl == null) {
                getLogger().warn(
                        "Cannot get the stats file through webpack-dev-server. "
                                + "The webpack port is unavailable via VaadinContext.");
            } else {
                getLogger().debug(
                        "Cannot get the stats file through webpack-dev-server, "
                                + "however it was found in the web context, "
                                + "which means that the application was build "
                                + "previously. To disable this message run the "
                                + "application in PRODUCTION mode.");
            }
        }
        return statsUrl;
    }

    /**
     * Validate that the found node and npm versions are new enough. Throws an
     * exception with a descriptive message if a version is too old.
     */
    public static void validateNodeAndNpmVersion() {
        try {
            List<String> nodeVersionCommand = new ArrayList<>();
            nodeVersionCommand.add(FrontendUtils.getNodeExecutable());
            nodeVersionCommand.add("--version");
            String[] nodeVersion = getVersion("node", nodeVersionCommand);
            validateToolVersion("node", nodeVersion,
                    Constants.SUPPORTED_NODE_MAJOR_VERSION,
                    Constants.SUPPORTED_NODE_MINOR_VERSION,
                    Constants.SHOULD_WORK_NODE_MAJOR_VERSION,
                    Constants.SHOULD_WORK_NODE_MINOR_VERSION);
        } catch (UnknownVersionException e) {
            getLogger().warn("Error checking if node is new enough", e);
        }

        try {
            List<String> npmVersionCommand = new ArrayList<>();
            npmVersionCommand.addAll(FrontendUtils.getNpmExecutable());
            npmVersionCommand.add("--version");
            String[] npmVersion = getVersion("npm", npmVersionCommand);
            validateToolVersion("npm", npmVersion,
                    Constants.SUPPORTED_NPM_MAJOR_VERSION,
                    Constants.SUPPORTED_NPM_MINOR_VERSION,
                    Constants.SHOULD_WORK_NPM_MAJOR_VERSION,
                    Constants.SHOULD_WORK_NPM_MINOR_VERSION);
        } catch (UnknownVersionException e) {
            getLogger().warn("Error checking if npm is new enough", e);
        }

    }

    static void validateToolVersion(String tool, String[] toolVersion,
            int supportedMajor, int supportedMinor, int shouldWorkMajor,
            int shouldWorkMinor) throws UnknownVersionException {
        if ("true".equalsIgnoreCase(
                System.getProperty(PARAM_IGNORE_VERSION_CHECKS))) {
            return;
        }

        if (isVersionAtLeast(tool, toolVersion, supportedMajor,
                supportedMinor)) {
            return;
        }
        if (isVersionAtLeast(tool, toolVersion, shouldWorkMajor,
                shouldWorkMinor)) {
            getLogger().warn(String.format(SHOULD_WORK, tool,
                    join(toolVersion, "."), supportedMajor, supportedMinor,
                    PARAM_IGNORE_VERSION_CHECKS));
            return;
        }

        throw new IllegalStateException(String.format(TOO_OLD, tool,
                join(toolVersion, "."), supportedMajor, supportedMinor,
                PARAM_IGNORE_VERSION_CHECKS));
    }

    static boolean isVersionAtLeast(String tool, String[] toolVersion,
            int requiredMajor, int requiredMinor)
            throws UnknownVersionException {
        try {
            int major = Integer.parseInt(toolVersion[0]);
            int minor = Integer.parseInt(toolVersion[1]);
            return (major > requiredMajor
                    || (major == requiredMajor && minor >= requiredMinor));
        } catch (NumberFormatException e) {
            throw new UnknownVersionException(tool, "Reported version "
                    + join(toolVersion, ".") + " could not be parsed", e);
        }
    }

    private static String join(String[] toolVersion, String separate) {
        return Stream.of(toolVersion).collect(Collectors.joining(separate));
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
            super("Unable to detect version of " + tool + "." + extraInfo);
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
            super("Unable to detect version of " + tool + "." + extraInfo,
                    cause);

        }
    }

    private static String[] getVersion(String tool, List<String> versionCommand)
            throws UnknownVersionException {
        String[] command = versionCommand
                .toArray(new String[versionCommand.size()]);
        try {
            Process process = new ProcessBuilder(command).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new UnknownVersionException(tool,
                        "Using command " + join(command, " "));
            }
            String output = streamToString(process.getInputStream());
            return output.replaceFirst("^v", "").replaceAll("\n", "")
                    .split("\\.", 3);
        } catch (InterruptedException | IOException e) {
            throw new UnknownVersionException(tool,
                    "Using command " + join(command, " "), e);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(FrontendUtils.class);
    }
}
