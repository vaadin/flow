/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Compiles the dev mode bundle if it is out of date.
 * <p>
 * Only used when running in dev mode without a dev server.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskRunDevBundleBuild implements FallibleCommand {

    private Options options;

    /**
     * Create an instance of the command.
     *
     * @param options
     *            the task options
     */
    TaskRunDevBundleBuild(Options options) {
        this.options = options;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        getLogger().info(
                "Creating a new express mode bundle. This can take a while but will only run when the project setup is changed, addons are added or frontend files are modified");

        runFrontendBuildTool("Vite", "vite/bin/vite.js", Collections.emptyMap(),
                "build");
    }

    public static boolean needsBuild(File npmFolder) {
        getLogger().info("Checking if an express mode bundle build is needed");

        try {
            boolean needsBuild = needsBuildInternal(npmFolder);
            if (needsBuild) {
                getLogger().info("An express mode bundle build is needed");
            } else {
                getLogger().info("An express mode bundle build is not needed");
            }
            return needsBuild;
        } catch (Exception e) {
            getLogger().error(
                    "Error when checking if an express mode bundle build is needed",
                    e);
            return true;
        }
    }

    private static boolean needsBuildInternal(File npmFolder)
            throws IOException {

        if (!FrontendUtils.getDevBundleFolder(npmFolder).exists()) {
            return true;
        }

        String statsJsonContent = FrontendUtils.findBundleStatsJson(npmFolder);
        if (statsJsonContent == null) {
            return true;
        }

        File packageJsonFile = new File(npmFolder, "package.json");

        String packageJsonHash = null;
        String bundlePackageJsonHash = null;
        JsonObject packageJson = null;

        if (packageJsonFile.exists()) {
            packageJson = Json.parse(FileUtils.readFileToString(packageJsonFile,
                    StandardCharsets.UTF_8));
            if (packageJson.hasKey("vaadin")
                    && packageJson.getObject("vaadin").hasKey("hash")) {
                packageJsonHash = packageJson.getObject("vaadin")
                        .getString("hash");
            }
        }

        JsonObject statsJson = Json.parse(statsJsonContent);
        if (statsJson.hasKey("packageJsonHash")) {
            bundlePackageJsonHash = statsJson.getString("packageJsonHash");
        }

        if (packageJsonHash != null && !packageJsonHash.isEmpty()) {
            if (packageJsonHash.equals(bundlePackageJsonHash)) {
                // Hash in the project matches the bundle hash. It must be ok to
                // use the bundle
                return false;
            }
        }

        if (!hasAllNpmModules(statsJson, packageJson)) {
            // The bundle (stats.json) does not include all packages listed in
            // package.json
            return true;
        }
        // getLogger().info(
        // "The package.json file has changed since the bundle was built
        // (package.json
        // hash "
        // + packageJsonHash + ", dev bundle hash "
        // + bundlePackageJsonHash + ")");

        return false;
    }

    private static boolean hasAllNpmModules(JsonObject statsJson,
            JsonObject packageJson) {
        JsonObject bundleNpmModules = statsJson.getObject("npmModules");
        JsonObject dependencies = packageJson.getObject("dependencies");
        for (String dependency : dependencies.keys()) {
            if (!bundleNpmModules.hasKey(dependency)) {
                getLogger().info("Dependency " + dependency
                        + " is missing from the bundle");
                return false;
            }
        }

        return true;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(TaskRunDevBundleBuild.class);
    }

    private void runFrontendBuildTool(String toolName, String executable,
            Map<String, String> environment, String... params)
            throws ExecutionFailedException {
        Logger logger = getLogger();

        FrontendToolsSettings settings = new FrontendToolsSettings(
                options.getNpmFolder().getAbsolutePath(),
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        settings.setNodeDownloadRoot(options.nodeDownloadRoot);
        settings.setForceAlternativeNode(options.requireHomeNodeExec);
        settings.setUseGlobalPnpm(options.useGlobalPnpm);
        settings.setAutoUpdate(options.nodeAutoUpdate);
        settings.setNodeVersion(options.nodeVersion);
        FrontendTools frontendTools = new FrontendTools(settings);

        File buildExecutable = new File(options.getNpmFolder(),
                "node_modules/" + executable);
        if (!buildExecutable.isFile()) {
            throw new IllegalStateException(String.format(
                    "Unable to locate %s executable by path '%s'. Double"
                            + " check that the plugin is executed correctly",
                    toolName, buildExecutable.getAbsolutePath()));
        }

        String nodePath;
        if (options.requireHomeNodeExec) {
            nodePath = frontendTools.forceAlternativeNodeExecutable();
        } else {
            nodePath = frontendTools.getNodeExecutable();
        }

        List<String> command = new ArrayList<>();
        command.add(nodePath);
        command.add(buildExecutable.getAbsolutePath());
        command.addAll(Arrays.asList(params));

        String commandString = command.stream()
                .collect(Collectors.joining(" "));

        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.environment().put("devBundle", "true");

        Process process = null;
        try {
            builder.directory(options.getNpmFolder());
            builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);

            process = builder.start();

            // This will allow to destroy the process which does IO regardless
            // whether it's executed in the same thread or another (may be
            // daemon) thread
            Runtime.getRuntime()
                    .addShutdownHook(new Thread(process::destroyForcibly));

            logger.debug("Output of `{}`:", commandString);
            StringBuilder toolOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String stdoutLine;
                while ((stdoutLine = reader.readLine()) != null) {
                    logger.debug(stdoutLine);
                    toolOutput.append(stdoutLine)
                            .append(System.lineSeparator());
                }
            }

            int errorCode = process.waitFor();

            if (errorCode != 0) {
                logger.error("Command `{}` failed:\n{}", commandString,
                        toolOutput);
                throw new ExecutionFailedException(
                        SharedUtil.capitalize(toolName)
                                + " build exited with a non zero status");
            } else {
                logger.info("Development frontend bundle built");
            }
        } catch (InterruptedException | IOException e) {
            logger.error("Error when running `{}`", commandString, e);
            if (e instanceof InterruptedException) {
                // Restore interrupted state
                Thread.currentThread().interrupt();
            }
            throw new ExecutionFailedException(
                    "Command '" + commandString + "' failed to finish", e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }
}
