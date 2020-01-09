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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.shared.util.SharedUtil;

import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;

/**
 * Run <code>npm install</code> after dependencies have been updated.
 *
 * @since 2.0
 */
public class TaskRunNpmInstall implements FallibleCommand {

    private final NodeUpdater packageUpdater;

    private final List<String> ignoredNodeFolders = Arrays
            .asList(".bin", "pnpm", ".ignored_pnpm", ".pnpm", ".modules.yaml");
    private final boolean disablePnpm;

    /**
     * Create an instance of the command.
     *
     * @param packageUpdater
     *            package-updater instance used for checking if previous
     *            execution modified the package.json file
     */
    TaskRunNpmInstall(NodeUpdater packageUpdater, boolean disablePnpm) {
        this.packageUpdater = packageUpdater;
        this.disablePnpm = disablePnpm;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        String toolName = disablePnpm ? "npm" : "pnpm";
        if (packageUpdater.modified || shouldRunNpmInstall()) {
            packageUpdater.log().info("Running `" + toolName + " install` to "
                    + "resolve and optionally download frontend dependencies. "
                    + "This may take a moment, please stand by...");
            runNpmInstall();
        } else {
            packageUpdater.log().info("Skipping `" + toolName + " install`.");
        }
    }

    private boolean shouldRunNpmInstall() {
        if (packageUpdater.nodeModulesFolder.isDirectory()) {
            // Ignore .bin and pnpm folders as those are always installed for
            // pnpm execution
            File[] installedPackages = packageUpdater.nodeModulesFolder
                    .listFiles((dir, name) -> !ignoredNodeFolders.contains(name));
            assert installedPackages != null;
            return installedPackages.length == 0
                    || (installedPackages.length == 1 && FLOW_NPM_PACKAGE_NAME
                            .startsWith(installedPackages[0].getName()));
        }
        return true;
    }

    /**
     * Installs frontend resources (using either pnpm or npm) after
     * `package.json` has been updated.
     */
    private void runNpmInstall() throws ExecutionFailedException {
        List<String> executable;
        String baseDir = packageUpdater.npmFolder.getAbsolutePath();
        try {
            executable = disablePnpm ? FrontendUtils.getNpmExecutable(baseDir)
                    : FrontendUtils.getPnpmExecutable(baseDir);
        } catch (IllegalStateException exception) {
            throw new ExecutionFailedException(exception.getMessage(),
                    exception);
        }
        List<String> command = new ArrayList<>(executable);
        command.add("install");

        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.environment().put("ADBLOCK", "1");
        builder.environment().put("NO_UPDATE_NOTIFIER", "1");
        builder.directory(packageUpdater.npmFolder);

        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

        String toolName = disablePnpm ? "npm" : "pnpm";

        String commandString = command.stream()
                .collect(Collectors.joining(" "));

        Process process = null;
        try {
            process = builder.start();

            packageUpdater.log().debug("Output of `{}`:", commandString);
            StringBuilder toolOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String stdoutLine;
                while ((stdoutLine = reader.readLine()) != null) {
                    packageUpdater.log().debug(stdoutLine);
                    toolOutput.append(stdoutLine);
                }
            }

            int errorCode = process.waitFor();
            if (errorCode != 0) {
                // Echo the stdout from pnpm/npm to error level log
                packageUpdater.log().error("Command `{}` failed:\n{}",
                        commandString, toolOutput);
                packageUpdater.log().error(
                        ">>> Dependency ERROR. Check that all required dependencies are "
                                + "deployed in {} repositories.",
                        toolName);
                throw new ExecutionFailedException(
                        SharedUtil.capitalize(toolName)
                                + " install has exited with non zero status. "
                                + "Some dependencies are not installed. Check "
                                + toolName + " command output");
            } else {
                packageUpdater.log().info(
                        "Frontend dependencies resolved successfully.");
            }
        } catch (InterruptedException | IOException e) {
            packageUpdater.log().error("Error when running `{} install`",
                    toolName, e);
            throw new ExecutionFailedException(
                    "Command '" + toolName + " install' failed to finish", e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

}
