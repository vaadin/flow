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
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JsonDecodingException;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.PACKAGE_LOCK_JSON;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
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

    // .vaadin/vaadin.json contains local installation data inside node_modules
    // This will help us know to execute even when another developer has pushed
    // a new hash to the code repository.
    private static final String VAADIN_JSON = ".vaadin/vaadin.json";

    static final String DEPENDENCIES = "dependencies";
    static final String VAADIN_DEP_KEY = "vaadin";
    static final String HASH_KEY = "hash";
    static final String DEV_DEPENDENCIES = "devDependencies";
    static final String OVERRIDES = "overrides";
    static final String PNPM = "pnpm";

    private static final String DEP_LICENSE_KEY = "license";
    private static final String DEP_LICENSE_DEFAULT = "UNLICENSED";
    private static final String DEP_NAME_KEY = "name";
    private static final String DEP_NAME_DEFAULT = "no-name";
    private static final String FRONTEND_RESOURCES_PATH = NodeUpdater.class
            .getPackage().getName().replace('.', '/') + "/";
    @Deprecated
    protected static final String DEP_NAME_FLOW_DEPS = "@vaadin/flow-deps";
    @Deprecated
    protected static final String DEP_NAME_FLOW_JARS = "@vaadin/flow-frontend";

    static final String VAADIN_VERSION = "vaadinVersion";
    static final String PROJECT_FOLDER = "projectFolder";

    /**
     * The {@link FrontendDependencies} object representing the application
     * dependencies.
     */
    protected final FrontendDependenciesScanner frontDeps;

    final ClassFinder finder;

    boolean modified;

    ObjectNode versionsJson;

    protected Options options;

    /**
     * Constructor.
     *
     * @param frontendDependencies
     *            a reusable frontend dependencies
     * @param options
     *            the task options
     */
    protected NodeUpdater(FrontendDependenciesScanner frontendDependencies,
            Options options) {
        this.finder = options.getClassFinder();
        this.frontDeps = frontendDependencies;
        this.options = options;
    }

    protected File getPackageJsonFile() {
        return new File(options.getNpmFolder(), PACKAGE_JSON);
    }

    protected File getPackageLockFile() {
        return new File(options.getNpmFolder(), PACKAGE_LOCK_JSON);
    }

    /**
     * Gets the platform pinned versions that are not overridden by the user in
     * package.json.
     *
     * @return {@code JsonNode} with the dependencies or empty {@code JsonNode}
     *         if file doesn't exist
     * @throws IOException
     *             when versions file could not be read
     */
    ObjectNode getPlatformPinnedDependencies() throws IOException {
        URL coreVersionsResource = finder
                .getResource(Constants.VAADIN_CORE_VERSIONS_JSON);
        if (coreVersionsResource == null) {
            log().info(
                    "Couldn't find {} file to pin dependency versions for core components."
                            + " Transitive dependencies won't be pinned for npm/pnpm/bun.",
                    Constants.VAADIN_CORE_VERSIONS_JSON);
            return JacksonUtils.createObjectNode();
        }

        ObjectNode versionsJson = getFilteredVersionsFromResource(
                coreVersionsResource, Constants.VAADIN_CORE_VERSIONS_JSON);

        URL vaadinVersionsResource = finder
                .getResource(Constants.VAADIN_VERSIONS_JSON);
        if (vaadinVersionsResource == null) {
            // vaadin is not on the classpath, only vaadin-core is present.
            return versionsJson;
        }

        ObjectNode vaadinVersionsJson = getFilteredVersionsFromResource(
                vaadinVersionsResource, Constants.VAADIN_VERSIONS_JSON);
        for (String key : JacksonUtils.getKeys(vaadinVersionsJson)) {
            versionsJson.put(key, vaadinVersionsJson.get(key).asString());
        }

        return versionsJson;
    }

    private ObjectNode getFilteredVersionsFromResource(URL versionsResource,
            String versionsOrigin) throws IOException {
        ObjectNode versionsJson;

        try (InputStream content = versionsResource.openStream()) {
            VersionsJsonConverter convert = new VersionsJsonConverter(
                    JacksonUtils.readTree(StringUtil.toUtf8Str(content)),
                    options.isReactEnabled()
                            && FrontendUtils.isReactModuleAvailable(options),
                    options.isNpmExcludeWebComponents());
            versionsJson = convert.getConvertedJson();
            versionsJson = new VersionsJsonFilter(getPackageJson(),
                    DEPENDENCIES)
                    .getFilteredVersions(versionsJson, versionsOrigin);
        }
        return versionsJson;
    }

    static Set<String> getGeneratedModules(File frontendFolder) {
        final Function<String, String> unixPath = str -> str.replace("\\", "/");

        File generatedImportsFolder = FrontendUtils
                .getFlowGeneratedFolder(frontendFolder);
        File webComponentsFolder = FrontendUtils
                .getFlowGeneratedWebComponentsFolder(frontendFolder);
        final URI baseDir = generatedImportsFolder.toURI();

        if (!webComponentsFolder.exists()) {
            return Collections.emptySet();
        }

        try {
            return FileIOUtils
                    .listFiles(webComponentsFolder, new String[] { "js" }, true)
                    .stream()
                    .map(file -> unixPath
                            .apply(baseDir.relativize(file.toURI()).getPath()))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read web components from " + webComponentsFolder,
                    e);
        }
    }

    ObjectNode getPackageJson() throws IOException {
        ObjectNode packageJson = getJsonFileContent(getPackageJsonFile());
        if (packageJson == null) {
            packageJson = JacksonUtils.createObjectNode();
            packageJson.put(DEP_NAME_KEY, DEP_NAME_DEFAULT);
            packageJson.put(DEP_LICENSE_KEY, DEP_LICENSE_DEFAULT);
            packageJson.put("type", "module");
        }

        addDefaultObjects(packageJson);
        addVaadinDefaultsToJson(packageJson);
        removePlugins(packageJson);

        return packageJson;
    }

    private void addDefaultObjects(ObjectNode json) {
        computeIfAbsent(json, DEPENDENCIES, JacksonUtils::createObjectNode);
        computeIfAbsent(json, DEV_DEPENDENCIES, JacksonUtils::createObjectNode);
    }

    private void removePlugins(ObjectNode packageJson) {
        Path targetFolder = Paths.get(options.getNpmFolder().toString(),
                options.getBuildDirectoryName(),
                FrontendPluginsUtil.PLUGIN_TARGET);

        if (!packageJson.has(DEV_DEPENDENCIES)) {
            return;
        }
        ObjectNode devDependencies = (ObjectNode) packageJson
                .get(DEV_DEPENDENCIES);

        String atVaadinPrefix = "@vaadin/";
        String pluginTargetPrefix = "./"
                + (options.getNpmFolder().toPath().relativize(targetFolder)
                        + "/").replace('\\', '/');

        // Clean previously installed plugins
        for (String depKey : JacksonUtils.getKeys(devDependencies)) {
            String depVersion = devDependencies.get(depKey).asString();
            if (depKey.startsWith(atVaadinPrefix)
                    && depVersion.startsWith(pluginTargetPrefix)) {
                devDependencies.remove(depKey);
            }
        }
    }

    static ObjectNode getJsonFileContent(File packageFile) throws IOException {
        ObjectNode jsonContent = null;
        if (packageFile.exists()) {
            String fileContent = Files.readString(packageFile.toPath(), UTF_8);
            try {
                jsonContent = (ObjectNode) JacksonUtils.readTree(fileContent);
            } catch (JsonDecodingException e) { // NOSONAR
                throw new RuntimeException(String
                        .format("Cannot parse package file '%s'", packageFile));
            }
        }
        return jsonContent;
    }

    void addVaadinDefaultsToJson(ObjectNode json) {
        ObjectNode vaadinPackages = computeIfAbsent(json, VAADIN_DEP_KEY,
                JacksonUtils::createObjectNode);

        computeIfAbsent(vaadinPackages, DEPENDENCIES, () -> {
            final ObjectNode dependencies = JacksonUtils.createObjectNode();
            getDefaultDependencies().forEach(dependencies::put);
            return dependencies;
        });
        computeIfAbsent(vaadinPackages, DEV_DEPENDENCIES, () -> {
            final ObjectNode devDependencies = JacksonUtils.createObjectNode();
            getDefaultDevDependencies().forEach(devDependencies::put);
            return devDependencies;
        });
        computeIfAbsent(vaadinPackages, HASH_KEY,
                () -> JacksonUtils.createNode(""));
    }

    private static <T extends JsonNode> T computeIfAbsent(ObjectNode jsonObject,
            String key, Supplier<T> valueSupplier) {
        T result = (T) jsonObject.get(key);
        if (result == null) {
            result = valueSupplier.get();
            jsonObject.set(key, result);
        }
        return result;
    }

    Map<String, String> getDefaultDependencies() {
        Map<String, String> dependencies = readDependencies("default",
                "dependencies");
        if (!isPolymerTemplateModuleAvailable(options)) {
            dependencies.remove("@polymer/polymer");
        }
        if (options.isReactEnabled()) {
            dependencies
                    .putAll(readDependencies("react-router", "dependencies"));
        } else {
            dependencies
                    .putAll(readDependencies("vaadin-router", "dependencies"));
        }
        if (FrontendUtils.isTailwindCssEnabled(options)) {
            dependencies
                    .putAll(readDependencies("tailwindcss", "dependencies"));
        }
        putHillaComponentsDependencies(dependencies, "dependencies");
        return dependencies;
    }

    Map<String, String> readDependencies(String id, String packageJsonKey) {
        try {
            Map<String, String> map = new HashMap<>();
            JsonNode dependencies = readPackageJson(id).get(packageJsonKey);
            if (dependencies == null) {
                log().error("Unable to find " + packageJsonKey + " from '" + id
                        + "'");
                return new HashMap<>();
            }
            for (String key : JacksonUtils.getKeys(dependencies)) {
                map.put(key, dependencies.get(key).asString());
            }

            return map;
        } catch (IOException e) {
            log().error(
                    "Unable to read " + packageJsonKey + " from '" + id + "'",
                    e);
            return new HashMap<>();
        }

    }

    JsonNode readPackageJson(String id) throws IOException {
        URL resource = options.getClassFinder()
                .getResource(FRONTEND_RESOURCES_PATH + "dependencies/" + id
                        + "/package.json");
        if (resource == null) {
            log().error("Unable to find package.json from '" + id + "'");

            return JacksonUtils.readTree("{\"%s\":{},\"%s\":{}}"
                    .formatted(DEPENDENCIES, DEV_DEPENDENCIES));
        }
        return JacksonUtils.readTree(FileIOUtils.urlToString(resource));
    }

    boolean hasPackageJson(String id) {
        return options.getClassFinder().getResource(FRONTEND_RESOURCES_PATH
                + "dependencies/" + id + "/package.json") != null;
    }

    Map<String, String> readDependenciesIfAvailable(String id,
            String packageJsonKey) {
        if (hasPackageJson(id)) {
            return readDependencies(id, packageJsonKey);
        }
        return new HashMap<>();
    }

    Map<String, String> getDefaultDevDependencies() {
        Map<String, String> defaults = new HashMap<>();
        defaults.putAll(readDependencies("default", "devDependencies"));
        defaults.putAll(readDependencies("vite", "devDependencies"));
        putHillaComponentsDependencies(defaults, "devDependencies");
        if (options.isReactEnabled()) {
            defaults.putAll(
                    readDependencies("react-router", "devDependencies"));
        }
        if (FrontendUtils.isTailwindCssEnabled(options)) {
            defaults.putAll(readDependencies("tailwindcss", "devDependencies"));
        }

        // Add workbox dependencies only when PWA is enabled
        if (frontDeps != null && frontDeps.getPwaConfiguration() != null
                && frontDeps.getPwaConfiguration().isEnabled()) {
            defaults.putAll(readDependencies("workbox", "devDependencies"));
        }

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
    boolean updateDefaultDependencies(ObjectNode packageJson) {
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
            log().debug("Added {} default dependencies to main package.json",
                    added);
        }
        return added > 0;
    }

    int addDependency(ObjectNode json, String key, String pkg, String version) {
        Objects.requireNonNull(json, "Json object need to be given");
        Objects.requireNonNull(key, "Json sub object needs to be give.");
        Objects.requireNonNull(pkg, "dependency package needs to be defined");

        ObjectNode vaadinDeps = (ObjectNode) json.get(VAADIN_DEP_KEY);
        if (!json.has(key)) {
            json.set(key, JacksonUtils.createObjectNode());
        }
        json = (ObjectNode) json.get(key);
        vaadinDeps = (ObjectNode) vaadinDeps.get(key);

        if (vaadinDeps.has(pkg)) {
            if (version == null) {
                version = vaadinDeps.get(pkg).asString();
            }
            return handleExistingVaadinDep(json, pkg, version, vaadinDeps);
        } else {
            vaadinDeps.put(pkg, version);
            if (!json.has(pkg) || isNewerVersion(json, pkg, version)) {
                json.put(pkg, version);
                log().debug("Added \"{}\": \"{}\" line.", pkg, version);
                return 1;
            }
        }
        return 0;
    }

    private boolean isNewerVersion(JsonNode json, String pkg, String version) {

        try {
            FrontendVersion newVersion = new FrontendVersion(version);
            FrontendVersion existingVersion = toVersion(json, pkg);
            return newVersion.isNewerThan(existingVersion);
        } catch (NumberFormatException e) {
            if (VAADIN_FORM_PKG.equals(pkg) && json.get(pkg).asString()
                    .contains(VAADIN_FORM_PKG_LEGACY_VERSION)) {
                return true;
            } else {
                // NPM package versions are not always easy to parse, see
                // https://docs.npmjs.com/cli/v8/configuring-npm/package-json#dependencies
                // for some examples. So let's return false for unparsable
                // versions, as we don't want them to be updated.
                log().warn("Package {} has unparseable version: {}", pkg,
                        e.getMessage());
                return false;
            }
        }
    }

    private int handleExistingVaadinDep(ObjectNode json, String pkg,
            String version, ObjectNode vaadinDeps) {
        boolean added = false;
        boolean updatedVaadinVersionSection = false;
        try {
            FrontendVersion vaadinVersion = toVersion(vaadinDeps, pkg);
            if (json.has(pkg)) {
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
            } else {
                json.put(pkg, version);
                added = true;
            }
        } catch (NumberFormatException e) { // NOSONAR
            /*
             * If the current version is not parseable, it can refer to a file
             * and we should leave it alone
             */
        }
        // always update vaadin version to the latest set version
        if (!version.equals(vaadinDeps.get(pkg).asString())) {
            vaadinDeps.put(pkg, version);
            updatedVaadinVersionSection = true;
        }

        if (added) {
            log().debug("Added \"{}\": \"{}\" line.", pkg, version);
        } else {
            // we made a change to the package json vaadin defaults
            // even if we didn't add to the dependencies.
            added = updatedVaadinVersionSection;
        }
        return added ? 1 : 0;
    }

    private static FrontendVersion toVersion(JsonNode json, String key) {
        return new FrontendVersion(json.get(key).asString());
    }

    String writePackageFile(JsonNode packageJson) throws IOException {
        return writePackageFile(packageJson,
                new File(options.getNpmFolder(), PACKAGE_JSON));
    }

    String writePackageFile(JsonNode json, File packageFile)
            throws IOException {
        String content = JacksonUtils.toFileJson(json);
        if (packageFile.exists() || options.isFrontendHotdeploy()
                || options.isBundleBuild()) {
            log().debug("writing file {}.", packageFile.getAbsolutePath());
            Files.createDirectories(packageFile.toPath().getParent());
            FileIOUtils.writeIfChanged(packageFile, content);
        }
        return content;
    }

    File getVaadinJsonFile() {
        return new File(new File(options.getNpmFolder(), NODE_MODULES),
                VAADIN_JSON);
    }

    ObjectNode getVaadinJsonContents() throws IOException {
        File vaadinJsonFile = getVaadinJsonFile();
        if (vaadinJsonFile.exists()) {
            String fileContent = Files.readString(vaadinJsonFile.toPath(),
                    UTF_8);
            return JacksonUtils.readTree(fileContent);
        } else {
            return JacksonUtils.createObjectNode();
        }
    }

    void updateVaadinJsonContents(Map<String, String> newContent)
            throws IOException {
        ObjectNode fileContent = getVaadinJsonContents();
        newContent.forEach(fileContent::put);
        File vaadinJsonFile = getVaadinJsonFile();
        Files.createDirectories(vaadinJsonFile.toPath().getParent());
        String content = fileContent.toPrettyString() + "\n";
        FileIOUtils.writeIfChanged(vaadinJsonFile, content);
    }

    Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Generate versions json file for version locking.
     *
     * @param packageJson
     *            the package json content
     * @throws IOException
     *             when file IO fails
     */
    protected void generateVersionsJson(ObjectNode packageJson)
            throws IOException {
        versionsJson = getPlatformPinnedDependencies();
        ObjectNode packageJsonVersions = generateVersionsFromPackageJson(
                packageJson);
        if (JacksonUtils.getKeys(versionsJson).isEmpty()) {
            versionsJson = packageJsonVersions;
        } else {
            for (String key : JacksonUtils.getKeys(packageJsonVersions)) {
                if (!versionsJson.has(key)) {
                    versionsJson.put(key,
                            packageJsonVersions.get(key).asString());
                }
            }
        }
    }

    /**
     * If we do not have the platform versions to lock we should lock any
     * versions in the package.json so we do not get multiple versions for
     * defined packages.
     *
     * @return versions Json based on package.json
     */
    private ObjectNode generateVersionsFromPackageJson(JsonNode packageJson) {
        ObjectNode versionsJson = JacksonUtils.createObjectNode();
        // if we don't have versionsJson lock package dependency versions.
        final JsonNode dependencies = packageJson.get(DEPENDENCIES);
        if (dependencies != null) {
            for (String key : JacksonUtils.getKeys(dependencies)) {
                versionsJson.put(key, dependencies.get(key).asString());
            }
        }

        return versionsJson;
    }

    /**
     * Adds Hilla components to package.json if Hilla is used in the project.
     *
     * @param dependencies
     *            to be added into package.json
     * @param packageJsonKey
     *            the key inside package.json containing the sub-list of
     *            dependencies to read and add
     * @see <a href=
     *      "https://github.com/vaadin/hilla/tree/main/packages/java/hilla/src/main/resources/com/vaadin/flow/server/frontend/dependencies/hilla/components</a>
     */
    private void putHillaComponentsDependencies(
            Map<String, String> dependencies, String packageJsonKey) {
        if (FrontendUtils.isHillaUsed(options.getFrontendDirectory(),
                options.getClassFinder())) {
            if (options.isReactEnabled()) {
                dependencies.putAll(readDependenciesIfAvailable(
                        "hilla/components/react", packageJsonKey));
                if (options.isNpmExcludeWebComponents()) {
                    // remove dependencies that depends on web components
                    dependencies.remove("@vaadin/hilla-react-crud");
                }
            } else {
                dependencies.putAll(readDependenciesIfAvailable(
                        "hilla/components/lit", packageJsonKey));
            }
        }
    }

    private boolean isPolymerTemplateModuleAvailable(Options options) {
        try {
            options.getClassFinder().loadClass(
                    "com.vaadin.flow.component.polymertemplate.PolymerTemplate");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
