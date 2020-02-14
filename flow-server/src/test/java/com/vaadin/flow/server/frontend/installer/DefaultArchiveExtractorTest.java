package com.vaadin.flow.server.frontend.installer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DefaultArchiveExtractorTest {

    public static final String ROOT_FILE = "root.file";
    public static final String SUBFOLDER_FILE = "subfolder/folder.file";
    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private String baseDir;
    private File targetDir;

    @Before
    public void setup() {
        baseDir = tmpDir.getRoot().getAbsolutePath();
        targetDir = new File(baseDir + "/extract");
    }

    @Test
    public void extractZip_contentsAreExtracted()
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

        Assert.assertTrue("Archive root.file was not extracted",
                new File(targetDir, ROOT_FILE).exists());
        Assert.assertTrue("Archive subfolder/folder.file was not extracted",
                new File(targetDir, SUBFOLDER_FILE).exists());
    }

    @Test
    public void extractTarGz_contentsAreExtracted()
            throws IOException, ArchiveExtractionException {
        File archiveFile = new File(baseDir, "archive.tar.gz");
        archiveFile.createNewFile();
        Path tempArchive = archiveFile.toPath();

        try (OutputStream fo = Files.newOutputStream(
                tempArchive); OutputStream gzo = new GzipCompressorOutputStream(
                fo); ArchiveOutputStream o = new TarArchiveOutputStream(gzo)) {
            o.putArchiveEntry(
                    o.createArchiveEntry(new File(ROOT_FILE), ROOT_FILE));
            o.closeArchiveEntry();
            o.putArchiveEntry(
                    o.createArchiveEntry(new File(SUBFOLDER_FILE),
                            SUBFOLDER_FILE));
            o.closeArchiveEntry();
        }

        new DefaultArchiveExtractor().extract(archiveFile, targetDir);

        Assert.assertTrue("Archive root.file was not extracted",
                new File(targetDir, ROOT_FILE).exists());
        Assert.assertTrue("Archive subfolder/folder.file was not extracted",
                new File(targetDir, SUBFOLDER_FILE).exists());
    }
}