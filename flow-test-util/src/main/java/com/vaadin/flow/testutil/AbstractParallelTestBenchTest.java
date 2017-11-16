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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.testbench.annotations.BrowserConfiguration;
import com.vaadin.testbench.annotations.BrowserFactory;
import com.vaadin.testbench.annotations.RunLocally;
import com.vaadin.testbench.annotations.RunOnHub;
import com.vaadin.testbench.parallel.Browser;
import com.vaadin.testbench.parallel.DefaultBrowserFactory;
import com.vaadin.testbench.parallel.ParallelTest;

/**
 * Abstract base class for parallel flow TestBench tests.
 */
@RunOnHub
@RunLocally
@BrowserFactory(DefaultBrowserFactory.class)
@LocalExecution
public class AbstractParallelTestBenchTest extends ParallelTest {

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

    @Before
    @Override
    public void setup() throws Exception {
        if(USE_HUB) {
            setDesiredCapabilities(Browser.CHROME.getDesiredCapabilities());
        }
        super.setup();
    }

    /**
     * Returns the URL to the root of the server, e.g. "http://localhost:8888"
     *
     * @return the URL to the root
     */
    protected String getRootURL() {
        return "http://" + getDeploymentHostname() + ":" + getDeploymentPort();
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

    @Override
    protected Browser getRunLocallyBrowser() {
        if (USE_HUB) {
            return null;
        }
        return Browser.CHROME;
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
        return getCurrentHostAddress();
    }

    /**
     * Returns host address that can be targeted from the outside, like from a
     * test hub.
     *
     * @return host address
     * @throws RuntimeException
     *             if host name could not be determined or
     *             {@link SocketException} was caught during the determination.
     */
    protected String getCurrentHostAddress() {
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

    private static Optional<String> getHostAddress(
            NetworkInterface nwInterface) {
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
        return getBrowserCapabilities(Browser.CHROME);
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

}
