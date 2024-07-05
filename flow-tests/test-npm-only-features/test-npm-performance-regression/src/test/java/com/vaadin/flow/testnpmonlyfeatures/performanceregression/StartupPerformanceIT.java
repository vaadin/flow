/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.performanceregression;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class StartupPerformanceIT extends ChromeBrowserTest {
    @Test
    public void devModeInitializerToWebpackUpIsBelowThreshold() {
        getDriver().get(getRootURL());
        waitForDevServer();

        // wait until dev server is ready and the component is rendered
        long timeoutTime = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < timeoutTime
                && !isElementPresent(By.id("performance-component"))) {
            getDriver().navigate().refresh();
        }

        int startupTime = measureLogEntryTimeDistance(
                "- Starting dev-mode updaters in",
                "- (Started|Reusing) webpack-dev-server", true);

        int npmInstallTime = measureLogEntryTimeDistance(
                "- Running `pnpm install`",
                "- Frontend dependencies resolved successfully", false);

        int startupTimeWithoutNpmInstallTime = startupTime - npmInstallTime;

        // https://github.com/vaadin/flow/issues/7596
        final int thresholdMs = Boolean.getBoolean(
                System.getProperty("vaadin.useDeprecatedV14Bootstrapping"))
                        ? 5500
                        : 12000;

        Assert.assertTrue(
                String.format("startup time expected <= %d but was %d",
                        thresholdMs, startupTimeWithoutNpmInstallTime),
                startupTimeWithoutNpmInstallTime <= thresholdMs);
    }

    private int measureLogEntryTimeDistance(String startFragment,
            String endFragment, boolean failIfNotFound) {
        Pattern startPattern = createPattern(startFragment);
        Pattern endPattern = createPattern(endFragment);
        AtomicInteger startTime = new AtomicInteger();
        AtomicInteger endTime = new AtomicInteger();
        try (Stream<String> lines = Files.lines(getLogPath())) {
            lines.forEach(line -> {
                Matcher matcherFirst = startPattern.matcher(line);
                if (matcherFirst.matches() && startTime.get() == 0) {
                    startTime.set(Integer.parseInt(matcherFirst.group(1)));
                }
                Matcher matcherEnd = endPattern.matcher(line);
                if (matcherEnd.matches()) {
                    endTime.set(Integer.parseInt(matcherEnd.group(1)));
                }
            });
            if (startTime.get() == 0 && failIfNotFound) {
                throw new RuntimeException("No match: " + startFragment);
            }
            if (endTime.get() == 0 && failIfNotFound) {
                throw new RuntimeException("No match: " + endFragment);
            }
            return Math.max(0, endTime.get() - startTime.get());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private Pattern createPattern(String fragment) {
        return Pattern.compile(String.format("([0-9]+).*%s.*", fragment));
    }

    private Path getLogPath() {
        File logFile = new File(System.getProperty("server.log.location"));
        return logFile.toPath();
    }
}
