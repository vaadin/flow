/*
 * Copyright 2000-2024 Vaadin Ltd.
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
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.FrontendUtils;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

/**
 * Goal that cleans the frontend files to a clean state.
 * <p>
 * Deletes Vaadin dependencies from package.json, the generated frontend folder
 * and the npm/pnpm-related files and folders:
 * <ul>
 * <li>node_modules
 * <li>pnpm-lock.yaml
 * <li>package-lock.json
 * </ul>
 *
 * @since 9.0
 */
@Mojo(name = "clean-frontend", defaultPhase = LifecyclePhase.PRE_CLEAN)
public class CleanFrontendMojo extends FlowModeAbstractMojo {

    public static final String VAADIN = "vaadin";
    public static final String DEPENDENCIES = "dependencies";
    public static final String DEV_DEPENDENCIES = "devDependencies";
    public static final String OVERRIDES = "overrides";

    public class Options {
        private boolean cleanPackageJson = true;

        private boolean removeDevBundle = true;
        private boolean removeFrontendGeneratedFolder = true;
        private boolean removeGeneratedTSFolder = true;
        private boolean removeNodeModules = true;
        private boolean removePackageLock = true;
        private boolean removePnpmFile = true;

        public Options withRemovePackageLock(boolean removePackageLock) {
            this.removePackageLock = removePackageLock;
            return this;
        }

        public Options withRemovePnpmFile(boolean removePnpmFile) {
            this.removePnpmFile = removePnpmFile;
            return this;
        }

        public Options withRemoveGeneratedTSFolder(
                boolean removeGeneratedTSFolder) {
            this.removeGeneratedTSFolder = removeGeneratedTSFolder;
            return this;
        }

        public Options withRemoveFrontendGeneratedFolder(
                boolean removeFrontendGeneratedFolder) {
            this.removeFrontendGeneratedFolder = removeFrontendGeneratedFolder;
            return this;
        }

        public Options withCleanPackageJson(boolean cleanPackageJson) {
            this.cleanPackageJson = cleanPackageJson;
            return this;
        }

        public Options withRemoveDevBundle(boolean removeDevBundle) {
            this.removeDevBundle = removeDevBundle;
            return this;
        }

        public Options withRemoveNodeModules(boolean removeNodeModules) {
            this.removeNodeModules = removeNodeModules;
            return this;
        }

        public boolean isRemovePackageLock() {
            return removePackageLock;
        }

        public boolean isRemovePnpmFile() {
            return removePnpmFile;
        }

        public boolean isRemoveGeneratedTSFolder() {
            return removeGeneratedTSFolder;
        }

        public boolean isRemoveFrontendGeneratedFolder() {
            return removeFrontendGeneratedFolder;
        }

        public boolean isCleanPackageJson() {
            return cleanPackageJson;
        }

        public boolean isRemoveDevBundle() {
            return removeDevBundle;
        }

        public boolean isRemoveNodeModules() {
            return removeNodeModules;
        }
    }

    @Override
    public void execute() throws MojoFailureException {
        runCleaning(new Options());
    }

    protected void runCleaning(Options options) throws MojoFailureException {
        if (options.isRemoveNodeModules()) {
            removeNodeModules();
        }

        if (options.isRemovePackageLock()) {
            // Cleanup (p)npm lock file.
            File lockFile = new File(npmFolder(), Constants.PACKAGE_LOCK_YAML);
            if (!lockFile.exists()) {
                lockFile = new File(npmFolder(), Constants.PACKAGE_LOCK_BUN);
            }
            if (!lockFile.exists()) {
                lockFile = new File(npmFolder(), Constants.PACKAGE_LOCK_JSON);
            }
            if (lockFile.exists()) {
                lockFile.delete();
            }
        }

        if (options.isRemovePnpmFile()) {
            // clean up .pnpmfile.cjs
            File pnpmfile = new File(npmFolder(), ".pnpmfile.cjs");
            if (pnpmfile.exists()) {
                pnpmfile.delete();
            }
        }

        // clean up generated files from frontend
        if (generatedTsFolder().exists()
                && options.isRemoveGeneratedTSFolder()) {
            try {
                FileUtils.deleteDirectory(generatedTsFolder());
            } catch (IOException exception) {
                throw new MojoFailureException(
                        "Failed to remove folder'"
                                + generatedTsFolder().getAbsolutePath() + "'",
                        exception);
            }
        }

        // cleanup hard-coded frontend generated folder
        // usually it is the same as generatedTsFolder, but if a custom fronted
        // folder is set all frontend generated files goes under
        // ${frontendDirectory}/generated
        File frontendGeneratedFolder = new File(frontendDirectory(),
                FrontendUtils.GENERATED);
        if (frontendGeneratedFolder.exists()
                && options.isRemoveFrontendGeneratedFolder()) {
            try {
                FileUtils.deleteDirectory(frontendGeneratedFolder);
            } catch (IOException exception) {
                throw new MojoFailureException("Failed to remove folder'"
                        + frontendGeneratedFolder.getAbsolutePath() + "'",
                        exception);
            }
        }

        try {
            // Clean up package json framework managed versions.
            File packageJsonFile = new File(npmFolder(), "package.json");
            if (packageJsonFile.exists() && options.isCleanPackageJson()) {
                JsonObject packageJson = Json.parse(FileUtils.readFileToString(
                        packageJsonFile, StandardCharsets.UTF_8.name()));

                cleanupPackage(packageJson);

                FileUtils.write(packageJsonFile,
                        JsonUtil.stringify(packageJson, 2) + "\n",
                        StandardCharsets.UTF_8.name());
            }
        } catch (IOException e) {
            throw new MojoFailureException(
                    "Failed to clean 'package.json' file", e);
        }

        if (options.removeDevBundle) {
            removeDevBundle();
        }
    }

