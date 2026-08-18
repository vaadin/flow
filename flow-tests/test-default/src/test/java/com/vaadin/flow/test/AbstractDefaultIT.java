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
package com.vaadin.flow.test;

import java.util.Collections;
import java.util.Enumeration;
import java.util.function.Supplier;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.testbench.BrowserTestBase;
import com.vaadin.testbench.DriverSupplier;

/**
 * Base class for the integration tests in the default configuration module.
 * <p>
 * This is the JUnit 6 (Jupiter) counterpart of the JUnit 4
 * {@code ChromeBrowserTest} used by the other test modules: it builds on
 * TestBench's {@link BrowserTestBase} and re-adds the navigation conveniences
 * ({@link #open()}, {@link #getRootURL()}, {@link #getTestPath()}) that
 * contributors expect. Tests annotate methods with
 * {@link com.vaadin.testbench.BrowserTest @BrowserTest}.
 */
public abstract class AbstractDefaultIT extends BrowserTestBase
        implements DriverSupplier {

    /**
     * Port the application is served on. Matches the {@code server.port} used
     * by the Spring Boot app and can be overridden for parallel module runs via
     * the {@code serverPort} system property.
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
     * Gets the absolute path to the test view, starting with a "/".
     * <p>
     * The default implementation derives the path from the {@link TestFor}
     * annotation on the test class, resolving the {@link Route @Route} of the
     * referenced view through {@link RouteUtil#getRoutePath} (which handles
     * naming-convention defaults, parent {@code @RoutePrefix} layouts and
     * absolute routes). Tests without a single corresponding view can override
     * this method instead of using {@link TestFor}.
     *
     * @return the path appended to {@link #getRootURL()} for the full test URL
     */
    protected String getTestPath() {
        TestFor testFor = getClass().getAnnotation(TestFor.class);
        if (testFor == null) {
            throw new IllegalStateException(getClass().getName()
                    + " must either be annotated with @TestFor(view) or override getTestPath()");
        }
        Class<? extends Component> viewType = testFor.value();
        if (!viewType.isAnnotationPresent(Route.class)) {
            throw new IllegalStateException(viewType.getName()
                    + " must declare a @Route to be used with @TestFor");
        }
        return "/" + RouteUtil.getRoutePath(ROUTE_RESOLUTION_CONTEXT, viewType);
    }

    /**
     * Minimal context for {@link RouteUtil#getRoutePath}. It carries no
     * {@code Lookup}, so route resolution falls back to the default naming
     * provider, which is all that is needed to compute a path from annotations.
     */
    private static final VaadinContext ROUTE_RESOLUTION_CONTEXT = new VaadinContext() {
        @Override
        public <T> T getAttribute(Class<T> type,
                Supplier<T> defaultValueSupplier) {
            return defaultValueSupplier != null ? defaultValueSupplier.get()
                    : null;
        }

        @Override
        public <T> void setAttribute(Class<T> clazz, T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttribute(Class<?> clazz) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration<String> getContextParameterNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String getContextParameter(String name) {
            return null;
        }
    };

    /**
     * Opens the view returned by {@link #getTestPath()} and waits for the dev
     * server to be ready.
     */
    protected void open() {
        getDriver().get(getTestURL());
        waitForDevServer();
    }

    /**
     * Returns the full URL to the test view.
     *
     * @return the URL for the test
     */
    protected String getTestURL() {
        return getRootURL() + getTestPath();
    }

    /**
     * Returns the URL to the root of the server, e.g. "http://localhost:8888".
     *
     * @return the URL to the root
     */
    protected String getRootURL() {
        return "http://localhost:" + SERVER_PORT;
    }
}
