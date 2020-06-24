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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.vaadin.flow.internal.BuildUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.commandToString;
import static com.vaadin.flow.server.frontend.NodeUpdater.HASH_KEY;
import static com.vaadin.flow.server.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static elemental.json.impl.JsonUtil.stringify;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Run <code>npm install</code> after dependencies have been updated.
 *
 * @since 2.0
 */
public class TaskRunNpmInstall implements FallibleCommand {

    private static final String DEV_DEPENDENCIES_PATH = "dev.dependencies.path";

    private static final String MODULES_YAML = ".modules.yaml";

    // .vaadin/vaadin.json contains local installation data inside node_modules
    // This will hep us know to execute even when another developer has pushed
    // a new hash to the code repository.
    private static final String INSTALL_HASH = ".vaadin/vaadin.json";

    private final NodeUpdater packageUpdater;

    private final List<String> ignoredNodeFolders = Arrays
            .asList(".bin", "pnpm", ".ignored_pnpm", ".pnpm", ".staging",
                    ".vaadin", MODULES_YAML);
    private final boolean enablePnpm;
    private final boolean requireHomeNodeExec;
    private final ClassFinder classFinder;

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example <code>"v12.16.0"</code>. Defaults to null which uses
     * the Vaadin-default node version - see {@link FrontendTools} for details.
     */
    public String nodeVersion = null;

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to null which will cause the downloader to use
     * {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
     */
    public URI nodeDownloadRoot = null;

    /**
     * Create an instance of the command.
     *
     * @param classFinder
     *            a reusable class finder
     * @param packageUpdater
     *            package-updater instance used for checking if previous
     *            execution modified the package.json file
     * @param enablePnpm
     *            whether PNPM should be used instead of NPM
     * @param requireHomeNodeExec
     *            whether vaadin home node executable has to be used
     */
    TaskRunNpmInstall(ClassFinder classFinder, NodeUpdater packageUpdater,
            boolean enablePnpm, boolean requireHomeNodeExec) {
        this.classFinder = classFinder;
        this.packageUpdater = packageUpdater;
        this.enablePnpm = enablePnpm;
        this.requireHomeNodeExec = requireHomeNodeExec;
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
            packageUpdater.log().info("Skipping `" + toolName + " install`.");
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
            if(vaadin == null) {
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

    /**
     * Generate versions json file.
     *
     * @return generated versions json file path
     * @throws IOException
     */
    protected String generateVersionsJson() throws IOException {
        URL resource = classFinder.getResource(Constants.VAADIN_VERSIONS_JSON);
        if (resource == null) {
            packageUpdater.log()
                    .warn("Couldn't find {} file to pin dependency versions."
                            + " Transitive dependencies won't be pinned for pnpm.",
                            Constants.VAADIN_VERSIONS_JSON);
        }
        try (InputStream content = resource == null ? null
                : resource.openStream()) {

            File versions = new File(packageUpdater.generatedFolder,
                    "versions.json");

            JsonObject versionsJson = getVersions(content);
            if (versionsJson == null) {
                return null;
            }
            FileUtils.write(versions, stringify(versionsJson, 2) + "\n",
                    StandardCharsets.UTF_8);
            Path versionsPath = versions.toPath();
            if (versions.isAbsolute()) {
                return FrontendUtils.getUnixRelativePath(
                        packageUpdater.npmFolder.toPath(), versionsPath);
            } else {
                return FrontendUtils.getUnixPath(versionsPath);
            }
        }
    }

    /**
     * Returns a path inside classpath to the file with dev dependencies locked.
     * <p>
     * The file may absent in the classapth.
     *
     * @return the path to the dev dependencies file in the classpath, not
     *         {@code null}
     */
    protected String getDevDependenciesFilePath() {
        return BuildUtil.getBuildProperty(DEV_DEPENDENCIES_PATH);
    }

    private JsonObject getVersions(InputStream platformVersions)
            throws IOException {
        JsonObject versionsJson = null;
        if (platformVersions != null) {
            VersionsJsonConverter convert = new VersionsJsonConverter(
                    Json.parse(IOUtils.toString(platformVersions,
                            StandardCharsets.UTF_8)));
            versionsJson = convert.getConvertedJson();
            versionsJson = new VersionsJsonFilter(
                    packageUpdater.getPackageJson(), NodeUpdater.DEPENDENCIES)
                            .getFilteredVersions(versionsJson);
        }

        String genDevDependenciesPath = getDevDependenciesFilePath();
        if (genDevDependenciesPath == null) {
            packageUpdater.log().error(
                    "Couldn't find dev dependencies file path from proeprties file. "
                            + "Dev dependencies won't be locked");
            return versionsJson;
        }
        JsonObject devDeps = readGeneratedDevDependencies(
                genDevDependenciesPath);
        if (devDeps == null) {
            return versionsJson;
        }
        devDeps = new VersionsJsonFilter(packageUpdater.getPackageJson(),
                NodeUpdater.DEV_DEPENDENCIES).getFilteredVersions(devDeps);
        if (versionsJson == null) {
            return devDeps;
        }
        for (String key : versionsJson.keys()) {
            devDeps.put(key, versionsJson.getString(key));
        }
        return devDeps;
    }

    private JsonObject readGeneratedDevDependencies(String path)
            throws IOException {
        URL resource = classFinder.getResource(path);
        if (resource == null) {
            packageUpdater.log().warn("Couldn't find  dev dependencies file. "
                    + "Dev dependencies won't be locked");
            return null;
        }
        try (InputStream content = resource.openStream()) {
            return Json
                    .parse(IOUtils.toString(content, StandardCharsets.UTF_8));
        }
    }

    private boolean shouldRunNpmInstall() {
        if (packageUpdater.nodeModulesFolder.isDirectory()) {
            // Ignore .bin and pnpm folders as those are always installed for
            // pnpm execution
            File[] installedPackages = packageUpdater.nodeModulesFolder
                    .listFiles(
                            (dir, name) -> !ignoredNodeFolders.contains(name));
            assert installedPackages != null;
            return installedPackages.length == 0 || (
                    installedPackages.length == 1 && FLOW_NPM_PACKAGE_NAME
                            .startsWith(installedPackages[0].getName())) || (
                    installedPackages.length > 0 && isVaadinHashUpdated());
        }
        return true;
    }

    private boolean isVaadinHashUpdated() {
        final File localHashFile = getLocalHashFile();
        if (localHashFile.exists()) {
            try {
                String fileContent = FileUtils
                        .readFileToString(localHashFile, UTF_8.name());
                JsonObject content = Json.parse(fileContent);
                if (content.hasKey(HASH_KEY)) {
                    final JsonObject packageJson = packageUpdater
                            .getPackageJson();
                    return !content.getString(HASH_KEY)
                            .equals(packageJson.getObject(VAADIN_DEP_KEY)
                                    .getString(HASH_KEY));
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
        try {
            cleanUp();
        } catch (IOException exception) {
            throw new ExecutionFailedException("Couldn't remove "
                    + packageUpdater.nodeModulesFolder + " directory",
                    exception);
        }

        if (enablePnpm) {
            try {
                createPnpmFile(generateVersionsJson());
            } catch (IOException exception) {
                throw new ExecutionFailedException(
                        "Failed to read frontend version data from vaadin-core "
                                + "and make it available to pnpm for locking transitive dependencies.\n"
                                + "Please report an issue, as a workaround try running project "
                                + "with npm by setting system variable -Dvaadin.pnpm.enable=false",
                        exception);
            }
        }

        List<String> executable;
        String baseDir = packageUpdater.npmFolder.getAbsolutePath();

        FrontendTools tools = new FrontendTools(baseDir,
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        if (nodeVersion != null) {
            tools.nodeVersion = nodeVersion;
        }
        tools.nodeDownloadRoot = nodeDownloadRoot;
        try {
            if (requireHomeNodeExec) {
                tools.forceAlternativeNodeExecutable();
            }
            executable = enablePnpm ? tools.getPnpmExecutable()
                    : tools.getNpmExecutable();
        } catch (IllegalStateException exception) {
            throw new ExecutionFailedException(exception.getMessage(),
                    exception);
        }
        List<String> command = new ArrayList<>(executable);
        command.add("install");

        if (packageUpdater.log().isDebugEnabled()) {
            packageUpdater.log().debug(commandToString(
                    packageUpdater.npmFolder.getAbsolutePath(), command));
        }

        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.environment().put("ADBLOCK", "1");
        builder.environment().put("NO_UPDATE_NOTIFIER", "1");
        builder.directory(packageUpdater.npmFolder);

        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

        String toolName = enablePnpm ? "pnpm" : "npm";

        Process process = null;
        try {
            process = builder.inheritIO().start();
            int errorCode = process.waitFor();
            if (errorCode != 0) {
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
                packageUpdater.log()
                        .info("Frontend dependencies resolved successfully.");
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
            packageUpdater.log().info("Generated pnpmfile hook file: '{}'",
                    pnpmFile);

            FileUtils.writeLines(pnpmFile,
                    modifyPnpmFile(pnpmFile, versionsPath));
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

    private void cleanUp() throws IOException {
        if (!packageUpdater.nodeModulesFolder.exists()) {
            return;
        }
        File modulesYaml = new File(packageUpdater.nodeModulesFolder,
                MODULES_YAML);
        boolean hasModulesYaml = modulesYaml.exists() && modulesYaml.isFile();
        if (!enablePnpm && hasModulesYaml) {
            FileUtils.forceDelete(packageUpdater.nodeModulesFolder);
        } else if (enablePnpm && !hasModulesYaml) {
            // presence of .staging dir with a "pnpm-*" folder means that pnpm
            // download is in progress, don't remove anything in this case
            File staging = new File(packageUpdater.nodeModulesFolder,
                    ".staging");
            if (!staging.isDirectory() || staging.listFiles(
                    (dir, name) -> name.startsWith("pnpm-")).length == 0) {
                FileUtils.forceDelete(packageUpdater.nodeModulesFolder);
            }
        }
    }
}
