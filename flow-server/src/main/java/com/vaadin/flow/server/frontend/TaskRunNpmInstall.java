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

import com.vaadin.flow.server.Command;

import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.NodeUpdater.log;

/**
 * Run <code>npm install</code> after dependencies have been updated.
 */
public class TaskRunNpmInstall implements Command {

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
    public void execute() {
        if (packageUpdater.modified || shouldRunNpmInstall()) {
            log().info("Running `npm install` ...");
            runNpmInstall();
        } else {
            log().info("Skipping `npm install`.");
        }
    }

    private boolean shouldRunNpmInstall() {
        if (packageUpdater.nodeModulesFolder.isDirectory()) {
            File[] installedPackages = packageUpdater.nodeModulesFolder.listFiles();
            return installedPackages == null
                    || (installedPackages.length == 1 && FLOW_NPM_PACKAGE_NAME
                            .startsWith(installedPackages[0].getName()));
        }
        return true;
    }

    /**
     * Executes `npm install` after `package.json` has been updated.
     */
    private void runNpmInstall() {
        List<String> command = new ArrayList<>(FrontendUtils
                .getNpmExecutable(packageUpdater.npmFolder.getAbsolutePath()));
        command.add("install");

        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.directory(packageUpdater.npmFolder);

        Process process = null;
        try {
            process = builder.inheritIO().start();
            int errorCode = process.waitFor();
            if (errorCode != 0) {
                log().error(
                        ">>> Dependency ERROR. Check that all required dependencies are deployed in npm repositories.");
            } else {
                log().info("package.json updated and npm dependencies installed. ");
            }
        } catch (InterruptedException | IOException e) {
            log().error("Error when running `npm install`", e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

}
