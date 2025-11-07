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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.Constants;

import static com.vaadin.flow.server.Constants.DEV_BUNDLE_JAR_PATH;

/**
 * Build-time utility class for bundle operations.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class BuildBundleUtils {

    private BuildBundleUtils() {
        // Util methods only
    }

    /**
     * Copy package-lock.json/.yaml file from existing dev-bundle for building
     * new bundle.
     *
     * @param options
     *            task options
     */
    public static void copyPackageLockFromBundle(Options options) {
        String lockFile;
        if (options.isEnablePnpm()) {
            lockFile = Constants.PACKAGE_LOCK_YAML;
        } else {
            lockFile = Constants.PACKAGE_LOCK_JSON;
        }
        File packageLock = new File(options.getNpmFolder(), lockFile);
        if (packageLock.exists()) {
            // NO-OP due to existing package-lock
            return;
        }

        try {
            copyAppropriatePackageLock(options, packageLock);
        } catch (IOException ioe) {
            getLogger().error(
                    "Failed to copy existing `" + lockFile + "` to use", ioe);
        }

    }

    private static void copyAppropriatePackageLock(Options options,
            File packageLock) throws IOException {
        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        String packageLockFile = options.isEnablePnpm()
                ? Constants.PACKAGE_LOCK_YAML
                : Constants.PACKAGE_LOCK_JSON;
        if (devBundleFolder.exists()) {
            File devPackageLock = new File(devBundleFolder, packageLockFile);
            if (devPackageLock.exists()) {
                FileUtils.copyFile(devPackageLock, packageLock);
                return;
            }
        }
        boolean hillaUsed = FrontendUtils.isHillaUsed(
                options.getFrontendDirectory(), options.getClassFinder());
        URL resource = null;
        if (hillaUsed) {
            resource = options.getClassFinder().getResource(
                    DEV_BUNDLE_JAR_PATH + "hybrid-" + packageLockFile);
        }
        if (resource == null) {
            // If Hilla is in used but the hybrid lock file is not found in the
            // dev-bundle, fallback to the standard.
            // Could happen if Flow, dev-bundle and Vaadin maven plugin are not
            // in sync because of project configuration.
            if (hillaUsed) {
                getLogger().debug(
                        "The '{}' template for hybrid application could not be found in dev-bundle JAR. Fallback to standard template.",
                        packageLockFile);
            }
            resource = options.getClassFinder()
                    .getResource(DEV_BUNDLE_JAR_PATH + packageLockFile);
        }
        if (resource != null) {
            FileUtils.write(packageLock,
                    IOUtils.toString(resource, StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8);
        } else {
            getLogger().debug(
                    "The '{}' file cannot be created because the dev-bundle JAR does not contain a suitable template.",
                    packageLockFile);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BuildBundleUtils.class);
    }
}
