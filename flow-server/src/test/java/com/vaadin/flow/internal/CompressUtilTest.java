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
package com.vaadin.flow.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
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
    public void hasFileInZip_onlyExistingEntryIsFound() throws IOException {
        File zip = temporaryFolder.newFile("prod.bundle");
        try (ZipOutputStream zipOut = new ZipOutputStream(
                new FileOutputStream(zip))) {
            zipOut.putNextEntry(new ZipEntry("config/templates/my-view.js"));
            zipOut.write("export {}".getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }

        Assert.assertTrue(
                CompressUtil.hasFileInZip(zip, "config/templates/my-view.js"));
        Assert.assertFalse(CompressUtil.hasFileInZip(zip,
                "config/templates/another-view.js"));
    }

    @Test
    public void hasFileInZip_fileIsNotAZip_throwsIOException()
            throws IOException {
        File notAZip = temporaryFolder.newFile("prod.bundle");
        Files.writeString(notAZip.toPath(), "not a zip");

        // ZipException is an IOException and is let through as it is
        ZipException exception = Assert.assertThrows(ZipException.class,
                () -> CompressUtil.hasFileInZip(notAZip, "config/stats.json"));
        Assert.assertNull("The failure should not be wrapped",
                exception.getCause());
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

    @Test
    public void deflateString_inflateString_returnsOriginal() {
        assertRoundTrip("{\"syncId\":3,\"title\":\"Ä € 日本\"}".repeat(50));
    }

    @Test
    public void deflateString_compressedLargerThanBuffer_inflateStringReturnsOriginal() {
        // Random characters barely compress, so the compressed form is larger
        // than the buffer used for deflating and takes several rounds
        Random random = new Random(42);
        String text = random
                .ints(40_000, 'a', 'z' + 1).collect(StringBuilder::new,
                        StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        byte[] compressed = assertRoundTrip(text);

        Assert.assertTrue(compressed.length > 3 * 4096);
    }

    private static byte[] assertRoundTrip(String text) {
        byte[] compressed = CompressUtil.deflateString(text);

        Assert.assertTrue(compressed.length < text.length());
        Assert.assertEquals(text, CompressUtil.inflateString(compressed));
        return compressed;
    }
}
