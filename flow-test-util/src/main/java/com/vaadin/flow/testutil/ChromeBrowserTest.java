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
package com.vaadin.flow.testutil;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.LogRecord;

import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.testcategory.ChromeTests;
import com.vaadin.flow.testutil.net.PortProber;
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
 * @since 1.0
 *
 */
@Category(ChromeTests.class)
public class ChromeBrowserTest extends ViewOrUITest {

    private static InetAddress ipv4All;
    private static InetAddress ipv6All;
    private static boolean cdpMismatchLogged = false;

    static {
        try {
            ipv4All = InetAddress.getByName("0.0.0.0");
            ipv6All = InetAddress.getByName("::0");
        } catch (UnknownHostException e) {
            throw new ExceptionInInitializerError(e);
        }

        // Log CDP mismatch message only once
        java.util.logging.Logger rootLogger = java.util.logging.Logger
                .getLogger("");
        java.util.logging.Handler handler = new ConsoleHandler();
        Arrays.stream(rootLogger.getHandlers())
                .forEach(rootLogger::removeHandler);
        rootLogger.addHandler(handler);
        handler.setFilter(new Filter() {
            @Override
            public boolean isLoggable(LogRecord record) {
                if (record.getMessage().matches(
                        "Unable to find an exact match for CDP version (.*), so returning the closest version found: (.*)")) {
                    if (cdpMismatchLogged) {
                        return false;
                    }
                    cdpMismatchLogged = true;
                }
                return true;
            }
        });
    }

    @Before
    @Override
    public void setup() throws Exception {
        if (Browser.CHROME == getRunLocallyBrowser() && !isJavaInDebugMode()) {
            setDriver(createHeadlessChromeDriver(
                    this::updateHeadlessChromeOptions));
        } else {
            super.setup();
        }
    }

    /**
     * Allows modifying the chrome options to be used when running on a local
     * Chrome.
     *
     * @param chromeOptions
     *            chrome options to use when running on a local Chrome
     */
    protected void updateHeadlessChromeOptions(ChromeOptions chromeOptions) {
    }

    static boolean isJavaInDebugMode() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments()
                .toString().contains("jdwp");
    }

    static WebDriver createHeadlessChromeDriver(
            Consumer<ChromeOptions> optionsUpdater) {
        for (int i = 0; i < 3; i++) {
            try {
                return tryCreateHeadlessChromeDriver(optionsUpdater);
            } catch (Exception e) {
                getLogger().warn(
                        "Unable to create chromedriver on attempt " + i, e);
            }
        }
        throw new RuntimeException(
                "Gave up trying to create a chromedriver instance");
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ChromeBrowserTest.class);
    }

    private static WebDriver tryCreateHeadlessChromeDriver(
            Consumer<ChromeOptions> optionsUpdater) {
        ChromeOptions headlessOptions = createHeadlessChromeOptions();
        optionsUpdater.accept(headlessOptions);

        int port = PortProber.findFreePort();
        ChromeDriverService service = new ChromeDriverService.Builder()
                .usingPort(port).withSilent(true).build();
        ChromeDriver chromeDriver = new ChromeDriver(service, headlessOptions);
        return TestBench.createDriver(chromeDriver);
    }

    @Override
    protected List<DesiredCapabilities> getHubBrowsersToTest() {
        return getBrowserCapabilities(Browser.CHROME);
    }

    @Override
    protected List<DesiredCapabilities> getBrowserCapabilities(
            Browser... browsers) {
        return customizeCapabilities(super.getBrowserCapabilities(browsers));
    }

    protected List<DesiredCapabilities> customizeCapabilities(
            List<DesiredCapabilities> capabilities) {

        capabilities.stream()
                .filter(cap -> "chrome".equalsIgnoreCase(cap.getBrowserName()))
                .forEach(cap -> cap.setCapability(ChromeOptions.CAPABILITY,
                        createHeadlessChromeOptions()));

        return capabilities;
    }

    static ChromeOptions createHeadlessChromeOptions() {
        final ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu");

        // When running tests in parallel, a tab may be treated as backgrounded
        // if its window is occluded (aka obscured) by another. This can prevent
        // requestAnimationFrame callbacks and other timing-sensitive events
        // from running, which may cause tests to fail. This flag disables that
        // behavior.
        options.addArguments("--disable-backgrounding-occluded-windows");

        return options;
    }
}
