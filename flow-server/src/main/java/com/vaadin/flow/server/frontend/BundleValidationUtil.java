/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.internal.FileIOUtils;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.server.webcomponent.WebComponentExporterTagExtractor;
import com.vaadin.flow.server.webcomponent.WebComponentExporterUtils;

import static com.vaadin.flow.server.Constants.DEV_BUNDLE_JAR_PATH;

/**
 * Bundle handling methods.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.1
 */
public final class BundleValidationUtil {

    private static final String FRONTEND_HASHES_STATS_KEY = "frontendHashes";

    /**
     * Checks if an application needs a new frontend bundle.
     *
     * @param options
     *            Flow plugin options
     * @param frontendDependencies
     *            frontend dependencies scanner to lookup for frontend imports
     * @param mode
     *            Vaadin application mode
     * @return true if a new frontend bundle is needed, false otherwise
     */
    public static boolean needsBuild(Options options,
            FrontendDependenciesScanner frontendDependencies, Mode mode) {
        getLogger().info("Checking if a {} mode bundle build is needed", mode);
        try {
            boolean needsBuild;
            if (mode.isProduction()) {
                if (options.isForceProductionBuild() || FrontendUtils
                        .isHillaUsed(options.getFrontendDirectory(),
                                options.getClassFinder())) {
                    if (options.isForceProductionBuild()) {
                        UsageStatistics.markAsUsed("flow/prod-build-requested",
                                null);
                    }
                    getLogger().info("Frontend build requested.");
                    saveResultInFile(true, options);
                    return true;
                } else {
                    needsBuild = needsBuildProdBundle(options,
                            frontendDependencies);
                    saveResultInFile(needsBuild, options);
                }
            } else if (Mode.DEVELOPMENT_BUNDLE == mode) {
                needsBuild = needsBuildDevBundle(options, frontendDependencies);
            } else if (Mode.DEVELOPMENT_FRONTEND_LIVERELOAD == mode) {
                return false;
            } else {
                throw new IllegalArgumentException("Unexpected mode");
            }

            if (needsBuild) {
                getLogger().info("A {} mode bundle build is needed", mode);
            } else {
                getLogger().info("A {} mode bundle build is not needed", mode);
            }
            return needsBuild;
        } catch (Exception e) {
            getLogger().error(String.format(
                    "Error when checking if a %s bundle build " + "is needed",
                    mode), e);
            return true;
        }
    }

    private static boolean needsBuildDevBundle(Options options,
            FrontendDependenciesScanner frontendDependencies)
            throws IOException {
        File npmFolder = options.getNpmFolder();
        File compressedDevBundle = new File(npmFolder,
                Constants.DEV_BUNDLE_COMPRESSED_FILE_LOCATION);
        if (!DevBundleUtils
                .getDevBundleFolder(npmFolder, options.getBuildDirectoryName())
                .exists() && !compressedDevBundle.exists()
                && !hasJarBundle(DEV_BUNDLE_JAR_PATH,
                        options.getClassFinder())) {
            getLogger().info("No dev-bundle found.");
            return true;
        }

        if (!DevBundleUtils
                .getDevBundleFolder(npmFolder, options.getBuildDirectoryName())
                .exists() && compressedDevBundle.exists()) {
            DevBundleUtils.unpackBundle(npmFolder,
                    new File(
                            new File(npmFolder,
                                    options.getBuildDirectoryName()),
                            Constants.DEV_BUNDLE_LOCATION));

        }

        if (options.isSkipDevBundle()) {
            // if skip dev bundle defined and we have a dev bundle,
            // cancel all checks and trust existing bundle
            getLogger()
                    .info("Skip dev bundle requested. Using existing bundle.");
            return false;
        }

        if (!DevBundleUtils
                .getDevBundleFolder(npmFolder, options.getBuildDirectoryName())
                .exists() && !options.isReactEnabled()) {
            // Using default dev bundle in a Jar.
            // Default dev bundle is packaged with react router only. With react
            // disabled, let's rebuild bundle to get vaadin router instead.
            getLogger().info("Bundle build required for non default router.");
            return true;
        }

        String statsJsonContent = DevBundleUtils.findBundleStatsJson(npmFolder,
                options.getBuildDirectoryName());

        if (statsJsonContent == null) {
            // without stats.json in bundle we can not say if it is up-to-date
            getLogger().info(
                    "No bundle's stats.json found for dev-bundle validation.");
            return true;
        }

        return needsBuildInternal(options, frontendDependencies,
                statsJsonContent);
    }

