/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CompressUtilTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void uncompressFile_legitimateNestedEntry_isExtracted()
            throws IOException {
        // Mirrors a real prod.bundle: a nested file entry without an explicit
        // directory entry for its parent folder.
        File zip = temporaryFolder.newFile("prod.bundle");
        try (ZipOutputStream zipOut = new ZipOutputStream(
                new FileOutputStream(zip))) {
            zipOut.putNextEntry(new ZipEntry("config/bundle-size.html"));
            zipOut.write("<html></html>".getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }

        File target = temporaryFolder.newFolder("unpacked");
        CompressUtil.uncompressFile(zip, target);

        File extracted = new File(target, "config/bundle-size.html");
        Assert.assertTrue(
                "Legitimate nested entry should have been extracted, but the "
                        + "Zip Slip guard rejected it",
                extracted.exists());
        Assert.assertEquals("<html></html>",
                Files.readString(extracted.toPath()));
    }

    @Test
    public void uncompressFile_zipSlipEntry_isRejected() throws IOException {
        File zip = temporaryFolder.newFile("evil.bundle");
        try (ZipOutputStream zipOut = new ZipOutputStream(
                new FileOutputStream(zip))) {
            zipOut.putNextEntry(new ZipEntry("../evil.txt"));
            zipOut.write("pwned".getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }

        File target = temporaryFolder.newFolder("unpacked");

        Assert.assertThrows(CompressionException.class,
                () -> CompressUtil.uncompressFile(zip, target));
        Assert.assertFalse(
                "Zip Slip entry must not be written outside the target dir",
                new File(target.getParentFile(), "evil.txt").exists());
    }
}
