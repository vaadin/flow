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
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.vaadin.flow.testcategory.ChromeTests;
import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.parallel.Browser;

/**
 * Base class for TestBench tests to run in Chrome with customized options,
 * which enable device emulation mode by default.
 * <p>
 * This facilitates testing with network connection overrides, e. g., using
 * offline mode in the tests.
 * <p>
 * It is required to set system property with path to the driver to be able to
 * run the test.
 * <p>
 * The test can be executed locally and on a test Hub. ChromeDriver is used if
 * test is executed locally.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@Category(ChromeTests.class)
public class ChromeDeviceTest extends ViewOrUITest {
    private DevToolsWrapper devTools = null;

    protected DevToolsWrapper getDevTools() {
        return devTools;
    }

    static boolean isJavaInDebugMode() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments()
                .toString().contains("jdwp");
    }

    @Before
    @Override
    public void setup() throws Exception {
        ChromeOptions chromeOptions = customizeChromeOptions(
                new ChromeOptions());

        WebDriver driver;
        if (Browser.CHROME == getRunLocallyBrowser()) {
            driver = new ChromeDriver(chromeOptions);
        } else {
            URL remoteURL = new URL(getHubURL());
            driver = new RemoteWebDriver(remoteURL, chromeOptions);
            setDevToolsRuntimeCapabilities((RemoteWebDriver) driver, remoteURL);
        }

        devTools = new DevToolsWrapper(driver);

        setDriver(TestBench.createDriver(driver));
    }

    /**
     * Customizes given Chrome options to enable network connection emulation.
     *
     * @param chromeOptions
     *            Chrome options to customize
     * @return customized Chrome options instance
     */
    protected ChromeOptions customizeChromeOptions(
            ChromeOptions chromeOptions) {
        final Map<String, Object> mobileEmulationParams = new HashMap<>();
        mobileEmulationParams.put("deviceMetrics",
                Map.of("width", 1280, "height", 950, "pixelRatio", 1));

        chromeOptions.setExperimentalOption("mobileEmulation",
                mobileEmulationParams);

        if (getDeploymentHostname().equals("localhost")) {
            // Use headless Chrome for running locally
            if (!isJavaInDebugMode()) {
                chromeOptions.addArguments("--headless=new", "--disable-gpu");
            }
        } else {
            // Enable service workers over http remote connection
            chromeOptions.addArguments(String.format(
                    "--unsafely-treat-insecure-origin-as-secure=%s",
                    getRootURL()));

            // NOTE: this flag is not supported in headless Chrome, see
            // https://crbug.com/814146

            // For test stability on Linux when not running headless.
            // https://stackoverflow.com/questions/50642308/webdriverexception-unknown-error-devtoolsactiveport-file-doesnt-exist-while-t
            chromeOptions.addArguments("--disable-dev-shm-usage");
        }

        return chromeOptions;
    }

    @Override
    protected List<DesiredCapabilities> getHubBrowsersToTest() {
        return getBrowserCapabilities(Browser.CHROME);
    }

    public void waitForServiceWorkerReady() {
        Assert.assertTrue("Should have navigator.serviceWorker",
                (Boolean) executeScript("return !!navigator.serviceWorker;"));

        // Wait until service worker is ready
        Assert.assertTrue("Should have service worker registered",
                (Boolean) ((JavascriptExecutor) getDriver()).executeAsyncScript(
                        "const done = arguments[arguments.length - 1];"
                                + "const timeout = new Promise("
                                + "  resolve => setTimeout(resolve, 100000)"
                                + ");" + "Promise.race(["
                                + "  navigator.serviceWorker.ready,"
                                + "  timeout])"
                                + ".then(result => done(!!result));"));
    }

    /**
     * Sets the `se:cdp` and `se:cdpVersion` capabilities for the remote web
     * driver. Note that the capabilities are set at runtime because they depend
     * on the session id that becomes only available after the driver is
     * initialized. Without these capabilities, Selenium cannot establish a
     * connection with DevTools.
     */
    private void setDevToolsRuntimeCapabilities(RemoteWebDriver driver,
            URL remoteUrl) throws RuntimeException {
        try {
            Field capabilitiesField = RemoteWebDriver.class
                    .getDeclaredField("capabilities");
            capabilitiesField.setAccessible(true);

            String sessionId = driver.getSessionId().toString();
            String devtoolsUrl = String.format("ws://%s:%s/devtools/%s/page",
                    remoteUrl.getHost(), remoteUrl.getPort(), sessionId);

            MutableCapabilities mutableCapabilities = (MutableCapabilities) capabilitiesField
                    .get(driver);
            mutableCapabilities.setCapability("se:cdp", devtoolsUrl);
            mutableCapabilities.setCapability("se:cdpVersion",
                    mutableCapabilities.getBrowserVersion());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to set DevTools capabilities for RemoteWebDriver");
        }
    }
}
