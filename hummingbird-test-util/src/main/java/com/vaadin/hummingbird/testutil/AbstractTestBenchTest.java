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
package com.vaadin.hummingbird.testutil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.hummingbird.router.View;
import com.vaadin.testbench.annotations.BrowserConfiguration;
import com.vaadin.testbench.annotations.BrowserFactory;
import com.vaadin.testbench.annotations.RunOnHub;
import com.vaadin.testbench.parallel.Browser;
import com.vaadin.testbench.parallel.DefaultBrowserFactory;

/**
 * Abstract base class for hummingbird TestBench tests, which are based on a
 * {@link View} class.
 */
@RunOnHub("tb3-hub.intra.itmill.com")
@BrowserFactory(DefaultBrowserFactory.class)
@LocalExecution
public abstract class AbstractTestBenchTest extends TestBenchHelpers {

    /**
     * Default port for test server, possibly overridden with system property.
     */
    private static final String DEFAULT_SERVER_PORT = "8888";

    /** System property key for the test server port. */
    public static final String SERVER_PORT_PROPERTY_KEY = "serverPort";
    /**
     * Server port resolved by system property
     * {@value #SERVER_PORT_PROPERTY_KEY} or the default
     * {@value #DEFAULT_SERVER_PORT}.
     */
    public static final int SERVER_PORT = Integer.parseInt(
            System.getProperty(SERVER_PORT_PROPERTY_KEY, DEFAULT_SERVER_PORT));

    public static final String USE_HUB_PROPERTY = "test.use.hub";

    public static final boolean USE_HUB = Boolean.TRUE.toString()
            .equals(System.getProperty(USE_HUB_PROPERTY, "false"));

    protected void open() {
        open((String[]) null);
    }

    protected void open(String... parameters) {
        String url = getTestURL(parameters);

        getDriver().get(url);
    }

    protected void openProduction(String... parameters) {
        String url = getTestURL(parameters);
        if (!url.contains("/view/")) {
            throw new IllegalArgumentException(
                    "Production mode is only available for /view/ URLs");
        }
        url = url.replace("/view/", "/view-production/");
        driver.get(url);

        getDriver().get(url);
    }

    /**
     * Opens URL that corresponds to current test case using JavaScript
     * {@code window.href} functionality. This allows to open page instantly,
     * not waiting for the page to load.
     *
     * <p>
     * <b>LIMITATION</b>
     * </p>
     * You will receive {@link NullPointerException} if you will use the
     * {@link com.vaadin.testbench.commands.TestBenchCommands#compareScreen(String)}
     * method or any its overload before page is fully loaded. This happens
     * because method {@link PhantomJSDriver#getScreenshotAs(OutputType)}
     * receives empty base64 string as a command execution result.
     *
     * @param parameters
     *            parameters to add to URL to open.
     */
    protected void openUsingJs(String... parameters) {
        ((JavascriptExecutor) getDriver()).executeScript(
                String.format("window.location='%s';", getTestURL(parameters)));
    }

    /**
     * Returns the URL to be used for the test.
     *
     * @param parameters
     *            query string parameters to add to the url
     *
     * @return the URL for the test
     */
    protected String getTestURL(String... parameters) {
        String url = getRootURL();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        url = url + getTestPath();

        if (parameters != null && parameters.length != 0) {
            if (!url.contains("?")) {
                url += "?";
            } else {
                url += "&";
            }

            url += Arrays.stream(parameters).collect(Collectors.joining("&"));
        }

        return url;
    }

    /**
     * Gets the absolute path to the test, starting with a "/".
     *
     * @return the path to the test, appended to {@link #getRootURL()} for the
     *         full test URL.
     */
    protected abstract String getTestPath();

    /**
     * Returns the URL to the root of the server, e.g. "http://localhost:8888"
     *
     * @return the URL to the root
     */
    protected String getRootURL() {
        return "http://" + getDeploymentHostname() + ":" + getDeploymentPort();
    }

