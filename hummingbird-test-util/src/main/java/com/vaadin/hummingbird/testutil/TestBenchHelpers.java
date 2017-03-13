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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.vaadin.testbench.TestBenchTestCase;

/**
 * Helpers for running testbench tests.
 */
public class TestBenchHelpers extends TestBenchTestCase {
    /**
     * Waits up to 10s for the given condition to become true. Use e.g. as
     * {@link #waitUntil(ExpectedCondition)}.
     *
     * @param condition
     *            the condition to wait for to become true
     * @param <T>
     *            the return type of the expected condition
     */
    protected <T> void waitUntil(ExpectedCondition<T> condition) {
        waitUntil(condition, 10);
    }

    /**
     * Waits the given number of seconds for the given condition to become true.
     * Use e.g. as {@link #waitUntil(ExpectedCondition)}.
     *
     * @param condition
     *            the condition to wait for to become true
     * @param timeoutInSeconds
     *            the number of seconds to wait
     * @param <T>
     *            the return type of the expected condition
     */
    protected <T> void waitUntil(ExpectedCondition<T> condition,
            long timeoutInSeconds) {
        new WebDriverWait(driver, timeoutInSeconds).until(condition);
    }

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
     * @param webComponent
     *            the web component owning shadow DOM to start search from
     * @param by
     *            the selector used to find element
     * @return an optional element, or an empty optional if no element found
     */
    protected Optional<WebElement> getInShadowRoot(WebElement webComponent,
            By by) {
        return findInShadowRoot(webComponent, by).stream().findFirst();
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
        WebElement root = (WebElement) getCommandExecutor()
                .executeScript("return arguments[0].shadowRoot", webComponent);
        return root.findElements(by);
    }

}