    private static boolean needsBuildProdBundle(Options options,
            FrontendDependenciesScanner frontendDependencies)
            throws IOException {

        if (!options.isReactEnabled() && !ProdBundleUtils
                .getProdBundle(options.getNpmFolder()).exists()) {
            // Using default prod bundle in a Jar.
            // Default prod bundle is packaged with react router only. With
            // react disabled, let's rebuild bundle to get vaadin router
            // instead.
            getLogger().info("Bundle build required for non default router.");
            return true;
        }
        String statsJsonContent = ProdBundleUtils.findBundleStatsJson(
                options.getNpmFolder(), options.getClassFinder());

        if (!options.getClassFinder()
                .getAnnotatedClasses(LoadDependenciesOnStartup.class)
                .isEmpty()) {
            getLogger()
                    .info("Custom eager routes defined. Require bundle build.");
            UsageStatistics.markAsUsed(
                    "flow/rebundle-reason-bundle-custom-loading", null);
            return true;
        }

        if (statsJsonContent == null) {
            // without stats.json in bundle we can not say if it is up-to-date
            getLogger().info(
                    "No bundle's stats.json found for production-bundle validation.");
            return true;
        }

        return needsBuildInternal(options, frontendDependencies,
                statsJsonContent);
    }

    private static boolean needsBuildInternal(Options options,
            FrontendDependenciesScanner frontendDependencies,
            String statsJsonContent) throws IOException {

        JsonNode packageJson = getPackageJson(options, frontendDependencies);
        JsonNode statsJson = JacksonUtils.readTree(statsJsonContent);

        // Get scanned @NpmPackage annotations
        final Map<String, String> npmPackages = frontendDependencies
                .getPackages();

        if (!BundleValidationUtil.hashAndBundleModulesEqual(statsJson,
                packageJson, npmPackages)) {
            UsageStatistics.markAsUsed("flow/rebundle-reason-missing-package",
                    null);
            // Hash in the project doesn't match the bundle hash or NpmPackages
            // are found missing in bundle.
            return true;
        }

        // In dev mode index html is served from frontend folder, not from
        // dev-bundle, so rebuild is not required for custom content.
        if (options.isProductionMode() && BundleValidationUtil
                .hasCustomIndexHtml(options, statsJson)) {
            UsageStatistics.markAsUsed("flow/rebundle-reason-custom-index-html",
                    null);
            return true;
        }
        // index.html hash has already been checked, if needed.
        // removing it from hashes map to prevent other unnecessary checks
        ((ObjectNode) statsJson.get(FRONTEND_HASHES_STATS_KEY))
                .remove(FrontendUtils.INDEX_HTML);

        if (isCommercialBannerConditionChanged(options, statsJson)) {
            UsageStatistics.markAsUsed(
                    "flow/rebundle-reason-commercial-banner-condition-changed",
                    null);
            return true;
        }
        // commercial banner file hash has already been checked, if needed.
        // removing it from hashes map to prevent other unnecessary checks
        ((ObjectNode) statsJson.get(FRONTEND_HASHES_STATS_KEY)).remove(
                FrontendUtils.GENERATED + FrontendUtils.COMMERCIAL_BANNER_JS);

        if (!BundleValidationUtil.frontendImportsFound(statsJson, options,
                frontendDependencies)) {
            UsageStatistics.markAsUsed(
                    "flow/rebundle-reason-missing-frontend-import", null);
            return true;
        }

        if (ThemeValidationUtil.themeConfigurationChanged(options, statsJson,
                frontendDependencies)) {
            UsageStatistics.markAsUsed(
                    "flow/rebundle-reason-changed-theme-config", null);
            return true;
        }

        if (ThemeValidationUtil.themeShadowDOMStylesheetsChanged(options,
                statsJson, frontendDependencies)) {
            UsageStatistics.markAsUsed(
                    "flow/rebundle-reason-changed-shadow-DOM-stylesheets",
                    null);
            return true;
        }

        if (BundleValidationUtil.exportedWebComponents(statsJson,
                options.getClassFinder())) {
            UsageStatistics.markAsUsed(
                    "flow/rebundle-reason-added-exported-component", null);
            return true;
        }

        return false;
    }

