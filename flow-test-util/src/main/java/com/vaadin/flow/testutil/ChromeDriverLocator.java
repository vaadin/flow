/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

package com.vaadin.flow.testutil;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiPredicate;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Locates a chromedriver binary.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
final class ChromeDriverLocator {
    private static final String WEBDRIVER_CHROME_DRIVER = "webdriver.chrome.driver";

    private ChromeDriverLocator() {
    }

    /**
     * Fills {@link ChromeDriverLocator#WEBDRIVER_CHROME_DRIVER} system property
     * with chromedriver path, does not override already existing value.
     */
    static void fillEnvironmentProperty() {
        if (AbstractTestBenchTest.USE_HUB) {
            return;
        }

        String chromedriverProperty = System
                .getProperty(WEBDRIVER_CHROME_DRIVER);
        if (chromedriverProperty == null
                || !new File(chromedriverProperty).exists()) {
            // This sets the same property
            WebDriverManager.chromedriver().setup();
        } else {
            System.out
                    .println("Using chromedriver from " + chromedriverProperty);
        }

    }

}
