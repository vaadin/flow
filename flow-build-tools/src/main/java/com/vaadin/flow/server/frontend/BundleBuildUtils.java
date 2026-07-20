/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.FileIOUtils;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;

import static com.vaadin.flow.server.Constants.DEV_BUNDLE_JAR_PATH;

public class BundleBuildUtils {

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BundleBuildUtils.class);
    }

    /**
     * Copy package-lock.json/.yaml file from existing dev-bundle for building
     * new bundle.
     *
     * @param options
     *            task options
     */
    public static void copyPackageLockFromBundle(Options options) {
        copyPackageLockFromBundle(options, FrontendTools.fromOptions(options));
    }

    static void copyPackageLockFromBundle(Options options,
            FrontendTools frontendTools) {
        try {
            if (FrontendBuildUtils.isPlatformMajorVersionUpdated(
                    options.getClassFinder(), options.getNodeModulesFolder(),
                    options.getNpmFolder(), options.getBuildDirectory())) {
                getLogger().info(
                        "Platform version updated. Skipping bundle lock file copy.");
                return;
            }
        } catch (IOException ioe) {
            getLogger().debug("Failed to validate platform version change.",
                    ioe);
        }
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
            copyAppropriatePackageLock(options, packageLock, frontendTools);
        } catch (IOException ioe) {
            getLogger().error(
                    "Failed to copy existing `" + lockFile + "` to use", ioe);
        }

    }

    private static void copyAppropriatePackageLock(Options options,
            File packageLock, FrontendTools frontendTools) throws IOException {
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
                writePackageLock(Files.readString(devPackageLock.toPath()),
                        packageLock, options, frontendTools);
                return;
            }
        }
        boolean hillaUsed = FrontendBuildUtils.isHillaUsed(
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
            String filecontents = FileIOUtils.urlToString(resource);
            writePackageLock(filecontents, packageLock, options, frontendTools);
        } else {
            getLogger().debug(
                    "The '{}' file cannot be created because the dev-bundle JAR does not contain a suitable template.",
                    packageLockFile);
        }
    }

    /**
     * Writes the seeded lock file, stripping the hardcoded {@code resolved}
     * download URLs for the npm {@code package-lock.json} when npm is
     * configured with a custom registry, so that {@code npm install}
     * re-resolves them against that registry instead of reusing the
     * {@code npmjs.org} URLs baked into the dev-bundle template. The
     * {@code integrity} content hashes are kept so npm still verifies the
     * downloaded tarballs against the versions Vaadin tested. In all other
     * cases the content is written verbatim.
     */
    private static void writePackageLock(String contents, File packageLock,
            Options options, FrontendTools frontendTools) throws IOException {
        String toWrite = contents;
        if (!options.isEnablePnpm()
                && frontendTools.hasCustomNpmRegistry(options.getNpmFolder())) {
            toWrite = stripResolvedUrls(contents, packageLock);
        }
        Files.writeString(packageLock.toPath(), toWrite);
    }

    private static String stripResolvedUrls(String contents, File packageLock) {
        try {
            ObjectNode root = JacksonUtils.readTree(contents);
            removeResolvedEntries(root.get("packages"));
            return JacksonUtils.toFileJson(root);
        } catch (RuntimeException e) {
            getLogger().debug(
                    "Could not process '{}' to strip registry URLs; copying it verbatim.",
                    packageLock.getName(), e);
            return contents;
        }
    }

    private static void removeResolvedEntries(JsonNode section) {
        if (section instanceof ObjectNode entries) {
            for (String name : entries.propertyNames()) {
                if (entries.get(name) instanceof ObjectNode entry) {
                    entry.remove("resolved");
                }
            }
        }
    }

}