    /**
     * Try removing the application bundles folder and old bundle folder, if
     * they exist.
     * <p>
     * Log a warning if there was an issue removing the folder.
     */
    private void removeDevBundle() {
        removeDevBundle(Constants.BUNDLE_LOCATION);
        // old bundle folder
        removeDevBundle("src/main/dev-bundle/");
    }

    /**
     * Try removing the given application bundle folder if it exists.
     * <p>
     * Log a warning if there was an issue removing the folder.
     */
    private void removeDevBundle(String bundleLocation) {
        File bundleDir = new File(npmFolder(), bundleLocation);
        try {
            if (bundleDir.exists()) {
                FileUtils.deleteDirectory(bundleDir);
            }
        } catch (IOException exception) {
            getLog().debug("Exception removing dev-bundle", exception);
            getLog().error("Failed to remove '" + bundleDir.getAbsolutePath()
                    + "'. Please remove it manually.");
        }
    }

    /**
     * Try to remove the node_modules folder.
     * <p>
     * Log a warning if there was an issue removing the folder.
     */
    private void removeNodeModules() {
        // Remove node_modules folder
        File nodeModules = new File(npmFolder(), "node_modules");
        try {
            FrontendUtils.deleteNodeModules(nodeModules);
        } catch (IOException exception) {
            getLog().debug("Exception removing node_modules", exception);
            getLog().error("Failed to remove '" + nodeModules.getAbsolutePath()
                    + "'. Please remove it manually.");
        }
    }

    private void cleanupPackage(JsonObject packageJson) {
        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        JsonObject devDependencies = packageJson.getObject(DEV_DEPENDENCIES);
        JsonObject overridesSection = packageJson.getObject(OVERRIDES);

        if (packageJson.hasKey(VAADIN)) {
            JsonObject vaadin = packageJson.getObject(VAADIN);
            JsonObject vaadinDependencies = vaadin.getObject(DEPENDENCIES);
            JsonObject vaadinDevDependencies = vaadin
                    .getObject(DEV_DEPENDENCIES);

            // Remove all
            cleanObject(dependencies, vaadinDependencies);
            cleanObject(devDependencies, vaadinDevDependencies);
            cleanObject(overridesSection, vaadinDependencies, false);

            packageJson.remove(VAADIN);
        }

        cleanFrameworkBuildDependenices(dependencies);
        cleanFrameworkBuildDependenices(devDependencies);

        // Remove the hash to get a npm install executed
        packageJson.remove("hash");

    }

    private void cleanObject(JsonObject target, JsonObject reference) {
        cleanObject(target, reference, true);
    }

    private void cleanObject(JsonObject target, JsonObject reference,
            boolean requireVersionsMatch) {
        if (target == null) {
            return;
        }
        Set<String> removeKeys = new HashSet<>();

        for (String key : target.keys()) {
            if (reference.hasKey(key) && (!requireVersionsMatch
                    || versionsMatch(target, reference, key))) {
                removeKeys.add(key);
            }
        }

        for (String key : removeKeys) {
            target.remove(key);
        }
    }

    private boolean versionsMatch(JsonObject target, JsonObject reference,
            String key) {
        return target.getString(key).equals(reference.getString(key));
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
    private void cleanFrameworkBuildDependenices(JsonObject dependencyObject) {
        if (dependencyObject == null) {
            return;
        }
        String buildTargetFolder = "./" + buildFolder();

        Set<String> removeKeys = new HashSet<>();
        for (String key : dependencyObject.keys()) {
            if (dependencyObject.getString(key).startsWith(buildTargetFolder)) {
                removeKeys.add(key);
            }
        }

        for (String key : removeKeys) {
            dependencyObject.remove(key);
        }
    }

}
