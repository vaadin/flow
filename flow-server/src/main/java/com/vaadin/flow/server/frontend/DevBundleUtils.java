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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.StringUtil;
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
    public static URL findBundleFile(File projectDir, String buildFolder,
            String filename) throws IOException {
        File devBundleFolder = getDevBundleFolder(projectDir, buildFolder);
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
    public static File getDevBundleFolder(File projectDir, String buildFolder) {
        return new File(new File(projectDir, buildFolder),
                Constants.DEV_BUNDLE_LOCATION);
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
    public static String findBundleStatsJson(File projectDir,
            String buildFolder) throws IOException {
        URL statsJson = findBundleFile(projectDir, buildFolder,
                "config/stats.json");
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

    /**
     * Compress the dev bundle at give location into src/main/bundles.
     *
     * @param devBundleFolder
     *            dev bundle location
     */
    public static void compressBundle(File projectDir, File devBundleFolder) {
        File bundleFile = new File(projectDir,
                Constants.DEV_BUNDLE_COMPRESSED_FILE_LOCATION);
        if (bundleFile.exists()) {
            bundleFile.delete();
        } else {
            bundleFile.getParentFile().mkdirs();
        }

        try (ZipOutputStream zipOut = new ZipOutputStream(
                new FileOutputStream(bundleFile))) {

            for (File child : devBundleFolder.listFiles()) {
                zip(child, child.getName(), zipOut);
            }
        } catch (IOException e) {
            throw new CompressionException(
                    "Failed to compress dev bundle files to '"
                            + bundleFile.getPath() + "'",
                    e);
        }
    }

    private static void zip(File fileToZip, String fileName,
            ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        fileName = fileName.replaceAll("\\\\", "/");
        if (fileToZip.isDirectory()) {
            if (!fileName.endsWith("/")) {
                fileName = fileName + "/";
            }
            zipOut.putNextEntry(new ZipEntry(fileName));
            zipOut.closeEntry();

            for (File child : fileToZip.listFiles()) {
                zip(child, fileName + child.getName(), zipOut);
            }
            return;
        }
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        }
        zipOut.closeEntry();
    }

    /**
     * Unpack the compressed dev bundle from src/main/bundles if it exists into
     * the given location.
     *
     * @param devBundleFolder
     *            unpacked dev bundle location
     */
    public static void unpackBundle(File projectDir, File devBundleFolder) {
        File bundleFile = new File(projectDir,
                Constants.DEV_BUNDLE_COMPRESSED_FILE_LOCATION);
        if (!bundleFile.exists()) {
            return;
        }
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(
                new FileInputStream(bundleFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(devBundleFolder, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException(
                                "Failed to create directory " + newFile);
                    }
                } else {

                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException(
                                "Failed to create directory " + parent);
                    }

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        } catch (IOException e) {
            throw new CompressionException(
                    "Failed to unpack '" + bundleFile.getPath() + "'", e);
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry)
            throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: "
                    + zipEntry.getName());
        }

        return destFile;
    }
}
