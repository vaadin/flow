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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.hummingbird.router.View;
import com.vaadin.ui.UI;

/**
 * Abstract base class for hummingbird TestBench tests, which are based on a
 * {@link View} class.
 */
public abstract class AbstractTestBenchTest extends TestBenchHelpers {

    private String hostnameAndPort = "http://localhost:8888";

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
     * Returns the URL to the root of the server, e.g. "http://localhost:8888"
     *
     * @return the URL to the root
     */
    protected String getRootURL() {
        return hostnameAndPort;
    }

    /**
     * Gets the absolute path to the test (UI, View or servlet), starting with a
     * "/".
     *
     * @return the path to the test, appended to {@link #getRootURL()} for the
     *         full test URL.
     */
    protected String getTestPath() {
        Class<? extends UI> uiClass = getUIClass();
        if (uiClass != null) {
            return "/run/" + uiClass.getName();
        }

        Class<? extends View> viewClass = getViewClass();
        if (viewClass != null) {
            return "/view/" + viewClass.getName();
        }

        throw new RuntimeException(
                "Could not find a View or UI class for the test. Ensure "
                        + getClass().getName().replaceFirst("IT$", "")
                        + "View/UI exists "
                        + " or override either getTestPath() or getViewClass()/getUIClass() in your test");

    }

    /**
     * Returns the UI class the current test is connected to.
     * <p>
     * Uses name matching and replaces "IT" with "UI"
     *
     * @return the UI class the current test is connected to or null if no UI
     *         class was found
     */
    @SuppressWarnings("unchecked")
    protected Class<? extends UI> getUIClass() {
        String uiClassName = getClass().getName().replaceFirst("IT$", "UI");
        try {
            Class<?> cls = Class.forName(uiClassName);
            if (UI.class.isAssignableFrom(cls)) {
                return (Class<? extends UI>) cls;
            }
        } catch (Exception e) {
            // Here only to please Sonar...
            getLogger().log(Level.FINE,
                    "UI for " + getClass().getName() + " not found", e);
        }
        return null;
    }

    private static Logger getLogger() {
        return Logger.getLogger(AbstractTestBenchTest.class.getName());
    }

    /**
     * Returns the View class the current test is connected to.
     * <p>
     * Uses name matching and replaces "IT" with "View"
     *
     * @return the View class the current test is connected to or null if no
     *         View class was found
     */
    @SuppressWarnings("unchecked")
    protected Class<? extends View> getViewClass() {
        String viewClassName = getClass().getName().replaceFirst("IT$", "View");
        try {
            Class<?> cls = Class.forName(viewClassName);
            if (View.class.isAssignableFrom(cls)) {
                return (Class<? extends View>) cls;
            }
        } catch (Exception e) {
            // Here only to please Sonar...
            getLogger().log(Level.FINE,
                    "View for " + getClass().getName() + " not found", e);
        }
        return null;
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