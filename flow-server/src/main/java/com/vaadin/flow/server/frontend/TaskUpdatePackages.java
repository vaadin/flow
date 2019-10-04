/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Updates <code>package.json</code> by visiting {@link NpmPackage} annotations
 * found in the classpath. It also visits classes annotated with
 * {@link NpmPackage}
 *
 * @since 2.0
 */
public class TaskUpdatePackages extends NodeUpdater {

    static final String APP_PACKAGE_HASH = "vaadinAppPackageHash";
    private static final String VERSION = "version";
    private static final String SHRINK_WRAP = "@vaadin/vaadin-shrinkwrap";
    private boolean forceCleanUp;

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
     */
    TaskUpdatePackages(ClassFinder finder,
            FrontendDependenciesScanner frontendDependencies, File npmFolder,
            File generatedPath, boolean forceCleanUp) {
        super(finder, frontendDependencies, npmFolder, generatedPath);
        this.forceCleanUp = forceCleanUp;
    }

    @Override
    public void execute() {
        try {
            Map<String, String> deps = frontDeps.getPackages();
            JsonObject packageJson = getAppPackageJson();
            if (packageJson == null) {
                packageJson = Json.createObject();
            }
            boolean isModified = updatePackageJsonDependencies(packageJson,
                    deps);
            if (isModified) {
                writeAppPackageFile(packageJson);
            }
            modified = checkPackageHash(packageJson);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Check and update the main package hash in all cases as we might have
     * updated the main package with new dependencies.
     *
     * @param packageJson
     *            application package json
     * @return true if hash has changed
     * @throws IOException
     *             thrown from write exception
     */
    private boolean checkPackageHash(JsonObject packageJson)
            throws IOException {
        String content = "";
        // If we have dependencies generate hash on ordered content.
        if (packageJson.hasKey("dependencies")) {
            JsonObject dependencies = packageJson.getObject("dependencies");
            content = Stream.of(dependencies.keys())
                    .map(key -> String.format("\"%s\": \"%s\"", key,
                            dependencies.get(key).asString()))
                    .sorted(String::compareToIgnoreCase)
                    .collect(Collectors.joining(",\n  "));
        }
        return updateAppPackageHash(getHash(content));
    }

    private boolean updatePackageJsonDependencies(JsonObject packageJson,
            Map<String, String> deps) throws IOException {
        boolean added = false;

        // Add application dependencies
        for (Entry<String, String> dep : deps.entrySet()) {
            added = addDependency(packageJson, DEPENDENCIES, dep.getKey(),
                    dep.getValue()) || added;
        }

        // Remove obsolete dependencies
        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        boolean doCleanUp = forceCleanUp;
        if (dependencies != null) {
            for (String key : dependencies.keys()) {
                if (!deps.containsKey(key)) {
                    dependencies.remove(key);
                }
            }
            doCleanUp = doCleanUp || !ensureReleaseVersion(dependencies);
        }

        if (doCleanUp) {
            cleanUp();
        }

        return added;
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

    private void cleanUp() throws IOException {
        File packageLock = getPackageLock();
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

    private String getCurrentShrinkWrapVersion() throws IOException {
        String shrinkWrapVersion = getShrinkWrapVersion(getMainPackageJson());
        if (shrinkWrapVersion != null) {
            return shrinkWrapVersion;
        }

        shrinkWrapVersion = getShrinkWrapVersion(getAppPackageJson());
        if (shrinkWrapVersion != null) {
            return shrinkWrapVersion;
        }

        File flowDeps = new File(nodeModulesFolder, DEP_NAME_FLOW_DEPS);
        shrinkWrapVersion = getShrinkWrapVersion(
                getPackageJson(new File(flowDeps, Constants.PACKAGE_JSON)));
        if (shrinkWrapVersion != null) {
            return shrinkWrapVersion;
        }

        shrinkWrapVersion = getPackageLockShrinkWrapVersion();
        return shrinkWrapVersion;
    }

    private String getPackageLockShrinkWrapVersion() throws IOException {
        File packageLock = getPackageLock();
        if (!packageLock.exists()) {
            return null;
        }
        JsonObject packageLockJson = getPackageJson(packageLock);
        if (packageLockJson == null) {
            return null;
        }
        if (!packageLockJson.hasKey(DEPENDENCIES)) {
            return null;
        }
        JsonObject dependencies = packageLockJson.getObject(DEPENDENCIES);
        if (!dependencies.hasKey(SHRINK_WRAP)) {
            return null;
        }

        JsonObject shrinkWrap = dependencies.getObject(SHRINK_WRAP);
        if (shrinkWrap.hasKey(VERSION)) {
            return shrinkWrap.get(VERSION).asString();
        }
        return null;
    }

    private File getPackageLock() {
        return new File(npmFolder, "package-lock.json");
    }

    private String getShrinkWrapVersion(JsonObject packageJson)
            throws IOException {
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

    private String getHash(String content) {
        if (content.isEmpty()) {
            return content;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(
                    digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // Unrecoverable runime exception, it may not happen
            throw new RuntimeException(
                    "Unable to find a provider for SHA-256 algorithm", e);
        }
    }

    private boolean updateAppPackageHash(String hash) throws IOException {
        JsonObject mainContent = getMainPackageJson();
        if (mainContent == null) {
            mainContent = Json.createObject();
        }
        boolean modified = !mainContent.hasKey(APP_PACKAGE_HASH)
                || !hash.equals(mainContent.getString(APP_PACKAGE_HASH));
        if (modified) {
            mainContent.put(APP_PACKAGE_HASH, hash);
            writeMainPackageFile(mainContent);
        }
        return modified;
    }

    private String bytesToHex(byte[] hash) {
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
