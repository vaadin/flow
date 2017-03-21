package com.vaadin.hummingbird.demo;

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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.hummingbird.demo.expensemanager.MemoryUsageMonitor;
import com.vaadin.hummingbird.testutil.AbstractTestBenchTest;

public class ExpenseSessionSizeIT {

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

    @Test
    public void testOverviewSessionSize() {
        String uri = "/overview";
        // Warm up the server
        openTestUIs(50, uri);
        getUsage();
        openTestUIs(50, uri);

        long overview = getSessionSize(uri);

        printTeamcityStats("expense-manager-session-size", overview);

        long openExpense = getSessionSize("/expense/4");

        printTeamcityStats("expense-manager-edit-size", openExpense);

    }

    private long getSessionSize(String uri) {
        // Repeat until we get two consecutive results within 0.1% of each other
        double lastResult = 0;
        for (int i = 0; i < 50; i++) {
            double currentResult = getMemoryPerUI(25, uri);

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

    private double getMemoryPerUI(int uiCount, String uri) {
        UsageReport before = getUsage();

        openTestUIs(uiCount, uri);

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

    private void openTestUIs(int uiCount, String uri) {
        // Submit to executor
        List<Future<?>> futures = Stream
                .generate(() -> uiOpenExecutor.submit(() -> openSession(uri)))
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

    private void openSession(String uri) {
        String url = getTestURL(uri);
        try {
            String response = IOUtils.toString(new URL(url),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private UsageReport getUsage() {
        String statusUrl = getTestURL(MemoryUsageMonitor.PATH);

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

    private void printTeamcityStats(String key, long value) {
        // ##teamcity[buildStatisticValue key=&#39;&lt;valueTypeKey&gt;&#39;
        // value=&#39;&lt;value&gt;&#39;]
        System.out.println("##teamcity[buildStatisticValue key='" + key
                + "' value='" + value + "']");
    }

    private String getTestURL(String path) {
        return AbstractTestBenchTest.getTestURL(
                "http://localhost:" + AbstractTestBenchTest.SERVER_PORT, path);
    }
}
