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
package com.vaadin.flow.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for compression and decompression of folders and files.
 *
 * @author Vaadin Ltd
 * @since 24.3
 */
public class CompressUtil {

    private CompressUtil() {
        // Utility class only
    }

    /**
     * Compress target directory and children into given target outfile. All
     * files will be zipped and the targetDirectory will not be in the package.
     *
     * @param targetDirectory
     *            directory content to compress
     * @param outfile
     *            file to compress directory content to
     */
    public static void compressDirectory(File targetDirectory, File outfile) {
        try (ZipOutputStream zipOut = new ZipOutputStream(
                new FileOutputStream(outfile))) {

            for (File child : targetDirectory.listFiles()) {
                zip(child, child.getName(), zipOut);
            }
        } catch (IOException e) {
            throw new CompressionException(
                    "Failed to compress bundle files to '" + outfile.getPath()
                            + "'",
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
     * Uncompress given zip file content to the target directory.
     *
     * @param zip
     *            file to uncompress
     * @param targetDirectory
     *            target directory to uncompress files to
     */
    public static void uncompressFile(File zip, File targetDirectory) {
        if (!zip.exists()) {
            return;
        }
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(
                new FileInputStream(zip))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(targetDirectory, zipEntry);
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
                    "Failed to unpack '" + zip.getPath() + "'", e);
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

    /**
     * Read a file content from the given zip file.
     *
     * @param zip
     *            Target zip file
     * @param filename
     *            Target file name
     * @return File content or {@code null} if not found
     * @throws IOException
     *             if an I/O error occurs
     */
    public static String readFileContentFromZip(File zip, String filename)
            throws IOException {
        try (ZipFile zipFile = new ZipFile(zip)) {
            ZipEntry entry = zipFile.getEntry(filename);
            if (entry == null) {
                return null;
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                return StringUtil.toUTF8String(inputStream);
            }
        } catch (ZipException e) {
            throw new IOException(e);
        }
    }
}
