/*
 * Copyright 2000-2023 Vaadin Ltd.
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
 * Helpers related to the development bundle.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class DevBundleUtils {

    private DevBundleUtils() {
        // Static helpers only
    }

    /**
     * Finds the given file inside the current development bundle.
     * <p>
     *
     * @param projectDir
     *            the project root folder
     * @param filename
     *            the file name inside the bundle
     * @return a URL referring to the file inside the bundle or {@code null} if
     *         the file was not found
     */
    public static URL findBundleFile(File projectDir, String filename)
            throws IOException {
        File devBundleFolder = getDevBundleFolder(projectDir);
        if (devBundleFolder.exists()) {
            // Has an application bundle
            File bundleFile = new File(devBundleFolder, filename);
            if (bundleFile.exists()) {
                return bundleFile.toURI().toURL();
            }
        }
        return TaskRunDevBundleBuild.class.getClassLoader()
                .getResource(Constants.DEV_BUNDLE_JAR_PATH + filename);
    }

    /**
     * Get the folder where an application specific bundle is stored.
     *
     * @param projectDir
     *            the project base directory
     * @return the bundle directory
     */
    public static File getDevBundleFolder(File projectDir) {
        return new File(projectDir, Constants.DEV_BUNDLE_LOCATION);
    }

    /**
     * Get the stats.json for the application specific development bundle.
     *
     * @param projectDir
     *            the project base directory
     * @return stats.json content or {@code null} if not found
     * @throws IOException
     *             if an I/O exception occurs.
     */
    public static String findBundleStatsJson(File projectDir)
            throws IOException {
        URL statsJson = findBundleFile(projectDir, "config/stats.json");
        if (statsJson == null) {
            getLogger().warn(
                    "There is no dev-bundle in the project or on the classpath nor is there a default bundle included.");
            return null;
        }

        return IOUtils.toString(statsJson, StandardCharsets.UTF_8);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DevBundleUtils.class);
    }

}
