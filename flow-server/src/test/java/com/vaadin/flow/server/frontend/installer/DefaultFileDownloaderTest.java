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
package com.vaadin.flow.server.frontend.installer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.installer.FileDownloader.ProgressListener;

public class DefaultFileDownloaderTest {

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private String baseDir;

    private DefaultFileDownloader downloader;

    @Before
    public void setup() {
        baseDir = tmpDir.getRoot().getAbsolutePath();
        downloader = new DefaultFileDownloader(
                new ProxyConfig(Collections.emptyList()));

    }

    @Test
    public void installNodeFromFileSystem_NodeIsInstalledToTargetDirectory()
            throws IOException, DownloadException {
        File targetDir = new File(baseDir + "/installation");

        Assert.assertFalse(
                "Clean test should not contain a installation folder",
                targetDir.exists());
        File downloadDir = tmpDir.newFolder(FrontendTools.DEFAULT_NODE_VERSION);
        String downloadFileName = "MyDownload.zip";

        File archiveFile = new File(downloadDir, downloadFileName);
        archiveFile.createNewFile();
        Path tempArchive = archiveFile.toPath();

        downloader.download(tempArchive.toUri(),
                new File(targetDir, downloadFileName), null, null, null);

        Assert.assertTrue("File was not 'downloaded' to target directory",
                new File(targetDir, downloadFileName).exists());
        Assert.assertFalse(
                "File 'downloaded' was a directory event though file expected",
                new File(targetDir, downloadFileName).isDirectory());
    }

    @Test
    public void nullProgressListenerWorks() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = "12345678901234567890".getBytes(StandardCharsets.UTF_8);
        downloader.copy(new ByteArrayInputStream(data), out, data.length, null);
    }

    @Test
    public void progressListenerCalledWhenSizeIsKnown() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = createData(8192 * 4);
        List<String> reportedProgress = new ArrayList<>();
        ProgressListener progressListener = (bytesTransferred, totalBytes,
                progress) -> {
            reportedProgress
                    .add(bytesTransferred + "," + totalBytes + "," + progress);
        };
        downloader.copy(new ByteArrayInputStream(data), out, data.length,
                progressListener);

        Assert.assertEquals(
                List.of("8192,32768,0.25", "16384,32768,0.5",
                        "24576,32768,0.75", "32768,32768,1.0"),
                reportedProgress);
    }

    @Test
    public void progressListenerCalledWhenSizeIsUnknown() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = createData(2202009);
        List<String> reportedProgress = new ArrayList<>();
        ProgressListener progressListener = (bytesTransferred, totalBytes,
                progress) -> {
            reportedProgress
                    .add(bytesTransferred + "," + totalBytes + "," + progress);
        };
        downloader.copy(new ByteArrayInputStream(data), out, -1,
                progressListener);
        Assert.assertEquals("1048576,-1,-1.0", reportedProgress.get(0));
        Assert.assertEquals("2097152,-1,-1.0", reportedProgress.get(1));
        Assert.assertEquals("2202009,-1,-1.0", reportedProgress.get(2));
    }

    private byte[] createData(int size) {
        String sequence = "1234567890";
        byte[] sequenceBytes = sequence.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[size];

        // Fill the result array with the repeated sequence
        for (int i = 0; i < size; i++) {
            result[i] = sequenceBytes[i % sequenceBytes.length];
        }

        return result;
    }
}
