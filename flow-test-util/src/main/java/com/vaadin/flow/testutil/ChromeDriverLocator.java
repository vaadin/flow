/*
 * Copyright 2000-2022 Vaadin Ltd.
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

package com.vaadin.flow.testutil;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import com.vaadin.flow.server.frontend.FrontendUtils;

import org.apache.commons.io.IOUtils;

/**
 * Locates chromedriver binary in the project and sets its valu
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
final class ChromeDriverLocator {
    private static final String WEBDRIVER_CHROME_DRIVER = "webdriver.chrome.driver";
    private static final String CHROMEDRIVER_NAME_PART = "chromedriver";
    // examples: driver\windows\googlechrome\64bit\chromedriver.exe
    private static final int MAX_DRIVER_SEARCH_DEPTH = 4;

    private ChromeDriverLocator() {
    }

    /**
     * Fills {@link ChromeDriverLocator#WEBDRIVER_CHROME_DRIVER} system property
     * with chromedriver path, does not override already existing value.
     * 
     * @throws UncheckedIOException
     *             on io exceptions of the
     *             {@link Files#find(Path, int, BiPredicate, FileVisitOption...)}
     *             method
     */
    static void fillEnvironmentProperty() {
        String chromedriverProperty = System
                .getProperty(WEBDRIVER_CHROME_DRIVER);
        if (chromedriverProperty == null
                || !new File(chromedriverProperty).exists()) {
            String location = getDriverLocation();
            if (location == null) {
                if (!AbstractTestBenchTest.USE_HUB) {
                    throw new IllegalStateException(
                            "Unable to find chromedriver. Ensure it is available as -D"
                                    + WEBDRIVER_CHROME_DRIVER
                                    + " (currently set to "
                                    + chromedriverProperty
                                    + ") or available in your PATH");
                }
            } else {
                System.out.println("Using chromedriver from " + location);
                System.setProperty(WEBDRIVER_CHROME_DRIVER, location);
            }
        }
    }

    private static String getDriverLocation() {
        Path driverDirectory = Paths.get("../../driver/");
        if (!driverDirectory.toFile().isDirectory()) {
            return getDriverFromPath();
        }

        List<Path> driverPaths = getDriverPaths(driverDirectory);

        if (driverPaths.isEmpty()) {
            System.out.println("No " + CHROMEDRIVER_NAME_PART + " found at \""
                    + driverDirectory.toAbsolutePath() + "\"\n"
                    + "  Verify that the path is correct and that driver-binary-downloader-maven-plugin has been run at least once.");
            return null;
        }

        if (driverPaths.size() > 1) {
            System.out.println(String.format(
                    "Have found multiple driver paths, using the first one from the list: %s",
                    driverPaths));
        }
        return driverPaths.get(0).toAbsolutePath().toString();

    }

    private static String getDriverFromPath() {
        try {
            Process p = new ProcessBuilder().command(FrontendUtils.isWindows() ? "where" : "which", "chromedriver")
                    .start();
            int exitCode = p.waitFor();
            if (exitCode == 0) {
                String location = IOUtils
                        .toString(p.getInputStream(), StandardCharsets.UTF_8)
                        .trim();
                if (location == null || location.isEmpty()) {
                    return null;
                }
                return location;
            }
        } catch (IOException e) {
            // Called at least if "which" was not found. Just ignore it all
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<Path> getDriverPaths(Path driverDirectory) {
        List<Path> driverPaths;
        try {
            driverPaths = Files
                    .find(driverDirectory, MAX_DRIVER_SEARCH_DEPTH,
                            ChromeDriverLocator::isChromeDriver)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Error trying to locate "
                    + CHROMEDRIVER_NAME_PART + " binary", e);
        }
        return driverPaths;
    }

    private static boolean isChromeDriver(Path path,
            BasicFileAttributes attributes) {
        return attributes.isRegularFile()
                && path.toString().toLowerCase(Locale.getDefault())
                        .contains(CHROMEDRIVER_NAME_PART);
    }
}
