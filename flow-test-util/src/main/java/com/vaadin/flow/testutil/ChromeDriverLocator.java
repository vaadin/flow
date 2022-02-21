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
