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
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import org.apache.commons.io.FileUtils;

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

    private static final String VERSION = "version";
    private static final String SHRINK_WRAP = "@vaadin/vaadin-shrinkwrap";
    protected static final String VAADIN_APP_PACKAGE_HASH = "vaadinAppPackageHash";
    protected static final String VAADIN_FLOW_DEPS = "@vaadin/flow-deps";
    private final boolean forceCleanUp;
    private final boolean enablePnpm;

    private static class RemoveFileVisitor extends SimpleFileVisitor<Path>
            implements Serializable {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            Files.delete(file);
            return super.visitFile(file, attrs);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
            Files.delete(dir);
            return super.postVisitDirectory(dir, exc);
        }
    }

    /**
     * Create an instance of the updater given all configurable parameters.
     *
     * @param finder
     *            a reusable class finder
     * @param frontendDependencies
     *            a reusable frontend dependencies
     * @param npmFolder
     *            folder with the `package.json` file
     * @param generatedPath
     *            folder where flow generated files will be placed.
     * @param forceCleanUp
     *            forces the clean up process to be run. If {@code false}, clean
     *            up will be performed when platform version update is detected.
     * @param enablePnpm
     *            if {@code true} then pnpm is used instead of npm, otherwise
     *            npm is used
     */
    TaskUpdatePackages(ClassFinder finder,
            FrontendDependenciesScanner frontendDependencies, File npmFolder,
            File generatedPath, boolean forceCleanUp, boolean enablePnpm) {
        super(finder, frontendDependencies, npmFolder, generatedPath);
        this.forceCleanUp = forceCleanUp;
        this.enablePnpm = enablePnpm;
    }

    @Override
    public void execute() {
        try {
            Map<String, String> scannedApplicationDependencies = frontDeps
                    .getPackages();
            JsonObject packageJson = getPackageJson();
            modified = updatePackageJsonDependencies(packageJson,
                    scannedApplicationDependencies);

            if (modified) {
                writePackageFile(packageJson);

                if (enablePnpm) {
                    // With pnpm dependency versions are pinned via pnpmfile.js
                    // (instead of @vaadin/vaadin-shrinkwrap). When updating
                    // a dependency in package.json, the old version may be
                    // left in the pnpm-lock.yaml file, causing duplicate
                    // dependencies. Work around this issue by deleting
                    // pnpm-lock.yaml ("pnpm install" will re-generate).
                    // For details, see:
                    // https://github.com/pnpm/pnpm/issues/2587
                    // https://github.com/vaadin/flow/issues/9719
                    deletePnpmLockFile();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    private boolean updatePackageJsonDependencies(JsonObject packageJson,
            Map<String, String> applicationDependencies) throws IOException {
        int added = 0;

        // Add application dependencies
        for (Entry<String, String> dep : applicationDependencies.entrySet()) {
            added += addDependency(packageJson, DEPENDENCIES, dep.getKey(),
                    dep.getValue());
        }

        /*
         * #10572 lock all platform internal versions for npm
         */
        List<String> pinnedPlatformDependencies = new ArrayList<>();
        if (!enablePnpm) {
            final JsonObject platformPinnedDependencies = getPlatformPinnedDependencies();
            if (platformPinnedDependencies != null) {
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
            }
        }

        if (added > 0) {
            log().debug("Added {} dependencies to main package.json", added);
        }

        // Remove obsolete dependencies
        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        List<String> dependencyCollection = Stream
                .concat(applicationDependencies.entrySet().stream(),
                        getDefaultDependencies().entrySet().stream())
                .map(Entry::getKey).collect(Collectors.toList());
        dependencyCollection.addAll(pinnedPlatformDependencies);

        boolean doCleanUp = forceCleanUp; // forced only in tests
        int removed = removeLegacyProperties(packageJson);
        removed += cleanDependencies(dependencyCollection, packageJson,
                DEPENDENCIES);
        if (dependencies != null) {
            // FIXME do not do cleanup of node_modules every time platform is
            // updated ?
            doCleanUp = doCleanUp
                    || !enablePnpm && isPlatformVersionUpdated(dependencies);
        }

        // Remove obsolete devDependencies
        dependencyCollection = new ArrayList<>(
                getDefaultDevDependencies().keySet());

        int removedDev = cleanDependencies(dependencyCollection, packageJson,
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

        return added > 0 || removed > 0 || !oldHash.equals(newHash);
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

    private boolean pinPlatformDependency(JsonObject packageJson,
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
        assert vaadinDeps != null :  "vaadin{ dependencies { } } should exist"; // exists at this point
        assert packageJsonDeps != null : "dependencies { } should exist";
        if (!packageJsonDeps.hasKey(pkg) || !vaadinDeps.hasKey(pkg)
                || !platformPinnedVersion.equals(
                        new FrontendVersion(packageJsonDeps.getString(pkg)))
                || !platformPinnedVersion.equals(
                        new FrontendVersion(vaadinDeps.getString(pkg)))) {
            packageJsonDeps.put(pkg, platformPinnedVersion.getFullVersion());
            vaadinDeps.put(pkg, platformPinnedVersion.getFullVersion());
            return true;
        }
        return false;
    }

    /**
     * Compares vaadin-shrinkwrap dependency version (which is the same as
     * platform version) from the {@code dependencies} object with the current
     * vaadin-shrinkwrap version (retrieved from file system: package.json,
     * package-lock.json). In case there was no existing shrinkwrap version,
     * then version is considered updated.
     *
     * @param dependencies
     *            dependencies object with the vaadin-shrinkwrap version
     * @return {@code true} if the version has changed, {@code false} if not
     * @throws IOException
     *             when file reading fails
     */
    private boolean isPlatformVersionUpdated(JsonObject dependencies)
            throws IOException {
        String shrinkWrapVersion = null;
        if (dependencies.hasKey(SHRINK_WRAP)) {
            shrinkWrapVersion = dependencies.getString(SHRINK_WRAP);
        }

        final String existingShrinkWrapVersion = getExistingShrinkWrapVersion();
        // if no existing shrinkwrap version is present, version is not
        // "updated"
        return !Objects.equals(shrinkWrapVersion, existingShrinkWrapVersion);
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
            if (object.hasKey(VAADIN_FLOW_DEPS)) {
                object.remove(VAADIN_FLOW_DEPS);
                log().debug("Removed \"{}\" as it's not generated anymore.",
                        VAADIN_FLOW_DEPS);
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
        File packageLock = getPackageLockFile();
        if (packageLock.exists()) {
            if (!packageLock.delete()) {
                throw new IOException("Could not remove "
                        + packageLock.getPath() + " file. "
                        + "This file has been generated with a different platform version. Try to remove it manually.");
            }
        }
        if (nodeModulesFolder.exists()) {
            removeDir(nodeModulesFolder);
        }
        File generatedNodeModules = new File(generatedFolder,
                FrontendUtils.NODE_MODULES);
        if (generatedNodeModules.exists()) {
            removeDir(generatedNodeModules);
        }
    }

    private void removeDir(File file) throws IOException {
        Files.walkFileTree(file.toPath(), new RemoveFileVisitor());
    }

    private String getExistingShrinkWrapVersion() throws IOException {
        String shrinkWrapVersion = getShrinkWrapVersion(getPackageJson());
        if (shrinkWrapVersion != null) {
            return shrinkWrapVersion;
        }

        shrinkWrapVersion = getPackageLockShrinkWrapVersion();
        return shrinkWrapVersion;
    }

    private String getPackageLockShrinkWrapVersion() throws IOException {
        JsonObject dependencies = getPackageLockDependencies();
        if (dependencies == null) {
            return null;
        }

        if (!dependencies.hasKey(SHRINK_WRAP)) {
            return null;
        }
        JsonObject shrinkWrap = dependencies.getObject(SHRINK_WRAP);
        if (shrinkWrap.hasKey(VERSION)) {
            return shrinkWrap.get(VERSION).asString();
        }
        return null;
    }

    private JsonObject getPackageLockDependencies() throws IOException {
        File packageLock = getPackageLockFile();
        if (!packageLock.exists()) {
            return null;
        }
        JsonObject packageLockJson = getJsonFileContent(packageLock);
        if (packageLockJson == null) {
            return null;
        }
        if (!packageLockJson.hasKey(DEPENDENCIES)) {
            return null;
        }
        JsonObject dependencies = packageLockJson.getObject(DEPENDENCIES);
        return dependencies;
    }

    private File getPackageLockFile() {
        return new File(npmFolder, "package-lock.json");
    }

    private String getShrinkWrapVersion(JsonObject packageJson) {
        if (packageJson == null) {
            return null;
        }
        if (packageJson.hasKey(DEPENDENCIES)) {
            JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
            if (dependencies.hasKey(SHRINK_WRAP)) {
                JsonValue value = dependencies.get(SHRINK_WRAP);
                return value.asString();
            }
        }
        return null;
    }

    private void deletePnpmLockFile() throws IOException {
        File lockFile = new File(npmFolder, "pnpm-lock.yaml");
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
        return getHash(hashContent.toString());
    }

    private static String getHash(String content) {
        if (content.isEmpty()) {
            return content;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(
                    digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // Unrecoverable runtime exception, that can not happen
            throw new RuntimeException(
                    "Unable to find a provider for SHA-256 algorithm", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder result = new StringBuilder();
        for (byte bit : hash) {
            String hex = Integer.toHexString(0xff & bit);
            if (hex.length() == 1) {
                result.append('0');
            }
            result.append(hex);
        }
        return result.toString();
    }

}
