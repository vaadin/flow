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

package com.vaadin.flow.plugin.base;

/**
 *
 */
public class CleanOptions {

    private boolean cleanPackageJson = true;

    private boolean removeDevBundle = true;
    private boolean removeFrontendGeneratedFolder = true;
    private boolean removeGeneratedTSFolder = true;
    private boolean removeNodeModules = true;
    private boolean removePackageLock = true;
    private boolean removePnpmFile = true;

    public CleanOptions withRemovePackageLock(boolean removePackageLock) {
        this.removePackageLock = removePackageLock;
        return this;
    }

    public CleanOptions withRemovePnpmFile(boolean removePnpmFile) {
        this.removePnpmFile = removePnpmFile;
        return this;
    }

    public CleanOptions withRemoveGeneratedTSFolder(
            boolean removeGeneratedTSFolder) {
        this.removeGeneratedTSFolder = removeGeneratedTSFolder;
        return this;
    }

    public CleanOptions withRemoveFrontendGeneratedFolder(
            boolean removeFrontendGeneratedFolder) {
        this.removeFrontendGeneratedFolder = removeFrontendGeneratedFolder;
        return this;
    }

    public CleanOptions withCleanPackageJson(boolean cleanPackageJson) {
        this.cleanPackageJson = cleanPackageJson;
        return this;
    }

    public CleanOptions withRemoveDevBundle(boolean removeDevBundle) {
        this.removeDevBundle = removeDevBundle;
        return this;
    }

    public CleanOptions withRemoveNodeModules(boolean removeNodeModules) {
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
