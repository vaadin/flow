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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
        // Keep track of Vaadin overrides in the vaadin.overrides section,
        // similar to vaadin.dependencies, in order to reduce conflicts with
        // the user overrides.

        // Collect all Vaadin overrides that need to be currently enabled
        final ObjectNode vaadinOverrides = getDefaultOverrides();

        // Add dependency locking overrides
        final JsonNode dependencies = packageJson.get(DEPENDENCIES);
        for (String dependency : JacksonUtils.getKeys(versionsJson)) {
            if (!vaadinOverrides.has(dependency) && shouldLockDependencyVersion(
                    dependency, dependencies, versionsJson)) {
                // Lock with a dependency reference
                vaadinOverrides.put(dependency, "$" + dependency);
            }
        }

        // Add platform versions
        final ObjectNode fullPlatformDependencies = getFullPlatformDependencies();
        final JsonNode devDependencies = packageJson.get(DEV_DEPENDENCIES);
        for (String dependency : JacksonUtils
                .getKeys(fullPlatformDependencies)) {
            try {
                FrontendVersion frontendVersion = new FrontendVersion(
                        fullPlatformDependencies.get(dependency).asString());
                if ("SNAPSHOT".equals(frontendVersion.getBuildIdentifier())) {
                    continue;
                }
                if (vaadinOverrides.has(dependency)
                        && vaadinOverrides.get(dependency).isString()
                        && vaadinOverrides.get(dependency).asString()
                                .startsWith("$")) {
                    // Already locked with a dependency reference, skip
                    continue;
                }
                if (dependencies.has(dependency)
                        || devDependencies.has(dependency)) {
                    // Skip platform overrides for existing dependencies
                    continue;
                }
                // Lock with a version number
                vaadinOverrides.put(dependency,
                        frontendVersion.getFullVersion());
            } catch (NumberFormatException nfe) {
                continue;
            }
        }

        // Flatten overrides to simplify diffing
        final Map<String, String> flatVaadinOverrides = flattenOverrides(
                vaadinOverrides);
        final ObjectNode lastVaadinOverrides = (ObjectNode) packageJson
                .get(VAADIN_DEP_KEY).get(OVERRIDES);
        final Map<String, String> flatLastVaadinOverrides = lastVaadinOverrides == null
                ? Map.of()
                : flattenOverrides(lastVaadinOverrides);

        boolean versionLockingUpdated = false;
        // Update overrides based on diff between current and last overrides
        final ObjectNode overridesSection = getOverridesSection(packageJson);
        for (final Map.Entry<String, String> entryToUpdate : flatVaadinOverrides
                .entrySet()) {
            final String lastValue = flatLastVaadinOverrides
                    .get(entryToUpdate.getKey());
            if (entryToUpdate.getValue().equals(
                    flatLastVaadinOverrides.get(entryToUpdate.getKey()))) {
                // Override value didn't change, skipping.
                continue;
            }
            final JsonNode lastUserValue;
            final List<String> keyPath = List
                    .of(entryToUpdate.getKey().split(">"));
            if (enablePnpm) {
                lastUserValue = overridesSection.get(entryToUpdate.getKey());
            } else {
                lastUserValue = JacksonUtils.getNestedKey(overridesSection,
                        keyPath);
            }
            boolean optOut = !Objects.equals(StringNode.valueOf(lastValue),
                    lastUserValue);
            if (optOut) {
                // Actual override value is different from last Vaadin override:
                // assume user opt-out and skip.
                continue;
            }
            versionLockingUpdated = true;
            if (enablePnpm) {
                // Use flat format for pnpm
                overridesSection.put(entryToUpdate.getKey(),
                        entryToUpdate.getValue());
            } else {
                putNestedOverride(overridesSection, keyPath,
                        entryToUpdate.getValue());
            }
        }
        for (final Map.Entry<String, String> entryToRemove : flatLastVaadinOverrides
                .entrySet()) {
            if (flatVaadinOverrides.containsKey(entryToRemove.getKey())) {
                // Override continues to exist, skipping.
                continue;
            }
            versionLockingUpdated = true;
            if (enablePnpm) {
                // Use flat format for pnpm
                overridesSection.remove(entryToRemove.getKey());
            }
            // Handle possibly nested overrides object
            final List<String> keyPath = List
                    .of(entryToRemove.getKey().split(">"));
            // Object format: { "dep": { ".": "1.0" } }
            final List<String> keyPathDotNested = Stream
                    .concat(keyPath.stream(), Stream.of(".")).toList();
            if (JacksonUtils.getNestedKey(overridesSection,
                    keyPathDotNested) != null) {
                JacksonUtils.removeNestedKey(overridesSection,
                        keyPathDotNested);
            }
            // Plain format: { "dep": "1.0" }
            if (JacksonUtils.getNestedKey(overridesSection, keyPath) != null) {
                JacksonUtils.removeNestedKey(overridesSection, keyPath);
            }
        }

        if (lastVaadinOverrides == null) {
            // Additional cleanup for overrides added before Vaadin overrides
            // section was introduced in PR #24008. Find and remove any obsolete
            // relative overrides.
            for (String overrideDependency : JacksonUtils
                    .getKeys(overridesSection)) {
                final boolean relativeOverride = overridesSection
                        .get(overrideDependency).stringValue("")
                        .startsWith("$");
                if (relativeOverride && !dependencies.has(overrideDependency)) {
                    overridesSection.remove(overrideDependency);
                }
            }
        }

        if (vaadinOverrides.isEmpty()) {
            // Clean up empty Vaadin overrides section
            ((ObjectNode) packageJson.get(VAADIN_DEP_KEY)).remove(OVERRIDES);
        } else {
            // Save Vaadin overrides section
            ((ObjectNode) packageJson.get(VAADIN_DEP_KEY)).set(OVERRIDES,
                    vaadinOverrides);
        }
        return versionLockingUpdated;
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

    private boolean shouldLockDependencyVersion(String dependency,
            JsonNode projectDependencies, JsonNode versionsJson) {
        String platformDefinedVersion = versionsJson.get(dependency).asString();

        if (isInternalPseudoDependency(platformDefinedVersion)) {
            return false;
        }

        if (projectDependencies.has(dependency)) {
            try {
                new FrontendVersion(
                        projectDependencies.get(dependency).asString());
            } catch (Exception e) {
                // Do not lock non-numeric versions, e.g. folder references
                return false;
            }
            return true;
        }

        return false;
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
