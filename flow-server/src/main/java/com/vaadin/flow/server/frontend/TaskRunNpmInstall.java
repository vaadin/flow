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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.server.ExecutionFailedException;

import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;

/**
 * Run <code>npm install</code> after dependencies have been updated.
 *
 * @since 2.0
 */
public class TaskRunNpmInstall implements FallibleCommand {

    static final String SKIPPING_NPM_INSTALL = "Skipping `npm install`.";
    static final String RUNNING_NPM_INSTALL = "Running `npm install` ...";

    private final NodeUpdater packageUpdater;

    /**
     * Create an instance of the command.
     *
     * @param packageUpdater
     *            package-updater instance used for checking if previous
     *            execution modified the package.json file
     */
    TaskRunNpmInstall(NodeUpdater packageUpdater) {
        this.packageUpdater = packageUpdater;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (packageUpdater.modified || shouldRunNpmInstall()) {
            packageUpdater.log().info(RUNNING_NPM_INSTALL);
            runNpmInstall();
        } else {
            packageUpdater.log().info(SKIPPING_NPM_INSTALL);
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
     * Executes `npm install` after `package.json` has been updated.
     */
    private void runNpmInstall() throws ExecutionFailedException {
        List<String> npmExecutable;
        try {
            npmExecutable = FrontendUtils.getNpmExecutable(
                    packageUpdater.npmFolder.getAbsolutePath());
        } catch (IllegalStateException exception) {
            throw new ExecutionFailedException(exception.getMessage(),
                    exception);
        }
        List<String> command = new ArrayList<>(npmExecutable);
        command.add("install");

        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.environment().put("ADBLOCK", "1");
        builder.directory(packageUpdater.npmFolder);

        Process process = null;
        try {
            process = builder.inheritIO().start();
            int errorCode = process.waitFor();
            if (errorCode != 0) {
                packageUpdater.log().error(
                        ">>> Dependency ERROR. Check that all required dependencies are deployed in npm repositories.");
                throw new ExecutionFailedException(
                        "Npm install has exited with non zero status. "
                                + "Some dependencies are not installed. Check npm command output");
            } else {
                packageUpdater.log().info(
                        "package.json updated and npm dependencies installed. ");
            }
        } catch (InterruptedException | IOException e) {
            packageUpdater.log().error("Error when running `npm install`", e);
            throw new ExecutionFailedException(
                    "Command 'npm install' failed to finish", e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

}
