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
package com.vaadin.base.devserver.stats;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.testutil.TestUtils;

/**
 * Tests that the telemetry notice is logged on first initialization and
 * suppressed on subsequent runs when the notice marker file already exists.
 */
public class DevModeUsageStatisticsLoggingTest {

    private Path tempDir;
    private File usageStatisticsFile;
    private StatisticsStorage storage;
    private StatisticsSender sender;

    private PrintStream originalErr;
    private ByteArrayOutputStream capturedErr;

    @Before
    public void setUp() throws Exception {
        // Create an isolated directory for this test so the notice marker file
        // does not collide with other tests
        tempDir = Files.createTempDirectory("vaadin-telemetry-test-");
        tempDir.toFile().deleteOnExit();

        usageStatisticsFile = tempDir.resolve("usage-statistics.json").toFile();
        copyStatsTemplate(usageStatisticsFile);

        storage = Mockito.spy(new StatisticsStorage());
        Mockito.when(storage.getUsageStatisticsFile())
                .thenReturn(usageStatisticsFile);

        sender = Mockito.spy(new StatisticsSender(storage));
        Mockito.doAnswer(inv -> null).when(sender)
                .triggerSendIfNeeded(Mockito.any());

        // Capture slf4j-simple output which goes to System.err by default
        originalErr = System.err;
        capturedErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(capturedErr, true,
                StandardCharsets.UTF_8.name()));
    }

    @After
    public void tearDown() throws Exception {
        System.setErr(originalErr);
        // Best-effort cleanup of temp dir
        try {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> p.toFile().delete());
        } catch (IOException ignore) {
        }
    }

    @Test
    public void logsTelemetryNoticeOnlyOnFirstRun() {
        // First init should log the telemetry notice since the marker file does
        // not exist
        DevModeUsageStatistics.init(tempDir.toFile(), storage, sender);
        String firstRunLogs = capturedErr.toString(StandardCharsets.UTF_8);
        Assert.assertTrue("Expected telemetry notice to be logged on first run",
                firstRunLogs.contains(
                        "Vaadin collects usage data in order to help us improve your experience."));

        // Clear captured logs
        capturedErr.reset();

        // Second init should NOT log the notice since the marker file now
        // exists
        DevModeUsageStatistics.init(tempDir.toFile(), storage, sender);
        String secondRunLogs = capturedErr.toString(StandardCharsets.UTF_8);
        Assert.assertFalse(
                "Telemetry notice must not be logged again after marker file is created",
                secondRunLogs.contains(
                        "Vaadin collects usage data in order to help us improve your experience."));
    }

    private static void copyStatsTemplate(File target) throws IOException {
        URL res = TestUtils
                .getTestResource("stats-data/usage-statistics-1.json");
        if (res == null) {
            throw new IOException(
                    "Test resource stats-data/usage-statistics-1.json not found");
        }
        byte[] bytes = Files.readAllBytes(new File(res.getFile()).toPath());
        Files.write(target.toPath(), bytes);
    }
}
