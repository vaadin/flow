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
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.server.webcomponent.WebComponentExporterTagExtractor;
import com.vaadin.flow.server.webcomponent.WebComponentExporterUtils;

import com.vaadin.flow.shared.util.SharedUtil;
import com.vaadin.flow.theme.ThemeDefinition;

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

    private static final Pattern THEME_PATH_PATTERN = Pattern
            .compile("themes\\/([\\s\\S]+?)\\/theme.json");

    private final Options options;

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
            getLogger().info("No dev-bundle found.");
            return true;
        }

        String statsJsonContent = FrontendUtils.findBundleStatsJson(npmFolder);
        if (statsJsonContent == null) {
            // without stats.json in bundle we can not say if it is up to date
            getLogger().info("No dev-bundle stats.json found for validation.");
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

        if (themeConfigurationChanged(options, statsJson,
                frontendDependencies)) {
            return true;
        }

        if (exportedWebComponents(statsJson, finder)) {
            return true;
        }

        return false;
    }

    private static boolean exportedWebComponents(JsonObject statsJson,
            ClassFinder finder) {
        try {
            Set<Class<?>> exporterRelatedClasses = new HashSet<>();
            finder.getSubTypesOf(WebComponentExporter.class.getName())
                    .forEach(exporterRelatedClasses::add);
            finder.getSubTypesOf(WebComponentExporterFactory.class.getName())
                    .forEach(exporterRelatedClasses::add);

            Set<String> webComponents = WebComponentExporterUtils
                    .getFactories(exporterRelatedClasses).stream()
                    .map(TaskRunDevBundleBuild::getTag)
                    .collect(Collectors.toSet());

            JsonArray webComponentsInStats = statsJson
                    .getArray("webComponents");

            if (webComponentsInStats == null) {
                if (!webComponents.isEmpty()) {
                    getLogger().info(
                            "Found embedded web components not yet included "
                                    + "into the dev bundle: {}",
                            String.join(", ", webComponents));
                    return true;
                }
                return false;
            } else {
                for (int index = 0; index < webComponentsInStats
                        .length(); index++) {
                    String webComponentInStats = webComponentsInStats
                            .getString(index);
                    webComponents.remove(webComponentInStats);
                }
            }

            if (!webComponents.isEmpty()) {
                getLogger().info(
                        "Found newly added embedded web components not "
                                + "yet included into the dev bundle: {}",
                        String.join(", ", webComponents));
                return true;
            }

            return false;

        } catch (ClassNotFoundException e) {
            getLogger()
                    .error("Unable to locate embedded web component classes.");
            return false;
        }
    }

    private static boolean themeConfigurationChanged(Options options,
            JsonObject statsJson,
            FrontendDependenciesScanner frontendDependencies)
            throws IOException {
        Map<String, String> themeJsonHashes = new HashMap<>();

        if (options.jarFiles == null) {
            return false;
        }

        options.jarFiles.stream().filter(File::exists)
                .filter(file -> !file.isDirectory())
                .forEach(jarFile -> calculateHashesForPackagedThemeJson(jarFile,
                        themeJsonHashes));

        ThemeDefinition themeDefinition = frontendDependencies
                .getThemeDefinition();
        Optional<String> projectThemeJsonHash = getHashForProjectThemeJson(
                options, themeDefinition);

        JsonObject hashesInStats = statsJson.getObject("themeJsonHashes");
        if (hashesInStats == null && (!themeJsonHashes.isEmpty()
                || projectThemeJsonHash.isPresent())) {
            getLogger().info(
                    "Found newly added theme configurations in 'theme.json'.");
            return true;
        }

        if (projectThemeJsonHash.isPresent()) {
            String projectThemeName = themeDefinition.getName();
            String key;
            if (hashesInStats.hasKey(projectThemeName)) {
                key = projectThemeName;
            } else if (hashesInStats.hasKey(Constants.DEV_BUNDLE_NAME)) {
                key = Constants.DEV_BUNDLE_NAME;
            } else {
                getLogger().info(
                        "Found newly added configuration for project theme '{}' in 'theme.json'.",
                        projectThemeName);
                return true;
            }

            if (!hashesInStats.getString(key)
                    .equals(projectThemeJsonHash.get())) {
                getLogger().info(
                        "Found new configuration for project theme '{}' in 'theme.json'.",
                        projectThemeName);
                return true;
            }
        }

        for (Map.Entry<String, String> themeHash : themeJsonHashes.entrySet()) {
            if (!hashesInStats.hasKey(themeHash.getKey())) {
                getLogger().info(
                        "Found new configuration for theme '{}' in 'theme.json'.",
                        themeHash.getKey());
                return true;
            } else if (!hashesInStats.getString(themeHash.getKey())
                    .equals(themeHash.getValue())) {
                getLogger().info(
                        "Found updated configuration for theme '{}' in 'theme.json'.",
                        themeHash.getKey());
                return true;
            }
        }

        return false;
    }

    private static boolean frontendImportsFound(JsonObject statsJson,
            Options options, ClassFinder finder,
            FrontendDependenciesScanner frontendDependencies)
            throws IOException {

        // Validate frontend requirements in flow-generated-imports.js
        final GenerateMainImports generateMainImports = new GenerateMainImports(
                finder, frontendDependencies, options, statsJson);
        generateMainImports.run();
        final List<String> imports = generateMainImports.getLines().stream()
                .filter(line -> line.startsWith("import"))
                .map(line -> line.substring(line.indexOf('\'') + 1,
                        line.lastIndexOf('\'')))
                .map(importString -> importString.contains("?")
                        ? importString.substring(0,
                                importString.lastIndexOf("?"))
                        : importString)
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

        String resourcePath = "generated/jar-resources/";
        final List<String> jarImports = imports.stream()
                .filter(importString -> importString.contains(resourcePath))
                .map(importString -> importString
                        .substring(importString.indexOf(resourcePath)
                                + resourcePath.length()))
                .collect(Collectors.toList());

        final List<String> projectImports = imports.stream()
                .filter(importString -> importString
                        .startsWith(FrontendUtils.FRONTEND_FOLDER_ALIAS)
                        && !importString.contains(resourcePath))
                .map(importString -> importString.substring(
                        FrontendUtils.FRONTEND_FOLDER_ALIAS.length()))
                .collect(Collectors.toList());

        final JsonObject frontendHashes = statsJson.getObject("frontendHashes");
        List<String> faultyContent = new ArrayList<>();

        for (String jarImport : jarImports) {
            final String jarResourceString = FrontendUtils
                    .getJarResourceString(jarImport);
            if (jarResourceString == null) {
                getLogger().info("No file found for '{}'", jarImport);
                return false;
            }
            compareFrontendHashes(frontendHashes, faultyContent, jarImport,
                    jarResourceString);
        }

        for (String projectImport : projectImports) {
            File frontendFile = new File(options.getFrontendDirectory(),
                    projectImport);
            if (!frontendFile.exists()) {
                getLogger().info("No file found for '{}'", projectImport);
                return false;
            }
            String frontendFileContent = FileUtils
                    .readFileToString(frontendFile, StandardCharsets.UTF_8);
            compareFrontendHashes(frontendHashes, faultyContent, projectImport,
                    frontendFileContent);
        }

        if (!faultyContent.isEmpty()) {
            logChangedFiles(faultyContent,
                    "Detected changed content for frontend files:\n{}");
            return false;
        }

        for (String indexFileName : Arrays.asList(FrontendUtils.INDEX_TS,
                FrontendUtils.INDEX_TSX, FrontendUtils.INDEX_JS)) {
            if (indexTsChanged(options, frontendHashes, indexFileName)) {
                return false;
            }
        }

        if (newOrDeletedFrontendFiles(options, frontendHashes, jarImports,
                projectImports)) {
            return false;
        }

        return true;
    }

    private static boolean newOrDeletedFrontendFiles(Options options,
            JsonObject frontendHashes, List<String> jarImports,
            List<String> projectImports) throws IOException {
        List<String> expectedFrontendFiles = new ArrayList<>(
                Arrays.asList(frontendHashes.keys()));
        expectedFrontendFiles.removeAll(jarImports);
        expectedFrontendFiles.removeAll(projectImports);
        expectedFrontendFiles.remove(FrontendUtils.INDEX_TS);
        expectedFrontendFiles.remove(FrontendUtils.INDEX_JS);
        expectedFrontendFiles.remove(FrontendUtils.INDEX_TSX);

        if (!expectedFrontendFiles.isEmpty()) {
            List<String> deleted = new ArrayList<>();
            List<String> changed = new ArrayList<>();
            for (String filePath : expectedFrontendFiles) {
                File frontendFile = new File(options.getFrontendDirectory(),
                        filePath);
                if (frontendFile.exists()) {
                    final String hash = calculateHash(
                            FileUtils.readFileToString(frontendFile,
                                    StandardCharsets.UTF_8));
                    if (!frontendHashes.getString(filePath).equals(hash)) {
                        changed.add(filePath);
                    }
                } else {
                    deleted.add(filePath);
                }
            }
            if (!changed.isEmpty()) {
                logChangedFiles(changed,
                        "Detected changed frontend files:\n{}");
            }
            if (!deleted.isEmpty()) {
                logChangedFiles(deleted,
                        "Detected deleted frontend files:\n{}");
            }
            return !changed.isEmpty() || !deleted.isEmpty();
        }
        return false;
    }

    private static boolean indexTsChanged(Options options,
            JsonObject frontendHashes, String fileName) throws IOException {
        File indexTs = new File(options.getFrontendDirectory(), fileName);
        if (indexTs.exists()) {
            if (!frontendHashes.hasKey(fileName)) {
                getLogger().info("'{}' added to the project", fileName);
                return true;
            }
            final String contentHash = calculateHash(FileUtils
                    .readFileToString(indexTs, StandardCharsets.UTF_8));
            if (!frontendHashes.getString(fileName).equals(contentHash)) {
                getLogger().info("'{}' is not up to date", fileName);
                return true;
            }
        } else if (frontendHashes.hasKey(fileName)) {
            getLogger().info("'{}' has been deleted", fileName);
            return true;
        }
        return false;
    }

    private static void logChangedFiles(List<String> frontendFiles,
            String message) {
        StringBuilder removedFiles = new StringBuilder();
        for (String file : frontendFiles) {
            removedFiles.append(" - ").append(file).append("\n");
        }
        getLogger().info(message, removedFiles);
    }

    private static void compareFrontendHashes(JsonObject frontendHashes,
            List<String> faultyContent, String frontendFilePath,
            String frontendFileContent) {
        final String contentHash = calculateHash(frontendFileContent);
        if (frontendHashes.hasKey(frontendFilePath) && !frontendHashes
                .getString(frontendFilePath).equals(contentHash)) {
            faultyContent.add(frontendFilePath);
        } else if (!frontendHashes.hasKey(frontendFilePath)) {
            getLogger().info("No hash info for '{}'", frontendFilePath);
            faultyContent.add(frontendFilePath);
        }
    }

    private static String calculateHash(String fileContent) {
        String content = fileContent.replaceAll("\\r\\n", "\n");
        return StringUtil.getHash(content, StandardCharsets.UTF_8);
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
            getLogger().error(
                    "No hash found for 'package.json' even though one should always be generated!");
            return false;
        }

        JsonObject bundleModules = statsJson
                .getObject("packageJsonDependencies");

        if (bundleModules == null) {
            getLogger().error(
                    "Dev bundle did not contain package json dependencies to validate.\n"
                            + "Rebuild of bundle needed.");
            return false;
        }

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

    /**
     * Get the package.json file from disk if available else generate in memory.
     * <p>
     * For the loaded file update versions as per in memory to get correct
     * application versions.
     *
     * @param options
     *            the task options
     * @param frontendDependencies
     *            frontend dependency scanner
     * @param finder
     *            classfinder
     * @return package.json content as JsonObject
     */
    private static JsonObject getPackageJson(Options options,
            FrontendDependenciesScanner frontendDependencies,
            ClassFinder finder) {
        File packageJsonFile = new File(options.getNpmFolder(), "package.json");

        if (packageJsonFile.exists()) {
            try {
                final JsonObject packageJson = Json
                        .parse(FileUtils.readFileToString(packageJsonFile,
                                StandardCharsets.UTF_8));
                cleanOldPlatformDependencies(packageJson);
                return getDefaultPackageJson(options, frontendDependencies,
                        finder, packageJson);
            } catch (IOException e) {
                getLogger().warn("Failed to read package.json", e);
            }
        } else {
            return getDefaultPackageJson(options, frontendDependencies, finder,
                    null);
        }
        return null;
    }

    protected static JsonObject getDefaultPackageJson(Options options,
            FrontendDependenciesScanner frontendDependencies,
            ClassFinder finder, JsonObject packageJson) {
        NodeUpdater nodeUpdater = new NodeUpdater(finder, frontendDependencies,
                options) {
            @Override
            public void execute() {
            }
        };
        try {
            if (packageJson == null) {
                packageJson = nodeUpdater.getPackageJson();
            }
            nodeUpdater.addVaadinDefaultsToJson(packageJson);
            nodeUpdater.updateDefaultDependencies(packageJson);

            final Map<String, String> applicationDependencies = frontendDependencies
                    .getPackages();

            // Add application dependencies
            for (Map.Entry<String, String> dep : applicationDependencies
                    .entrySet()) {
                nodeUpdater.addDependency(packageJson, NodeUpdater.DEPENDENCIES,
                        dep.getKey(), dep.getValue());
            }

            final String hash = TaskUpdatePackages
                    .generatePackageJsonHash(packageJson);
            packageJson.getObject(NodeUpdater.VAADIN_DEP_KEY)
                    .put(NodeUpdater.HASH_KEY, hash);

            final JsonObject platformPinnedDependencies = nodeUpdater
                    .getPlatformPinnedDependencies();
            for (String key : platformPinnedDependencies.keys()) {
                // need to double check that not overriding a scanned
                // dependency since add-ons should be able to downgrade
                // version through exclusion
                if (!applicationDependencies.containsKey(key)) {
                    TaskUpdatePackages.pinPlatformDependency(packageJson,
                            platformPinnedDependencies, key);
                }
            }

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

    private static void calculateHashesForPackagedThemeJson(
            File jarFileToLookup, Map<String, String> packagedThemeHashes) {
        JarContentsManager jarContentsManager = new JarContentsManager();
        if (jarContentsManager.containsPath(jarFileToLookup,
                Constants.RESOURCES_THEME_JAR_DEFAULT)) {
            List<String> themeJsons = jarContentsManager.findFiles(
                    jarFileToLookup, Constants.RESOURCES_THEME_JAR_DEFAULT,
                    "theme.json");
            for (String themeJson : themeJsons) {
                byte[] byteContent = jarContentsManager
                        .getFileContents(jarFileToLookup, themeJson);
                String content = IOUtils.toString(byteContent, "UTF-8");

                Matcher matcher = THEME_PATH_PATTERN.matcher(themeJson);
                if (!matcher.find()) {
                    throw new IllegalStateException(
                            "Packaged theme folders structure is incorrect, should have META-INF/resources/themes/[theme-name]/");
                }
                String themeName = matcher.group(1);
                String hash = calculateHash(content);
                packagedThemeHashes.put(themeName, hash);
            }
        }
    }

    private static Optional<String> getHashForProjectThemeJson(Options options,
            ThemeDefinition themeDefinition) throws IOException {
        if (themeDefinition != null) {
            String themeName = themeDefinition.getName();
            File projectThemeJson = new File(options.getFrontendDirectory(),
                    Constants.APPLICATION_THEME_ROOT + "/" + themeName + "/"
                            + "theme.json");
            if (projectThemeJson.exists()) {
                String content = FileUtils.readFileToString(projectThemeJson,
                        StandardCharsets.UTF_8);
                content = content.replaceAll("\\r\\n", "\n");
                return Optional.of(
                        StringUtil.getHash(content, StandardCharsets.UTF_8));
            }
        }
        return Optional.empty();
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

    /**
     * Removes Vaadin managed dependencies found under "vaadin:dependencies"
     * from "dependencies" block if the package name and version match. Needed
     * for cases when Vaadin provided dependencies are not a part of the
     * platform anymore, so these dependencies are not treated as a developer
     * provided dependencies and, thus, a new bundle generation is not triggered
     * erroneously.
     * <p>
     * Examples: polymer iron-list dependencies or Vaadin web components with a
     * deprecated "vaadin-" prefix that are not a part of platform 24.0 anymore.
     *
     * @param packageJson
     *            content of the package.json content red from a file
     */
    private static void cleanOldPlatformDependencies(JsonObject packageJson) {
        if (packageJson == null
                || !hasFrameworkDependencyObjects(packageJson)) {
            return;
        }

        JsonObject dependencies = packageJson
                .getObject(NodeUpdater.DEPENDENCIES);
        JsonObject vaadinDependencies = packageJson
                .getObject(NodeUpdater.VAADIN_DEP_KEY)
                .getObject(NodeUpdater.DEPENDENCIES);

        for (String vaadinDependency : vaadinDependencies.keys()) {
            String version = vaadinDependencies.getString(vaadinDependency);
            if (dependencies.hasKey(vaadinDependency) && version
                    .equals(dependencies.getString(vaadinDependency))) {
                dependencies.remove(vaadinDependency);
                getLogger().debug(
                        "Old Vaadin provided dependency '{}':'{}' has been removed from package.json",
                        vaadinDependency, version);
            }
        }
    }

    private static boolean hasFrameworkDependencyObjects(
            JsonObject packageJson) {
        return packageJson.hasKey(NodeUpdater.VAADIN_DEP_KEY)
                && packageJson.getObject(NodeUpdater.VAADIN_DEP_KEY)
                        .hasKey(NodeUpdater.DEPENDENCIES)
                && packageJson.hasKey(NodeUpdater.DEPENDENCIES);
    }

    private static String getTag(
            WebComponentExporterFactory<? extends Component> factory) {
        WebComponentExporterTagExtractor exporterTagExtractor = new WebComponentExporterTagExtractor();
        return exporterTagExtractor.apply(factory);
    }
}
