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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.Platform;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

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
     * @param finder
     *            a reusable class finder
     * @param frontendDependencies
     *            a reusable frontend dependencies
     * @param options
     *            the task options
     */
    TaskUpdatePackages(ClassFinder finder,
            FrontendDependenciesScanner frontendDependencies, Options options) {
        super(finder, frontendDependencies, options);
        this.jarResourcesFolder = options.getJarFrontendResourcesFolder();
        this.forceCleanUp = options.isCleanNpmFiles();
        this.enablePnpm = options.isEnablePnpm();
    }

    @Override
    public void execute() {
        try {
            Map<String, String> scannedApplicationDependencies = frontDeps
                    .getPackages();
            JsonObject packageJson = getPackageJson();
            modified = updatePackageJsonDependencies(packageJson,
                    scannedApplicationDependencies);
            generateVersionsJson(packageJson);
            boolean npmVersionLockingUpdated = lockVersionForNpm(packageJson);

            if (modified || npmVersionLockingUpdated) {
                writePackageFile(packageJson);
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    boolean lockVersionForNpm(JsonObject packageJson) throws IOException {
        boolean versionLockingUpdated = false;

        JsonObject overridesSection = getOverridesSection(packageJson);
        final JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        for (String dependency : versionsJson.keys()) {
            if (!overridesSection.hasKey(dependency)
                    && shouldLockDependencyVersion(dependency, dependencies,
                            versionsJson)) {
                overridesSection.put(dependency, "$" + dependency);
                versionLockingUpdated = true;
            }
        }
        final JsonObject devDependencies = packageJson
                .getObject(DEV_DEPENDENCIES);
        for (String dependency : overridesSection.keys()) {
            if (!dependencies.hasKey(dependency)
                    && !devDependencies.hasKey(dependency)
                    && overridesSection.getString(dependency).startsWith("$")) {
                overridesSection.remove(dependency);
                versionLockingUpdated = true;
            }
        }

        return versionLockingUpdated;
    }

    private boolean shouldLockDependencyVersion(String dependency,
            JsonObject projectDependencies, JsonObject versionsJson) {
        String platformDefinedVersion = versionsJson.getString(dependency);

        if (isInternalPseudoDependency(platformDefinedVersion)) {
            return false;
        }

        if (projectDependencies.hasKey(dependency)) {
            try {
                new FrontendVersion(projectDependencies.getString(dependency));
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

    private JsonObject getOverridesSection(JsonObject packageJson) {
        JsonObject overridesSection = packageJson.getObject(OVERRIDES);
        if (overridesSection == null) {
            overridesSection = Json.createObject();
            packageJson.put(OVERRIDES, overridesSection);
        }
        return overridesSection;
    }

    @Override
    String writePackageFile(JsonObject json) throws IOException {
        sortObject(json, DEPENDENCIES);
        sortObject(json, DEV_DEPENDENCIES);
        sortObject(json, VAADIN_DEP_KEY);
        return super.writePackageFile(json);
    }

    private void sortObject(JsonObject json, String key) {
        if (!json.hasKey(key)) {
            return;
        }
        JsonObject object = json.get(key);
        JsonObject ordered = orderKeys(object);
        Stream.of(object.keys()).forEach(object::remove);
        // add ordered keys back
        Stream.of(ordered.keys()).forEach(prop -> {
            JsonValue value = ordered.get(prop);
            object.put(prop, value);
        });
    }

    private JsonObject orderKeys(JsonObject object) {
        String[] keys = object.keys();
        Arrays.sort(keys);
        JsonObject result = Json.createObject();
        for (String key : keys) {
            JsonValue value = object.get(key);
            if (value instanceof JsonObject) {
                value = orderKeys((JsonObject) value);
            }
            result.put(key, value);
        }
        return result;
    }

    @SuppressWarnings("squid:S134")
    private boolean updatePackageJsonDependencies(JsonObject packageJson,
            Map<String, String> applicationDependencies) throws IOException {
        int added = 0;

        // Add application dependencies
        for (Entry<String, String> dep : applicationDependencies.entrySet()) {
            added += addDependency(packageJson, DEPENDENCIES, dep.getKey(),
                    dep.getValue());
        }

        /*
         * #10572 lock all platform internal versions
         */
        List<String> pinnedPlatformDependencies = new ArrayList<>();
        final JsonObject platformPinnedDependencies = getPlatformPinnedDependencies();
        for (String key : platformPinnedDependencies.keys()) {
            // need to double check that not overriding a scanned
            // dependency since add-ons should be able to downgrade
            // version through exclusion
            if (!applicationDependencies.containsKey(key)
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

        // Remove obsolete dependencies
        List<String> dependencyCollection = Stream
                .concat(applicationDependencies.entrySet().stream(),
                        getDefaultDependencies().entrySet().stream())
                .map(Entry::getKey).collect(Collectors.toList());
        dependencyCollection.addAll(pinnedPlatformDependencies);

        boolean doCleanUp = forceCleanUp; // forced only in tests
        int removed = removeLegacyProperties(packageJson);
        removed += cleanDependencies(dependencyCollection, packageJson,
                DEPENDENCIES);

        // FIXME do not do cleanup of node_modules every time platform is
        // updated ?
        doCleanUp = doCleanUp || (!enablePnpm && isPlatformVersionUpdated());

        // Remove obsolete devDependencies
        dependencyCollection = new ArrayList<>(
                getDefaultDevDependencies().keySet());

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

        String oldHash = packageJson.getObject(VAADIN_DEP_KEY)
                .getString(HASH_KEY);
        String newHash = generatePackageJsonHash(packageJson);
        // update packageJson hash value, if no changes it will not be written
        packageJson.getObject(VAADIN_DEP_KEY).put(HASH_KEY, newHash);

        return added > 0 || removed > 0 || removedDev > 0
                || !oldHash.equals(newHash);
    }

    private int cleanDependencies(List<String> dependencyCollection,
            JsonObject packageJson, String dependencyKey) {
        int removed = 0;

        JsonObject dependencyObject = packageJson.getObject(dependencyKey);
        JsonObject vaadinDependencyObject = packageJson
                .getObject(VAADIN_DEP_KEY).getObject(dependencyKey);
        if (dependencyObject != null) {
            for (String key : dependencyObject.keys()) {
                if (!dependencyCollection.contains(key)
                        && vaadinDependencyObject.hasKey(key)) {
                    dependencyObject.remove(key);
                    vaadinDependencyObject.remove(key);
                    log().debug("Removed \"{}\".", key);
                    removed++;
                }
            }
        }
        return removed;
    }

    protected static boolean pinPlatformDependency(JsonObject packageJson,
            JsonObject platformPinnedVersions, String pkg) {
        final FrontendVersion platformPinnedVersion = FrontendUtils
                .getPackageVersionFromJson(platformPinnedVersions, pkg,
                        "vaadin_dependencies.json");
        if (platformPinnedVersion == null) {
            return false;
        }

        final JsonObject vaadinDeps = packageJson.getObject(VAADIN_DEP_KEY)
                .getObject(DEPENDENCIES);
        final JsonObject packageJsonDeps = packageJson.getObject(DEPENDENCIES);
        // packages exist at this point
        assert vaadinDeps != null : "vaadin{ dependencies { } } should exist";
        assert packageJsonDeps != null : "dependencies { } should exist";

        FrontendVersion packageJsonVersion = null, vaadinDepsVersion = null;
        try {
            if (packageJsonDeps.hasKey(pkg)) {
                packageJsonVersion = new FrontendVersion(
                        packageJsonDeps.getString(pkg));
            }
        } catch (NumberFormatException e) {
            // Overridden to a file link in package.json, do not change
            return false;
        }
        try {
            if (vaadinDeps.hasKey(pkg)) {
                vaadinDepsVersion = new FrontendVersion(
                        vaadinDeps.getString(pkg));
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
     * Compares current platform version with the one last recorded as installed
     * in node_modules/.vaadin/vaadin_version. In case there was no existing
     * platform version recorder and node_modules exists, then platform is
     * considered updated.
     *
     * @return {@code true} if the version has changed, {@code false} if not
     * @throws IOException
     *             when file reading fails
     */
    private boolean isPlatformVersionUpdated() throws IOException {
        // if no record of current version is present, version is not
        // considered updated
        Optional<String> platformVersion = getVaadinVersion(finder);
        if (platformVersion.isPresent()
                && options.getNodeModulesFolder().exists()) {
            JsonObject vaadinJsonContents = getVaadinJsonContents();
            // If no record of previous version, version is considered updated
            if (!vaadinJsonContents.hasKey(NodeUpdater.VAADIN_VERSION)) {
                return true;
            }
            return !Objects.equals(
                    vaadinJsonContents.getString(NodeUpdater.VAADIN_VERSION),
                    platformVersion.get());
        }
        return false;
    }

    static Optional<String> getVaadinVersion(ClassFinder finder) {
        URL coreVersionsResource = finder
                .getResource(Constants.VAADIN_CORE_VERSIONS_JSON);

        if (coreVersionsResource == null) {
            return Optional.empty();
        }
        try (InputStream vaadinVersionsStream = coreVersionsResource
                .openStream()) {
            final JsonObject versionsJson = Json.parse(IOUtils
                    .toString(vaadinVersionsStream, StandardCharsets.UTF_8));
            if (versionsJson.hasKey("platform")) {
                return Optional.of(versionsJson.getString("platform"));
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(Platform.class)
                    .error("Unable to determine version information", e);
        }

        return Optional.empty();
    }

    /**
     * Cleans up any previous version properties from the packageJson object if
     * present.
     *
     * @param packageJson
     *            JsonObject of current package.json contents
     * @return amount of removed properties
     * @throws IOException
     *             thrown if removal of package-lock.json fails
     */
    private int removeLegacyProperties(JsonObject packageJson)
            throws IOException {
        int result = 0;
        /*
         * In modern Flow versions "@vaadin/flow-deps" should not exist.
         */
        if (packageJson.hasKey(DEPENDENCIES)) {
            JsonObject object = packageJson.getObject(DEPENDENCIES);
            if (object.hasKey(DEP_NAME_FLOW_DEPS)) {
                object.remove(DEP_NAME_FLOW_DEPS);
                log().debug("Removed \"{}\" as it's not generated anymore.",
                        DEP_NAME_FLOW_DEPS);
                result++;
            }
            if (object.hasKey(DEP_NAME_FLOW_JARS)) {
                object.remove(DEP_NAME_FLOW_JARS);
                log().debug("Removed \"{}\" as it's not needed anymore.",
                        DEP_NAME_FLOW_JARS);
                result++;
            }
        }
        if (packageJson.hasKey(VAADIN_APP_PACKAGE_HASH)) {
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
            FileUtils.forceDelete(getPackageLockFile());
        }
        return result;
    }

    private void cleanUp() throws IOException {
        FrontendUtils.deleteNodeModules(options.getNodeModulesFolder());

        if (jarResourcesFolder != null && jarResourcesFolder.exists()) {
            // This feels like cleanup done in the wrong place but is left here
            // for historical reasons
            for (File file : jarResourcesFolder.listFiles()) {
                file.delete();
            }
        }
    }

    private void deletePnpmLockFile() throws IOException {
        File lockFile = new File(options.getNpmFolder(), "pnpm-lock.yaml");
        if (lockFile.exists()) {
            FileUtils.forceDelete(lockFile);
        }
    }

    /**
     * Generate hash for package dependencies. This will consider both
     * 'dependencies' and 'devDependencies' of the packageJson format
     * JsonObject.
     * <p>
     * Dependencies will be sorted by key so that different runs for same
     * dependencies in different order will not trigger npm install.
     *
     * @param packageJson
     *            JsonObject built in the same format as package.json
     * @return has for dependencies and devDependencies
     */
    static String generatePackageJsonHash(JsonObject packageJson) {
        StringBuilder hashContent = new StringBuilder();
        if (packageJson.hasKey(DEPENDENCIES)) {
            JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
            hashContent.append("\"dependencies\": {");
            String sortedDependencies = Arrays.stream(dependencies.keys())
                    .sorted(String::compareToIgnoreCase)
                    .map(key -> String.format("\"%s\": \"%s\"", key,
                            dependencies.getString(key)))
                    .collect(Collectors.joining(",\n  "));
            hashContent.append(sortedDependencies);
            hashContent.append("}");
        }
        if (packageJson.hasKey(DEV_DEPENDENCIES)) {
            if (hashContent.length() > 0) {
                hashContent.append(",\n");
            }
            JsonObject devDependencies = packageJson
                    .getObject(DEV_DEPENDENCIES);
            hashContent.append("\"devDependencies\": {");
            String sortedDevDependencies = Arrays.stream(devDependencies.keys())
                    .sorted(String::compareToIgnoreCase)
                    .map(key -> String.format("\"%s\": \"%s\"", key,
                            devDependencies.getString(key)))
                    .collect(Collectors.joining(",\n  "));
            hashContent.append(sortedDevDependencies);
            hashContent.append("}");
        }
        return StringUtil.getHash(hashContent.toString());
    }

}
