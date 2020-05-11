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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.FallibleCommand;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.TaskUpdatePackages.HASH_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Run <code>npm install</code> after dependencies have been updated.
 *
 * @since 2.0
 */
public class TaskRunNpmInstall implements FallibleCommand {

    static final String SKIPPING_NPM_INSTALL = "Skipping `npm install`.";

    private final NodeUpdater packageUpdater;

    // .vaadin/vaadin.json contains local installation data inside node_modules
    // This will hep us know to execute even when another developer has pushed
    // a new hash to the code repository.
    private static final String INSTALL_HASH = ".vaadin/vaadin.json";

    private final List<String> ignoredNodeFolders = Arrays
            .asList(".bin", ".staging", ".vaadin");

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
            packageUpdater.log().info("Running `npm install` to "
                    + "resolve and optionally download frontend dependencies. "
                    + "This may take a moment, please stand by...");
            runNpmInstall();

            updateLocalHash();
        } else {
            packageUpdater.log().info(SKIPPING_NPM_INSTALL);
        }
    }


    /**
     * Updates the local hash to node_modules.
     * <p>
     * This is for handling updated package to the code repository by another
     * developer as then the hash is updated and we may just be missing one
     * module.
     */
    private void updateLocalHash() {
        try {
            if(!packageUpdater.getMainPackageJson().hasKey(HASH_KEY)) {
                packageUpdater.log().warn("No hash found for full package.json.");
                return;
            }
            final String hash = packageUpdater.getMainPackageJson().getString(HASH_KEY);

            final JsonObject localHash = Json.createObject();
            localHash.put(HASH_KEY, hash);

            final File localHashFile = getLocalHashFile();
            FileUtils.forceMkdirParent(localHashFile);
            String content = JsonUtil.stringify(localHash, 2) + "\n";
            FileUtils.writeStringToFile(localHashFile, content, UTF_8.name());

        } catch (IOException e) {
            packageUpdater.log().warn("Failed to update node_modules hash.", e);
        }
    }

    private File getLocalHashFile() {
        return new File(packageUpdater.nodeModulesFolder, INSTALL_HASH);
    }

    private boolean shouldRunNpmInstall() {
        if (packageUpdater.nodeModulesFolder.isDirectory()) {
            // Ignore installation files
            File[] installedPackages = packageUpdater.nodeModulesFolder
                    .listFiles(
                            (dir, name) -> !ignoredNodeFolders.contains(name));
            assert installedPackages != null;
            return installedPackages.length == 0
                    || (installedPackages.length == 1 && FLOW_NPM_PACKAGE_NAME
                            .startsWith(installedPackages[0].getName()))
                    ||  (installedPackages.length > 0 && isVaadinHashUpdated());
        }
        return true;
    }

    private boolean isVaadinHashUpdated() {
        final File localHashFile = getLocalHashFile();
        if (!localHashFile.exists()) {
            return true;
        }
        try {
            String fileContent = FileUtils
                    .readFileToString(localHashFile, UTF_8.name());
            JsonObject content = Json.parse(fileContent);
            if (content.hasKey(HASH_KEY)) {
                final JsonObject packageJson = packageUpdater
                        .getMainPackageJson();
                if (packageJson == null || !packageJson.hasKey(HASH_KEY)) {
                    return false;
                }
                return !content.getString(HASH_KEY)
                        .equals(packageJson.getString(HASH_KEY));
            }
        } catch (IOException e) {
            packageUpdater.log()
                    .warn("Failed to load hashes forcing npm execution", e);
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

        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

        String commandString = command.stream()
                .collect(Collectors.joining(" "));

        Process process = null;
        try {
            process = builder.start();

            packageUpdater.log().debug("Output of `{}`:", commandString);
            StringBuilder toolOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(),
                            UTF_8))) {
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
                        ">>> Dependency ERROR. Check that all required dependencies are deployed in npm repositories.");
                throw new ExecutionFailedException(
                        "Npm install has exited with non zero status. "
                                + "Some dependencies are not installed. Check npm command output");
            } else {
                packageUpdater.log()
                        .info("Frontend dependencies resolved successfully.");
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
