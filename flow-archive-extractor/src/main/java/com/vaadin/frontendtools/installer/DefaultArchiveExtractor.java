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
package com.vaadin.frontendtools.installer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;

/**
 * Default implementation for file archive extraction.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 */
public final class DefaultArchiveExtractor implements ArchiveExtractor {

    @Override
    public void extract(File archiveFile, File destinationDirectory)
            throws ArchiveExtractionException {
        try {
            if (archiveFile.getAbsolutePath().endsWith("msi")) {
                extractMSIArchive(archiveFile, destinationDirectory);
            } else if (archiveFile.getAbsolutePath().endsWith("zip")) {
                extractZipArchive(archiveFile, destinationDirectory);
            } else {
                extractGzipTarArchive(archiveFile, destinationDirectory);
            }
        } catch (IOException e) {
            throw new ArchiveExtractionException(
                    "Could not extract archive: '" + archiveFile + "'", e);
        }
    }

    private void extractMSIArchive(File archiveFile, File destinationDirectory)
            throws IOException, ArchiveExtractionException {
        String command = "msiexec /a " + archiveFile.getAbsolutePath()
                + " /qn TARGETDIR=\"" + destinationDirectory + "\"";
        Process child = Runtime.getRuntime().exec(command);
        try {
            int result = child.waitFor();
            if (result != 0) {
                throw new ArchiveExtractionException(
                        "Could not extract " + archiveFile.getAbsolutePath()
                                + "; return code " + result);
            }
        } catch (InterruptedException e) {
            throw new ArchiveExtractionException(
                    "Unexpected interruption of while waiting for extraction process",
                    e);
        }
    }

    private void extractZipArchive(File archiveFile, File destinationDirectory)
            throws IOException {
        ZipFile zipFile = new ZipFile(archiveFile);
        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                final File destPath = new File(destinationDirectory
                        + File.separator + entry.getName());
                prepDestination(destPath, entry.isDirectory());

                copyZipFileContents(zipFile, entry, destPath);
            }
        } finally {
            zipFile.close();
        }
    }

    /**
     * Copy ZipEntry file contents to target path.
     *
     * @param zipFile
     *            zip file
     * @param entry
     *            zip entry
     * @param destinationFile
     *            destination
     * @throws IOException
     *             thrown if copying fails
     */
    private void copyZipFileContents(ZipFile zipFile, ZipEntry entry,
            File destinationFile) throws IOException {
        if (entry.isDirectory()) {
            return;
        }
        try (InputStream in = zipFile.getInputStream(entry);
                OutputStream out = new FileOutputStream(destinationFile)) {
            IOUtils.copy(in, out);
        }
    }

    private void extractGzipTarArchive(File archive, File destinationDirectory)
            throws IOException {
        // TarArchiveInputStream can be constructed with a normal
        // FileInputStream if
        // we ever need to extract regular '.tar' files.

        try (FileInputStream fis = new FileInputStream(archive);
                GzipCompressorInputStream gis = new GzipCompressorInputStream(
                        fis);
                TarArchiveInputStream tarIn = new TarArchiveInputStream(gis)) {

            TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
            String canonicalDestinationDirectory = destinationDirectory
                    .getCanonicalPath();
            while (tarEntry != null) {
                // Create a file for this tarEntry
                final File destPath = new File(destinationDirectory
                        + File.separator + tarEntry.getName());
                prepDestination(destPath, tarEntry.isDirectory());

                if (!startsWithPath(destPath.getCanonicalPath(),
                        canonicalDestinationDirectory)) {
                    throw new IOException("Expanding " + tarEntry.getName()
                            + " would create file outside of "
                            + canonicalDestinationDirectory);
                }

                copyTarFileContents(tarIn, tarEntry, destPath);
                tarEntry = tarIn.getNextTarEntry();
            }
        }
    }

    /**
     * Copy TarArchiveEntry file contents to target path. Set file to executable
     * if marked so in the entry.
     *
     * @param tarIn
     *            tar archive input stream
     * @param tarEntry
     *            tar archive entry
     * @param destinationFile
     *            destination
     * @throws IOException
     *             thrown if copying fails
     */
    private void copyTarFileContents(TarArchiveInputStream tarIn,
            TarArchiveEntry tarEntry, File destinationFile) throws IOException {
        if (tarEntry.isDirectory()) {
            return;
        }
        destinationFile.createNewFile();
        boolean isExecutable = (tarEntry.getMode() & 0100) > 0;
        destinationFile.setExecutable(isExecutable);

        try (FileOutputStream out = new FileOutputStream(destinationFile)) {
            IOUtils.copy(tarIn, out);
        }
    }

    private void prepDestination(File path, boolean directory)
            throws IOException {
        if (directory) {
            path.mkdirs();
        } else {
            if (!path.getParentFile().exists()) {
                path.getParentFile().mkdirs();
            }
            if (!path.getParentFile().canWrite()) {
                throw new AccessDeniedException(String.format(
                        "Could not get write permissions for '%s'",
                        path.getParentFile().getAbsolutePath()));
            }
        }
    }

    /**
     * Do multiple file system checks that should enable the extractor to work
     * on any file system whether or not it's case sensitive or not.
     *
     * @param destPath
     *            destination path
     * @param destDir
     *            destination directory
     * @return true is destination path starts with destination directory
     */
    private boolean startsWithPath(String destPath, String destDir) {
        if (destPath.startsWith(destDir)) {
            return true;
        } else if (destDir.length() > destPath.length()) {
            return false;
        } else {
            if (new File(destPath).exists()
                    && !(new File(destPath.toLowerCase()).exists())) {
                return false;
            }

            return destPath.toLowerCase().startsWith(destDir.toLowerCase());
        }
    }
}
