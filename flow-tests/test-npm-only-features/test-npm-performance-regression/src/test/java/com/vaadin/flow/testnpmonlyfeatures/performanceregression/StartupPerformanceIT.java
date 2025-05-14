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
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class StartupPerformanceIT extends ChromeBrowserTest {

    @Test
    public void devModeInitializerToViteUpIsBelowThreshold() {
        devModeInitializerToDevServerUpIsBelowThreshold("Vite");
    }

    @Test
    @Ignore("Webpack specific Test")
    public void devModeInitializerToWebpackUpIsBelowThreshold() {
        devModeInitializerToDevServerUpIsBelowThreshold("Webpack");
    }

    private void devModeInitializerToDevServerUpIsBelowThreshold(
            String serverName) {
        getDriver().get(getRootURL());
        waitForDevServer();

        long timeoutTime = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < timeoutTime
                && !isElementPresent(By.id("performance-component"))) {
            getDriver().navigate().refresh();
        }

        int startupTime = measureLogEntryTimeDistance(
                "- Starting dev-mode updaters in",
                "- (Started|Reusing) " + serverName, true);

        int npmInstallTime = measureLogEntryTimeDistance(
                "- Running `pnpm install`",
                "- Frontend dependencies resolved successfully", false);

        int startupTimeWithoutNpmInstallTime = startupTime - npmInstallTime;

        // https://github.com/vaadin/flow/issues/7596
        final int thresholdMs = 20000;
        String key = "webpack-time";
        printTeamcityStats(key, startupTimeWithoutNpmInstallTime);

        Assert.assertTrue(
                String.format("startup time expected <= %d but was %d",
                        thresholdMs, startupTimeWithoutNpmInstallTime),
                startupTimeWithoutNpmInstallTime <= thresholdMs);
    }

    private void printTeamcityStats(String key, long value) {
        // ##teamcity[buildStatisticValue key=&#39;&lt;valueTypeKey&gt;&#39;
        // value=&#39;&lt;value&gt;&#39;]
        System.out.println("##teamcity[buildStatisticValue key='" + key
                + "' value='" + value + "']");

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
