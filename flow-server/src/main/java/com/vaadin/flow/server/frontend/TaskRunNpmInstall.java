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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.commandToString;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEV_DEPENDENCIES;
import static com.vaadin.flow.server.frontend.NodeUpdater.HASH_KEY;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static elemental.json.impl.JsonUtil.stringify;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Run <code>npm install</code> after dependencies have been updated.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskRunNpmInstall implements FallibleCommand {

    private static final String MODULES_YAML = ".modules.yaml";

    // .vaadin/vaadin.json contains local installation data inside node_modules
    // This will hep us know to execute even when another developer has pushed
    // a new hash to the code repository.
    private static final String INSTALL_HASH = ".vaadin/vaadin.json";

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
            + "%n        node_modules, package-lock.json, webpack.generated.js, pnpm-lock.yaml, pnpmfile.js"
            + "%n======================================================================================================%n";

    private final NodeUpdater packageUpdater;

    private final List<String> ignoredNodeFolders = Arrays.asList(".bin",
            "pnpm", ".ignored_pnpm", ".pnpm", ".staging", ".vaadin",
            MODULES_YAML);
    private final boolean enablePnpm;
    private final boolean requireHomeNodeExec;
    private final boolean autoUpdate;

    private final String nodeVersion;
    private final URI nodeDownloadRoot;
    private final boolean useGlobalPnpm;

    private List<String> additionalPostinstallPackages;

    /**
     * Create an instance of the command.
     *
     * @param packageUpdater
     *            package-updater instance used for checking if previous
     *            execution modified the package.json file
     * @param enablePnpm
     *            whether PNPM should be used instead of NPM
     * @param requireHomeNodeExec
     *            whether vaadin home node executable has to be used
     * @param nodeVersion
     *            The node.js version to be used when node.js is installed
     *            automatically by Vaadin, for example <code>"v16.0.0"</code>.
     *            Use {@value FrontendTools#DEFAULT_NODE_VERSION} by default.
     * @param nodeDownloadRoot
     *            Download node.js from this URL. Handy in heavily firewalled
     *            corporate environments where the node.js download can be
     *            provided from an intranet mirror. Use
     *            {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT} by default.
     * @param useGlobalPnpm
     *            use globally installed pnpm instead of the default one (see
     *            {@link FrontendTools#DEFAULT_PNPM_VERSION})
     * @param autoUpdate
     *            {@code true} to automatically update to a new node version
     * @param additionalPostinstallPackages
     *            a list of packages to run postinstall for
     */
    TaskRunNpmInstall(NodeUpdater packageUpdater, boolean enablePnpm,
            boolean requireHomeNodeExec, String nodeVersion,
            URI nodeDownloadRoot, boolean useGlobalPnpm, boolean autoUpdate,
            List<String> additionalPostinstallPackages) {
        this.packageUpdater = packageUpdater;
        this.enablePnpm = enablePnpm;
        this.requireHomeNodeExec = requireHomeNodeExec;
        this.nodeVersion = Objects.requireNonNull(nodeVersion);
        this.nodeDownloadRoot = Objects.requireNonNull(nodeDownloadRoot);
        this.useGlobalPnpm = useGlobalPnpm;
        this.autoUpdate = autoUpdate;
        this.additionalPostinstallPackages = Objects
                .requireNonNull(additionalPostinstallPackages);
    }

    @Override
    public void execute() throws ExecutionFailedException {
        String toolName = enablePnpm ? "pnpm" : "npm";
        if (packageUpdater.modified || shouldRunNpmInstall()) {
            packageUpdater.log().info("Running `" + toolName + " install` to "
                    + "resolve and optionally download frontend dependencies. "
                    + "This may take a moment, please stand by...");
            runNpmInstall();

            updateLocalHash();
        } else {
            packageUpdater.log().info(
                    "Skipping `{} install` because the frontend packages are already "
                            + "installed in the folder '{}' and the hash in the file '{}' is the same as in '{}'",
                    toolName,
                    packageUpdater.nodeModulesFolder.getAbsolutePath(),
                    getLocalHashFile().getAbsolutePath(),
                    Constants.PACKAGE_JSON);

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
            final JsonObject vaadin = packageUpdater.getPackageJson()
                    .getObject(VAADIN_DEP_KEY);
            if (vaadin == null) {
                packageUpdater.log().warn("No vaadin object in package.json");
                return;
            }
            final String hash = vaadin.getString(HASH_KEY);

            final JsonObject localHash = Json.createObject();
            localHash.put(HASH_KEY, hash);

            final File localHashFile = getLocalHashFile();
            FileUtils.forceMkdirParent(localHashFile);
            String content = stringify(localHash, 2) + "\n";
            FileUtils.writeStringToFile(localHashFile, content, UTF_8.name());

        } catch (IOException e) {
            packageUpdater.log().warn("Failed to update node_modules hash.", e);
        }
    }

    private File getLocalHashFile() {
        return new File(packageUpdater.nodeModulesFolder, INSTALL_HASH);
    }

    private boolean shouldRunNpmInstall() {
        if (!packageUpdater.nodeModulesFolder.isDirectory()) {
            return true;
        }
        // Ignore .bin and pnpm folders as those are always installed for
        // pnpm execution
        File[] installedPackages = packageUpdater.nodeModulesFolder
                .listFiles((dir, name) -> !ignoredNodeFolders.contains(name));
        assert installedPackages != null;
        if (installedPackages.length == 0) {
            // Nothing installed
            return true;
        } else if (installedPackages.length == 1 && FLOW_NPM_PACKAGE_NAME
                .startsWith(installedPackages[0].getName())) {
            // Only flow-frontend installed
            return true;
        } else {
            return isVaadinHashUpdated();
        }
    }

    private boolean isVaadinHashUpdated() {
        final File localHashFile = getLocalHashFile();
        if (localHashFile.exists()) {
            try {
                String fileContent = FileUtils.readFileToString(localHashFile,
                        UTF_8.name());
                JsonObject content = Json.parse(fileContent);
                if (content.hasKey(HASH_KEY)) {
                    final JsonObject packageJson = packageUpdater
                            .getPackageJson();
                    return !content.getString(HASH_KEY).equals(packageJson
                            .getObject(VAADIN_DEP_KEY).getString(HASH_KEY));
                }
            } catch (IOException e) {
                packageUpdater.log()
                        .warn("Failed to load hashes forcing npm execution", e);
            }
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
        if (enablePnpm) {
            try {
                createPnpmFile(packageUpdater.generateVersionsJson());
            } catch (IOException exception) {
                throw new ExecutionFailedException(
                        "Failed to read frontend version data from vaadin-core "
                                + "and make it available to pnpm for locking transitive dependencies.\n"
                                + "Please report an issue, as a workaround try running project "
                                + "with npm by setting system variable -Dvaadin.pnpm.enable=false",
                        exception);
            }
            try {
                createNpmRcFile();
            } catch (IOException exception) {
                logger.warn(".npmrc generation failed; pnpm "
                        + "package installation may require manaually passing "
                        + "the --shamefully-hoist flag", exception);
            }
        }

        String baseDir = packageUpdater.npmFolder.getAbsolutePath();

        FrontendToolsSettings settings = new FrontendToolsSettings(baseDir,
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        settings.setNodeDownloadRoot(nodeDownloadRoot);
        settings.setForceAlternativeNode(requireHomeNodeExec);
        settings.setUseGlobalPnpm(useGlobalPnpm);
        settings.setAutoUpdate(autoUpdate);
        settings.setNodeVersion(nodeVersion);
        FrontendTools tools = new FrontendTools(settings);
        List<String> npmExecutable;
        List<String> npmInstallCommand;
        List<String> postinstallCommand;

        try {
            if (requireHomeNodeExec) {
                tools.forceAlternativeNodeExecutable();
            }
            if (enablePnpm) {
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
        npmInstallCommand.add("install");

        postinstallCommand.add("run");
        postinstallCommand.add("postinstall");

        if (logger.isDebugEnabled()) {
            logger.debug(
                    commandToString(packageUpdater.npmFolder.getAbsolutePath(),
                            npmInstallCommand));
        }

        String toolName = enablePnpm ? "pnpm" : "npm";

        String commandString = npmInstallCommand.stream()
                .collect(Collectors.joining(" "));

        logger.info("using '{}' for frontend package installation",
                String.join(" ", npmInstallCommand));

        Process process = null;
        try {
            process = runNpmCommand(npmInstallCommand,
                    packageUpdater.npmFolder);

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
        postinstallPackages.add("esbuild");
        postinstallPackages.add("@vaadin/vaadin-usage-statistics");
        postinstallPackages.addAll(additionalPostinstallPackages);

        for (String postinstallPackage : postinstallPackages) {
            if (postinstallPackage.trim().equals("")) {
                continue;
            }

            // Execute "npm run postinstall" in the desired folders in
            // node_modules
            File packageFolder = new File(packageUpdater.nodeModulesFolder,
                    postinstallPackage);
            if (!packageFolder.exists()) {
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

    private Process runNpmCommand(List<String> command, File workingDirectory)
            throws IOException {
        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.environment().put("ADBLOCK", "1");
        builder.environment().put("NO_UPDATE_NOTIFIER", "1");
        builder.directory(workingDirectory);
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = builder.start();

        // This will allow to destroy the process which does IO regardless
        // whether it's executed in the same thread or another (may be
        // daemon) thread
        Runtime.getRuntime()
                .addShutdownHook(new Thread(process::destroyForcibly));

        return process;
    }

    /*
     * The pnpmfile.js file is recreated from scratch every time when `pnpm
     * install` is executed. It doesn't take much time to recreate it and it's
     * not supposed that it can be modified by the user. This is done in the
     * same way as for webpack.generated.js.
     */
    private void createPnpmFile(String versionsPath) throws IOException {
        if (versionsPath == null) {
            return;
        }

        File pnpmFile = new File(packageUpdater.npmFolder.getAbsolutePath(),
                "pnpmfile.js");
        try (InputStream content = TaskRunNpmInstall.class
                .getResourceAsStream("/pnpmfile.js")) {
            if (content == null) {
                throw new IOException(
                        "Couldn't find template pnpmfile.js in the classpath");
            }
            FileUtils.copyInputStreamToFile(content, pnpmFile);
            packageUpdater.log().debug("Generated pnpmfile hook file: '{}'",
                    pnpmFile);

            FileUtils.writeLines(pnpmFile,
                    modifyPnpmFile(pnpmFile, versionsPath));
        }
    }

    /*
     * Create an .npmrc file the project directory if there is none.
     */
    private void createNpmRcFile() throws IOException {
        File npmrcFile = new File(packageUpdater.npmFolder.getAbsolutePath(),
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

    private List<String> modifyPnpmFile(File generatedFile, String versionsPath)
            throws IOException {
        List<String> lines = FileUtils.readLines(generatedFile,
                StandardCharsets.UTF_8);
        int i = 0;
        for (String line : lines) {
            if (line.startsWith("const versionsFile")) {
                lines.set(i,
                        "const versionsFile = require('path').resolve(__dirname, '"
                                + versionsPath + "');");
            }
            i++;
        }
        return lines;
    }

    private void cleanUp() throws ExecutionFailedException {
        if (!packageUpdater.nodeModulesFolder.exists()) {
            return;
        }
        File modulesYaml = new File(packageUpdater.nodeModulesFolder,
                MODULES_YAML);
        boolean hasModulesYaml = modulesYaml.exists() && modulesYaml.isFile();
        if (!enablePnpm && hasModulesYaml) {
            deleteNodeModules(packageUpdater.nodeModulesFolder);
        } else if (enablePnpm && !hasModulesYaml) {
            // presence of .staging dir with a "pnpm-*" folder means that pnpm
            // download is in progress, don't remove anything in this case
            File staging = new File(packageUpdater.nodeModulesFolder,
                    ".staging");
            if (!staging.isDirectory() || staging.listFiles(
                    (dir, name) -> name.startsWith("pnpm-")).length == 0) {
                deleteNodeModules(packageUpdater.nodeModulesFolder);
            }
        }
    }

    private void deleteNodeModules(File nodeModulesFolder)
            throws ExecutionFailedException {
        try {
            FileUtils.forceDelete(nodeModulesFolder);
        } catch (IOException exception) {
            Logger log = packageUpdater.log();
            log.debug("Exception removing node_modules", exception);
            log.error("Failed to remove '"
                    + packageUpdater.nodeModulesFolder.getAbsolutePath()
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
