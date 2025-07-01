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

package com.vaadin.flow.plugin.base;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.FrontendUtils;

/**
 * Utility class for cleaning the frontend files to a clean state.
 */
public class CleanFrontendUtil {

    public static final String VAADIN = "vaadin";
    public static final String DEPENDENCIES = "dependencies";
    public static final String DEV_DEPENDENCIES = "devDependencies";
    public static final String OVERRIDES = "overrides";
    public static final String PNPM = "pnpm";

    /**
     * Exception thrown when cleaning the frontend fails.
     */
    public static class CleanFrontendException extends Exception {
        public CleanFrontendException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Cleans the frontend files to a clean state.
     * <p>
     * Deletes Vaadin dependencies from package.json, the generated frontend
     * folder and the npm/pnpm-related files and folders:
     * <ul>
     * <li>node_modules
     * <li>pnpm-lock.yaml
     * <li>package-lock.json
     * </ul>
     */
    public static void runCleaning(PluginAdapterBase adapter,
            CleanOptions options) throws CleanFrontendException {
        if (FrontendUtils.isHillaUsed(adapter.frontendDirectory(),
                adapter.getClassFinder())) {
            options.withRemovePackageLock(false).withRemoveNodeModules(false);
        }

        if (options.isRemoveNodeModules()) {
            removeNodeModules(adapter);
        }
        if (options.isRemovePackageLock()) {
            // Cleanup (p)npm lock file.
            File lockFile = new File(adapter.npmFolder(),
                    Constants.PACKAGE_LOCK_YAML);
            if (!lockFile.exists()) {
                lockFile = new File(adapter.npmFolder(),
                        Constants.PACKAGE_LOCK_BUN);
            }
            if (!lockFile.exists()) {
                lockFile = new File(adapter.npmFolder(),
                        Constants.PACKAGE_LOCK_BUN_1_2);
            }
            if (!lockFile.exists()) {
                lockFile = new File(adapter.npmFolder(),
                        Constants.PACKAGE_LOCK_JSON);
            }
            if (lockFile.exists()) {
                lockFile.delete();
            }
        }

        if (options.isRemovePnpmFile()) {
            // clean up .pnpmfile.cjs
            File pnpmfile = new File(adapter.npmFolder(), ".pnpmfile.cjs");
            if (pnpmfile.exists()) {
                pnpmfile.delete();
            }
        }

        // clean up generated files from frontend
        if (adapter.generatedTsFolder().exists()
                && options.isRemoveGeneratedTSFolder()) {
            try {
                FileUtils.deleteDirectory(adapter.generatedTsFolder());
            } catch (IOException exception) {
                throw new CleanFrontendException("Failed to remove folder'"
                        + adapter.generatedTsFolder().getAbsolutePath() + "'",
                        exception);
            }
        }

        // cleanup hard-coded frontend generated folder
        // usually it is the same as generatedTsFolder, but if a custom fronted
        // folder is set all frontend generated files goes under
        // ${frontendDirectory}/generated
        File frontendGeneratedFolder = new File(adapter.frontendDirectory(),
                FrontendUtils.GENERATED);
        if (frontendGeneratedFolder.exists()
                && options.isRemoveFrontendGeneratedFolder()) {
            try {
                FileUtils.deleteDirectory(frontendGeneratedFolder);
            } catch (IOException exception) {
                throw new CleanFrontendException("Failed to remove folder'"
                        + frontendGeneratedFolder.getAbsolutePath() + "'",
                        exception);
            }
        }

        try {
            // Clean up package json framework managed versions.
            File packageJsonFile = new File(adapter.npmFolder(),
                    "package.json");
            if (packageJsonFile.exists() && options.isCleanPackageJson()) {
                ObjectNode packageJson = JacksonUtils
                        .readTree(FileUtils.readFileToString(packageJsonFile,
                                StandardCharsets.UTF_8.name()));

                cleanupPackage(adapter, packageJson);

                FileUtils.write(packageJsonFile,
                        packageJson.toPrettyString() + "\n",
                        StandardCharsets.UTF_8.name());
            }
        } catch (IOException e) {
            throw new CleanFrontendException(
                    "Failed to clean 'package.json' file", e);
        }

        if (options.isRemoveDevBundle()) {
            removeDevBundle(adapter);
        }
    }

    /**
     * Try removing the application bundles folder and old bundle folder, if
     * they exist.
     * <p>
     * Log a warning if there was an issue removing the folder.
     */
    private static void removeDevBundle(PluginAdapterBase adapter) {
        removeDevBundle(adapter, Constants.BUNDLE_LOCATION);
        // old bundle folder
        removeDevBundle(adapter, "src/main/dev-bundle/");
    }

