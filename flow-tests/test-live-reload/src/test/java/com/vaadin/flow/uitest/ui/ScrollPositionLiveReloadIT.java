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

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

@NotThreadSafe
public class ScrollPositionLiveReloadIT extends AbstractLiveReloadIT {

    // Inner scroll container has no ID — found by CSS selector to verify
    // scroll restoration works for elements identified by DOM path
    private static final String INNER_SCROLL_SELECTOR = "#outer-scroll > div:nth-of-type(1)";

    @Test
    public void scrollPositionPreservedAfterUIRefresh() {
        open();
        waitForElementPresent(By.id("item-50"));

        scrollAllContainers();

        int windowScrollBefore = getScrollY();
        int outerScrollBefore = getScrollTop("#outer-scroll");
        int innerScrollBefore = getScrollTop(INNER_SCROLL_SELECTOR);

        Assert.assertTrue("Window should be scrolled down",
                windowScrollBefore > 100);
        Assert.assertTrue("Outer container should be scrolled down",
                outerScrollBefore > 50);
        Assert.assertTrue("Inner container (no ID) should be scrolled down",
                innerScrollBefore > 50);

        String attachIdBefore = getAttachId();

        // Simulate hot-swap: directly trigger onReload on the dev-tools
        // WebSocket connection. In a real hot-swap, the server pushes the
        // reload message directly via WebSocket without a prior UIDL update.
        executeScript(
                "document.querySelector('vaadin-dev-tools').frontendConnection.onReload('full-refresh')");

        // Wait for the UI to refresh
        waitUntil(d -> !attachIdBefore.equals(getAttachId()), 10);

        waitForScrollRestoration("window", windowScrollBefore);
        waitForScrollRestoration("#outer-scroll", outerScrollBefore);
        waitForScrollRestoration(INNER_SCROLL_SELECTOR, innerScrollBefore);
    }

    @Test
    public void scrollPositionPreservedAfterFullPageReload() {
        open();
        waitForElementPresent(By.id("item-50"));

        scrollAllContainers();

        int windowScrollBefore = getScrollY();
        int outerScrollBefore = getScrollTop("#outer-scroll");
        int innerScrollBefore = getScrollTop(INNER_SCROLL_SELECTOR);

        Assert.assertTrue("Window should be scrolled down",
                windowScrollBefore > 100);
        Assert.assertTrue("Outer container should be scrolled down",
                outerScrollBefore > 50);
        Assert.assertTrue("Inner container (no ID) should be scrolled down",
                innerScrollBefore > 50);

        // Simulate hot-swap full reload: saves scroll to sessionStorage
        // and calls window.location.reload().
        executeScript(
                "document.querySelector('vaadin-dev-tools').frontendConnection.onReload('reload')");

        // Wait for the page to reload and render
        waitForElementPresent(By.id("item-50"));

        waitForScrollRestoration("window", windowScrollBefore);
        waitForScrollRestoration("#outer-scroll", outerScrollBefore);
        waitForScrollRestoration(INNER_SCROLL_SELECTOR, innerScrollBefore);
    }

    private void scrollAllContainers() {
        // Scroll the inner container (no ID, found by CSS selector)
        executeScript("document.querySelector(arguments[0]).scrollTop = 300",
                INNER_SCROLL_SELECTOR);
        // Scroll the outer container
        executeScript(
                "document.querySelector('#outer-scroll').scrollTop = 400");
        // Scroll the window
        executeScript("document.getElementById('item-50').scrollIntoView()");
        sleep(500);
    }

    private int getScrollTop(String cssSelector) {
        return ((Number) executeScript(
                "return document.querySelector(arguments[0]).scrollTop",
                cssSelector)).intValue();
    }

    private void waitForScrollRestoration(String target, int expectedScrollY) {
        if ("window".equals(target)) {
            waitUntil(d -> Math.abs(getScrollY() - expectedScrollY) < 5, 10);
        } else {
            waitUntil(d -> Math.abs(getScrollTop(target) - expectedScrollY) < 5,
                    10);
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
