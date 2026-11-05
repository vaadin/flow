/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.testutil.junit5;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.vaadin.testbench.BrowserTestBase;
import com.vaadin.testbench.DriverSupplier;

/**
 * Base class for the integration tests that run on the JUnit Platform.
 * <p>
 * This is the JUnit 6 (Jupiter) counterpart of the JUnit 4
 * {@code ChromeBrowserTest} in {@code flow-test-util}: it builds on TestBench's
 * {@link BrowserTestBase}, supplies a headless Chrome driver and adds the
 * navigation helpers {@link #open(String)} and {@link #getRootURL()}. Tests
 * annotate methods with {@link com.vaadin.testbench.BrowserTest @BrowserTest}.
 */
public abstract class AbstractBrowserIT extends BrowserTestBase
        implements DriverSupplier {

    /**
     * Port the application is served on, read from the {@code serverPort}
     * system property that the root POM's failsafe configuration sets from
     * {@code server.port}, so that modules running in parallel do not share a
     * port.
     */
    public static final int SERVER_PORT = Integer
            .parseInt(System.getProperty("serverPort", "8888"));

    /**
     * Supplies the local Chrome driver used by TestBench. The same headless
     * options as the JUnit 4 {@code ChromeBrowserTest} are applied so the tests
     * run reliably in CI and other headless/container environments.
     *
     * @return a configured Chrome {@link WebDriver}
     */
    @Override
    public WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu",
                "--disable-backgrounding-occluded-windows");
        // Required when running in CI/containers that disable the Chrome
        // sandbox (no unprivileged user namespaces) and have a small /dev/shm.
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        return new ChromeDriver(options);
    }

    /**
     * Opens the given path on the test server and waits for the dev server to
     * be ready.
     * <p>
     * Waiting is required because in dev mode requests made while the frontend
     * bundle is still being built are answered with a placeholder page. That
     * page carries neither the view contents nor a Flow client, so navigating
     * without waiting makes element lookups and server side navigation, such as
     * forwarding, fail.
     *
     * @param path
     *            the path to open, starting with a "/"
     */
    protected void open(String path) {
        getDriver().get(getRootURL() + path);
        waitForDevServer();
    }

    /**
     * Returns the URL to the root of the server, with no trailing slash, e.g.
     * "http://localhost:8888".
     *
     * @return the URL to the root
     */
    protected String getRootURL() {
        return "http://localhost:" + SERVER_PORT;
    }
}