    /**
     * Executes the given JavaScript.
     *
     * @param script
     *            the script to execute
     * @param args
     *            optional arguments for the script
     * @return whatever
     *         {@link org.openqa.selenium.JavascriptExecutor#executeScript(String, Object...)}
     *         returns
     */
    protected Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) getDriver()).executeScript(script, args);
    }

    /**
     * Sets up the test to run using Phantom JS.
     *
     */
    protected void setupPhantomJsDriver() {
        setupPhantomJsDriver(new DesiredCapabilities());
    }

    /**
     * Sets up the test to run using Phantom JS with the additional capabilities
     * specified.
     *
     * @param extraCapabilities
     *            additional capabilities to pass to the driver
     */
    protected void setupPhantomJsDriver(Capabilities extraCapabilities) {
        DesiredCapabilities cap = DesiredCapabilities.phantomjs();
        cap.merge(extraCapabilities);
        PhantomJSDriver driver = new PhantomJSDriver(cap);
        setDriver(driver);
    }

    /**
     * Compares the given reference screenshot to the current and fails the test
     * if it doesn't match.
     *
     * @param identifier
     *            the identifier to use for the screenshot, becomes part of the
     *            screenshot name in the
     *            {@code module-folder/reference-screenshots/} folder
     * @throws IOException
     *             if there was a problem accessing the reference image
     */
    protected void verifyScreenshot(String identifier) throws IOException {
        Assert.assertTrue(
                "SCREENSHOT MATCH FAILURE: <" + identifier
                        + "> does not match expected.",
                testBench().compareScreen(identifier));
    }

    /**
     * Produces a collection of browsers to run the test on. This method is
     * executed by the test runner when determining how many test methods to
     * invoke and with what parameters. For each returned value a test method is
     * ran and before running that,
     * {@link #setDesiredCapabilities(DesiredCapabilities)} is invoked with the
     * value returned by this method.
     *
     * @return The browsers to run the test on
     */
    @BrowserConfiguration
    public List<DesiredCapabilities> getBrowsersToTest() {
        if (getLocalExecution().isPresent()) {
            return getBrowserCapabilities(getLocalExecution().get().value());
        }
        return getHubBrowsersToTest();
    }

    /**
     * Gets the browsers capabilities list to execute test on the tests Hub.
     * <p>
     * This list will be used only for the tests Hub. Local test execution is
     * managed by {@link LocalExecution} annotation.
     * <p>
     * The method {@link #getBrowsersToTest()} delegates the logic to this
     * method in case {@link #getLocalExecution()} return value is an empty
     * optional (i.e. the tests Hub is used).
     * 
     * @return the browsers capabilities list to execute test on the tests Hub
     */
    protected List<DesiredCapabilities> getHubBrowsersToTest() {
        return getBrowserCapabilities(Browser.IE11, Browser.FIREFOX,
                Browser.CHROME, Browser.PHANTOMJS);
    }

    /**
     * Gets browser capabilities for the provided <code>browsers</code>.
     * 
     * @param browsers
     *            a browsers list
     * @return the capabilities for the given <code>browsers</code>
     */
    protected List<DesiredCapabilities> getBrowserCapabilities(
            Browser... browsers) {
        List<DesiredCapabilities> capabilities = new ArrayList<>();
        for (Browser browser : browsers) {
            capabilities.add(browser.getDesiredCapabilities());
        }
        return capabilities;
    }

    /**
     * Used to determine what URL to initially open for the test.
     *
     * @return the host name of development server
     */
    protected String getDeploymentHostname() {
        if (getLocalExecution().isPresent()) {
            return "localhost";
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface nwInterface = interfaces.nextElement();
                if (!nwInterface.isUp() || nwInterface.isLoopback()
                        || nwInterface.isVirtual()) {
                    continue;
                }
                Optional<String> address = getHostAddress(nwInterface);
                if (address.isPresent()) {
                    return address.get();
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException("Could not find the host name", e);
        }
        throw new RuntimeException(
                "No compatible (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) ip address found.");
    }

    /**
     * Used to determine what port the test is running on.
     *
     * @return The port the test is running on, by default
     *         AbstractTestBenchTest.DEFAULT_SERVER_PORT
     */
    protected int getDeploymentPort() {
        return SERVER_PORT;
    }

    @Override
    protected String getHubHostname() {
        if (getLocalExecution().isPresent()) {
            return "localhost";
        }
        return super.getHubHostname();
    }

    @Override
    protected Browser getRunLocallyBrowser() {
        if (getLocalExecution().isPresent()) {
            return getLocalExecution().get().value();
        }
        return super.getRunLocallyBrowser();
    }

    @Override
    protected String getRunLocallyBrowserVersion() {
        if (getLocalExecution().isPresent()) {
            return getLocalExecution().get().browserVersion();
        }
        return super.getRunLocallyBrowserVersion();
    }

    /**
     * Gets local execution ({@link LocalExecution}) configuration for the test.
     * <p>
     * If this method return an empty optional then test with be run on the test
     * Hub
     * 
     * @see LocalExecution
     * 
     * @return an optional configuration, or an empty optional if configuration
     *         is disabled or not available
     * 
     */
    protected Optional<LocalExecution> getLocalExecution() {
        if (USE_HUB) {
            return Optional.empty();
        }
        return Optional
                .ofNullable(getClass().getAnnotation(LocalExecution.class))
                .filter(LocalExecution::active);
    }

    private Optional<String> getHostAddress(NetworkInterface nwInterface) {
        Enumeration<InetAddress> addresses = nwInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress address = addresses.nextElement();
            if (address.isLoopbackAddress()) {
                continue;
            }
            if (address.isSiteLocalAddress()) {
                return Optional.of(address.getHostAddress());
            }
        }
        return Optional.empty();
    }
}
