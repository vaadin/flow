/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.flow.testcategory.ChromeTests;
import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.parallel.Browser;

/**
 * Base class for TestBench tests to run locally in the Chrome browser.
 * <p>
 * It is required to set system property with path to the driver to be able to
 * run the test.
 * <p>
 * The test can be executed locally and on a test Hub. Chrome browser is used
 * only if test is executed locally. The test Hub target browsers can be
 * configured via {@link #getHubBrowsersToTest()}.
 *
 *
 * @author Vaadin Ltd
 *
 */
@Category(ChromeTests.class)
public class ChromeBrowserTest extends ViewOrUITest {
    private static final String WEBDRIVER_CHROME_DRIVER = "webdriver.chrome.driver";
    private static final String CHROMEDRIVER_NAME_PART = "chromedriver";

    static {
        if (System.getProperty(WEBDRIVER_CHROME_DRIVER) == null) {
            Optional.ofNullable(findDriver()).ifPresent(driverLocation -> System
                    .setProperty(WEBDRIVER_CHROME_DRIVER, driverLocation));
        }
    }

    @Before
    @Override
    public void setup() throws Exception {
        if (Browser.CHROME == getRunLocallyBrowser()) {
            setDriver(createHeadlessChromeDriver());
        } else {
            super.setup();
        }
    }

    private WebDriver createHeadlessChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        return TestBench.createDriver(new ChromeDriver(options));
    }

    private static String findDriver() {
        String osDir = findOsDir();
        String bitsDir = findBitsDir();

        if (osDir == null || bitsDir == null) {
            System.out.println("Could not determine " + CHROMEDRIVER_NAME_PART + " location.\n"
                    + "  Set the system property " + WEBDRIVER_CHROME_DRIVER
                    + " or update code in" + ChromeBrowserTest.class.getName()
                    + " to recognize your operating system.");
            return null;
        }

        try {
            String path = "../../driver/" + osDir + "/googlechrome/" + bitsDir
                    + "/" + CHROMEDRIVER_NAME_PART;
            File driverLocation = new File(path).getCanonicalFile();

            if (driverLocation.exists()) {
                return driverLocation.getPath();
            } else {
                System.out.println("No " + CHROMEDRIVER_NAME_PART + " found at \""
                        + driverLocation + "\"\n"
                        + "  Verify that the path is correct and that driver-binary-downloader-maven-plugin has been run at least once.");
                return null;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Error trying to locate " + CHROMEDRIVER_NAME_PART + " binary", e);
        }
    }

    private static String findBitsDir() {
        switch (System.getProperty("os.arch", "")) {
        case "x86_64":
        case "amd64":
            return "64bit";
        default:
            return null;
        }
    }

    private static String findOsDir() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac")) {
            return "osx";
        } else if ("linux".equals(osName)) {
            return osName;
        } else {
            return null;
        }
    }

    @Override
    protected List<DesiredCapabilities> getHubBrowsersToTest() {
        return getBrowserCapabilities(Browser.CHROME);
    }
}