    /**
     * Check if jar bundle exists on given path.
     *
     * @param jarPath
     *            JAR path where bunlde to check is located
     * @param finder
     *            the class finder to use for locating resources
     * @return {@code true} if bundle stats.json is found
     */
    public static boolean hasJarBundle(String jarPath, ClassFinder finder) {
        final URL resource = finder.getResource(jarPath + "config/stats.json");
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
     * @return package.json content as JsonNode
     */
    public static JsonNode getPackageJson(Options options,
            FrontendDependenciesScanner frontendDependencies) {
        File packageJsonFile = new File(options.getNpmFolder(), "package.json");

        if (packageJsonFile.exists()) {
            try {
                final ObjectNode packageJson = JacksonUtils
                        .readTree(Files.readString(packageJsonFile.toPath()));
                cleanOldPlatformDependencies(packageJson);
                return getDefaultPackageJson(options, frontendDependencies,
                        packageJson);
            } catch (IOException e) {
                getLogger().warn("Failed to read package.json", e);
            }
        } else {
            return getDefaultPackageJson(options, frontendDependencies, null);
        }
        return null;
    }

    public static JsonNode getDefaultPackageJson(Options options,
            FrontendDependenciesScanner frontendDependencies,
            ObjectNode packageJson) {
        NodeUpdater nodeUpdater = new NodeUpdater(frontendDependencies,
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

            Map<String, String> filteredApplicationDependencies = new ExclusionFilter(
                    options.getClassFinder(),
                    options.isReactEnabled()
                            && FrontendUtils.isReactModuleAvailable(options),
                    options.isNpmExcludeWebComponents())
                    .exclude(applicationDependencies);

            // Add application dependencies
            for (Map.Entry<String, String> dep : filteredApplicationDependencies
                    .entrySet()) {
                nodeUpdater.addDependency(packageJson, NodeUpdater.DEPENDENCIES,
                        dep.getKey(), dep.getValue());
            }

            final String hash = TaskUpdatePackages
                    .generatePackageJsonHash(packageJson);
            ((ObjectNode) packageJson.get(NodeUpdater.VAADIN_DEP_KEY))
                    .put(NodeUpdater.HASH_KEY, hash);

            final JsonNode platformPinnedDependencies = nodeUpdater
                    .getPlatformPinnedDependencies();
            for (String key : JacksonUtils
                    .getKeys(platformPinnedDependencies)) {
                // need to double check that not overriding a scanned
                // dependency since add-ons should be able to downgrade
                // version through exclusion
                if (!filteredApplicationDependencies.containsKey(key)) {
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
    private static void cleanOldPlatformDependencies(JsonNode packageJson) {
        if (packageJson == null
                || !hasFrameworkDependencyObjects(packageJson)) {
            return;
        }

        ObjectNode dependencies = (ObjectNode) packageJson
                .get(NodeUpdater.DEPENDENCIES);
        JsonNode vaadinDependencies = packageJson
                .get(NodeUpdater.VAADIN_DEP_KEY).get(NodeUpdater.DEPENDENCIES);

        for (String vaadinDependency : JacksonUtils
                .getKeys(vaadinDependencies)) {
            String version = vaadinDependencies.get(vaadinDependency)
                    .textValue();
            if (dependencies.has(vaadinDependency) && version
                    .equals(dependencies.get(vaadinDependency).textValue())) {
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
    public static boolean hashAndBundleModulesEqual(JsonNode statsJson,
            JsonNode packageJson, Map<String, String> npmPackages) {

        String packageJsonHash = BundleValidationUtil
                .getPackageJsonHash(packageJson);
        String bundlePackageJsonHash = BundleValidationUtil
                .getStatsHash(statsJson);

        if (packageJsonHash == null || packageJsonHash.isEmpty()) {
            getLogger().error(
                    "No hash found for 'package.json' even though one should always be generated!");
            return false;
        }

        JsonNode bundleModules = statsJson.get("packageJsonDependencies");

        if (bundleModules == null) {
            getLogger().error(
                    "Bundle did not contain package json dependencies to validate.\n"
                            + "Rebuild of bundle needed.");
            return false;
        }

        // Check that bundle modules contains all package dependencies
        if (packageJsonHash.equals(bundlePackageJsonHash)) {
            if (!dependenciesContainsAllPackages(npmPackages, bundleModules)) {
                return false;
            }
        }

        JsonNode dependencies = packageJson.get("dependencies");

        List<String> dependenciesList = JacksonUtils.getKeys(dependencies)
                .stream()
                // skip checking flow-frontend as it was used in previous
                // versions as an alias for ./target/flow-frontend
                .filter(pkg -> !"@vaadin/flow-frontend".equals(pkg))
                .collect(Collectors.toList());

        List<String> missingFromBundle = dependenciesList.stream()
                .filter(pkg -> !bundleModules.has(pkg))
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
                .filter(pkg -> !versionAccepted(
                        dependencies.get(pkg).textValue(),
                        bundleModules.get(pkg).textValue()))
                .collect(Collectors.toList());

        if (!missingFromBundle.isEmpty()) {
            for (String pkg : missingFromBundle) {
                getLogger().info(
                        "Dependency {}:{} has the wrong version {} in the bundle",
                        pkg, dependencies.get(pkg).textValue(),
                        bundleModules.get(pkg).textValue());
            }
            return false;
        }

        return true;
    }

    private static boolean versionAccepted(String expected, String actual) {
        FrontendVersion expectedVersion;
        try {
            expectedVersion = new FrontendVersion(expected);
        } catch (NumberFormatException ex) {
            expectedVersion = null;
        }
        FrontendVersion actualVersion;
        try {
            actualVersion = new FrontendVersion(actual);
        } catch (NumberFormatException ex) {
            actualVersion = null;
        }

        if (expectedVersion == null && actualVersion == null) {
            return Objects.equals(expected, actual);
        } else if (expectedVersion == null || actualVersion == null) {
            // expected or actual version is referencing a local package
            // while the other one is a parsable version
            getLogger().debug(
                    "Version '{}' cannot be parsed and compared to '{}'",
                    expectedVersion == null ? expected : actual,
                    expectedVersion == null ? actual : expected);
            return false;
        }

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
            Map<String, String> npmPackages, JsonNode dependencies) {
        final List<String> collect = npmPackages.keySet().stream()
                .filter(pkg -> !(dependencies.has(pkg)
                        && versionAccepted(dependencies.get(pkg).textValue(),
                                npmPackages.get(pkg))))
                .collect(Collectors.toList());
        if (!collect.isEmpty()) {
            collect.forEach(dependency -> getLogger().info("Dependency "
                    + dependency + " is missing from the bundle"));
            return false;
        }
        return true;
    }

    public static boolean exportedWebComponents(JsonNode statsJson,
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

            ArrayNode webComponentsInStats = (ArrayNode) statsJson
                    .get("webComponents");

            if (webComponentsInStats == null) {
                if (!webComponents.isEmpty()) {
                    getLogger().info(
                            "Found embedded web components not yet included "
                                    + "into the bundle: {}",
                            String.join(", ", webComponents));
                    return true;
                }
                return false;
            } else {
                for (int index = 0; index < webComponentsInStats
                        .size(); index++) {
                    String webComponentInStats = webComponentsInStats.get(index)
                            .textValue();
                    webComponents.remove(webComponentInStats);
                }
            }

            if (!webComponents.isEmpty()) {
                getLogger().info(
                        "Found newly added embedded web components not "
                                + "yet included into the bundle: {}",
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

    public static boolean frontendImportsFound(JsonNode statsJson,
            Options options, FrontendDependenciesScanner frontendDependencies)
            throws IOException {

        // Validate frontend requirements in flow-generated-imports.js
        final GenerateMainImports generateMainImports = new GenerateMainImports(
                frontendDependencies, options, statsJson);
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
        LinkedHashSet<String> uniqueImports = new LinkedHashSet<>(imports);
        ArrayNode statsBundle = statsJson.has("bundleImports")
                ? (ArrayNode) statsJson.get("bundleImports")
                : JacksonUtils.createArrayNode();
        final List<String> missingFromBundle = uniqueImports.stream().filter(
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
        final List<String> jarImports = uniqueImports.stream()
                .filter(importString -> importString.contains(resourcePath))
                .map(importString -> importString
                        .substring(importString.indexOf(resourcePath)
                                + resourcePath.length()))
                .collect(Collectors.toList());

        final List<String> projectImports = uniqueImports.stream()
                .filter(importString -> importString
                        .startsWith(FrontendUtils.FRONTEND_FOLDER_ALIAS)
                        && !importString.contains(resourcePath))
                .map(importString -> importString.substring(
                        FrontendUtils.FRONTEND_FOLDER_ALIAS.length()))
                .collect(Collectors.toList());

        final JsonNode frontendHashes = statsJson
                .get(FRONTEND_HASHES_STATS_KEY);
        List<String> faultyContent = new ArrayList<>();

        for (String jarImport : jarImports) {
            final String jarResourceString = FrontendUtils
                    .getJarResourceString(jarImport, options.getClassFinder());
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

            String frontendFileContent = Files
                    .readString(frontendFile.toPath());
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

    private static boolean hasCustomIndexHtml(Options options,
            JsonNode statsJson) throws IOException {
        File indexHtml = new File(options.getFrontendDirectory(),
                FrontendUtils.INDEX_HTML);
        if (indexHtml.exists()) {
            final JsonNode frontendHashes = statsJson
                    .get(FRONTEND_HASHES_STATS_KEY);
            String frontendFileContent = Files.readString(indexHtml.toPath());
            List<String> faultyContent = new ArrayList<>();
            String frontendFileContentHash = compareFrontendHashes(
                    frontendHashes, faultyContent, FrontendUtils.INDEX_HTML,
                    frontendFileContent);
            if (!faultyContent.isEmpty()) {
                logChangedFiles(faultyContent,
                        "Detected changed content for frontend files:");
                logOldIndexHtmlWarning(frontendFileContentHash);
                return true;
            }
        }
        return false;
    }

    private static void logOldIndexHtmlWarning(String frontendFileContentHash) {
        // Detecting and warning about old default index.html content based on
        // hash calculated for older index.html contents (23.x,24.x).
        if (List.of(
                "49a6fa3fd70a6c36f32cd5389611b54611413fd6f8c430745bd3e0dd8c5a86c9",
                "9134a82f3ebcc72d303b78b843ba17b973fb5a7f5cfcd8868566a4d234cc7782",
                "c939e4dd2e34a02be02d8682215130119a8666ee3ed2f8f78de527464bffcfaf",
                "fa38cdf6d106b713195d7c56537443f8fa282607e4636a8e6c1da56f675135b1",
                "59ab33ffe4cdd1aa96ee4e03da8d99248ca89b9ea70d84cf2016787f29687472")
                .contains(frontendFileContentHash)) {
            getLogger().warn(
                    "index.html matches the old Vaadin default. Update it to the latest by removing old and rebuild.");
        }
    }

    private static boolean indexFileAddedOrDeleted(Options options,
            JsonNode frontendHashes) {
        Collection<String> indexFiles = Arrays.asList(FrontendUtils.INDEX_TS,
                FrontendUtils.INDEX_JS, FrontendUtils.INDEX_TSX);
        for (String indexFile : indexFiles) {
            File file = new File(options.getFrontendDirectory(), indexFile);
            if (file.exists() && !frontendHashes.has(indexFile)) {
                getLogger().info("Detected added {} file", indexFile);
                return true;
            } else if (!file.exists() && frontendHashes.has(indexFile)) {
                getLogger().info("Detected deleted {} file", indexFile);
                return true;
            }
        }
        return false;
    }

    private static boolean isCommercialBannerConditionChanged(Options options,
            JsonNode statsJson) throws IOException {
        final JsonNode frontendHashes = statsJson
                .get(FRONTEND_HASHES_STATS_KEY);
        boolean commercialBannerRequested = options.isCommercialBannerEnabled();
        String commercialBannerPath = FrontendUtils.GENERATED
                + FrontendUtils.COMMERCIAL_BANNER_JS;
        boolean hasCommercialBannerHash = frontendHashes
                .has(commercialBannerPath);
        if (!commercialBannerRequested && hasCommercialBannerHash) {
            getLogger().info(
                    "Detected commercial banner file but commercial banner is not enabled");
            return true;
        }
        if (!options.isProductionMode() && hasCommercialBannerHash) {
            getLogger().info(
                    "Detected commercial banner file but commercial banner is not applied in development bundle");
            return true;
        }
        if (commercialBannerRequested && options.isProductionMode()) {
            if (!hasCommercialBannerHash) {
                getLogger().info("Detected missing commercial banner file");
                return true;
            }

            List<String> faultyContent = new ArrayList<>();
            compareFrontendHashes(frontendHashes, faultyContent,
                    commercialBannerPath,
                    new TaskGenerateCommercialBanner(options).getFileContent());
            if (!faultyContent.isEmpty()) {
                getLogger().info(
                        "Detected changed content for commercial banner file");
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> getRemainingImports(
            List<String> jarImports, List<String> projectImports,
            JsonNode frontendHashes) {
        Map<String, String> remainingImportEntries = new HashMap<>();
        List<String> remainingKeys = JacksonUtils.getKeys(frontendHashes);

        remainingKeys.removeAll(jarImports);
        remainingKeys.removeAll(projectImports);

        if (!remainingKeys.isEmpty()) {
            for (String key : remainingKeys) {
                remainingImportEntries.put(key,
                        frontendHashes.get(key).textValue());
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
                            Files.readString(frontendFile.toPath()));
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

    private static String compareFrontendHashes(JsonNode frontendHashes,
            List<String> faultyContent, String frontendFilePath,
            String frontendFileContent) {
        final String contentHash = calculateHash(frontendFileContent);
        if (frontendHashes.has(frontendFilePath) && !frontendHashes
                .get(frontendFilePath).textValue().equals(contentHash)) {
            faultyContent.add(frontendFilePath);
        } else if (!frontendHashes.has(frontendFilePath)) {
            getLogger().info("No hash info for '{}'", frontendFilePath);
            faultyContent.add(frontendFilePath);
        }
        return contentHash;
    }

    public static String calculateHash(String fileContent) {
        String content = fileContent.replaceAll("\\r\\n", "\n");
        return StringUtil.getHash(content, StandardCharsets.UTF_8);
    }

    private static boolean arrayContainsString(ArrayNode array, String string) {
        string = string.replace("Frontend/", "./");
        for (int i = 0; i < array.size(); i++) {
            if (string.equals(
                    array.get(i).textValue().replace("Frontend/", "./"))) {
                return true;
            }
        }
        return false;
    }

    public static String getStatsHash(JsonNode statsJson) {
        if (statsJson.has("packageJsonHash")) {
            return statsJson.get("packageJsonHash").textValue();
        }

        return null;
    }

    public static String getPackageJsonHash(JsonNode packageJson) {
        if (packageJson != null && packageJson.has("vaadin")
                && packageJson.get("vaadin").has("hash")) {
            return packageJson.get("vaadin").get("hash").textValue();
        }

        return null;
    }

    private static boolean hasFrameworkDependencyObjects(JsonNode packageJson) {
        return packageJson.has(NodeUpdater.VAADIN_DEP_KEY)
                && packageJson.get(NodeUpdater.VAADIN_DEP_KEY)
                        .has(NodeUpdater.DEPENDENCIES)
                && packageJson.has(NodeUpdater.DEPENDENCIES);
    }

    public static void logChangedFiles(List<String> frontendFiles,
            String message) {
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

    public static URL getProdBundleResource(String filename,
            ClassFinder finder) {
        return finder.getResource(Constants.PROD_BUNDLE_JAR_PATH + filename);
    }

    /**
     * Checks if a new production bundle is needed by restoring re-bundle
     * checker result flag from a temporal file.
     *
     * @param resourceOutputFolder
     *            output directory for generated non-served resources
     * @return true if a new bundle is needed, false otherwise
     */
    public static boolean needsBundleBuild(File resourceOutputFolder) {
        final File needsBuildFile = new File(resourceOutputFolder,
                Constants.NEEDS_BUNDLE_BUILD_FILE);
        if (!needsBuildFile.exists()) {
            getLogger().error("Require bundle build due to missing '{}' file.",
                    Constants.NEEDS_BUNDLE_BUILD_FILE);
            return true;
        }
        try {
            String content = Files.readString(needsBuildFile.toPath());
            return Boolean.parseBoolean(content);
        } catch (IOException e) {
            getLogger().error(
                    "Failed to read re-bundle checker result from file", e);
            return true;
        } finally {
            FileIOUtils.deleteFileQuietly(needsBuildFile);
        }
    }

    private static void saveResultInFile(boolean needsBundle, Options options)
            throws IOException {
        File needsBuildFile = new File(options.getResourceOutputDirectory(),
                Constants.NEEDS_BUNDLE_BUILD_FILE);
        File targetDir = needsBuildFile.getParentFile();
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        Files.writeString(needsBuildFile.toPath(),
                Boolean.toString(needsBundle));
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BundleValidationUtil.class);
    }
}
