/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import elemental.json.JsonObject;
import elemental.json.JsonValue;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;

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
     *         a reusable class finder
     * @param frontendDependencies
     *         a reusable frontend dependencies
     * @param npmFolder
     *         folder with the `package.json` file
     * @param generatedPath
     *         folder where flow generated files will be placed.
     * @param forceCleanUp
     *         forces the clean up process to be run. If {@code false}, clean
     *         up will be performed when platform version update is detected.
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

        // Add application dependencies
        for (Entry<String, String> dep : deps.entrySet()) {
            added += addDependency(packageJson, DEPENDENCIES, dep.getKey(),
                    dep.getValue());
        }

        if (added > 0) {
            log().info("Added {} dependencies to '{}'", added,
                    new File(generatedFolder, PACKAGE_JSON).getPath());
        }

        // Remove obsolete dependencies
        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        List<String> dependencyCollection = Stream.concat(deps.entrySet().stream(),
                getDefaultDependencies().entrySet().stream())
                .map(Entry::getKey).collect(Collectors.toList());

        JsonObject vaadinDependencies = packageJson.getObject(VAADIN_DEP_KEY)
                .getObject(DEPENDENCIES);
        boolean doCleanUp = forceCleanUp;
        int removed = 0;
        if (dependencies != null) {
            for (String key : dependencies.keys()) {
                if (!dependencyCollection.contains(key) && vaadinDependencies.hasKey(key)) {
                    dependencies.remove(key);
                    log().debug("Removed \"{}\".", key);
                    removed++;
                }
            }
            doCleanUp = doCleanUp || !ensureReleaseVersion(dependencies);
        }

        if (added > 0) {
            log().info("Removed {} dependencies", removed);
        }

        if (doCleanUp) {
            cleanUp();
        }

        return added > 0 || removed > 0;
    }

    /**
     * Compares vaadin-shrinkwrap dependency version from the
     * {@code dependencies} object with the current vaadin-shrinkwrap version
     * (retrieved from various sources like package.json, package-lock.json).
     * Removes package-lock.json file and node_modules,
     * target/frontend/node_modules folders in case the versions are different.
     *
     * @param dependencies
     *         dependencies object with the vaadin-shrinkwrap version
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

    private String getCurrentShrinkWrapVersion() throws IOException {
        String shrinkWrapVersion = getShrinkWrapVersion(getPackageJson());
        if (shrinkWrapVersion != null) {
            return shrinkWrapVersion;
        }

        shrinkWrapVersion = getPackageLockShrinkWrapVersion();
        return shrinkWrapVersion;
    }

    private String getPackageLockShrinkWrapVersion() throws IOException {
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
        if (!dependencies.hasKey(SHRINK_WRAP)) {
            return null;
        }

        JsonObject shrinkWrap = dependencies.getObject(SHRINK_WRAP);
        if (shrinkWrap.hasKey(VERSION)) {
            return shrinkWrap.get(VERSION).asString();
        }
        return null;
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
}
