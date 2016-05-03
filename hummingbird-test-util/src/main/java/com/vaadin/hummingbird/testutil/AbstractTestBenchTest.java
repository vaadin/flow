/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.hummingbird.router.View;
import com.vaadin.testbench.ScreenshotOnFailureRule;
import com.vaadin.testbench.TestBenchDriverProxy;

/**
 * Abstract base class for hummingbird TestBench tests, which are based on a
 * {@link View} class.
 */
public abstract class AbstractTestBenchTest extends TestBenchHelpers {

    /**
     * The rule used for screenshot failures.
     */
    @Rule
    public ScreenshotOnFailureRule screenshotOnFailure = new ScreenshotOnFailureRule(
            this, true);

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
    public static final String SERVER_PORT = System
            .getProperty(SERVER_PORT_PROPERTY_KEY, DEFAULT_SERVER_PORT);

    private String hostnameAndPort = "http://localhost:" + SERVER_PORT;

    protected void open() {
        open((String[]) null);
    }

    protected void open(String... parameters) {
        String url = getTestURL();
        if (parameters != null && parameters.length != 0) {
            if (!url.contains("?")) {
                url += "?";
            } else {
                url += "&";
            }

            url += Arrays.stream(parameters).collect(Collectors.joining("&"));
        }

        getDriver().get(url);
    }

    /**
     * Returns the URL to be used for the test.
     *
     * @return the URL for the test
     */
    protected String getTestURL() {
        String url = getRootURL();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url + getTestPath();
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
        return hostnameAndPort;
    }

    /**
     * Executes the given Javascript.
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
     */
    protected void setupPhantomJsDriver() {
        DesiredCapabilities cap = DesiredCapabilities.phantomjs();
        FixedPhantomJSDriver driver = new FixedPhantomJSDriver(cap);
        setDriver(driver);
        driver.setTestBenchDriverProxy((TestBenchDriverProxy) getDriver());
    }

}
