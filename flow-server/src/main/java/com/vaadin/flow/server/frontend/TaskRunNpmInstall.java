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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
            packageUpdater.log().info("Running `" + toolName + " install` ...");
            runNpmInstall();
        } else {
            packageUpdater.log().info("Skipping `" + toolName + " install`.");
        }
    }

    private boolean shouldRunNpmInstall() {
        if (packageUpdater.nodeModulesFolder.isDirectory()) {
            File[] installedPackages = packageUpdater.nodeModulesFolder
                    .listFiles();
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
        builder.directory(packageUpdater.npmFolder);

        String toolName = disablePnpm ? "npm" : "pnpm";

        Process process = null;
        try {
            process = builder.inheritIO().start();
            int errorCode = process.waitFor();
            if (errorCode != 0) {
                packageUpdater.log().error(
                        ">>> Dependency ERROR. Check that all required dependencies are deployed in "
                                + toolName + " repositories.");
                throw new ExecutionFailedException(
                        SharedUtil.capitalize(toolName)
                                + " install has exited with non zero status. "
                                + "Some dependencies are not installed. Check "
                                + toolName + " command output");
            } else {
                packageUpdater.log().info(
                        "package.json updated and dependencies are installed. ");
            }
        } catch (InterruptedException | IOException e) {
            packageUpdater.log()
                    .error("Error when running `" + toolName + " install`", e);
            throw new ExecutionFailedException(
                    "Command '" + toolName + " install' failed to finish", e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

}
