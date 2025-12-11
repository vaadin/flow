/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.ArrayList;
import java.util.List;

/**
 * FrontendTools class fot manually testing NodeJS installation.
 */
public class FrontendToolsExecutor {

    /**
     * Manual testing utility to demonstrate which Node.js installation will be
     * used.
     * <p>
     * The resolution logic uses any installed Node.js >= minimum supported
     * version (v24.0.0). If no suitable installation exists, it installs the
     * preferred version specified by -DnodeVersion.
     * <p>
     * Usage examples:
     * <ul>
     * <li>Test with global node: {@code mvn exec:java
     * -Dexec.mainClass="com.vaadin.flow.server.frontend.FrontendToolsTest"
     * -Dexec.classpathScope=test}</li>
     * <li>Test forcing alternative: {@code mvn exec:java ...
     * -Dalternative=true}</li>
     * <li>Test with custom preferred version: {@code mvn exec:java ...
     * -DnodeVersion=v24.5.0}</li>
     * </ul>
     *
     * @param args
     *            command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Node.js Resolution Test");
        System.out.println("=".repeat(80));

        try {
            // Read configuration from system properties
            boolean forceAlternative = Boolean.getBoolean("alternative");
            String preferredVersion = System.getProperty("nodeVersion",
                    FrontendTools.DEFAULT_NODE_VERSION);
            String baseDir = System.getProperty("baseDir",
                    System.getProperty("user.dir"));

            System.out.println("\nConfiguration:");
            System.out.println("  Base directory: " + baseDir);
            System.out.println("  Supported version for global: >= "
                    + FrontendTools.SUPPORTED_NODE_VERSION.getFullVersion());
            System.out
                    .println("  Minimum auto-installed version (~/.vaadin): >= "
                            + FrontendTools.MINIMUM_AUTO_INSTALLED_NODE
                                    .getFullVersion());
            System.out.println("  Maximum major version: "
                    + FrontendTools.MAX_SUPPORTED_NODE_MAJOR_VERSION);
            System.out.println("  Preferred version (to install if needed): "
                    + preferredVersion);
            System.out.println("  Force alternative node: " + forceAlternative);
            System.out.println();

            // Create FrontendTools instance
            FrontendToolsSettings settings = new FrontendToolsSettings(baseDir,
                    () -> FrontendUtils.getVaadinHomeDirectory()
                            .getAbsolutePath());
            settings.setNodeVersion(preferredVersion);
            settings.setForceAlternativeNode(forceAlternative);

            FrontendTools tools = new FrontendTools(settings);

            // Get resolved node information
            String nodeExecutable = tools.getNodeExecutable();
            String actualVersionUsed = tools.getNodeVersion().getFullVersion();
            String npmVersion = tools.getNpmVersion().getFullVersion();

            System.out.println("Resolved Node.js installation:");
            System.out.println("  Node executable: " + nodeExecutable);
            System.out.println("  Actual version used: " + actualVersionUsed);
            System.out.println("  npm version: " + npmVersion);

            // Check if using global or alternative installation
            File nodeFile = new File(nodeExecutable);
            boolean isGlobal = !nodeFile.getAbsolutePath()
                    .contains(FrontendUtils.getVaadinHomeDirectory().getName());

            System.out.println("\nInstallation type: "
                    + (isGlobal ? "GLOBAL" : "ALTERNATIVE (~/.vaadin)"));

            if (!isGlobal) {
                System.out.println("  Location: " + FrontendUtils
                        .getVaadinHomeDirectory().getAbsolutePath());
            }

            // Try to run node --version to verify it works
            System.out.println("\nVerification:");
            try {
                List<String> versionCommand = new ArrayList<>();
                versionCommand.add(nodeExecutable);
                versionCommand.add("--version");
                FrontendVersion version = FrontendUtils.getVersion("node",
                        versionCommand);
                System.out.println("  ✓ Node executable is working");
                System.out.println(
                        "  ✓ Verified version: " + version.getFullVersion());
            } catch (Exception e) {
                System.out.println("  ✗ Failed to verify node executable: "
                        + e.getMessage());
            }

            System.out.println("\n" + "=".repeat(80));
            System.out.println("Resolution completed successfully");
            System.out.println("=".repeat(80));

        } catch (Exception e) {
            System.err.println("\n" + "=".repeat(80));
            System.err.println("ERROR: Resolution failed");
            System.err.println("=".repeat(80));
            System.err.println("\nException: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            System.err.println("\nStack trace:");
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
