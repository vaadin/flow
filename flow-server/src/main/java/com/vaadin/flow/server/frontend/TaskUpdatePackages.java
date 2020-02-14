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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;

/**
 * Updates <code>package.json</code> by visiting {@link NpmPackage} annotations
 * found in the classpath. It also visits classes annotated with
 * {@link NpmPackage}
 *
 * @since 2.0
 */
public class TaskUpdatePackages extends NodeUpdater {

    private static final String VAADIN_FLOW_DEPS = "@vaadin/flow-deps";
    private static final String VERSION = "version";
    private static final String SHRINK_WRAP = "@vaadin/vaadin-shrinkwrap";
    private final boolean forceCleanUp;
    private final boolean enablePnpm;

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
     * @param flowResourcesPath
     *            folder where flow dependencies taken from resources files will
     *            be placed.
     *         folder where flow generated files will be placed.
     * @param forceCleanUp
     *            forces the clean up process to be run. If {@code false}, clean
     *            up will be performed when platform version update is detected.
     * @param enablePnpm
     *            if {@code true} then pnpm is used instead of npm, otherwise
     *            npm is used
     */
    TaskUpdatePackages(ClassFinder finder,
            FrontendDependenciesScanner frontendDependencies, File npmFolder,
            File generatedPath, File flowResourcesPath, boolean forceCleanUp, boolean enablePnpm) {
        super(finder, frontendDependencies, npmFolder, generatedPath, flowResourcesPath);
        this.forceCleanUp = forceCleanUp;
        this.enablePnpm = enablePnpm;
    }

    @Override
    public void execute() {
        try {
            Map<String, String> deps = frontDeps.getPackages();
            JsonObject packageJson = getPackageJson();
            modified = updatePackageJsonDependencies(packageJson, deps);


            if (modified) {
                writePackageFile(packageJson);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean updatePackageJsonDependencies(JsonObject packageJson,
            Map<String, String> deps) throws IOException {
        int added = 0;

        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        // Update the dependency for the folder with resources
        updateFrontendDependency(dependencies);

        // Add application dependencies
        for (Entry<String, String> dep : deps.entrySet()) {
            added += addDependency(packageJson, DEPENDENCIES, dep.getKey(),
                    dep.getValue());
        }

        if (added > 0) {
            log().info("Added {} dependencies to main package.json", added);
        }

        // Remove obsolete dependencies
        List<String> dependencyCollection = Stream
                .concat(deps.entrySet().stream(),
                        getDefaultDependencies().entrySet().stream())
                .map(Entry::getKey).collect(Collectors.toList());

        JsonObject vaadinDependencies = packageJson.getObject(VAADIN_DEP_KEY)
                .getObject(DEPENDENCIES);
        boolean doCleanUp = forceCleanUp;
        int removed = ensureVersionUpgrade(packageJson) ? 1 : 0;
        if (dependencies != null) {
            for (String key : dependencies.keys()) {
                if (!dependencyCollection.contains(key)
                        && vaadinDependencies.hasKey(key)) {
                    dependencies.remove(key);
                    log().debug("Removed \"{}\".", key);
                    removed++;
                }
            }
            doCleanUp = doCleanUp
                    || !enablePnpm && !ensureReleaseVersion(dependencies);
        }

        if (removed > 0) {
            log().info("Removed {} dependencies", removed);
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


    private int updateFrontendDependency(JsonObject json) {
        if (flowResourcesFolder != null
                // Skip if deps are copied directly to `node_modules` folder
                && !flowResourcesFolder.toString().contains(NODE_MODULES)) {

            String depsPkg = "./" + FrontendUtils.getUnixRelativePath(
                    npmFolder.getAbsoluteFile().toPath(),
                    flowResourcesFolder.getAbsoluteFile().toPath());
            if (!json.hasKey(DEP_NAME_FLOW_JARS) || !depsPkg.equals(json.getString(DEP_NAME_FLOW_JARS))) {
                json.put(DEP_NAME_FLOW_JARS, depsPkg);
                return 1;
            }
        } else {
            if (json.hasKey(DEP_NAME_FLOW_JARS)) {
                json.remove(DEP_NAME_FLOW_JARS);
                return 1;
            }
        }

        return 0;
    }

    /**
     * Compares vaadin-shrinkwrap dependency version from the
     * {@code dependencies} object with the current vaadin-shrinkwrap version
     * (retrieved from various sources like package.json, package-lock.json).
     * Removes package-lock.json file and node_modules,
     * target/frontend/node_modules folders in case the versions are different.
     *
     * @param dependencies
     *            dependencies object with the vaadin-shrinkwrap version
     * @throws IOException
     */
    private boolean ensureReleaseVersion(JsonObject dependencies)
            throws IOException {
        String shrinkWrapVersion = null;
        if (dependencies.hasKey(SHRINK_WRAP)) {
            shrinkWrapVersion = dependencies.getString(SHRINK_WRAP);
        }

        return Objects.equals(shrinkWrapVersion, getCurrentShrinkWrapVersion());
    }

    private boolean ensureVersionUpgrade(JsonObject packageJson)
            throws IOException {
        boolean result = false;
        /*
         * In modern Flow versions "@vaadin/flow-deps" should not exist.
         */
        if (packageJson.hasKey(DEPENDENCIES)) {
            JsonObject object = packageJson.getObject(DEPENDENCIES);
            if (object.hasKey(VAADIN_FLOW_DEPS)) {
                object.remove(VAADIN_FLOW_DEPS);
                result = true;
            }
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

        removeDir(nodeModulesFolder);

        if (flowResourcesFolder != null && flowResourcesFolder.exists()) {
            // Clean all files but `package.json`
            for (File file: flowResourcesFolder.listFiles()) {
                if (!file.getName().equals(PACKAGE_JSON)) {
                    file.delete();
                }
            }
        }

        File generatedNodeModules = new File(generatedFolder, NODE_MODULES);
        if (generatedNodeModules.exists()) {
            removeDir(generatedNodeModules);
        }
    }

    private void removeDir(File folder) throws IOException {
        FileUtils.deleteDirectory(folder);
    }

    private String getCurrentShrinkWrapVersion() throws IOException {
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
