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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

/**
 * A class for static methods and definitions that might be
 * used in different locations.
 */
public class FrontendUtils {

    /**
     * Folder with frontend content. In regular java web projects it is
     * `src/main/webapp` but in flow we prefer the project root directory so as
     * we don't pollute sources folders with generated or installed stuff.
     *
     */
    public static final String WEBAPP_FOLDER = "./";

    /**
     * The name of the webpack configuration file.
     */
    public static final String WEBPACK_CONFIG ="webpack.config.js";

    /**
     * NPM package name that will be used for the javascript files present in
     * jar resources that will to be copied to the npm folder so as they are
     * accessible to webpack.
     */
    public static final String FLOW_NPM_PACKAGE_NAME = "@vaadin/flow-frontend/";


    /**
     * Default location for the installed node packages.
     */
    public static final String NODE_MODULES = "node_modules/";

    /**
     * Relative path to the folder containing application frontend source files.
     */
    public static final String FLOW_FRONTEND = "frontend/";

    /**
     * Folder that contains generated javascript files used to build the
     * application. Normally it would be relative to the target folder
     * configured by maven.
     */
    public static final String FLOW_GENERATED_FOLDER = "frontend/";

    /**
     * Target folder.
     */
    public static final String TARGET = "target/";

    /**
     * File that contains Flow application imports, javascript, and theme annotations.
     * It is also the entry-point for webpack.
     * It should be relative to the target folder.
     */
    public static final String FLOW_IMPORTS_FILE = "frontend/generated-flow-imports.js";

    /**
     *
     * A parameter for overriding the
     * {@link FrontendUtils#FLOW_IMPORTS_FILE} default value for the file
     * with all Flow project imports.
     */
    public static final String MAIN_JS_PARAM = "vaadin.frontend.jsFile";

    /**
     * A special prefix to use in the webpack config to tell webpack to look for
     * the import starting with a prefix in the Flow project frontend directory.
     */
    public static final String WEBPACK_PREFIX_ALIAS = "Frontend/";

    private static final String NOT_FOUND =
            "%n%n======================================================================================================"
            + "%nFailed to determine '%s' tool."
            + "%nPlease install it either:"
            + "%n  - by following the https://nodejs.org/en/download/ guide to install it globally"
            + "%n  - or by running the frontend-maven-plugin goal to install it in this project:"
            + "%n  $ mvn com.github.eirslett:frontend-maven-plugin:LATEST:install-node-and-npm -DnodeVersion=v11.6.0 "
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
        return System.getProperty("project.basedir", System.getProperty("user.dir", "."));
    }

    /**
     * Locate <code>node</code> executable.
     *
     * @return the full path to the executable
     */
    public static String getNodeExecutable() {
        String command = isWindows() ? "node.exe" : "node";
        String defaultNode = FrontendUtils.isWindows() ? "node/node.exe" : "node/node";
        return getExecutable(command, defaultNode).getAbsolutePath();
    }

    /**
     * Locate <code>npm</code> executable.
     *
     * @return the a list of all commands in sequence that need to be executed
     *         to have npm running
     */
    public static List<String> getNpmExecutable() {
        // If `node` is not found in PATH, `node/node_modules/npm/bin/npm` will not work
        // because it's a shell or windows script that looks for node and will fail.
        // Thus we look for the `mpn-cli` node script instead
        File file = new File(getBaseDir(), "node/node_modules/npm/bin/npm-cli.js");
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
            file = defaultLocation == null ? frontendToolsLocator.tryLocateTool(cmd).orElse(null)
                    : Optional.of(new File(getBaseDir(), defaultLocation))
                            .filter(frontendToolsLocator::verifyTool)
                            .orElseGet(() -> frontendToolsLocator.tryLocateTool(cmd).orElse(null));
        } catch (Exception e) { //NOSONAR
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
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8.name()))) {

            ret = br.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException exception) {
            // ignore exception on close()
            LoggerFactory.getLogger(FrontendUtils.class).warn("Couldn't close template input stream", exception);
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
}
