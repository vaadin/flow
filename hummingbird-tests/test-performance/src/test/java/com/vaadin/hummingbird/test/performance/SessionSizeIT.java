/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.test.performance;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.hummingbird.testutil.AbstractTestBenchTest;

// Extending AbstractTestBenchTest to get a consistent getTestURL() method
public class SessionSizeIT extends AbstractTestBenchTest {

    private static final Pattern usagePattern = Pattern
            .compile("Heap usage with (\\d+) UIs: (\\d+)");

    private static ExecutorService uiOpenExecutor;

    private static class UsageReport {
        private final int uiCount;
        private final long memoryUsage;

        private UsageReport(int uiCount, long memoryUsage) {
            this.uiCount = uiCount;
            this.memoryUsage = memoryUsage;
        }
    }

    @Override
    protected String getTestPath() {
        return HelloWorldUI.PATH;
    }

    @Test
    public void testSessionSize() {
        // Warm up the server
        openTestUIs(1000, 1);
        getUsage();
        openTestUIs(1000, 1);

        long oneButtonSize = getSessionSize(1);

        HelloWorldIT.printTeamcityStats("helloworld-session-size",
                oneButtonSize);

        long fiftyButtonsSize = getSessionSize(50);

        long sizePerButton = (fiftyButtonsSize - oneButtonSize) / 49;

        HelloWorldIT.printTeamcityStats("helloworld-button-size",
                sizePerButton);

    }

    private long getSessionSize(int buttonCount) {
        // Repeat until we get two consecutive results within 0.1% of each other
        double lastResult = 0;
        for (int i = 0; i < 50; i++) {
            double currentResult = getMemoryPerUI(500, buttonCount);

            double delta = Math.abs(lastResult - currentResult);
            double deltaLimit = currentResult * 0.001;

            if (delta < deltaLimit) {
                return (long) lastResult;
            } else {
                lastResult = currentResult;
            }
        }

        Assert.fail("Session size does not stabilize");
        return -1;
    }

    private double getMemoryPerUI(int uiCount, int buttonCount) {
        UsageReport before = getUsage();

        openTestUIs(uiCount, buttonCount);

        UsageReport after = getUsage();

        // Verify that no UI is missing and no UI has been collected
        Assert.assertEquals(before.uiCount + uiCount, after.uiCount);

        double delta = after.memoryUsage - before.memoryUsage;

        return delta / uiCount;
    }

    @BeforeClass
    public static void startExecutor() {
        uiOpenExecutor = Executors.newFixedThreadPool(
                Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
    }

    @AfterClass
    public static void stopExecutor() {
        uiOpenExecutor.shutdownNow();
    }

    private void openTestUIs(int uiCount, int buttonCount) {
        // Submit to executor
        List<Future<?>> futures = Stream
                .generate(() -> uiOpenExecutor
                        .submit(() -> openSession(buttonCount)))
                .limit(uiCount).collect(Collectors.toList());

        // Wait for all tasks to finish
        futures.forEach(f -> {
            try {
                f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private UsageReport getUsage() {
        String statusUrl = getTestURL() + ".." + MemoryUsageMonitor.PATH;

        try {
            String line = IOUtils
                    .toString(new URL(statusUrl), StandardCharsets.UTF_8)
                    .trim();

            Matcher matcher = usagePattern.matcher(line);
            if (!matcher.matches()) {
                throw new RuntimeException("Unexptected response: " + line);
            }

            int uiCount = Integer.parseInt(matcher.group(1));
            long memoryUsage = Long.parseLong(matcher.group(2));

            return new UsageReport(uiCount, memoryUsage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openSession(int buttonCount) {
        String url = getTestURL("buttons=" + buttonCount);
        try {
            String response = IOUtils.toString(new URL(url),
                    StandardCharsets.UTF_8);
            Assert.assertEquals(buttonCount,
                    StringUtils.countMatches(response, "Hello"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
