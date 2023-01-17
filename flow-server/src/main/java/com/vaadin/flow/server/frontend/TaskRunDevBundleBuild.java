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
import java.net.URL;
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
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.DEV_BUNDLE_JAR_PATH;

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

    public static boolean needsBuild(Options options,
            FrontendDependenciesScanner frontendDependencies,
            ClassFinder finder) {
        getLogger().info("Checking if an express mode bundle build is needed");

        try {
            boolean needsBuild = needsBuildInternal(options,
                    frontendDependencies, finder);
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

    protected static boolean needsBuildInternal(Options options,
            FrontendDependenciesScanner frontendDependencies,
            ClassFinder finder) throws IOException {
        File npmFolder = options.getNpmFolder();

        if (!FrontendUtils.getDevBundleFolder(npmFolder).exists()
                && !hasJarBundle()) {
            return true;
        }

        String statsJsonContent = FrontendUtils.findBundleStatsJson(npmFolder);
        if (statsJsonContent == null) {
            // without stats.json in bundle we can not say if it is up to date
            return true;
        }

        JsonObject packageJson = getPackageJson(options, frontendDependencies,
                finder);
        JsonObject statsJson = Json.parse(statsJsonContent);

        // Get scanned @NpmPackage annotations
        final Map<String, String> npmPackages = frontendDependencies
                .getPackages();

        if (!hashAndBundleModulesEqual(statsJson, packageJson, npmPackages)) {
            // Hash in the project doesn't match the bundle hash or NpmPackages
            // are found missing in bundle.
            return true;
        }
        if (!frontendImportsFound(statsJson, options, finder,
                frontendDependencies)) {
            return true;
        }

        return false;
    }

    private static boolean frontendImportsFound(JsonObject statsJson,
            Options options, ClassFinder finder,
            FrontendDependenciesScanner frontendDependencies) {

        // Validate frontend requirements in generated-flow-imports.js
        final GenerateMainImports generateMainImports = new GenerateMainImports(
                finder, frontendDependencies, options, statsJson);
        generateMainImports.run();
        final List<String> imports = generateMainImports.getLines().stream()
                .filter(line -> line.startsWith("import"))
                .map(line -> line.substring(line.indexOf('\'') + 1,
                        line.lastIndexOf('\'')))
                .collect(Collectors.toList());
        JsonArray statsBundle = statsJson.hasKey("bundleImports")
                ? statsJson.getArray("bundleImports")
                : Json.createArray();
        final List<String> missingFromBundle = imports.stream().filter(
                importString -> !arrayContainsString(statsBundle, importString))
                .collect(Collectors.toList());

        if (!missingFromBundle.isEmpty()) {
            for (String dependency : missingFromBundle) {
                getLogger().info("Frontend import " + dependency
                        + " is missing from the bundle");
            }
            return false;
        }

        return true;
    }

    private static boolean arrayContainsString(JsonArray array, String string) {
        string = string.replace("Frontend/", "./");
        for (int i = 0; i < array.length(); i++) {
            if (string.equals(array.getString(i).replace("Frontend/", "./"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasJarBundle() {
        final URL resource = TaskRunDevBundleBuild.class.getClassLoader()
                .getResource(DEV_BUNDLE_JAR_PATH + "config/stats.json");
        return resource != null;
    }

    /**
     * Verify that package hash versions are equal and that all project
     * npmPackages are in bundle.
     *
     *
     * @param statsJson
     *            devBundle statsJson
     * @param packageJson
     *            packageJson
     * @param npmPackages
     *            npm packages map
     * @return {@code true} if up to date
     */
    private static boolean hashAndBundleModulesEqual(JsonObject statsJson,
            JsonObject packageJson, Map<String, String> npmPackages) {

        String packageJsonHash = getPackageJsonHash(packageJson);
        String bundlePackageJsonHash = getStatsHash(statsJson);

        if (packageJsonHash == null || packageJsonHash.isEmpty()) {
            getLogger().warn("No hash found for 'package.json'.");
            return false;
        }

        JsonObject bundleModules = statsJson.getObject("npmModules");

        // Check that bundle modules contains all package dependencies
        if (packageJsonHash.equals(bundlePackageJsonHash)) {
            if (!dependenciesContainsAllPackages(npmPackages, bundleModules)) {
                return false;
            }
        }

        JsonObject dependencies = packageJson.getObject("dependencies");

        List<String> missingFromBundle = Arrays.stream(dependencies.keys())
                .filter(pkg -> !bundleModules.hasKey(pkg))
                .collect(Collectors.toList());

        if (!missingFromBundle.isEmpty()) {
            for (String dependency : missingFromBundle) {
                getLogger().info("Dependency " + dependency
                        + " is missing from the bundle");
            }
            return false;
        }

        // We know here that all dependencies exist
        missingFromBundle = Arrays.stream(dependencies.keys())
                .filter(pkg -> !versionAccepted(dependencies.getString(pkg),
                        bundleModules.getString(pkg)))
                .collect(Collectors.toList());

        if (!missingFromBundle.isEmpty()) {
            for (String pkg : missingFromBundle) {
                getLogger().info(
                        "Dependency {}:{} has the wrong version {} in the bundle",
                        pkg, dependencies.getString(pkg),
                        bundleModules.getString(pkg));
            }
            return false;
        }

        return true;
    }

    private static boolean versionAccepted(String expected, String actual) {
        FrontendVersion expectedVersion = new FrontendVersion(expected);
        FrontendVersion actualVersion = new FrontendVersion(actual);

        if (expected.startsWith("~")) {
            boolean correctRange = expectedVersion
                    .getMajorVersion() == actualVersion.getMajorVersion()
                    && expectedVersion.getMinorVersion() == actualVersion
                            .getMinorVersion();
            // Installed version may be newer than the package version.
            return (expectedVersion.isEqualTo(actualVersion)
                    || expectedVersion.isOlderThan(actualVersion))
                    && correctRange;
        } else if (expected.startsWith("^")) {
            boolean correctRange = expectedVersion
                    .getMajorVersion() == actualVersion.getMajorVersion();
            // Installed version may be newer than the package version.
            return (expectedVersion.isEqualTo(actualVersion)
                    || expectedVersion.isOlderThan(actualVersion))
                    && correctRange;
        }
        return expectedVersion.isEqualTo(actualVersion);
    }

    private static JsonObject getPackageJson(Options options,
            FrontendDependenciesScanner frontendDependencies,
            ClassFinder finder) {
        File packageJsonFile = new File(options.getNpmFolder(), "package.json");

        if (packageJsonFile.exists()) {
            try {
                return Json.parse(FileUtils.readFileToString(packageJsonFile,
                        StandardCharsets.UTF_8));
            } catch (IOException e) {
                getLogger().warn("Failed to read package.json", e);
            }
        } else {
            JsonObject packageJson = getDefaultPackageJson(options,
                    frontendDependencies, finder);
            if (packageJson != null) {
                return packageJson;
            }
        }
        return null;
    }

    protected static JsonObject getDefaultPackageJson(Options options,
            FrontendDependenciesScanner frontendDependencies,
            ClassFinder finder) {
        NodeUpdater nodeUpdater = new NodeUpdater(finder, frontendDependencies,
                options) {
            @Override
            public void execute() {
            }
        };
        try {
            JsonObject packageJson = nodeUpdater.getPackageJson();
            nodeUpdater.updateDefaultDependencies(packageJson);
            final String hash = TaskUpdatePackages
                    .generatePackageJsonHash(packageJson);
            packageJson.getObject(NodeUpdater.VAADIN_DEP_KEY)
                    .put(NodeUpdater.HASH_KEY, hash);
            return packageJson;
        } catch (IOException e) {
            getLogger().warn("Failed to generate package.json", e);
        }
        return null;
    }

    private static String getStatsHash(JsonObject statsJson) {
        if (statsJson.hasKey("packageJsonHash")) {
            return statsJson.getString("packageJsonHash");
        }

        return null;
    }

    private static String getPackageJsonHash(JsonObject packageJson) {
        if (packageJson != null && packageJson.hasKey("vaadin")
                && packageJson.getObject("vaadin").hasKey("hash")) {
            return packageJson.getObject("vaadin").getString("hash");
        }

        return null;
    }

    /**
     * Check that all npmPackages are in the given dependency object.
     *
     * @param npmPackages
     *            {@code @NpmPackage} key-value map
     * @param dependencies
     *            json object containing dependencies to check against
     * @return {@code false} if all packages are found
     */
    private static boolean dependenciesContainsAllPackages(
            Map<String, String> npmPackages, JsonObject dependencies) {
        final List<String> collect = npmPackages.keySet().stream()
                .filter(pkg -> !(dependencies.hasKey(pkg) && versionAccepted(
                        dependencies.getString(pkg), npmPackages.get(pkg))))
                .collect(Collectors.toList());
        if (!collect.isEmpty()) {
            collect.forEach(dependency -> getLogger().info("Dependency "
                    + dependency + " is missing from the bundle"));
            return false;
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
