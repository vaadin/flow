/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;

public class StartupPerformanceIT {
    @Test
    public void devModeInitializerToWebpackUpIsBelow5500ms() {
        int startupTime = measureLogEntryTimeDistance(
                "com.vaadin.flow.server.startup.DevModeInitializer - Starting dev-mode updaters in",
                "dev-webpack.*Time: [0-9]+ms", true);

        int npmInstallTime = measureLogEntryTimeDistance(
                "dev-updater - Running `npm install`",
                "dev-updater - package.json updated and npm dependencies installed",
                false);

        int startupTimeWithoutNpmInstallTime = startupTime - npmInstallTime;

        final int thresholdMs = 5500;
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
        try {
            Files.lines(getLogPath()).forEach(line -> {
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
