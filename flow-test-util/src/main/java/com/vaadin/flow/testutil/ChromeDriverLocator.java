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

package com.vaadin.flow.testutil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

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

    private ChromeDriverLocator() {}

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
        if (System.getProperty(WEBDRIVER_CHROME_DRIVER) == null) {
            Optional.ofNullable(getDriverLocation())
                    .ifPresent(driverLocation -> System.setProperty(
                            WEBDRIVER_CHROME_DRIVER, driverLocation));
        }
    }

    private static String getDriverLocation() {
        Path driverDirectory = Paths.get("../../driver/");
        if (!driverDirectory.toFile().isDirectory()) {
            System.out.println(String.format(
                    "Could not find driver directory: %s", driverDirectory));
            return null;
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
