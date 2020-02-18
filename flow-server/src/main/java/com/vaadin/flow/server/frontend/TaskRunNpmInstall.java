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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.Json;

import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.YELLOW;
import static com.vaadin.flow.server.frontend.FrontendUtils.commandToString;
import static com.vaadin.flow.server.frontend.FrontendUtils.console;
import static elemental.json.impl.JsonUtil.stringify;

/**
 * Run <code>npm install</code> after dependencies have been updated.
 *
 * @since 2.0
 */
public class TaskRunNpmInstall implements FallibleCommand {

    private static final String MODULES_YAML = ".modules.yaml";

    private final NodeUpdater packageUpdater;

    private final List<String> ignoredNodeFolders = Arrays.asList(".bin",
            "pnpm", ".ignored_pnpm", ".pnpm", MODULES_YAML);
    private final boolean enablePnpm;

    /**
     * Create an instance of the command.
     *
     * @param packageUpdater
     *            package-updater instance used for checking if previous
     *            execution modified the package.json file
     * @param enablePnpm
     *            whether PNPM should be used instead of NPM
     */
    TaskRunNpmInstall(NodeUpdater packageUpdater, boolean enablePnpm) {
        this.packageUpdater = packageUpdater;
        this.enablePnpm = enablePnpm;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        String toolName = enablePnpm ? "pnpm" : "npm";
        if (packageUpdater.modified || shouldRunNpmInstall()) {
            packageUpdater.log().info("Running `" + toolName + " install` to "
                    + "resolve and optionally download frontend dependencies. "
                    + "This may take a moment, please stand by...");
            runNpmInstall();
        } else {
            packageUpdater.log().info("Skipping `" + toolName + " install`.");
        }
    }

    /**
     * Generate versions json file.
     *
     * @return generated versions json file path
     * @throws IOException
     */
    protected String generateVersionsJson() throws IOException {
        try (InputStream content = TaskRunNpmInstall.class
                .getResourceAsStream("/" + Constants.VAADIN_VERSIONS_JSON)) {
            if (content == null) {
                packageUpdater.log().warn(
                        "Couldn't find {} file to pin dependency versions."
                                + " Transitive dependencies won't be pinned for pnpm.",
                        Constants.VAADIN_VERSIONS_JSON);
                return null;
            }
            File versions = new File(packageUpdater.generatedFolder,
                    "versions.json");
            VersionsJsonConverter convert = new VersionsJsonConverter(Json
                    .parse(IOUtils.toString(content, StandardCharsets.UTF_8)));
            FileUtils.write(versions, stringify(convert.convert(), 2) + "\n",
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

    private boolean shouldRunNpmInstall() {
        if (packageUpdater.nodeModulesFolder.isDirectory()) {
            // Ignore .bin and pnpm folders as those are always installed for
            // pnpm execution
            File[] installedPackages = packageUpdater.nodeModulesFolder
                    .listFiles(
                            (dir, name) -> !ignoredNodeFolders.contains(name));
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

        try {
            cleanUp();
        } catch (IOException exception) {
            throw new ExecutionFailedException("Couldn't remove "
                    + packageUpdater.nodeModulesFolder + " directory",
                    exception);
        }

        List<String> executable;
        String baseDir = packageUpdater.npmFolder.getAbsolutePath();
        try {
            executable = enablePnpm ? FrontendUtils.getPnpmExecutable(baseDir)
                    : FrontendUtils.getNpmExecutable(baseDir);
        } catch (IllegalStateException exception) {
            throw new ExecutionFailedException(exception.getMessage(),
                    exception);
        }
        List<String> command = new ArrayList<>(executable);
        command.add("install");

        console(YELLOW, commandToString(
                packageUpdater.npmFolder.getAbsolutePath(), command));

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
        if (hasModulesYaml != enablePnpm) {
            FileUtils.forceDelete(packageUpdater.nodeModulesFolder);
        }
    }

}
