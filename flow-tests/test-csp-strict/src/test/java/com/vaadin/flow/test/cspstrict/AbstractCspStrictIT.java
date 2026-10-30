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
package com.vaadin.flow.test.cspstrict;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.vaadin.testbench.BrowserTestBase;
import com.vaadin.testbench.DriverSupplier;

/**
 * Base class for the integration tests of the strict Content Security Policy
 * module, on JUnit 6 (Jupiter) through TestBench's {@link BrowserTestBase}.
 * <p>
 * Every test opens a view in the browser, which enforces the policy Vaadin
 * sends, and ends with {@link #assertNoCspViolations()}: the scenario of the
 * view is done when the browser reports nothing. The violations are collected
 * by the inline script that {@link CspViolationCollector} adds to every page.
 */
public abstract class AbstractCspStrictIT extends BrowserTestBase
        implements DriverSupplier {

    /**
     * Port the application is served on. Failsafe passes the
     * {@code server.port} property of the pom as the {@code serverPort} system
     * property, and the same property is filtered into the
     * {@code application.properties} of the Spring Boot app, so overriding
     * {@code server.port} on the Maven command line moves both. The fallback is
     * for running a test from the IDE against an app started by Maven.
     */
    public static final int SERVER_PORT = Integer
            .parseInt(System.getProperty("serverPort", "8890"));

    /**
     * Reason of the tests disabled because no page renders yet: the constructor
     * of {@code Page} registers event data for the browser details, which the
     * strict mode rejects, so creating the {@code UI} fails for every request.
     * Goes away with the first fix of
     * https://github.com/vaadin/flow/issues/26152.
     */
    public static final String NO_UI_IN_STRICT_MODE = "Blocked by https://github.com/vaadin/flow/issues/26152: "
            + "Page's constructor registers event data, so no UI can be created in strict mode";

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
     * Gets the path of the view under test, starting with a "/".
     *
     * @return the path appended to {@link #getRootURL()}
     */
    protected abstract String getTestPath();

    /**
     * Opens the view under test.
     */
    protected void open() {
        getDriver().get(getRootURL() + getTestPath());
    }

    /**
     * Gets the URL of the root of the server, e.g. "http://localhost:8890".
     *
     * @return the root URL
     */
    protected String getRootURL() {
        return "http://localhost:" + SERVER_PORT;
    }

    /**
     * Fetches the bootstrap page of the view under test without a browser, to
     * check the response headers and the HTML as sent.
     *
     * @return the response
     */
    protected HttpResponse<String> fetchBootstrapPage()
            throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest
                    .newBuilder(URI.create(getRootURL() + getTestPath()))
                    .build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    /**
     * Asserts that the browser has not reported a Content Security Policy
     * violation since the page was opened. The failure message lists the
     * violations, each with the directive, the blocked URI, the source location
     * and a sample of the blocked content.
     */
    protected void assertNoCspViolations() {
        Object violations = ((JavascriptExecutor) getDriver())
                .executeScript("return JSON.stringify("
                        + CspViolationCollector.VIOLATIONS + ", null, 1)");
        // Qualified: TestBench's inherited assertEquals(WebElement, WebElement)
        // shadows the static import
        Assertions.assertEquals("[]", violations,
                "The browser reported Content Security Policy violations");
    }
}
