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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;

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
     * Check that the folder structure does not meet a proper npm mode project.
     * It is useful to run V13 projects in V14 before they have been migrated.
     *
     * @return whether the project needs to be run in bower mode
     */
    public static boolean isBowerLegacyMode() {
        boolean hasBowerFrontend = new File(getBaseDir(), "src/main/webapp/frontend").isDirectory();
        boolean hasNpmFrontend = new File(getBaseDir(), "frontend").isDirectory();
        boolean hasNpmConfig = new File(getBaseDir(), PACKAGE_JSON).exists()
                && new File(getBaseDir(), WEBPACK_CONFIG).exists();
        

        return hasBowerFrontend && !hasNpmFrontend && !hasNpmConfig ? true : false;
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
}
