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

/**
 * Options for cleaning the frontend files to a clean state.
 */
public class CleanOptions {

    private boolean cleanPackageJson = true;

    private boolean removeDevBundle = true;
    private boolean removeFrontendGeneratedFolder = true;
    private boolean removeGeneratedTSFolder = true;
    private boolean removeNodeModules = true;
    private boolean removePackageLock = true;
    private boolean removePnpmFile = true;

    /**
     * Set to false to keep package lock file (pnpm-lock.yaml or bun.lockb or
     * package-lock.json).
     *
     * @param removePackageLock
     *            whether to remove lock file
     * @return for chaining
     */
    public CleanOptions withRemovePackageLock(boolean removePackageLock) {
        this.removePackageLock = removePackageLock;
        return this;
    }

    /**
     * Set to false to keep the .pnpmfile.cjs file as is.
     *
     * @param removePnpmFile
     *            whether to remove .pnpmfile.cjs file
     * @return for chaining
     */
    public CleanOptions withRemovePnpmFile(boolean removePnpmFile) {
        this.removePnpmFile = removePnpmFile;
        return this;
    }

    /**
     * Set to false to keep the generated ts folder as is.
     *
     * @param removeGeneratedTSFolder
     *            whether to remove the generated ts folder
     * @return for chaining
     */
    public CleanOptions withRemoveGeneratedTSFolder(
            boolean removeGeneratedTSFolder) {
        this.removeGeneratedTSFolder = removeGeneratedTSFolder;
        return this;
    }

    /**
     * Set to false to keep the frontend generated folder as is.
     *
     * @param removeFrontendGeneratedFolder
     *            whether to remove the frontend generated folder
     * @return for chaining
     */
    public CleanOptions withRemoveFrontendGeneratedFolder(
            boolean removeFrontendGeneratedFolder) {
        this.removeFrontendGeneratedFolder = removeFrontendGeneratedFolder;
        return this;
    }

    /**
     * Set to false to keep the package.json file as is.
     *
     * @param cleanPackageJson
     *            whether to clean the package.json file
     * @return for chaining
     */
    public CleanOptions withCleanPackageJson(boolean cleanPackageJson) {
        this.cleanPackageJson = cleanPackageJson;
        return this;
    }

    /**
     * Set to false to keep the dev bundle as is.
     *
     * @param removeDevBundle
     *            whether to remove the dev bundle
     * @return for chaining
     */
    public CleanOptions withRemoveDevBundle(boolean removeDevBundle) {
        this.removeDevBundle = removeDevBundle;
        return this;
    }

    /**
     * Set to false to keep the node_modules folder as is.
     *
     * @param removeNodeModules
     *            whether to remove the node_modules folder
     * @return for chaining
     */
    public CleanOptions withRemoveNodeModules(boolean removeNodeModules) {
        this.removeNodeModules = removeNodeModules;
        return this;
    }

    /**
     * @return whether to remove package lock file (pnpm-lock.yaml or bun.lockb
     *         or package-lock.json)
     */
    public boolean isRemovePackageLock() {
        return removePackageLock;
    }

    /**
     * @return whether to remove .pnpmfile.cjs file
     */
    public boolean isRemovePnpmFile() {
        return removePnpmFile;
    }

    /**
     * @return whether to remove the generated ts folder where flow will put TS
     *         API files for client projects.
     */
    public boolean isRemoveGeneratedTSFolder() {
        return removeGeneratedTSFolder;
    }

    /**
     * @return whether to remove the frontend generated folder
     */
    public boolean isRemoveFrontendGeneratedFolder() {
        return removeFrontendGeneratedFolder;
    }

    /**
     * @return whether to clean the package.json file
     */
    public boolean isCleanPackageJson() {
        return cleanPackageJson;
    }

    /**
     * @return whether to remove the dev bundle
     */
    public boolean isRemoveDevBundle() {
        return removeDevBundle;
    }

    /**
     * @return whether to remove the node_modules folder
     */
    public boolean isRemoveNodeModules() {
        return removeNodeModules;
    }
}
