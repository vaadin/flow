package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.server.webcomponent.WebComponentExporterTagExtractor;
import com.vaadin.flow.server.webcomponent.WebComponentExporterUtils;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * Bundle handling methods.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.1
 */
public class BundleValidationUtil {

    /**
     * Check if jar bundle exists on given path.
     *
     * @param jarPath
     *            JAR path where bunlde to check is located
     * @return {@code true} if bundle stats.json is found
     */
    static boolean hasJarBundle(String jarPath) {
        final URL resource = TaskRunDevBundleBuild.class.getClassLoader()
                .getResource(jarPath + "config/stats.json");
        return resource != null;
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
    static JsonObject getPackageJson(Options options,
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

    static JsonObject getDefaultPackageJson(Options options,
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
    static boolean hashAndBundleModulesEqual(JsonObject statsJson,
            JsonObject packageJson, Map<String, String> npmPackages) {

        String packageJsonHash = BundleValidationUtil
                .getPackageJsonHash(packageJson);
        String bundlePackageJsonHash = BundleValidationUtil
                .getStatsHash(statsJson);

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

        List<String> dependenciesList = Arrays.stream(dependencies.keys())
                // skip checking flow-frontend as it was used in previous
                // versions as an alias for ./target/flow-frontend
                .filter(pkg -> !"@vaadin/flow-frontend".equals(pkg))
                .collect(Collectors.toList());

        List<String> missingFromBundle = dependenciesList.stream()
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
        missingFromBundle = dependenciesList.stream()
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

    static boolean exportedWebComponents(JsonObject statsJson,
            ClassFinder finder) {
        try {
            Set<Class<?>> exporterRelatedClasses = new HashSet<>();
            finder.getSubTypesOf(WebComponentExporter.class.getName())
                    .forEach(exporterRelatedClasses::add);
            finder.getSubTypesOf(WebComponentExporterFactory.class.getName())
                    .forEach(exporterRelatedClasses::add);

            Set<String> webComponents = WebComponentExporterUtils
                    .getFactories(exporterRelatedClasses).stream()
                    .map(BundleValidationUtil::getTag)
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

    private static String getTag(
            WebComponentExporterFactory<? extends Component> factory) {
        WebComponentExporterTagExtractor exporterTagExtractor = new WebComponentExporterTagExtractor();
        return exporterTagExtractor.apply(factory);
    }

    static boolean frontendImportsFound(JsonObject statsJson, Options options,
            ClassFinder finder,
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
                    "Detected changed content for frontend files:");
            return false;
        }

        if (indexFileAddedOrDeleted(options, frontendHashes)) {
            return false;
        }

        Map<String, String> remainingImports = getRemainingImports(jarImports,
                projectImports, frontendHashes);

        if (importedFrontendFilesChanged(options.getFrontendDirectory(),
                remainingImports)) {
            return false;
        }

        return true;
    }

    private static boolean indexFileAddedOrDeleted(Options options,
            JsonObject frontendHashes) {
        Collection<String> indexFiles = Arrays.asList(FrontendUtils.INDEX_TS,
                FrontendUtils.INDEX_JS, FrontendUtils.INDEX_TSX);
        for (String indexFile : indexFiles) {
            File file = new File(options.getFrontendDirectory(), indexFile);
            if (file.exists() && !frontendHashes.hasKey(indexFile)) {
                getLogger().info("Detected added {} file", indexFile);
                return true;
            } else if (!file.exists() && frontendHashes.hasKey(indexFile)) {
                getLogger().info("Detected deleted {} file", indexFile);
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> getRemainingImports(
            List<String> jarImports, List<String> projectImports,
            JsonObject frontendHashes) {
        Map<String, String> remainingImportEntries = new HashMap<>();
        List<String> remainingKeys = new ArrayList<>(
                Arrays.asList(frontendHashes.keys()));

        remainingKeys.removeAll(jarImports);
        remainingKeys.removeAll(projectImports);

        if (!remainingKeys.isEmpty()) {
            for (String key : remainingKeys) {
                remainingImportEntries.put(key, frontendHashes.getString(key));
            }
            return remainingImportEntries;
        }

        return Collections.emptyMap();
    }

    /**
     * Checks for possible changes in TS/JS files in frontend folder which are
     * referenced from other TS/JS files with 'import {foo} from 'Frontend/bar'.
     *
     * @param frontendDirectory
     *            folder with frontend resources in the project
     * @param remainingImports
     *            frontend resource imports listed in the bundle, but not found
     *            with class scanner.
     * @return true if changes detected, false otherwise
     * @throws IOException
     *             if exception is thrown while reading files content
     */
    private static boolean importedFrontendFilesChanged(File frontendDirectory,
            Map<String, String> remainingImports) throws IOException {
        if (!remainingImports.isEmpty()) {
            List<String> changed = new ArrayList<>();
            for (Map.Entry<String, String> importEntry : remainingImports
                    .entrySet()) {
                String filePath = importEntry.getKey();
                String expectedHash = importEntry.getValue();
                File frontendFile = new File(frontendDirectory, filePath);
                if (frontendFile.exists()) {
                    final String hash = calculateHash(
                            FileUtils.readFileToString(frontendFile,
                                    StandardCharsets.UTF_8));
                    if (!expectedHash.equals(hash)) {
                        changed.add(filePath);
                    }
                }
            }
            if (!changed.isEmpty()) {
                logChangedFiles(changed, "Detected changed frontend files:");
                return true;
            }
        }
        return false;
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

    static String calculateHash(String fileContent) {
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

    static String getStatsHash(JsonObject statsJson) {
        if (statsJson.hasKey("packageJsonHash")) {
            return statsJson.getString("packageJsonHash");
        }

        return null;
    }

    static String getPackageJsonHash(JsonObject packageJson) {
        if (packageJson != null && packageJson.hasKey("vaadin")
                && packageJson.getObject("vaadin").hasKey("hash")) {
            return packageJson.getObject("vaadin").getString("hash");
        }

        return null;
    }

    private static boolean hasFrameworkDependencyObjects(
            JsonObject packageJson) {
        return packageJson.hasKey(NodeUpdater.VAADIN_DEP_KEY)
                && packageJson.getObject(NodeUpdater.VAADIN_DEP_KEY)
                        .hasKey(NodeUpdater.DEPENDENCIES)
                && packageJson.hasKey(NodeUpdater.DEPENDENCIES);
    }

    static void logChangedFiles(List<String> frontendFiles, String message) {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException(
                    "Changed files message cannot be empty");
        }
        if (message.contains("{}")) {
            throw new IllegalArgumentException(
                    "Changed files message shouldn't include '{}' placeholder");
        }
        message += "\n{}";
        StringBuilder handledFiles = new StringBuilder();
        for (String file : frontendFiles) {
            handledFiles.append(" - ").append(file).append("\n");
        }
        getLogger().info(message, handledFiles);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BundleValidationUtil.class);
    }
}