    /**
     * Try removing the given application bundle folder if it exists.
     * <p>
     * Log a warning if there was an issue removing the folder.
     */
    private static void removeDevBundle(PluginAdapterBase adapter,
            String bundleLocation) {
        File bundleDir = new File(adapter.npmFolder(), bundleLocation);
        try {
            if (bundleDir.exists()) {
                FileUtils.deleteDirectory(bundleDir);
            }
        } catch (IOException exception) {
            adapter.logDebug("Exception removing dev-bundle", exception);
            adapter.logError("Failed to remove '" + bundleDir.getAbsolutePath()
                    + "'. Please remove it manually.");
        }
    }

    /**
     * Try to remove the node_modules folder.
     * <p>
     * Log a warning if there was an issue removing the folder.
     */
    private static void removeNodeModules(PluginAdapterBase adapter) {
        // Remove node_modules folder
        File nodeModules = new File(adapter.npmFolder(), "node_modules");
        try {
            FrontendUtils.deleteNodeModules(nodeModules);
        } catch (IOException exception) {
            adapter.logDebug("Exception removing node_modules", exception);
            adapter.logError(
                    "Failed to remove '" + nodeModules.getAbsolutePath()
                            + "'. Please remove it manually.");
        }
    }

    private static void cleanupPackage(PluginAdapterBase adapter,
            ObjectNode packageJson) {
        ObjectNode dependencies = (ObjectNode) packageJson.get(DEPENDENCIES);
        ObjectNode devDependencies = (ObjectNode) packageJson
                .get(DEV_DEPENDENCIES);
        ObjectNode overridesSection = (ObjectNode) packageJson.get(OVERRIDES);
        ObjectNode pnpmOverridesSection = packageJson.has(PNPM)
                ? (ObjectNode) packageJson.get(PNPM).get(OVERRIDES)
                : null;

        if (packageJson.has(VAADIN)) {
            ObjectNode vaadin = (ObjectNode) packageJson.get(VAADIN);
            ObjectNode vaadinDependencies = (ObjectNode) vaadin
                    .get(DEPENDENCIES);
            ObjectNode vaadinDevDependencies = (ObjectNode) vaadin
                    .get(DEV_DEPENDENCIES);

            // Remove all
            cleanObject(dependencies, vaadinDependencies);
            cleanObject(devDependencies, vaadinDevDependencies);
            cleanObject(overridesSection, vaadinDependencies, false);
            cleanObject(pnpmOverridesSection, vaadinDependencies, false);

            packageJson.remove(VAADIN);
        }

        cleanFrameworkBuildDependenices(adapter, dependencies);
        cleanFrameworkBuildDependenices(adapter, devDependencies);

        // Remove the hash to get a npm install executed
        packageJson.remove("hash");

    }

    private static void cleanObject(ObjectNode target, ObjectNode reference) {
        cleanObject(target, reference, true);
    }

    private static void cleanObject(ObjectNode target, ObjectNode reference,
            boolean requireVersionsMatch) {
        if (target == null) {
            return;
        }
        Set<String> removeKeys = new HashSet<>();

        for (String key : JacksonUtils.getKeys(target)) {
            if (reference.has(key) && (!requireVersionsMatch
                    || versionsMatch(target, reference, key))) {
                removeKeys.add(key);
            }
        }

        for (String key : removeKeys) {
            target.remove(key);
        }
    }

    private static boolean versionsMatch(ObjectNode target,
            ObjectNode reference, String key) {
        return target.get(key).textValue()
                .equals(reference.get(key).textValue());
    }

    /**
     * Clean any dependencies that target the build folder in the given json
     * object.
     * <p>
     * With default settings it would mean all starting with {@code ./target}.
     *
     * @param dependencyObject
     *            json object to clean
     */
    private static void cleanFrameworkBuildDependenices(
            PluginAdapterBase adapter, ObjectNode dependencyObject) {
        if (dependencyObject == null) {
            return;
        }
        String buildTargetFolder = "./" + adapter.buildFolder();

        Set<String> removeKeys = new HashSet<>();
        for (String key : JacksonUtils.getKeys(dependencyObject)) {
            if (dependencyObject.get(key).textValue()
                    .startsWith(buildTargetFolder)) {
                removeKeys.add(key);
            }
        }

        for (String key : removeKeys) {
            dependencyObject.remove(key);
        }
    }
}
