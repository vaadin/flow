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

import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.ui.UI;

/**
 * Abstract base class for hummingbird testbench tests.
 */
public class AbstractTestBenchTest extends TestBenchHelpers {

    private static final String UI_NOT_FOUND_EXCEPTION_MESSAGE = "Could not determine UI class. Ensure the test is named UIClassIT and is in the same package as the UIClass";
    /**
     * Default port for test server, possibly overridden with system property
     */
    private static final String DEFAULT_SERVER_PORT = "8888";

    /** System property key for the test server port */
    public static final String SERVER_PORT_PROPERTY_KEY = "serverPort";
    public static final String SERVER_PORT = System
            .getProperty(SERVER_PORT_PROPERTY_KEY, DEFAULT_SERVER_PORT);

    private String baseUrl = "http://localhost:" + SERVER_PORT;

    protected void open() {
        open(getUIClass());
    }

    protected void open(String... parameters) {
        open(getUIClass(), parameters);
    }

    protected void open(Class<?> uiClass, String... parameters) {
        String url = getTestURL(uiClass);
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
     * Returns the URL to be used for the test for the provided UI class.
     *
     * @param uiClass
     *            the UI class to show
     * @return the URL for the test
     */
    protected String getTestURL(Class<?> uiClass) {
        String url = getBaseUrl();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url + getDeploymentPath(uiClass);
    }

    protected String getDeploymentPath(Class<?> uiClass) {
        return "/run/" + uiClass.getName();
    }

    protected String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Returns the UI class the current test is connected to. Uses the enclosing
     * class if the test class is a static inner class to a UI class.
     *
     * Test which are not enclosed by a UI class must implement this method and
     * return the UI class they want to test.
     *
     * Note that this method will update the test name to the enclosing class to
     * be compatible with TB2 screenshot naming
     *
     * @return the UI class the current test is connected to
     */
    protected Class<?> getUIClass() {
        final String exceptionMessage = UI_NOT_FOUND_EXCEPTION_MESSAGE;
        try {
            // Convention: SomeIT uses the SomeUI UI class
            String uiClassName = getClass().getName().replaceFirst("IT$", "UI");
            Class<?> cls = Class.forName(uiClassName);
            if (UI.class.isAssignableFrom(cls)) {
                return cls;
            } else {
                throw new RuntimeException(exceptionMessage);
            }
        } catch (Exception e) {
            throw new RuntimeException(exceptionMessage, e);
        }
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

}