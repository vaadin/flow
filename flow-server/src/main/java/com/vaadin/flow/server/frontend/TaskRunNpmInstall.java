/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.JsonObject;

import static com.vaadin.flow.server.frontend.FrontendUtils.commandToString;
import static com.vaadin.flow.server.frontend.NodeUpdater.HASH_KEY;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_VERSION;
import static com.vaadin.flow.server.frontend.NodeUpdater.PROJECT_FOLDER;

/**
 * Run <code>npm install</code> after dependencies have been updated.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskRunNpmInstall implements FallibleCommand {

    private static final String MODULES_YAML = ".modules.yaml";

    private static final String NPM_VALIDATION_FAIL_MESSAGE = "%n%n======================================================================================================"
            + "%nThe path to npm cache contains whitespaces, and the currently installed npm version doesn't accept this."
            + "%nMost likely your Windows user home path contains whitespaces."
            + "%nTo workaround it, please change the npm cache path by using the following command:"
            + "%n    npm config set cache [path-to-npm-cache] --global"
            + "%n(you may also want to exclude the whitespaces with 'dir /x' to use the same dir),"
            + "%nor upgrade the npm version to 7 (or newer) by:"
            + "%n 1) Running 'npm-windows-upgrade' tool with Windows PowerShell:"
            + "%n        Set-ExecutionPolicy Unrestricted -Scope CurrentUser -Force"
            + "%n        npm install -g npm-windows-upgrade"
            + "%n        npm-windows-upgrade"
            + "%n 2) Manually installing a newer version of npx: npm install -g npx"
            + "%n 3) Manually installing a newer version of pnpm: npm install -g pnpm"
            + "%n 4) Deleting the following files from your Vaadin project's folder (if present):"
            + "%n        node_modules, package-lock.json, webpack.generated.js, pnpm-lock.yaml"
            + "%n======================================================================================================%n";

    private final NodeUpdater packageUpdater;

    private final List<String> ignoredNodeFolders = Arrays.asList(".bin",
            "pnpm", ".ignored_pnpm", ".pnpm", ".staging", ".vaadin",
            MODULES_YAML);

    private final Options options;

    /**
     * Create an instance of the command.
     *
     * @param packageUpdater
     *            package-updater instance used for checking if previous
     *            execution modified the package.json file
     * @param options
     *            the options for the task
     */
    TaskRunNpmInstall(NodeUpdater packageUpdater, Options options) {
        this.packageUpdater = packageUpdater;
        this.options = options;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        String toolName = options.isEnablePnpm() ? "pnpm" : "npm";
        String command = "install";
        if (options.isCiBuild()) {
            if (options.isEnablePnpm()) {
                command += " --frozen-lockfile";
            } else {
                command = "ci";
            }
        }

        if (packageUpdater.modified || shouldRunNpmInstall()) {
            packageUpdater.log().info("Running `" + toolName + " " + command
                    + "` to "
                    + "resolve and optionally download frontend dependencies. "
                    + "This may take a moment, please stand by...");
            runNpmInstall();

            updateLocalHash();
        } else {
            packageUpdater.log().info(
                    "Skipping `{} {}` because the frontend packages are already "
                            + "installed in the folder '{}' and the hash in the file '{}' is the same as in '{}'",
                    toolName, command,
                    options.getNodeModulesFolder().getAbsolutePath(),
                    packageUpdater.getVaadinJsonFile().getAbsolutePath(),
                    Constants.PACKAGE_JSON);

        }
    }

    /**
     * Updates
     *
     * <pre>
     * node_modules/.vaadin/vaadin.json
     * </pre>
     *
     * with package.json hash, project folder and the platform version.
     * <p>
     * This is for handling updated package to the code repository by another
     * developer as then the hash is updated and we may just be missing one
     * module, as well as for detecting that the platform version has changed
     * which may require a deeper cleanup.
     */
    private void updateLocalHash() {
        try {
            final JsonObject vaadin = packageUpdater.getPackageJson()
                    .getObject(VAADIN_DEP_KEY);
            if (vaadin == null) {
                packageUpdater.log().warn("No vaadin object in package.json");
                return;
            }
            final String hash = vaadin.getString(HASH_KEY);

            final Map<String, String> updates = new HashMap<>();
            updates.put(HASH_KEY, hash);
            TaskUpdatePackages.getVaadinVersion(packageUpdater.finder)
                    .ifPresent(s -> updates.put(VAADIN_VERSION, s));
            updates.put(PROJECT_FOLDER,
                    options.getNpmFolder().getAbsolutePath());
            packageUpdater.updateVaadinJsonContents(updates);
        } catch (IOException e) {
            packageUpdater.log().warn("Failed to update node_modules hash.", e);
        }
    }

    private boolean shouldRunNpmInstall() {
        if (!options.getNodeModulesFolder().isDirectory()) {
            return true;
        }
        // Ignore .bin and pnpm folders as those are always installed for
        // pnpm execution
        File[] installedPackages = options.getNodeModulesFolder()
                .listFiles((dir, name) -> !ignoredNodeFolders.contains(name));
        assert installedPackages != null;
        if (installedPackages.length == 0) {
            // Nothing installed
            return true;
        }

        return isVaadinHashOrProjectFolderUpdated();
    }

    boolean isVaadinHashOrProjectFolderUpdated() {
        try {
            JsonObject nodeModulesVaadinJson = packageUpdater
                    .getVaadinJsonContents();
            if (nodeModulesVaadinJson.hasKey(HASH_KEY)) {
                final JsonObject packageJson = packageUpdater.getPackageJson();
                if (!nodeModulesVaadinJson.getString(HASH_KEY)
                        .equals(packageJson.getObject(VAADIN_DEP_KEY)
                                .getString(HASH_KEY))) {
                    return true;
                }

                if (nodeModulesVaadinJson.hasKey(PROJECT_FOLDER)
                        && !options.getNpmFolder().getAbsolutePath()
                                .equals(nodeModulesVaadinJson
                                        .getString(PROJECT_FOLDER))) {
                    return true;
                }

                return false;
            }
        } catch (IOException e) {
            packageUpdater.log()
                    .warn("Failed to load hashes forcing npm execution", e);
        }
        return true;
    }

    /**
     * Installs frontend resources (using either pnpm or npm) after
     * `package.json` has been updated.
     */
    private void runNpmInstall() throws ExecutionFailedException {
        // Do possible cleaning before generating any new files.
        cleanUp();

        Logger logger = packageUpdater.log();

        String baseDir = options.getNpmFolder().getAbsolutePath();

        FrontendToolsSettings settings = new FrontendToolsSettings(baseDir,
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        settings.setNodeDownloadRoot(options.getNodeDownloadRoot());
        settings.setForceAlternativeNode(options.isRequireHomeNodeExec());
        settings.setUseGlobalPnpm(options.isUseGlobalPnpm());
        settings.setAutoUpdate(options.isNodeAutoUpdate());
        settings.setNodeVersion(options.getNodeVersion());
        FrontendTools tools = new FrontendTools(settings);
        tools.validateNodeAndNpmVersion();

        if (options.isEnablePnpm()) {
            try {
                createNpmRcFile();
            } catch (IOException exception) {
                logger.warn(".npmrc generation failed; pnpm "
                        + "package installation may require manually passing "
                        + "the --shamefully-hoist flag", exception);
            }
        }

        List<String> npmExecutable;
        List<String> npmInstallCommand;
        List<String> postinstallCommand;

        try {
            if (options.isRequireHomeNodeExec()) {
                tools.forceAlternativeNodeExecutable();
            }
            if (options.isEnablePnpm()) {
                validateInstalledNpm(tools);
                npmExecutable = tools.getPnpmExecutable();
            } else {
                npmExecutable = tools.getNpmExecutable();
            }
            npmInstallCommand = new ArrayList<>(npmExecutable);
            postinstallCommand = new ArrayList<>(npmExecutable);
            // This only works together with "install"
            postinstallCommand.remove("--shamefully-hoist=true");

        } catch (IllegalStateException exception) {
            throw new ExecutionFailedException(exception.getMessage(),
                    exception);
        }

        npmInstallCommand.add("--ignore-scripts");

        if (options.isCiBuild()) {
            if (options.isEnablePnpm()) {
                npmInstallCommand.add("install");
                npmInstallCommand.add("--frozen-lockfile");
            } else {
                npmInstallCommand.add("ci");
            }
        } else {
            npmInstallCommand.add("install");
        }

        postinstallCommand.add("run");
        postinstallCommand.add("postinstall");

        if (logger.isDebugEnabled()) {
            logger.debug(
                    commandToString(options.getNpmFolder().getAbsolutePath(),
                            npmInstallCommand));
        }

        String toolName = options.isEnablePnpm() ? "pnpm" : "npm";

        String commandString = npmInstallCommand.stream()
                .collect(Collectors.joining(" "));

        logger.info("using '{}' for frontend package installation",
                String.join(" ", npmInstallCommand));

        // Log a stronger request for patience if package-lock.json is
        // missing as "npm install" in this case can take minutes
        // https://github.com/vaadin/flow/issues/12825
        File packageLockFile = packageUpdater.getPackageLockFile();
        if (!options.isEnablePnpm() && !packageLockFile.exists()) {
            packageUpdater.log().warn("package-lock.json is missing from this "
                    + "project. This may cause the npm package installation to "
                    + "take several minutes. It is recommended to keep the "
                    + "package-lock.json file persistently in your project. "
                    + "Please stand by...");
        }

        Process process = null;
        try {
            process = runNpmCommand(npmInstallCommand, options.getNpmFolder());

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
                // Echo the stdout from pnpm/npm to error level log
                logger.error("Command `{}` failed:\n{}", commandString,
                        toolOutput);
                logger.error(
                        ">>> Dependency ERROR. Check that all required dependencies are "
                                + "deployed in {} repositories.",
                        toolName);
                throw new ExecutionFailedException(
                        SharedUtil.capitalize(toolName)
                                + " install has exited with non zero status. "
                                + "Some dependencies are not installed. Check "
                                + toolName + " command output");
            } else {
                logger.info("Frontend dependencies resolved successfully.");
            }
        } catch (InterruptedException | IOException e) {
            logger.error("Error when running `{} install`", toolName, e);
            if (e instanceof InterruptedException) {
                // Restore interrupted state
                Thread.currentThread().interrupt();
            }
            throw new ExecutionFailedException(
                    "Command '" + toolName + " install' failed to finish", e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }

        List<String> postinstallPackages = new ArrayList<>();
        postinstallPackages.add(".");
        postinstallPackages.add("esbuild");
        postinstallPackages.add("@vaadin/vaadin-usage-statistics");
        postinstallPackages.addAll(options.getPostinstallPackages());

        for (String postinstallPackage : postinstallPackages) {
            File packageJsonFile = getPackageJsonForModule(postinstallPackage);
            if (packageJsonFile == null || !packageJsonFile.exists()) {
                continue;
            }
            File packageFolder = packageJsonFile.getParentFile();

            try {
                JsonObject packageJson = TaskGeneratePackageJson
                        .getJsonFileContent(packageJsonFile);
                if (!containsPostinstallScript(packageJson)) {
                    logger.debug(
                            "Skipping postinstall for '{}' as no postinstall script was found in the package.json",
                            postinstallPackage);
                    continue;
                }
            } catch (IOException ioe) {
                logger.error(
                        "Couldn't read package.json for {}. Skipping postinstall",
                        ioe);
                continue;
            }

            logger.debug("Running postinstall for '{}'", postinstallPackage);
            try {
                process = runNpmCommand(postinstallCommand, packageFolder);
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    // Restore interrupted state
                    Thread.currentThread().interrupt();
                }
                throw new ExecutionFailedException(
                        "Error when running postinstall script for '"
                                + postinstallPackage + "'",
                        e);
            }
        }
    }

    private File getPackageJsonForModule(String module) {
        if (module.trim().equals("")) {
            return null;
        }
        if (module.equals(".")) {
            // The location of the project package.json
            return new File(options.getNpmFolder(), "package.json");
        }

        return new File(new File(options.getNodeModulesFolder(), module),
                "package.json");

    }

    private boolean containsPostinstallScript(JsonObject packageJson) {
        return packageJson != null && packageJson.hasKey("scripts")
                && packageJson.getObject("scripts").hasKey("postinstall");
    }

    private Process runNpmCommand(List<String> command, File workingDirectory)
            throws IOException {
        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.environment().put("ADBLOCK", "1");
        builder.environment().put("NO_UPDATE_NOTIFIER", "1");
        builder.directory(workingDirectory);
        builder.redirectInput(ProcessBuilder.Redirect.PIPE);
        builder.redirectErrorStream(true);

        Process process = builder.start();

        // This will allow to destroy the process which does IO regardless
        // whether it's executed in the same thread or another (may be
        // daemon) thread
        Runtime.getRuntime()
                .addShutdownHook(new Thread(process::destroyForcibly));

        return process;
    }

    /*
     * Create an .npmrc file the project directory if there is none.
     */
    private void createNpmRcFile() throws IOException {
        File npmrcFile = new File(options.getNpmFolder().getAbsolutePath(),
                ".npmrc");
        boolean shouldWrite;
        if (npmrcFile.exists()) {
            List<String> lines = FileUtils.readLines(npmrcFile,
                    StandardCharsets.UTF_8);
            if (lines.stream().anyMatch(line -> line
                    .contains("NOTICE: this is an auto-generated file"))) {
                shouldWrite = true;
            } else {
                // Looks like this file was not generated by Vaadin
                if (lines.stream()
                        .noneMatch(line -> line.contains("shamefully-hoist"))) {
                    String message = "Custom .npmrc file ({}) found in "
                            + "project; pnpm package installation may "
                            + "require passing the --shamefully-hoist flag";
                    packageUpdater.log().info(message, npmrcFile);
                }
                shouldWrite = false;
            }
        } else {
            shouldWrite = true;
        }
        if (shouldWrite) {
            try (InputStream content = TaskRunNpmInstall.class
                    .getResourceAsStream("/npmrc")) {
                if (content == null) {
                    throw new IOException(
                            "Couldn't find template npmrc in the classpath");
                }
                FileUtils.copyInputStreamToFile(content, npmrcFile);
                packageUpdater.log().debug("Generated pnpm configuration: '{}'",
                        npmrcFile);
            }
        }
    }

    private void cleanUp() throws ExecutionFailedException {
        if (!options.getNodeModulesFolder().exists()) {
            return;
        }

        if (options.isCiBuild()) {
            deleteNodeModules(options.getNodeModulesFolder());
        } else {
            File modulesYaml = new File(options.getNodeModulesFolder(),
                    MODULES_YAML);
            boolean hasModulesYaml = modulesYaml.exists()
                    && modulesYaml.isFile();
            if (!options.isEnablePnpm() && hasModulesYaml) {
                deleteNodeModules(options.getNodeModulesFolder());
            } else if (options.isEnablePnpm() && !hasModulesYaml) {
                // presence of .staging dir with a "pnpm-*" folder means that
                // pnpm download is in progress, don't remove anything in this
                // case
                File staging = new File(options.getNodeModulesFolder(),
                        ".staging");
                if (!staging.isDirectory() || staging.listFiles(
                        (dir, name) -> name.startsWith("pnpm-")).length == 0) {
                    deleteNodeModules(options.getNodeModulesFolder());
                }
            }
        }
    }

    private void deleteNodeModules(File nodeModulesFolder)
            throws ExecutionFailedException {
        try {
            FrontendUtils.deleteNodeModules(nodeModulesFolder);
        } catch (IOException exception) {
            Logger log = packageUpdater.log();
            log.debug("Exception removing node_modules", exception);
            log.error("Failed to remove '"
                    + options.getNodeModulesFolder().getAbsolutePath()
                    + "'. Please remove it manually.");
            throw new ExecutionFailedException(
                    "Exception removing node_modules. Please remove it manually.");
        }
    }

    private void validateInstalledNpm(FrontendTools tools)
            throws IllegalStateException {
        File npmCacheDir = null;
        try {
            npmCacheDir = tools.getNpmCacheDir();
        } catch (FrontendUtils.CommandExecutionException
                | IllegalStateException e) {
            packageUpdater.log().warn("Failed to get npm cache directory", e);
        }

        if (npmCacheDir != null
                && !tools.folderIsAcceptableByNpm(npmCacheDir)) {
            throw new IllegalStateException(
                    String.format(NPM_VALIDATION_FAIL_MESSAGE));
        }
    }
}
