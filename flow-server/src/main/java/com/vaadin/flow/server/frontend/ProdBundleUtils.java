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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.Constants;

/**
 * Helpers related to the production bundle.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ProdBundleUtils {

    private ProdBundleUtils() {
        // Static helpers only
    }

    /**
     * Get the application specific production bundle file.
     *
     * @param projectDir
     *            the project base directory
     * @return the bundle directory
     */
    public static File getProdBundle(File projectDir) {
        return new File(projectDir,
                Constants.PROD_BUNDLE_COMPRESSED_FILE_LOCATION);
    }

    /**
     * Get the stats.json for the application specific production bundle or from
     * the default bundle if it exists.
     *
     * @param projectDir
     *            the project base directory
     * @param finder
     *            class finder
     * @return stats.json content or {@code null} if not found
     * @throws IOException
     *             if an I/O exception occurs.
     * @deprecated Use {@link #findBundleStatsJson(File)} instead
     */
    @Deprecated
    public static String findBundleStatsJson(File projectDir,
            Object finder) throws IOException {
        return findBundleStatsJson(projectDir);
    }

    /**
     * Get the stats.json for the application specific production bundle or from
     * the default bundle if it exists.
     *
     * @param projectDir
     *            the project base directory
     * @return stats.json content or {@code null} if not found
     * @throws IOException
     *             if an I/O exception occurs.
     */
    public static String findBundleStatsJson(File projectDir)
            throws IOException {
        String statsFile = "config/stats.json";
        File prodBundleFile = getProdBundle(projectDir);
        if (prodBundleFile.exists()) {
            // Has a production bundle
            try {
                String stats = CompressUtil
                        .readFileContentFromZip(prodBundleFile, statsFile);
                if (stats != null) {
                    return stats;
                }
            } catch (IOException e) {
                getLogger().error(
                        "Failed to read stats.json from the production bundle",
                        e);
            }
        }

        URL statsJson = Thread.currentThread().getContextClassLoader()
                .getResource(Constants.PROD_BUNDLE_JAR_PATH + statsFile);
        if (statsJson == null) {
            getLogger().warn(
                    "There is no prod-bundle in the project or on the classpath nor is there a default production bundle included.");
            return null;
        }

        return IOUtils.toString(statsJson, StandardCharsets.UTF_8);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ProdBundleUtils.class);
    }

    /**
     * Compress the prod bundle at give location into src/main/bundles.
     *
     * @param projectDir
     *            current project root directory
     * @param prodBundleFolder
     *            prod bundle location
     */
    public static void compressBundle(File projectDir, File prodBundleFolder) {
        File bundleFile = new File(projectDir,
                Constants.PROD_BUNDLE_COMPRESSED_FILE_LOCATION);
        if (bundleFile.exists()) {
            bundleFile.delete();
        } else {
            bundleFile.getParentFile().mkdirs();
        }
        CompressUtil.compressDirectory(prodBundleFolder, bundleFile);
    }

    /**
     * Unpack the compressed prod bundle from src/main/bundles if it exists into
     * the given location.
     *
     * @param projectDir
     *            current project root directory
     * @param prodBundleFolder
     *            unpacked prod bundle location
     */
    public static void unpackBundle(File projectDir, File prodBundleFolder) {
        File bundleFile = new File(projectDir,
                Constants.PROD_BUNDLE_COMPRESSED_FILE_LOCATION);
        CompressUtil.uncompressFile(bundleFile, prodBundleFolder);
    }

}
