package com.vaadin.hummingbird.uitest;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.testbench.ScreenshotOnFailureRule;
import com.vaadin.ui.UI;

public class AbstractTestBenchTest extends TestBenchHelpers {

    @Rule
    public ScreenshotOnFailureRule screenshotOnFailure = new ScreenshotOnFailureRule(
            this, true);

    private String baseUrl = "http://localhost:8888";

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
        try {
            // Convention: SomeIT uses the SomeUI UI class
            String uiClassName = getClass().getName().replaceFirst("IT$", "UI");
            Class<?> cls = Class.forName(uiClassName);
            if (UI.class.isAssignableFrom(cls)) {
                return cls;
            }
        } catch (Exception e) {
        }
        throw new RuntimeException(
                "Could not determine UI class. Ensure the test is named UIClassIT and is in the same package as the UIClass");
    }

    /**
     * Executes the given Javascript
     *
     * @param script
     *            the script to execute
     * @return whatever
     *         {@link org.openqa.selenium.JavascriptExecutor#executeScript(String, Object...)}
     *         returns
     */
    protected Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) getDriver()).executeScript(script, args);
    }

}