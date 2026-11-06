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
package com.vaadin.flow.uitest.ui;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;

@NotThreadSafe
public class ScrollPositionLiveReloadIT extends AbstractLiveReloadIT {

    // Inner scroll container has no ID — found by CSS selector to verify
    // scroll restoration works for elements identified by DOM path
    private static final String INNER_SCROLL_SELECTOR = "#outer-scroll > div:nth-of-type(1)";

    private static final int TOLERANCE_PX = 5;

    // Set on the document before a reload is triggered, so that the test can
    // tell the reloaded page from the one the reload was triggered on
    private static final String DOCUMENT_MARKER = "__scrollPositionTestDocument";

    // Reported instead of a scroll position for a container that is not in the
    // DOM, which is a different thing from a container scrolled to the top
    private static final int ELEMENT_MISSING = -1;

    private static final String SCROLL_POSITIONS_SCRIPT = """
            const inner = document.querySelector(arguments[0]);
            const outer = document.querySelector('#outer-scroll');
            const missing = arguments[1];
            return [window.scrollY, outer ? outer.scrollTop : missing,
                    inner ? inner.scrollTop : missing];
            """;

    @Test
    public void scrollPositionPreservedAfterUIRefresh() {
        open();
        waitForElementPresent(By.id("item-50"));

        List<Integer> scrollBefore = scrollAllContainers();

        String attachIdBefore = getAttachId();

        // Simulate hot-swap: directly trigger onReload on the dev-tools
        // WebSocket connection. In a real hot-swap, the server pushes the
        // reload message directly via WebSocket without a prior UIDL update.
        executeScript(
                "document.querySelector('vaadin-dev-tools').frontendConnection.onReload('full-refresh')");

        // Wait for the UI to refresh
        waitUntil(d -> !attachIdBefore.equals(getAttachId()), 10);

        waitForScrollRestoration(scrollBefore);
    }

    @Test
    public void scrollPositionPreservedAfterFullPageReload() {
        open();
        waitForElementPresent(By.id("item-50"));

        List<Integer> scrollBefore = scrollAllContainers();

        markDocument();

        // Simulate hot-swap full reload: saves scroll to sessionStorage
        // and calls window.location.reload().
        executeScript(
                "document.querySelector('vaadin-dev-tools').frontendConnection.onReload('reload')");

        waitForNewDocument();
        waitForElementPresent(By.id("item-50"));

        waitForScrollRestoration(scrollBefore);
    }

    /**
     * Scrolls the window and both containers and returns the positions once
     * they have stopped changing.
     *
     * @return the window, outer container and inner container scroll positions
     */
    private List<Integer> scrollAllContainers() {
        // Scroll the inner container (no ID, found by CSS selector)
        executeScript("document.querySelector(arguments[0]).scrollTop = 300",
                INNER_SCROLL_SELECTOR);
        // Scroll the outer container
        executeScript(
                "document.querySelector('#outer-scroll').scrollTop = 400");
        // Scroll the window
        executeScript("document.getElementById('item-50').scrollIntoView()");

        List<Integer> positions = waitForStableScrollPositions();

        Assert.assertTrue("Window should be scrolled down, but was at "
                + positions.get(0), positions.get(0) > 100);
        Assert.assertTrue("Outer container should be scrolled down, but was at "
                + positions.get(1), positions.get(1) > 50);
        Assert.assertTrue(
                "Inner container (no ID) should be scrolled down, but was at "
                        + positions.get(2),
                positions.get(2) > 50);
        return positions;
    }

    private List<Integer> getScrollPositions() {
        List<?> values = (List<?>) executeScript(SCROLL_POSITIONS_SCRIPT,
                INNER_SCROLL_SELECTOR, ELEMENT_MISSING);
        return values.stream().map(value -> ((Number) value).intValue())
                .toList();
    }

    private List<Integer> waitForStableScrollPositions() {
        AtomicReference<List<Integer>> previous = new AtomicReference<>(
                getScrollPositions());
        AtomicReference<List<Integer>> stable = new AtomicReference<>();
        try {
            waitUntil(d -> {
                List<Integer> current = getScrollPositions();
                if (current.equals(previous.get())) {
                    stable.set(current);
                    return true;
                }
                previous.set(current);
                return false;
            }, 10);
        } catch (TimeoutException e) {
            Assert.fail(
                    "Scroll positions (window, outer container, inner container) kept changing, last seen "
                            + describe(previous.get()));
        }
        return stable.get();
    }

    private void waitForScrollRestoration(List<Integer> expected) {
        AtomicReference<List<Integer>> actual = new AtomicReference<>();
        try {
            waitUntil(d -> {
                actual.set(getScrollPositions());
                return isRestored(actual.get(), expected);
            }, 10);
        } catch (TimeoutException e) {
            Assert.fail(
                    "Scroll positions (window, outer container, inner container) were not restored. Expected "
                            + expected + " but was " + describe(actual.get()));
        }
    }

    private String describe(List<Integer> positions) {
        return positions.stream()
                .map(position -> position == ELEMENT_MISSING ? "<no element>"
                        : position.toString())
                .toList().toString();
    }

    private boolean isRestored(List<Integer> actual, List<Integer> expected) {
        return IntStream.range(0, expected.size()).allMatch(
                i -> Math.abs(actual.get(i) - expected.get(i)) < TOLERANCE_PX);
    }

    private void markDocument() {
        executeScript("window[arguments[0]] = true", DOCUMENT_MARKER);
    }

    /**
     * Waits until the marker set by {@link #markDocument()} is gone, i.e. until
     * the browser has actually loaded a new document. Without this, assertions
     * can run against the page the reload was triggered on, which still has the
     * original scroll positions.
     */
    private void waitForNewDocument() {
        try {
            waitUntil(d -> {
                try {
                    return !Boolean.TRUE.equals(executeScript(
                            "return window[arguments[0]] === true",
                            DOCUMENT_MARKER));
                } catch (WebDriverException e) {
                    // Script execution fails while the page is being replaced
                    return false;
                }
            }, 10);
        } catch (TimeoutException e) {
            Assert.fail(
                    "The browser never loaded a new document, so the reload did not happen");
        }
    }
}
