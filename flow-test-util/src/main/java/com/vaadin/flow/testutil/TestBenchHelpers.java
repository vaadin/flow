/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.LoggerFactory;

import com.vaadin.testbench.parallel.ParallelTest;

/**
 * Helpers for running testbench tests.
 *
 * @since 1.0
 */
public class TestBenchHelpers extends ParallelTest {

    /**
     * Waits up to 10s for the given condition to become false. Use e.g. as
     * {@link #waitUntilNot(ExpectedCondition)}.
     *
     * @param condition
     *            the condition to wait for to become false
     * @param <T>
     *            the return type of the expected condition
     */
    protected <T> void waitUntilNot(ExpectedCondition<T> condition) {
        waitUntilNot(condition, 10);
    }

    /**
     * Returns true if an element can be found from the driver with given
     * selector.
     *
     * @param by
     *            the selector used to find element
     * @return true if the element can be found
     */
    public boolean isElementPresent(By by) {
        try {
            WebElement element = getDriver().findElement(by);
            return element != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Simulate DnD of {@code source} element into the {@code target} element.
     *
     * @param source
     * @param target
     */
    public void dragAndDrop(WebElement source, WebElement target) {
        getCommandExecutor().executeScript(LazyDndSimulationLoad.DND_SCRIPT,
                source, target);
    }

    /**
     * Waits the given number of seconds for the given condition to become
     * false. Use e.g. as {@link #waitUntilNot(ExpectedCondition)}.
     *
     * @param condition
     *            the condition to wait for to become false
     * @param timeoutInSeconds
     *            the number of seconds to wait
     * @param <T>
     *            the return type of the expected condition
     */
    protected <T> void waitUntilNot(ExpectedCondition<T> condition,
            long timeoutInSeconds) {
        waitUntil(ExpectedConditions.not(condition), timeoutInSeconds);
    }

    protected void waitForElementPresent(final By by) {
        waitUntil(ExpectedConditions.presenceOfElementLocated(by));
    }

    protected void waitForElementNotPresent(final By by) {
        waitUntil(input -> input.findElements(by).isEmpty());
    }

    protected void waitForElementVisible(final By by) {
        waitUntil(ExpectedConditions.visibilityOfElementLocated(by));
    }

    /**
     * Checks if the given element has the given class name.
     *
     * Matches only full class names, i.e. has ("foo") does not match
     * class="foobar"
     *
     * @param element
     *            the element to test
     * @param className
     *            the class names to match
     * @return <code>true</code> if matches, <code>false</code> if not
     */
    protected boolean hasCssClass(WebElement element, String className) {
        String classes = element.getAttribute("class");
        if (classes == null || classes.isEmpty()) {
            return className == null || className.isEmpty();
        }
        return Stream.of(classes.split(" ")).anyMatch(className::equals);
    }

    /**
     * Assert that the two elements are equal.
     * <p>
     * Can be removed if https://dev.vaadin.com/ticket/18484 is fixed.
     *
     * @param expectedElement
     *            the expected element
     * @param actualElement
     *            the actual element
     */
    protected static void assertEquals(WebElement expectedElement,
            WebElement actualElement) {
        WebElement unwrappedExpected = expectedElement;
        WebElement unwrappedActual = actualElement;
        while (unwrappedExpected instanceof WrapsElement) {
            unwrappedExpected = ((WrapsElement) unwrappedExpected)
                    .getWrappedElement();
        }
        while (unwrappedActual instanceof WrapsElement) {
            unwrappedActual = ((WrapsElement) unwrappedActual)
                    .getWrappedElement();
        }
        Assert.assertEquals(unwrappedExpected, unwrappedActual);
    }

    /**
     * Returns <code>true</code> if a component can be found with given By
     * selector in the shadow DOM of the {@code webComponent}.
     *
     * @param webComponent
     *            the web component owning shadow DOM to start search from
     * @param by
     *            the selector used to find element
     * @return <code>true</code> if the component can be found
     */
    protected boolean isPresentInShadowRoot(WebElement webComponent, By by) {
        return !findInShadowRoot(webComponent, by).isEmpty();
    }

    /**
     * Find the first {@link WebElement} using the given {@link By} selector.
     *
     * @param shadowRootOwner
     *            the web component owning shadow DOM to start search from
     * @param by
     *            the selector used to find element
     * @return an element from shadow root, if located
     * @throws AssertionError
     *             if shadow root is not present or element is not found in the
     *             shadow root
     */
    protected WebElement getInShadowRoot(WebElement shadowRootOwner, By by) {
        return getShadowRoot(shadowRootOwner).findElements(by).stream()
                .findFirst().orElseThrow(() -> new AssertionError(
                        "Could not find required element in the shadowRoot"));
    }

    /**
     * Find all {@link WebElement}s using the given {@link By} selector.
     *
     * @param webComponent
     *            the web component owning shadow DOM to start search from
     * @param by
     *            the selector used to find elements
     * @return a list of found elements
     */
    protected List<WebElement> findInShadowRoot(WebElement webComponent,
            By by) {
        return getShadowRoot(webComponent).findElements(by);
    }

    /**
     * Executes the given JavaScript.
     * <p>
     * To send arguments to the script, you can use the <code>arguments</code>
     * variable. <br>
     * For example:
     * <code>executeScript("window.alert(arguments[0]);", "Alert message!");</code>.
     * <p>
     * To be able to use the return value of the JavaScript, you must explicitly
     * declare a <code>return</code> statement. <br>
     * For example: <code>executeScript("return window.name;");</code>.
     *
     * @param script
     *            the script to execute
     * @param args
     *            optional arguments for the script
     * @return whatever
     *         {@link JavascriptExecutor#executeScript(String, Object...)}
     *         returns
     */
    @Override
    protected Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) getDriver()).executeScript(script, args);
    }

    /**
     * Scrolls the page by given amount of x and y deltas. Actual scroll values
     * can be different if any delta is bigger then the corresponding document
     * dimension.
     *
     * @param deltaX
     *            the offset in pixels to scroll horizontally
     * @param deltaY
     *            the offset in pixels to scroll vertically
     */
    protected void scrollBy(int deltaX, int deltaY) {
        executeScript("window.scrollBy(" + deltaX + ',' + deltaY + ");");
    }

    /**
     * Scrolls the page to the element given using javascript.
     *
     * Standard Selenium api does not work for current newest Chrome and
     * ChromeDriver.
     *
     * @param element
     *            the element to scroll to, not {@code null}
     */
    protected void scrollToElement(WebElement element) {
        Objects.requireNonNull(element,
                "The element to scroll to should not be null");
        getCommandExecutor().executeScript("arguments[0].scrollIntoView(true);",
                element);
    }

    /**
     * Scrolls the page to the element specified and clicks it.
     *
     * @param element
     *            the element to scroll to and click
     */
    protected void scrollIntoViewAndClick(WebElement element) {
        scrollToElement(element);
        element.click();
    }

    /**
     * Gets current scroll position on x axis.
     *
     * @return current scroll position on x axis.
     */
    protected int getScrollX() {
        return ((Long) executeScript("return window.pageXOffset")).intValue();
    }

    /**
     * Gets current scroll position on y axis.
     *
     * @return current scroll position on y axis.
     */
    protected int getScrollY() {
        return ((Long) executeScript("return window.pageYOffset")).intValue();
    }

    /**
     * Clicks on the element, using JS. This method is more convenient then
     * Selenium {@code findElement(By.id(urlId)).click()}, because Selenium
     * method changes scroll position, which is not always needed.
     *
     * @param elementId
     *            id of the
     */
    protected void clickElementWithJs(String elementId) {
        executeScript(String.format("document.getElementById('%s').click();",
                elementId));
    }

    /**
     * Clicks on the element, using JS. This method is more convenient then
     * Selenium {@code element.click()}, because Selenium method changes scroll
     * position, which is not always needed.
     *
     * @param element
     *            the element to be clicked on
     */
    protected void clickElementWithJs(WebElement element) {
        executeScript("arguments[0].click();", element);
    }

    /**
     * Gets the log entries from the browser that have the given logging level
     * or higher.
     *
     * @param level
     *            the minimum severity of logs included
     * @return log entries from the browser
     */
    protected List<LogEntry> getLogEntries(Level level) {
        // https://github.com/vaadin/testbench/issues/1233
        getCommandExecutor().waitForVaadin();

        return driver.manage().logs().get(LogType.BROWSER).getAll().stream()
                .filter(logEntry -> logEntry.getLevel().intValue() >= level
                        .intValue())
                // we always have this error
                .filter(logEntry -> !logEntry.getMessage()
                        .contains("favicon.ico"))
                .collect(Collectors.toList());
    }

    /**
     * Checks browser's log entries, throws an error for any client-side error
     * and logs any client-side warnings.
     *
     * @param acceptableMessagePredicate
     *            allows to ignore log entries whose message is accaptable
     *
     * @throws AssertionError
     *             if an error is found in the browser logs
     */
    protected void checkLogsForErrors(
            Predicate<String> acceptableMessagePredicate) {
        getLogEntries(Level.WARNING).forEach(logEntry -> {
            if ((Objects.equals(logEntry.getLevel(), Level.SEVERE)
                    || logEntry.getMessage().contains("404"))
                    && !acceptableMessagePredicate
                            .test(logEntry.getMessage())) {
                throw new AssertionError(String.format(
                        "Received error message in browser log console right after opening the page, message: %s",
                        logEntry));
            } else {
                LoggerFactory.getLogger(TestBenchHelpers.class.getName()).warn(
                        "This message in browser log console may be a potential error: '{}'",
                        logEntry);
            }
        });
    }

    /**
     * Checks browser's log entries, throws an error for any client-side error
     * and logs any client-side warnings.
     *
     * @throws AssertionError
     *             if an error is found in the browser logs
     */
    protected void checkLogsForErrors() {
        checkLogsForErrors(msg -> false);
    }

    private WebElement getShadowRoot(WebElement webComponent) {
        waitUntil(driver -> getCommandExecutor().executeScript(
                "return arguments[0].shadowRoot", webComponent) != null);
        WebElement shadowRoot = (WebElement) getCommandExecutor()
                .executeScript("return arguments[0].shadowRoot", webComponent);
        Assert.assertNotNull("Could not locate shadowRoot in the element",
                shadowRoot);
        return shadowRoot;
    }

    /**
     * Calls the {@code blur()} function on the current active element of the
     * page, if any.
     */
    public void blur() {
        executeScript(
                "!!document.activeElement ? document.activeElement.blur() : 0");
    }

    private static class LazyDndSimulationLoad {
        private static final String DND_SCRIPT = loadDnDEmulation();

        private static String loadDnDEmulation() {
            InputStream stream = TestBenchHelpers.class
                    .getResourceAsStream("/dnd-simulation.js");
            try {
                return IOUtils.readLines(stream, StandardCharsets.UTF_8)
                        .stream().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
