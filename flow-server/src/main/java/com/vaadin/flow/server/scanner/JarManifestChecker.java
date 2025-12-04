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
package com.vaadin.flow.server.scanner;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for checking if JAR files contain the Vaadin package version
 * manifest attribute.
 * <p>
 * This is used by the annotation scanner to determine which JARs should be
 * scanned for Vaadin annotations. Only JARs with the
 * {@code Vaadin-Package-Version} manifest attribute are considered Vaadin
 * add-ons.
 * <p>
 * Results are cached to avoid repeated JAR reads for performance.
 *
 * @since 25.0
 */
public class JarManifestChecker {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(JarManifestChecker.class);

    /**
     * The manifest attribute name that identifies a Vaadin add-on package.
     */
    public static final String VAADIN_PACKAGE_VERSION = "Vaadin-Package-Version";

    private static final ConcurrentHashMap<File, Boolean> manifestCache = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private JarManifestChecker() {
        // Utility class
    }

    /**
     * Checks if a JAR file has the Vaadin-Package-Version manifest attribute.
     * <p>
     * Results are cached to avoid repeated JAR reads. The cache uses the File
     * object as the key, so if the same file is checked multiple times, the
     * manifest is only read once.
     *
     * @param jarFile
     *            the JAR file to check, not null
     * @return {@code true} if the JAR has a manifest with the
     *         Vaadin-Package-Version attribute, {@code false} otherwise
     */
    public static boolean hasVaadinManifest(File jarFile) {
        if (jarFile == null) {
            return false;
        }
        return manifestCache.computeIfAbsent(jarFile,
                JarManifestChecker::checkManifest);
    }

    private static boolean checkManifest(File jarFile) {
        if (!jarFile.exists() || !jarFile.isFile()) {
            LOGGER.debug("File does not exist or is not a file: {}", jarFile);
            return false;
        }

        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                LOGGER.trace("No manifest found in {}", jarFile);
                return false;
            }

            Attributes attrs = manifest.getMainAttributes();
            if (attrs == null) {
                LOGGER.trace("No main attributes in manifest of {}", jarFile);
                return false;
            }

            String packageVersion = attrs.getValue(VAADIN_PACKAGE_VERSION);
            if (packageVersion != null) {
                LOGGER.debug("Found {} manifest attribute in {}: {}",
                        VAADIN_PACKAGE_VERSION, jarFile.getName(),
                        packageVersion);
                return true;
            }

            LOGGER.trace("No {} attribute in manifest of {}",
                    VAADIN_PACKAGE_VERSION, jarFile);
            return false;

        } catch (IOException e) {
            LOGGER.debug("Error reading manifest from {}", jarFile, e);
            return false;
        }
    }

    /**
     * Clears the manifest cache. This is mainly useful for testing purposes.
     */
    public static void clearCache() {
        manifestCache.clear();
        LOGGER.debug("Manifest cache cleared");
    }

    /**
     * Returns the current size of the manifest cache. This is mainly useful
     * for testing and monitoring purposes.
     *
     * @return the number of cached manifest check results
     */
    public static int getCacheSize() {
        return manifestCache.size();
    }
}
