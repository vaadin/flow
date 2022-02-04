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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v97.network.Network;
import org.openqa.selenium.mobile.NetworkConnection;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.MutableCapabilities;

import com.vaadin.flow.testcategory.ChromeTests;
import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.TestBenchDriverProxy;
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

    /**
     * Sets up the chrome driver path in a system variable.
     */
    @BeforeClass
    public static void setChromeDriverPath() {
        ChromeDriverLocator.fillEnvironmentProperty();
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
            driver = new RemoteWebDriver(new URL(getHubURL()), chromeOptions);
            enableDevToolsForRemoteWebDriver((RemoteWebDriver) driver);
        }

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
        // Unfortunately using offline emulation ("setNetworkConnection"
        // session command) in Chrome requires the "networkConnectionEnabled"
        // capability, which is:
        // - Not W3C WebDriver API compliant, so we disable W3C protocol
        // - device mode: mobileEmulation option with some device settings

        final Map<String, Object> mobileEmulationParams = new HashMap<>();
        mobileEmulationParams.put("deviceName", "Laptop with touch");

        chromeOptions.setExperimentalOption("w3c", false);
        chromeOptions.setExperimentalOption("mobileEmulation",
                mobileEmulationParams);
        chromeOptions.setCapability("networkConnectionEnabled", true);

        if (getDeploymentHostname().equals("localhost")) {
            // Use headless Chrome for running locally
            if (!isJavaInDebugMode()) {
                chromeOptions.addArguments("--headless", "--disable-gpu");
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

    /**
     * Change network connection type in the browser.
     *
     * @param connectionType
     *            the new connection type
     * @throws IOException
     */
    protected void setConnectionType(
            NetworkConnection.ConnectionType connectionType)
            throws IOException {
        RemoteWebDriver driver = (RemoteWebDriver) ((TestBenchDriverProxy) getDriver())
                .getWrappedDriver();
        final Map<String, Integer> parameters = new HashMap<>();
        parameters.put("type", connectionType.hashCode());
        final Map<String, Object> connectionParams = new HashMap<>();
        connectionParams.put("parameters", parameters);
        Response response = driver.getCommandExecutor()
                .execute(new Command(driver.getSessionId(),
                        "setNetworkConnection", connectionParams));
        if (response.getStatus() != 0) {
            throw new RuntimeException("Unable to set connection type");
        }
    }

    /**
     * Controls the throttling `Offline` option in DevTools via the respective
     * Selenium API.
     *
     * @param isEnabled
     *            whether to enable the offline mode.
     */
    protected void setOfflineEnabled(Boolean isEnabled) {
        RemoteWebDriver remoteDriver = (RemoteWebDriver) ((TestBenchDriverProxy) getDriver())
                .getWrappedDriver();
        WebDriver driver = new Augmenter().augment(remoteDriver);
        DevTools devTools = ((HasDevTools) driver).getDevTools();
        devTools.createSessionIfThereIsNotOne();
        devTools.send(Network.emulateNetworkConditions(isEnabled, -1, -1, -1,
                Optional.empty()));
    }

    /**
     * Controls the `Disable cache` option in DevTools via the respective
     * Selenium API.
     *
     * @param isDisabled
     *            whether to disable the browser cache.
     */
    protected void setCacheDisabled(Boolean isDisabled) {
        RemoteWebDriver remoteDriver = (RemoteWebDriver) ((TestBenchDriverProxy) getDriver())
                .getWrappedDriver();
        WebDriver driver = new Augmenter().augment(remoteDriver);
        DevTools devTools = ((HasDevTools) driver).getDevTools();
        devTools.createSessionIfThereIsNotOne();
        devTools.send(Network.enable(Optional.empty(), Optional.empty(),
                Optional.of(100000000)));
        devTools.send(Network.setCacheDisabled(isDisabled));
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

    private void enableDevToolsForRemoteWebDriver(RemoteWebDriver driver) {
        // Before proceeding, reach into the driver and manipulate the
        // capabilities to
        // include the se:cdp and se:cdpVersion keys.
        try {
            URL hubUrl = new URL(getHubURL());

            Field capabilitiesField = RemoteWebDriver.class
                    .getDeclaredField("capabilities");
            capabilitiesField.setAccessible(true);

            String sessionId = driver.getSessionId().toString();
            String devtoolsUrl = String.format("ws://%s:%s/devtools/%s/page",
                    hubUrl.getHost(), hubUrl.getPort(), sessionId);

            MutableCapabilities mutableCapabilities = (MutableCapabilities) capabilitiesField
                    .get(driver);
            mutableCapabilities.setCapability("se:cdp", devtoolsUrl);
            mutableCapabilities.setCapability("se:cdpVersion",
                    mutableCapabilities.getBrowserVersion());
        } catch (Exception e) {
            System.err.println(
                    "Failed to spoof RemoteWebDriver capabilities :sadpanda:");
        }
    }
}
