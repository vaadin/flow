package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FileIOUtils;
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
     *                task options
     */
    public static void copyPackageLockFromBundle(Options options) {
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
                Files.copy(devPackageLock.toPath(), packageLock.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
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
            Files.writeString(packageLock.toPath(), filecontents);
        } else {
            getLogger().debug(
                    "The '{}' file cannot be created because the dev-bundle JAR does not contain a suitable template.",
                    packageLockFile);
        }
    }

}
