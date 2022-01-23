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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static elemental.json.impl.JsonUtil.stringify;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Base abstract class for frontend updaters that needs to be run when in
 * dev-mode or from the flow maven plugin.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public abstract class NodeUpdater implements FallibleCommand {

    private static final String VAADIN_FORM_PKG_LEGACY_VERSION = "flow-frontend/form";

    private static final String VAADIN_FORM_PKG = "@vaadin/form";

    /**
     * Relative paths of generated should be prefixed with this value, so they
     * can be correctly separated from {projectDir}/frontend files.
     */
    public static final String GENERATED_PREFIX = "GENERATED/";

    static final String DEPENDENCIES = "dependencies";
    static final String VAADIN_DEP_KEY = "vaadin";
    static final String HASH_KEY = "hash";
    static final String DEV_DEPENDENCIES = "devDependencies";
    static final String OVERRIDES = "overrides";

    private static final String DEP_LICENSE_KEY = "license";
    private static final String DEP_LICENSE_DEFAULT = "UNLICENSED";
    private static final String DEP_NAME_KEY = "name";
    private static final String DEP_NAME_DEFAULT = "no-name";
    private static final String DEP_MAIN_KEY = "main";
    protected static final String DEP_NAME_FLOW_DEPS = "@vaadin/flow-deps";
    protected static final String DEP_NAME_FLOW_JARS = "@vaadin/flow-frontend";
    private static final String DEP_MAIN_VALUE = "index";
    private static final String DEP_VERSION_KEY = "version";
    private static final String DEP_VERSION_DEFAULT = "1.0.0";
    private static final String ROUTER_VERSION = "1.7.4";
    protected static final String POLYMER_VERSION = "3.2.0";

    /**
     * Base directory for {@link Constants#PACKAGE_JSON},
     * {@link FrontendUtils#WEBPACK_CONFIG}, {@link FrontendUtils#NODE_MODULES}.
     */
    protected final File npmFolder;

    /**
     * The path to the {@link FrontendUtils#NODE_MODULES} directory.
     */
    protected final File nodeModulesFolder;

    /**
     * Base directory for flow generated files.
     */
    protected final File generatedFolder;

    /**
     * Base directory for flow dependencies coming from jars.
     */
    protected final File flowResourcesFolder;

    /**
     * The {@link FrontendDependencies} object representing the application
     * dependencies.
     */
    protected final FrontendDependenciesScanner frontDeps;

    protected String buildDir;

    final ClassFinder finder;

    boolean modified;

    FeatureFlags featureFlags;

    /**
     * path to the versions.json file
     */
    String versionsPath;

    /**
     * Constructor.
     *
     * @param finder
     *            a reusable class finder
     * @param frontendDependencies
     *            a reusable frontend dependencies
     * @param npmFolder
     *            folder with the `package.json` file
     * @param generatedPath
     *            folder where flow generated files will be placed.
     * @param flowResourcesPath
     *            folder where flow dependencies will be copied to.
     * @param buildDir
     *            the used build directory
     * @param featureFlags
     *            FeatureFlags for this build
     */
    protected NodeUpdater(ClassFinder finder,
            FrontendDependenciesScanner frontendDependencies, File npmFolder,
            File generatedPath, File flowResourcesPath, String buildDir,
            FeatureFlags featureFlags) {
        this.frontDeps = frontendDependencies;
        this.finder = finder;
        this.npmFolder = npmFolder;
        this.nodeModulesFolder = new File(npmFolder, NODE_MODULES);
        this.generatedFolder = generatedPath;
        this.flowResourcesFolder = flowResourcesPath;
        this.buildDir = buildDir;
        this.featureFlags = featureFlags;
    }

    private File getPackageJsonFile() {
        return new File(npmFolder, PACKAGE_JSON);
    }

    /**
     * Gets the platform pinned versions that are not overridden by the user in
     * package.json.
     *
     * @return json object with the dependencies or {@code null}
     * @throws IOException
     *             when versions file could not be read
     */
    JsonObject getPlatformPinnedDependencies() throws IOException {
        URL resource = finder.getResource(Constants.VAADIN_VERSIONS_JSON);
        if (resource == null) {
            log().info("Couldn't find {} file to pin dependency versions."
                    + " Transitive dependencies won't be pinned for pnpm.",
                    Constants.VAADIN_VERSIONS_JSON);
        }

        JsonObject versionsJson = null;
        try (InputStream content = resource == null ? null
                : resource.openStream()) {

            if (content != null) {
                VersionsJsonConverter convert = new VersionsJsonConverter(
                        Json.parse(IOUtils.toString(content,
                                StandardCharsets.UTF_8)));
                versionsJson = convert.getConvertedJson();
                versionsJson = new VersionsJsonFilter(getPackageJson(),
                        DEPENDENCIES).getFilteredVersions(versionsJson);
            }
        }
        return versionsJson;
    }

    static Set<String> getGeneratedModules(File directory,
            Set<String> excludes) {
        if (!directory.exists()) {
            return Collections.emptySet();
        }

        final Function<String, String> unixPath = str -> str.replace("\\", "/");

        final URI baseDir = directory.toURI();

        return FileUtils.listFiles(directory, new String[] { "js" }, true)
                .stream().filter(file -> {
                    String path = unixPath.apply(file.getPath());
                    if (path.contains("/node_modules/")) {
                        return false;
                    }
                    return excludes.stream().noneMatch(
                            postfix -> path.endsWith(unixPath.apply(postfix)));
                })
                .map(file -> GENERATED_PREFIX + unixPath
                        .apply(baseDir.relativize(file.toURI()).getPath()))
                .collect(Collectors.toSet());
    }

    String resolveResource(String importPath) {
        String resolved = importPath;
        if (!importPath.startsWith("@")) {

            // We only should check here those paths starting with './' when all
            // flow components
            // have the './' prefix
            String resource = resolved.replaceFirst("^\\./+", "");
            if (hasMetaInfResource(resource)) {
                if (!resolved.startsWith("./")) {
                    log().warn(
                            "Use the './' prefix for files in JAR files: '{}', please update your component.",
                            importPath);
                }
                resolved = FLOW_NPM_PACKAGE_NAME + resource;
            }
        }
        return resolved;
    }

    private boolean hasMetaInfResource(String resource) {
        return finder.getResource(
                RESOURCES_FRONTEND_DEFAULT + "/" + resource) != null
                || finder.getResource(COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT
                        + "/" + resource) != null;
    }

    JsonObject getPackageJson() throws IOException {
        JsonObject packageJson = getJsonFileContent(getPackageJsonFile());
        if (packageJson == null) {
            packageJson = Json.createObject();
            packageJson.put(DEP_NAME_KEY, DEP_NAME_DEFAULT);
            packageJson.put(DEP_LICENSE_KEY, DEP_LICENSE_DEFAULT);
        }

        addDefaultObjects(packageJson);
        addVaadinDefaultsToJson(packageJson);
        addWebpackPlugins(packageJson);

        return packageJson;
    }

    private void addDefaultObjects(JsonObject json) {
        computeIfAbsent(json, DEPENDENCIES, Json::createObject);
        computeIfAbsent(json, DEV_DEPENDENCIES, Json::createObject);
    }

    private void addWebpackPlugins(JsonObject packageJson) {
        final List<String> plugins = WebpackPluginsUtil.getPlugins();

        Path targetFolder = Paths.get(npmFolder.toString(), buildDir,
                WebpackPluginsUtil.PLUGIN_TARGET);

        JsonObject devDependencies;
        if (packageJson.hasKey(DEV_DEPENDENCIES)) {
            devDependencies = packageJson.getObject(DEV_DEPENDENCIES);
        } else {
            devDependencies = Json.createObject();
            packageJson.put(DEV_DEPENDENCIES, devDependencies);
        }

        String atVaadinPrefix = "@vaadin/";
        String pluginTargetPrefix = "./"
                + (npmFolder.toPath().relativize(targetFolder) + "/")
                        .replace('\\', '/');
        plugins.stream().filter(plugin -> targetFolder.toFile().exists())
                .forEach(plugin -> {
                    String pluginTarget = pluginTargetPrefix + plugin;
                    devDependencies.put(atVaadinPrefix + plugin, pluginTarget);
                });

        // Remove plugins previously installed but no longer needed
        for (String depKey : devDependencies.keys()) {
            String depVersion = devDependencies.getString(depKey);
            if (depKey.startsWith(atVaadinPrefix)
                    && depVersion.startsWith(pluginTargetPrefix)) {
                final String pluginName = depKey
                        .substring(atVaadinPrefix.length());
                if (!plugins.contains(pluginName)) {
                    devDependencies.remove(depKey);
                }
            }
        }
    }

    JsonObject getResourcesPackageJson() throws IOException {
        JsonObject packageJson = getJsonFileContent(
                new File(flowResourcesFolder, PACKAGE_JSON));
        if (packageJson == null) {
            packageJson = Json.createObject();
            packageJson.put(DEP_NAME_KEY, DEP_NAME_FLOW_JARS);
            packageJson.put(DEP_LICENSE_KEY, DEP_LICENSE_DEFAULT);
            packageJson.put(DEP_MAIN_KEY, DEP_MAIN_VALUE);
            packageJson.put(DEP_VERSION_KEY, DEP_VERSION_DEFAULT);
        }
        return packageJson;
    }

    static JsonObject getJsonFileContent(File packageFile) throws IOException {
        JsonObject jsonContent = null;
        if (packageFile.exists()) {
            String fileContent = FileUtils.readFileToString(packageFile,
                    UTF_8.name());
            try {
                jsonContent = Json.parse(fileContent);
            } catch (JsonException e) { // NOSONAR
                throw new JsonException(String
                        .format("Cannot parse package file '%s'", packageFile));
            }
        }
        return jsonContent;
    }

    void addVaadinDefaultsToJson(JsonObject json) {
        JsonObject vaadinPackages = computeIfAbsent(json, VAADIN_DEP_KEY,
                Json::createObject);

        computeIfAbsent(vaadinPackages, DEPENDENCIES, () -> {
            final JsonObject dependencies = Json.createObject();
            getDefaultDependencies().forEach(dependencies::put);
            return dependencies;
        });
        computeIfAbsent(vaadinPackages, DEV_DEPENDENCIES, () -> {
            final JsonObject devDependencies = Json.createObject();
            getDefaultDevDependencies().forEach(devDependencies::put);
            return devDependencies;
        });
        computeIfAbsent(vaadinPackages, HASH_KEY, () -> Json.create(""));
    }

    private static <T extends JsonValue> T computeIfAbsent(
            JsonObject jsonObject, String key, Supplier<T> valueSupplier) {
        T result = jsonObject.get(key);
        if (result == null) {
            result = valueSupplier.get();
            jsonObject.put(key, result);
        }
        return result;
    }

    Map<String, String> getDefaultDependencies() {
        Map<String, String> defaults = new HashMap<>();

        defaults.put("@vaadin/router", ROUTER_VERSION);

        defaults.put("@polymer/polymer", POLYMER_VERSION);

        defaults.put("lit", "2.0.0");

        // Constructable style sheets is only implemented for chrome,
        // polyfill needed for FireFox et.al. at the moment
        defaults.put("construct-style-sheets-polyfill", "3.0.4");

        defaults.put("@vaadin/common-frontend", "0.0.17");

        return defaults;
    }

    Map<String, String> getDefaultDevDependencies() {
        Map<String, String> defaults = new HashMap<>();

        defaults.put("typescript", "4.5.3");
        defaults.put("@babel/core", "7.16.7");

        final String WORKBOX_VERSION = "6.4.2";

        if (featureFlags.isEnabled(FeatureFlags.VITE)) {
            defaults.put("vite", "v2.7.4");
            defaults.put("rollup-plugin-brotli", "3.1.0");
            defaults.put("vite-plugin-checker", "0.3.4");
            defaults.put("mkdirp", "1.0.4"); // for application-theme-plugin
            defaults.put("workbox-build", "6.4.1");
        } else {
            // Webpack plugins and helpers
            defaults.put("esbuild-loader", "2.15.1");
            defaults.put("html-webpack-plugin", "4.5.1");
            defaults.put("fork-ts-checker-webpack-plugin", "6.2.1");
            defaults.put("webpack", "4.46.0");
            defaults.put("webpack-cli", "4.9.0");
            defaults.put("webpack-dev-server", "4.1.1");
            defaults.put("compression-webpack-plugin", "4.0.1");
            defaults.put("extra-watch-webpack-plugin", "1.0.3");
            defaults.put("webpack-merge", "4.2.2");
            defaults.put("css-loader", "4.2.1");
            defaults.put("extract-loader", "5.1.0");
            defaults.put("lit-css-loader", "0.1.0");
            defaults.put("file-loader", "6.2.0");
            defaults.put("loader-utils", "2.0.0");
            defaults.put("workbox-webpack-plugin", WORKBOX_VERSION);

            // Forcing chokidar version for now until new babel version is
            // available
            // check out https://github.com/babel/babel/issues/11488
            defaults.put("chokidar", "^3.5.0");
        }
        defaults.put("workbox-core", WORKBOX_VERSION);
        defaults.put("workbox-precaching", WORKBOX_VERSION);
        defaults.put("glob", "7.1.6");

        return defaults;
    }

    /**
     * Updates default dependencies and development dependencies to
     * package.json.
     *
     * @param packageJson
     *            package.json json object to update with dependencies
     * @return true if items were added or removed from the {@code packageJson}
     */
    boolean updateDefaultDependencies(JsonObject packageJson) {
        int added = 0;

        for (Map.Entry<String, String> entry : getDefaultDependencies()
                .entrySet()) {
            added += addDependency(packageJson, DEPENDENCIES, entry.getKey(),
                    entry.getValue());
        }

        for (Map.Entry<String, String> entry : getDefaultDevDependencies()
                .entrySet()) {
            added += addDependency(packageJson, DEV_DEPENDENCIES,
                    entry.getKey(), entry.getValue());
        }

        if (added > 0) {
            log().info("Added {} default dependencies to main package.json",
                    added);
        }
        return added > 0;
    }

    int addDependency(JsonObject json, String key, String pkg, String version) {
        Objects.requireNonNull(json, "Json object need to be given");
        Objects.requireNonNull(key, "Json sub object needs to be give.");
        Objects.requireNonNull(pkg, "dependency package needs to be defined");

        JsonObject vaadinDeps = json.getObject(VAADIN_DEP_KEY);
        if (!json.hasKey(key)) {
            json.put(key, Json.createObject());
        }
        json = json.get(key);
        vaadinDeps = vaadinDeps.getObject(key);

        if (vaadinDeps.hasKey(pkg)) {
            if (version == null) {
                version = vaadinDeps.getString(pkg);
            }
            return handleExistingVaadinDep(json, pkg, version, vaadinDeps);
        } else {
            vaadinDeps.put(pkg, version);
            if (!json.hasKey(pkg) || isNewerVersion(json, pkg, version)) {
                json.put(pkg, version);
                log().debug("Added \"{}\": \"{}\" line.", pkg, version);
                return 1;
            }
        }
        return 0;
    }

    private boolean isNewerVersion(JsonObject json, String pkg,
            String version) {
        try {
            FrontendVersion newVersion = new FrontendVersion(version);
            FrontendVersion existingVersion = toVersion(json, pkg);
            return newVersion.isNewerThan(existingVersion);
        } catch (NumberFormatException e) {
            if (VAADIN_FORM_PKG.equals(pkg) && json.getString(pkg)
                    .contains(VAADIN_FORM_PKG_LEGACY_VERSION)) {
                return true;
            } else {
                throw e;
            }
        }
    }

    private int handleExistingVaadinDep(JsonObject json, String pkg,
            String version, JsonObject vaadinDeps) {
        boolean added = false;
        FrontendVersion vaadinVersion = toVersion(vaadinDeps, pkg);
        if (json.hasKey(pkg)) {
            try {
                FrontendVersion packageVersion = toVersion(json, pkg);
                FrontendVersion newVersion = new FrontendVersion(version);
                // Vaadin and package.json versions are the same, but dependency
                // updates (can be up or down)
                if (vaadinVersion.isEqualTo(packageVersion)
                        && !vaadinVersion.isEqualTo(newVersion)) {
                    json.put(pkg, version);
                    added = true;
                    // if vaadin and package not the same, but new version is
                    // newer
                    // update package version.
                } else if (newVersion.isNewerThan(packageVersion)) {
                    json.put(pkg, version);
                    added = true;
                }
            } catch (NumberFormatException e) { // NOSONAR
                /*
                 * If the current version is not parseable, it can refer to a
                 * file and we should leave it alone
                 */
            }
        } else {
            json.put(pkg, version);
            added = true;
        }
        // always update vaadin version to the latest set version
        vaadinDeps.put(pkg, version);

        if (added) {
            log().debug("Added \"{}\": \"{}\" line.", pkg, version);
        } else {
            // we made a change to the package json vaadin defaults
            // even if we didn't add to the dependencies.
            added = !vaadinVersion.isEqualTo(new FrontendVersion(version));
        }
        return added ? 1 : 0;
    }

    private static FrontendVersion toVersion(JsonObject json, String key) {
        return new FrontendVersion(json.getString(key));
    }

    String writePackageFile(JsonObject packageJson) throws IOException {
        return writePackageFile(packageJson, new File(npmFolder, PACKAGE_JSON));
    }

    String writeResourcesPackageFile(JsonObject packageJson)
            throws IOException {
        return writePackageFile(packageJson,
                new File(flowResourcesFolder, PACKAGE_JSON));
    }

    String writePackageFile(JsonObject json, File packageFile)
            throws IOException {
        log().debug("writing file {}.", packageFile.getAbsolutePath());
        FileUtils.forceMkdirParent(packageFile);
        String content = stringify(json, 2) + "\n";
        FileUtils.writeStringToFile(packageFile, content, UTF_8.name());
        return content;
    }

    Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Generate versions json file for version locking.
     *
     * @return generated versions json file path
     * @throws IOException
     *             when file IO fails
     */
    protected String generateVersionsJson() throws IOException {
        File versions = new File(generatedFolder, "versions.json");

        JsonObject versionsJson = getPlatformPinnedDependencies();
        if (versionsJson == null) {
            versionsJson = generateVersionsFromPackageJson();
        }
        FileUtils.write(versions, stringify(versionsJson, 2) + "\n",
                StandardCharsets.UTF_8);
        Path versionsPath = versions.toPath();
        if (versions.isAbsolute()) {
            return FrontendUtils.getUnixRelativePath(npmFolder.toPath(),
                    versionsPath);
        } else {
            return FrontendUtils.getUnixPath(versionsPath);
        }
    }

    /**
     * If we do not have the platform versions to lock we should lock any
     * versions in the package.json so we do not get multiple versions for
     * defined packages.
     *
     * @return versions Json based on package.json
     * @throws IOException
     *             If reading package.json fails
     */
    private JsonObject generateVersionsFromPackageJson() throws IOException {
        JsonObject versionsJson = Json.createObject();
        // if we don't have versionsJson lock package dependency versions.
        final JsonObject packageJson = getPackageJson();
        final JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        final JsonObject devDependencies = packageJson
                .getObject(DEV_DEPENDENCIES);
        if (dependencies != null) {
            for (String key : dependencies.keys()) {
                versionsJson.put(key, dependencies.getString(key));
            }
        }
        if (devDependencies != null) {
            for (String key : devDependencies.keys()) {
                versionsJson.put(key, devDependencies.getString(key));
            }
        }

        return versionsJson;
    }
}
