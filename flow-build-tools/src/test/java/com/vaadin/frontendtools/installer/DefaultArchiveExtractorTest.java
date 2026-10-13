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
package com.vaadin.frontendtools.installer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.internal.FrontendUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class DefaultArchiveExtractorTest {

    public static final String ROOT_FILE = "root.file";
    public static final String SUBFOLDER_FILE = "subfolder/folder.file";
    @TempDir
    File tmpDir;

    private String baseDir;
    private File targetDir;

    @BeforeEach
    void setup() {
        baseDir = tmpDir.getAbsolutePath();
        targetDir = new File(baseDir + "/extract");
    }

    @Test
    void extractZip_contentsAreExtracted()
            throws IOException, ArchiveExtractionException {
        File archiveFile = new File(baseDir, "archive.zip");
        archiveFile.createNewFile();
        Path tempArchive = archiveFile.toPath();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                Files.newOutputStream(tempArchive))) {
            zipOutputStream.putNextEntry(new ZipEntry(ROOT_FILE));
            zipOutputStream.closeEntry();
            zipOutputStream.putNextEntry(new ZipEntry(SUBFOLDER_FILE));
            zipOutputStream.closeEntry();
        }

        new DefaultArchiveExtractor().extract(archiveFile, targetDir);

        assertTrue(new File(targetDir, ROOT_FILE).exists(),
                "Archive root.file was not extracted");
        assertTrue(new File(targetDir, SUBFOLDER_FILE).exists(),
                "Archive subfolder/folder.file was not extracted");
    }

    @Test
    void extractTarGz_contentsAreExtracted()
            throws IOException, ArchiveExtractionException {
        File archiveFile = new File(baseDir, "archive.tar.gz");
        archiveFile.createNewFile();
        Path tempArchive = archiveFile.toPath();

        try (OutputStream fo = Files.newOutputStream(tempArchive);
                OutputStream gzo = new GzipCompressorOutputStream(fo);
                ArchiveOutputStream o = new TarArchiveOutputStream(gzo)) {
            o.putArchiveEntry(
                    o.createArchiveEntry(new File(ROOT_FILE), ROOT_FILE));
            o.closeArchiveEntry();
            o.putArchiveEntry(o.createArchiveEntry(new File(SUBFOLDER_FILE),
                    SUBFOLDER_FILE));
            o.closeArchiveEntry();
        }

        new DefaultArchiveExtractor().extract(archiveFile, targetDir);

        assertTrue(new File(targetDir, ROOT_FILE).exists(),
                "Archive root.file was not extracted");
        assertTrue(new File(targetDir, SUBFOLDER_FILE).exists(),
                "Archive subfolder/folder.file was not extracted");
    }

    @Test
    void extractTarAsZip_ArchiveExtractionExceptionIsThrown()
            throws IOException, ArchiveExtractionException {
        File archiveFile = new File(baseDir, "archive.zip");
        archiveFile.createNewFile();
        Path tempArchive = archiveFile.toPath();

        try (OutputStream fo = Files.newOutputStream(tempArchive);
                OutputStream gzo = new GzipCompressorOutputStream(fo);
                ArchiveOutputStream o = new TarArchiveOutputStream(gzo)) {
            o.putArchiveEntry(
                    o.createArchiveEntry(new File(ROOT_FILE), ROOT_FILE));
            o.closeArchiveEntry();
        }

        DefaultArchiveExtractor extractor = new DefaultArchiveExtractor();
        assertThrows(ArchiveExtractionException.class,
                () -> extractor.extract(archiveFile, targetDir));
    }

    @Test
    void zipArchive_containsBadZipFilePath()
            throws IOException, ArchiveExtractionException {
        File archiveFile = new File(baseDir, "archive.zip");
        archiveFile.createNewFile();
        Path tempArchive = archiveFile.toPath();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                Files.newOutputStream(tempArchive))) {
            zipOutputStream.putNextEntry(new ZipEntry("../" + ROOT_FILE));
            zipOutputStream.closeEntry();
            zipOutputStream.putNextEntry(new ZipEntry(SUBFOLDER_FILE));
            zipOutputStream.closeEntry();
        }

        DefaultArchiveExtractor extractor = new DefaultArchiveExtractor();
        ArchiveExtractionException archiveExtractionException = assertThrows(
                ArchiveExtractionException.class,
                () -> extractor.extract(archiveFile, targetDir));

        assertEquals("Entry is outside of the target dir: ../root.file",
                archiveExtractionException.getCause().getMessage());
    }

    @Test
    void extractTarGz_symbolicLinkIsExtractedAsLink()
            throws IOException, ArchiveExtractionException {
        assumeFalse(FrontendUtils.isWindows());
        File archiveFile = createTarGzWithSymbolicLink("bin/npm",
                "../lib/npm-cli.js");

        new DefaultArchiveExtractor().extract(archiveFile, targetDir);

        Path link = targetDir.toPath().resolve("bin/npm");
        assertTrue(Files.isSymbolicLink(link),
                "Symbolic link was not extracted as a link");
        assertEquals(Path.of("../lib/npm-cli.js"),
                Files.readSymbolicLink(link));
        assertEquals("cli", Files.readString(link));
    }

    @Test
    void extractTarGz_symbolicLinkOutsideTarget_ArchiveExtractionExceptionIsThrown()
            throws IOException {
        File archiveFile = createTarGzWithSymbolicLink("bin/npm",
                "../../outside.js");

        DefaultArchiveExtractor extractor = new DefaultArchiveExtractor();
        assertThrows(ArchiveExtractionException.class,
                () -> extractor.extract(archiveFile, targetDir));
    }

    private File createTarGzWithSymbolicLink(String linkName, String target)
            throws IOException {
        File archiveFile = new File(baseDir, "archive.tar.gz");
        byte[] content = "cli".getBytes();
        try (OutputStream fo = Files.newOutputStream(archiveFile.toPath());
                OutputStream gzo = new GzipCompressorOutputStream(fo);
                TarArchiveOutputStream o = new TarArchiveOutputStream(gzo)) {
            TarArchiveEntry file = new TarArchiveEntry("lib/npm-cli.js");
            file.setSize(content.length);
            o.putArchiveEntry(file);
            o.write(content);
            o.closeArchiveEntry();
            TarArchiveEntry link = new TarArchiveEntry(linkName,
                    TarArchiveEntry.LF_SYMLINK);
            link.setLinkName(target);
            o.putArchiveEntry(link);
            o.closeArchiveEntry();
        }
        return archiveFile;
    }
}
