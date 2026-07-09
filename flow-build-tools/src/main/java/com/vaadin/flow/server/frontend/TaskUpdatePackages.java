/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.internal.FileIOUtils;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.FrontendVersion;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import static com.vaadin.flow.server.frontend.VersionsJsonConverter.JS_VERSION;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.NPM_NAME;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.NPM_VERSION;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.VAADIN_CORE_NPM_PACKAGE;

/**
 * Updates <code>package.json</code> by visiting {@link NpmPackage} annotations
 * found in the classpath. It also visits classes annotated with
 * {@link NpmPackage}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class TaskUpdatePackages extends NodeUpdater {

    protected static final String VAADIN_APP_PACKAGE_HASH = "vaadinAppPackageHash";
    private final boolean forceCleanUp;
    private final boolean enablePnpm;
    private File jarResourcesFolder;

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param options
     *            the task options
     */
    TaskUpdatePackages(Options options) {
        super(options);
        this.jarResourcesFolder = options.getJarFrontendResourcesFolder();
        this.forceCleanUp = options.isCleanNpmFiles();
        this.enablePnpm = options.isEnablePnpm();
    }

    @Override
    public void execute() {
        try {
            final FrontendDependenciesScanner frontDeps = options
                    .getFrontendDependenciesScanner();
            Map<String, String> scannedApplicationDependencies = frontDeps
                    .getPackages();
            Map<String, String> scannedApplicationDevDependencies = frontDeps
                    .getDevPackages();
            ObjectNode packageJson = getPackageJson();
            modified = updatePackageJsonDependencies(packageJson,
                    scannedApplicationDependencies,
                    scannedApplicationDevDependencies);
            generateVersionsJson(packageJson);
            modified = lockVersionForNpm(packageJson) || modified;

            // Recompute hash
            final String finalHash = generatePackageJsonHash(packageJson);
            final JsonNode hashNode = JacksonUtils.getNestedKey(packageJson,
                    List.of(VAADIN_DEP_KEY, HASH_KEY));
            modified = !finalHash
                    .equals(hashNode != null ? hashNode.stringValue() : "")
                    || modified;
            if (modified) {
                ((ObjectNode) packageJson.get(VAADIN_DEP_KEY)).put(HASH_KEY,
                        finalHash);

                if (!packageJson.has("type") || !packageJson.get("type")
                        .asString().equals("module")) {
                    packageJson.put("type", "module");
                    log().info(
                            """
                                    Adding package.json type as module to enable ES6 modules which is now required.
                                    With this change sources need to use 'import' instead of 'require' for imports.
                                    """);
                }
                writePackageFile(packageJson);
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    boolean lockVersionForNpm(ObjectNode packageJson) throws IOException {
        final JsonNode dependencies = packageJson.get(DEPENDENCIES);
        final JsonNode devDependencies = packageJson.get(DEV_DEPENDENCIES);

        final Map<String, String> platformVersions = collectPlatformVersions();
        final ObjectNode vaadinOverrides = computeVaadinOverrides(
                platformVersions, dependencies, devDependencies);

        final ObjectNode overridesSection = getOverridesSection(packageJson);
        final Map<String, String> overridesBefore = flattenOverrides(
                overridesSection);

        removeManagedOverrides(overridesSection,
                managedOverrideKeys(platformVersions), dependencies,
                devDependencies);
        applyOverrides(overridesSection, flattenOverrides(vaadinOverrides));

        boolean updated = !overridesBefore
                .equals(flattenOverrides(overridesSection));
        updated |= removeLegacyVaadinOverrides(packageJson);
        return updated;
    }

    /**
     * Collects the versions of all platform-managed packages. When no platform
     * versions are available, {@code versionsJson} falls back to the versions
     * declared in package.json so that those get locked as well.
     */
    private Map<String, String> collectPlatformVersions() throws IOException {
        final Map<String, String> platformVersions = new HashMap<>();
        final ObjectNode fullPlatformDependencies = getFullPlatformDependencies();
        for (String dependency : JacksonUtils
                .getKeys(fullPlatformDependencies)) {
            platformVersions.put(dependency,
                    fullPlatformDependencies.get(dependency).asString());
        }
        for (String dependency : JacksonUtils.getKeys(versionsJson)) {
            platformVersions.putIfAbsent(dependency,
                    versionsJson.get(dependency).asString());
        }
        return platformVersions;
    }

    /**
     * Builds the overrides Vaadin wants to enforce: a dependency reference
     * ({@code $dependency}) when the package is declared directly, the platform
     * version otherwise.
     */
    private ObjectNode computeVaadinOverrides(
            Map<String, String> platformVersions, JsonNode dependencies,
            JsonNode devDependencies) {
        final ObjectNode vaadinOverrides = getDefaultOverrides();
        for (Map.Entry<String, String> platformEntry : platformVersions
                .entrySet()) {
            final String dependency = platformEntry.getKey();
            if (vaadinOverrides.has(dependency)) {
                // Already provided by the default (e.g. workbox) overrides.
                continue;
            }
            final FrontendVersion platformVersion = parseLockableVersion(
                    platformEntry.getValue());
            if (platformVersion == null) {
                continue;
            }
            final String directVersion = directDependencyVersion(dependencies,
                    devDependencies, dependency);
            if (directVersion == null) {
                // Not declared directly, pin to the platform version.
                vaadinOverrides.put(dependency,
                        platformVersion.getFullVersion());
            } else if (isNumericVersion(directVersion)) {
                // Locked by a dependency/devDependency; reference it so the
                // declared version is enforced for transitive uses too.
                vaadinOverrides.put(dependency, "$" + dependency);
            }
            // A non-numeric direct dependency (e.g. a folder link) is left as
            // is, without an override.
        }
        return vaadinOverrides;
    }

    /**
     * Parses a platform version, returning {@code null} for build-folder,
     * SNAPSHOT or otherwise non-numeric versions that should not be locked.
     */
    private FrontendVersion parseLockableVersion(String version) {
        if (isInternalPseudoDependency(version)) {
            return null;
        }
        try {
            final FrontendVersion frontendVersion = new FrontendVersion(
                    version);
            return "SNAPSHOT".equals(frontendVersion.getBuildIdentifier())
                    ? null
                    : frontendVersion;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /**
     * Top-level override keys Vaadin manages: platform packages and the default
     * overrides Vaadin may add (e.g. workbox).
     */
    private Set<String> managedOverrideKeys(
            Map<String, String> platformVersions) {
        final Set<String> managedKeys = new HashSet<>(
                platformVersions.keySet());
        managedKeys.addAll(JacksonUtils.getKeys(getManagedDefaultOverrides()));
        return managedKeys;
    }

    /**
     * Removes the overrides Vaadin manages and any dependency reference whose
     * target is no longer a dependency. User-defined overrides are kept.
     */
    private void removeManagedOverrides(ObjectNode overridesSection,
            Set<String> managedKeys, JsonNode dependencies,
            JsonNode devDependencies) {
        for (String key : JacksonUtils.getKeys(overridesSection)) {
            final String topLevelKey = enablePnpm ? key.split(">", 2)[0] : key;
            if (managedKeys.contains(topLevelKey) || isDanglingReference(
                    overridesSection.get(key), dependencies, devDependencies)) {
                overridesSection.remove(key);
            }
        }
    }

    /**
     * A dependency reference ({@code $dependency}) is dangling when the
     * referenced package is not declared as a dependency or devDependency.
     */
    private static boolean isDanglingReference(JsonNode value,
            JsonNode dependencies, JsonNode devDependencies) {
        return value.isString() && value.stringValue().startsWith("$")
                && directDependencyVersion(dependencies, devDependencies,
                        value.stringValue().substring(1)) == null;
    }

    /**
     * Writes the given flattened overrides into the overrides section, using
     * the flat format for pnpm and the nested format for npm.
     */
    private void applyOverrides(ObjectNode overridesSection,
            Map<String, String> overrides) {
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            if (enablePnpm) {
                overridesSection.put(entry.getKey(), entry.getValue());
            } else {
                putNestedOverride(overridesSection,
                        List.of(entry.getKey().split(">")), entry.getValue());
            }
        }
    }

    /**
     * Removes the obsolete {@code vaadin.overrides} tracking section written by
     * earlier Flow versions.
     *
     * @return {@code true} if the section was present and removed
     */
    private static boolean removeLegacyVaadinOverrides(ObjectNode packageJson) {
        final ObjectNode vaadinSection = (ObjectNode) packageJson
                .get(VAADIN_DEP_KEY);
        if (vaadinSection.has(OVERRIDES)) {
            vaadinSection.remove(OVERRIDES);
            return true;
        }
        return false;
    }

    private static String directDependencyVersion(JsonNode dependencies,
            JsonNode devDependencies, String pkg) {
        if (dependencies != null && dependencies.has(pkg)) {
            return dependencies.get(pkg).asString();
        }
        if (devDependencies != null && devDependencies.has(pkg)) {
            return devDependencies.get(pkg).asString();
        }
        return null;
    }

    private static boolean isNumericVersion(String version) {
        try {
            new FrontendVersion(version);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void putNestedOverride(ObjectNode overrides, List<String> keyPath,
            String value) {
        JsonNode existingNode = JacksonUtils.getNestedKey(overrides, keyPath);
        if (existingNode != null && existingNode.isObject()) {
            // Add as a "." property in existing object
            ((ObjectNode) existingNode).put(".", value);
            return;
        }
        JacksonUtils.setNestedKey(overrides, keyPath, StringNode.valueOf(value),
                (plainValueNode) -> {
                    // Create and use an intermediate nested object
                    final ObjectNode objectNode = JacksonUtils
                            .createObjectNode();
                    if (plainValueNode != null && plainValueNode.isString()) {
                        // Upgrade plain string override to nested object
                        // { "dep": "1.0" } => { "dep" : { ".": "1.0" } }
                        objectNode.set(".", plainValueNode);
                    }
                    return objectNode;
                });
    }

    /**
     * Collect all platform npm dependencies from vaadin-core-versions.json and
     * vaadin-versions.json to use in overrides so that any component versions
     * get locked even when they are transitive.
     *
     * @return json containing all npm keys and versions
     * @throws IOException
     *             thrown for exception reading stream
     */
    private ObjectNode getFullPlatformDependencies() throws IOException {
        ObjectNode platformDependencies = JacksonUtils.createObjectNode();
        URL coreVersionsResource = finder
                .getResource(Constants.VAADIN_CORE_VERSIONS_JSON);
        if (coreVersionsResource == null) {
            return platformDependencies;
        }

        try (InputStream content = coreVersionsResource.openStream()) {
            collectDependencies(
                    JacksonUtils.readTree(StringUtil.toUTF8String(content)),
                    platformDependencies);
        }

        URL vaadinVersionsResource = finder
                .getResource(Constants.VAADIN_VERSIONS_JSON);
        if (vaadinVersionsResource == null) {
            // vaadin is not on the classpath, only vaadin-core is present.
            return platformDependencies;
        }

        try (InputStream content = vaadinVersionsResource.openStream()) {
            collectDependencies(
                    JacksonUtils.readTree(StringUtil.toUTF8String(content)),
                    platformDependencies);
        }

        return platformDependencies;
    }

    private void collectDependencies(JsonNode obj, ObjectNode collection) {
        for (String key : JacksonUtils.getKeys(obj)) {
            JsonNode value = obj.get(key);
            if (!(value instanceof ObjectNode)) {
                continue;
            }
            if (value.has(NPM_NAME)) {
                String npmName = value.get(NPM_NAME).asString();
                if (Objects.equals(npmName, VAADIN_CORE_NPM_PACKAGE)) {
                    return;
                }
                String version;
                if (value.has(NPM_VERSION)) {
                    version = value.get(NPM_VERSION).asString();
                } else if (value.has(JS_VERSION)) {
                    version = value.get(JS_VERSION).asString();
                } else {
                    log().debug(
                            "dependency '{}' has no 'npmVersion'/'jsVersion'.",
                            npmName);
                    continue;
                }
                collection.put(npmName, version);
            } else {
                collectDependencies(value, collection);
            }
        }
    }

    private boolean isInternalPseudoDependency(String dependencyVersion) {
        return dependencyVersion != null && dependencyVersion
                .startsWith("./" + options.getBuildDirectoryName());
    }

    private ObjectNode getOverridesSection(ObjectNode packageJson) {
        ObjectNode overridesSection;
        ObjectNode oldOverrides;
        if (options.isEnablePnpm()) {
            ObjectNode pnpm = (ObjectNode) packageJson.get(PNPM);
            if (pnpm == null) {
                pnpm = JacksonUtils.createObjectNode();
                packageJson.set(PNPM, pnpm);
            }
            overridesSection = (ObjectNode) pnpm.get(OVERRIDES);
            if (overridesSection == null) {
                overridesSection = JacksonUtils.createObjectNode();
                pnpm.set(OVERRIDES, overridesSection);
            }
            oldOverrides = (ObjectNode) packageJson.get(OVERRIDES);
            if (oldOverrides != null) {
                // convert npm overrides to flat format for pnpm
                flattenOverrides(oldOverrides).forEach(overridesSection::put);
                // remove npm overrides when moving to pnpm
                packageJson.remove(OVERRIDES);
            }
            return overridesSection;
        }
        overridesSection = (ObjectNode) packageJson.get(OVERRIDES);
        if (overridesSection == null) {
            overridesSection = JacksonUtils.createObjectNode();
            packageJson.set(OVERRIDES, overridesSection);
        }
        if (packageJson.has(PNPM)) {
            ObjectNode pnpm = (ObjectNode) packageJson.get(PNPM);
            oldOverrides = (ObjectNode) pnpm.get(OVERRIDES);
            // convert pnpm overrides to nested format for npm
            for (String key : oldOverrides.propertyNames()) {
                final List<String> keyPath = List.of(key.split(">"));
                putNestedOverride(overridesSection, keyPath,
                        oldOverrides.get(key).stringValue());
            }
            // remove pnpm overrides when moving to npm
            pnpm.remove(OVERRIDES);
            if (pnpm.isEmpty()) {
                packageJson.remove(PNPM);
            }
        }
        return overridesSection;
    }

    /**
     * Converts npm nested overrides to pnpm flat format. pnpm uses a different
     * syntax for nested dependency overrides, using '&gt;' as a separator.
     * <p>
     * Example: {"workbox-build": {"dep": "1.0"}} becomes
     * {"workbox-build&gt;dep": "1.0"}
     *
     * @param nestedOverrides
     *            the nested override structure (npm format)
     * @return flattened override structure (pnpm format)
     */
    private Map<String, String> flattenOverrides(ObjectNode nestedOverrides) {
        final Map<String, String> flatOverrides = new HashMap<>();
        for (String key : JacksonUtils.getKeys(nestedOverrides)) {
            JsonNode value = nestedOverrides.get(key);
            if (value.isObject()) {
                // "." refers to key dependency
                if (value.has(".")) {
                    flatOverrides.put(key, value.get(".").stringValue());
                    // Use a shallow value object copy without the "." property
                    ObjectNode filtered = JacksonUtils.createObjectNode();
                    for (String nestedProp : value.propertyNames()) {
                        if (nestedProp.equals(".")) {
                            continue;
                        }
                        filtered.set(nestedProp, value.get(nestedProp));
                    }
                    value = filtered;
                }
                // Nested override: {"workbox-build": {"dep": "1.0"}}
                final Map<String, String> childOverrides = flattenOverrides(
                        (ObjectNode) value);
                for (Map.Entry<String, String> childEntry : childOverrides
                        .entrySet()) {
                    flatOverrides.put(key + ">" + childEntry.getKey(),
                            childEntry.getValue());
                }
            } else {
                // Already flat, keep as-is
                flatOverrides.put(key, value.stringValue());
            }
        }
        return flatOverrides;
    }

    @Override
    String writePackageFile(JsonNode json) throws IOException {
        sortObject(json, DEPENDENCIES);
        sortObject(json, DEV_DEPENDENCIES);
        sortObject(json, VAADIN_DEP_KEY);
        return super.writePackageFile(json);
    }

    private void sortObject(JsonNode json, String key) {
        if (!json.has(key)) {
            return;
        }
        ObjectNode object = (ObjectNode) json.get(key);
        ObjectNode ordered = orderKeys(object);
        JacksonUtils.getKeys(object).forEach(object::remove);
        // add ordered keys back
        JacksonUtils.getKeys(ordered).forEach(prop -> {
            JsonNode value = ordered.get(prop);
            object.set(prop, value);
        });
    }

    private ObjectNode orderKeys(JsonNode object) {
        String[] keys = JacksonUtils.getKeys(object).toArray(String[]::new);
        Arrays.sort(keys);
        ObjectNode result = JacksonUtils.createObjectNode();
        for (String key : keys) {
            JsonNode value = object.get(key);
            if (value instanceof ObjectNode) {
                value = orderKeys(value);
            }
            result.set(key, value);
        }
        return result;
    }

    @SuppressWarnings("squid:S134")
    private boolean updatePackageJsonDependencies(ObjectNode packageJson,
            Map<String, String> applicationDependencies,
            Map<String, String> applicationDevDependencies) throws IOException {
        int added = 0;

        Map<String, String> filteredApplicationDependencies = new ExclusionFilter(
                finder,
                options.isReactEnabled()
                        && FrontendBuildUtils.isReactModuleAvailable(options),
                options.isNpmExcludeWebComponents())
                .exclude(applicationDependencies);

        // Add application dependencies
        for (Entry<String, String> dep : filteredApplicationDependencies
                .entrySet()) {
            added += addDependency(packageJson, DEPENDENCIES, dep.getKey(),
                    dep.getValue());
        }

        // Add application dev dependencies.
        int addedDev = 0;
        for (Entry<String, String> devDep : applicationDevDependencies
                .entrySet()) {
            addedDev += addDependency(packageJson, DEV_DEPENDENCIES,
                    devDep.getKey(), devDep.getValue());
        }

        /*
         * #10572 lock all platform internal versions
         */
        List<String> pinnedPlatformDependencies = new ArrayList<>();
        final ObjectNode platformPinnedDependencies = getPlatformPinnedDependencies();
        for (String key : JacksonUtils.getKeys(platformPinnedDependencies)) {
            // need to double check that not overriding a scanned
            // dependency since add-ons should be able to downgrade
            // version through exclusion
            if (!filteredApplicationDependencies.containsKey(key)
                    && pinPlatformDependency(packageJson,
                            platformPinnedDependencies, key)) {
                added++;
            }
            // make sure platform pinned dependency is not cleared
            pinnedPlatformDependencies.add(key);
        }

        if (added > 0) {
            log().debug("Added {} dependencies to main package.json", added);
        }

        if (addedDev > 0) {
            log().debug("Added {} devDependencies to main package.json", added);
        }

        // Remove obsolete dependencies
        List<String> dependencyCollection = Stream
                .concat(filteredApplicationDependencies.entrySet().stream(),
                        getDefaultDependencies().entrySet().stream())
                .map(Entry::getKey).collect(Collectors.toList());
        dependencyCollection.addAll(pinnedPlatformDependencies);

        boolean doCleanUp = forceCleanUp; // forced only in tests
        int removed = removeLegacyProperties(packageJson);
        removed += cleanDependencies(dependencyCollection, packageJson,
                DEPENDENCIES);

        // FIXME do not do cleanup of node_modules every time platform is
        // updated ?
        doCleanUp = doCleanUp || (!enablePnpm && FrontendBuildUtils
                .isPlatformVersionUpdated(finder, options.getNpmFolder(),
                        options.getNodeModulesFolder()));

        // Remove obsolete devDependencies
        dependencyCollection = new ArrayList<>(
                getDefaultDevDependencies().keySet());
        dependencyCollection.addAll(applicationDevDependencies.keySet());

        int removedDev = 0;
        removedDev = cleanDependencies(dependencyCollection, packageJson,
                DEV_DEPENDENCIES);

        if (removed > 0) {
            log().debug("Removed {} dependencies", removed);
        }
        if (removedDev > 0) {
            log().debug("Removed {} devDependencies", removedDev);
        }

        if (doCleanUp) {
            cleanUp();
        }

        return added > 0 || addedDev > 0 || removed > 0 || removedDev > 0;
    }

    private int cleanDependencies(List<String> dependencyCollection,
            JsonNode packageJson, String dependencyKey) {
        int removed = 0;

        ObjectNode dependencyObject = (ObjectNode) packageJson
                .get(dependencyKey);
        ObjectNode vaadinDependencyObject = (ObjectNode) packageJson
                .get(VAADIN_DEP_KEY).get(dependencyKey);
        if (dependencyObject != null) {
            for (String key : JacksonUtils.getKeys(dependencyObject)) {
                if (!dependencyCollection.contains(key)
                        && vaadinDependencyObject.has(key)) {
                    dependencyObject.remove(key);
                    vaadinDependencyObject.remove(key);
                    log().debug("Removed \"{}\".", key);
                    removed++;
                }
            }
        }
        return removed;
    }

    protected static boolean pinPlatformDependency(JsonNode packageJson,
            JsonNode platformPinnedVersions, String pkg) {
        final FrontendVersion platformPinnedVersion = FrontendUtils
                .getPackageVersionFromJson(platformPinnedVersions, pkg,
                        "vaadin_dependencies.json");
        if (platformPinnedVersion == null) {
            return false;
        }

        final ObjectNode vaadinDeps = (ObjectNode) packageJson
                .get(VAADIN_DEP_KEY).get(DEPENDENCIES);
        final ObjectNode packageJsonDeps = (ObjectNode) packageJson
                .get(DEPENDENCIES);
        // packages exist at this point
        assert vaadinDeps != null : "vaadin{ dependencies { } } should exist";
        assert packageJsonDeps != null : "dependencies { } should exist";

        FrontendVersion packageJsonVersion = null, vaadinDepsVersion = null;
        try {
            if (packageJsonDeps.has(pkg)) {
                packageJsonVersion = new FrontendVersion(
                        packageJsonDeps.get(pkg).asString());
            }
        } catch (NumberFormatException e) {
            // Overridden to a file link in package.json, do not change
            return false;
        }
        try {
            if (vaadinDeps.has(pkg)) {
                vaadinDepsVersion = new FrontendVersion(
                        vaadinDeps.get(pkg).asString());
            }
        } catch (NumberFormatException e) {
            // Vaadin defines a non-numeric version. Not sure what the case
            // would be but probably it should be pinned like any other version
        }

        if ((vaadinDepsVersion != null && packageJsonVersion != null)
                && !vaadinDepsVersion.equals(packageJsonVersion)) {
            // The user has overridden the version, use that
            return false;
        }

        if (platformPinnedVersion.equals(packageJsonVersion)
                && platformPinnedVersion.equals(vaadinDepsVersion)) {
            return false;
        }

        packageJsonDeps.put(pkg, platformPinnedVersion.getFullVersion());
        vaadinDeps.put(pkg, platformPinnedVersion.getFullVersion());
        return true;
    }

    /**
     * Cleans up any previous version properties from the packageJson object if
     * present.
     *
     * @param packageJson
     *            JsonNode of current package.json contents
     * @return amount of removed properties
     * @throws IOException
     *             thrown if removal of package-lock.json fails
     */
    private int removeLegacyProperties(ObjectNode packageJson)
            throws IOException {
        int result = 0;
        /*
         * In modern Flow versions "@vaadin/flow-deps" should not exist.
         */
        if (packageJson.has(DEPENDENCIES)) {
            ObjectNode object = (ObjectNode) packageJson.get(DEPENDENCIES);
            if (object.has(DEP_NAME_FLOW_DEPS)) {
                object.remove(DEP_NAME_FLOW_DEPS);
                log().debug("Removed \"{}\" as it's not generated anymore.",
                        DEP_NAME_FLOW_DEPS);
                result++;
            }
            if (object.has(DEP_NAME_FLOW_JARS)) {
                object.remove(DEP_NAME_FLOW_JARS);
                log().debug("Removed \"{}\" as it's not needed anymore.",
                        DEP_NAME_FLOW_JARS);
                result++;
            }
        }
        if (packageJson.has(VAADIN_APP_PACKAGE_HASH)) {
            packageJson.remove(VAADIN_APP_PACKAGE_HASH);
            log().debug("Removed \"{}\" as it's not used.",
                    VAADIN_APP_PACKAGE_HASH);
            result++;
        }
        if (!enablePnpm) {
            return result;
        }
        /*
         * In case of PNPM tool the package-lock should not be used at all.
         */
        File packageLockFile = getPackageLockFile();
        if (packageLockFile.exists()) {
            FileIOUtils.delete(getPackageLockFile());
        }
        return result;
    }

    private void cleanUp() throws IOException {
        try {
            FrontendUtils.deleteNodeModules(options.getNodeModulesFolder());
        } catch (IOException exception) {
            File nodeModules = options.getNodeModulesFolder();
            log().debug("Exception removing node_modules", exception);
            StringBuilder messageWithReason = new StringBuilder(
                    "Removal of '{}'");
            if (!forceCleanUp) {
                messageWithReason.append(", due to platform version update,");
            }
            messageWithReason.append(
                    " failed. Remove it manually if there are problems with the build.");
            log().error(messageWithReason.toString(),
                    nodeModules.getAbsolutePath());
        }

        if (jarResourcesFolder != null && jarResourcesFolder.exists()) {
            // This feels like cleanup done in the wrong place but is left here
            // for historical reasons
            for (File file : jarResourcesFolder.listFiles()) {
                file.delete();
            }
        }
    }

    /**
     * Generate hash for package dependencies. This will consider both
     * 'dependencies' and 'devDependencies' of the packageJson format JsonNode.
     * <p>
     * Dependencies will be sorted by key so that different runs for same
     * dependencies in different order will not trigger npm install.
     *
     * @param packageJson
     *            JsonNode built in the same format as package.json
     * @return has for dependencies and devDependencies
     */
    static String generatePackageJsonHash(JsonNode packageJson) {
        StringBuilder hashContent = new StringBuilder();
        if (packageJson.has(DEPENDENCIES)) {
            JsonNode dependencies = packageJson.get(DEPENDENCIES);
            hashContent.append("\"dependencies\": {");
            String sortedDependencies = JacksonUtils.getKeys(dependencies)
                    .stream().sorted(String::compareToIgnoreCase)
                    .map(key -> String.format("\"%s\": \"%s\"", key,
                            dependencies.get(key).asString()))
                    .collect(Collectors.joining(",\n  "));
            hashContent.append(sortedDependencies);
            hashContent.append("}");
        }
        if (packageJson.has(DEV_DEPENDENCIES)) {
            if (!hashContent.isEmpty()) {
                hashContent.append(",\n");
            }
            JsonNode devDependencies = packageJson.get(DEV_DEPENDENCIES);
            hashContent.append("\"devDependencies\": {");
            String sortedDevDependencies = JacksonUtils.getKeys(devDependencies)
                    .stream().sorted(String::compareToIgnoreCase)
                    .map(key -> String.format("\"%s\": \"%s\"", key,
                            devDependencies.get(key).asString()))
                    .collect(Collectors.joining(",\n  "));
            hashContent.append(sortedDevDependencies);
            hashContent.append("}");
        }
        // Include npm overrides in hash
        if (packageJson.has(OVERRIDES)) {
            JsonNode overrides = packageJson.get(OVERRIDES);
            // Only include overrides in hash if section has actual content
            if (overrides.isObject() && !overrides.isEmpty()
                    && !hashContent.isEmpty()) {
                hashContent.append(",\n");
                hashContent.append("\"overrides\": ");
                final ObjectNode sortedOverrides = JacksonUtils
                        .createObjectNode();
                JacksonUtils.getKeys(overrides).stream()
                        .sorted(String::compareToIgnoreCase)
                        .forEachOrdered(key -> {
                            sortedOverrides.set(key, overrides.get(key));
                        });
                hashContent.append(JacksonUtils.toFileJson(sortedOverrides));
            }
        }
        // Include pnpm overrides in hash
        if (packageJson.has(PNPM) && packageJson.get(PNPM).has(OVERRIDES)) {
            JsonNode overrides = packageJson.get(PNPM).get(OVERRIDES);
            // Only include overrides in hash if section has actual content
            if (overrides.isObject() && !overrides.isEmpty()
                    && !hashContent.isEmpty()) {
                hashContent.append(",\n");
                hashContent.append("\"pnpm.overrides\": ");
                final ObjectNode sortedOverrides = JacksonUtils
                        .createObjectNode();
                JacksonUtils.getKeys(overrides).stream()
                        .sorted(String::compareToIgnoreCase)
                        .forEachOrdered(key -> {
                            sortedOverrides.set(key, overrides.get(key));
                        });
                hashContent.append(JacksonUtils.toFileJson(sortedOverrides));
            }
        }
        return StringUtil.getHash(hashContent.toString());
    }

}
