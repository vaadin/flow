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
package com.vaadin.flow.spring.sseaddon;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PushIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/ui/";
    }

    @Before
    public void openView() {
        open();
        waitForElementPresent(By.id("transport"));
    }

    @Test
    public void pushUsesAddOnTransportWithoutAtmosphere() {
        Assert.assertEquals("SsePushConnection",
                findElement(By.id("transport")).getText());
        Assert.assertEquals("Atmosphere should not be loaded", Boolean.TRUE,
                executeScript("return window.vaadinPush === undefined"));
        Assert.assertEquals(List.of(), loadedResources("vaadinPush"));
        // The client side of the transport is in the frontend bundle
        Assert.assertEquals(List.of(), loadedResources("sse-push"));
        Assert.assertEquals(Boolean.TRUE,
                executeScript("return !!window.Vaadin.Flow.pushTransport"));
    }

    @Test
    public void runsInProductionMode() {
        Assert.assertTrue("Dev tools should not be loaded",
                findElements(By.tagName("vaadin-dev-tools")).isEmpty());
    }

    @Test
    public void serverPushesUpdates() {
        waitForServerCounterAbove(getServerCounter());
    }

    @Test
    public void clientRequestsWorkWhilePushing() {
        for (int i = 1; i <= 3; i++) {
            findElement(By.id("incrementClientCounter")).click();
            int expected = i;
            waitUntil(driver -> getCounter("clientCounter") == expected);
        }
        waitForServerCounterAbove(getServerCounter());
    }

    @Test
    public void largeMultibyteMessageIsPushedIntact() {
        findElement(By.id("pushLargeMessage")).click();
        String expected = PushView.createLargeMessage(20_000);
        waitUntil(driver -> expected.length() == ((Number) executeScript(
                "return document.getElementById('message').textContent.length"))
                .intValue());
        Assert.assertEquals(expected, executeScript(
                "return document.getElementById('message').textContent"));
        waitForServerCounterAbove(getServerCounter());
    }

    @Test
    public void silentlyLostStreamIsReplaced() {
        waitForServerCounterAbove(0);
        findElement(By.id("loseStreamSilently")).click();
        int counter = getServerCounter();

        // The client notices after two keep-alive intervals of silence, 2 s
        // each in this module, and the held pushes follow on the new stream
        waitUntil(driver -> getServerCounter() > counter + 1, 15);
    }

    @Test
    public void pushWorksAfterRefresh() {
        waitForServerCounterAbove(0);
        getDriver().navigate().refresh();
        waitForElementPresent(By.id("transport"));
        waitForServerCounterAbove(0);
    }

    private List<?> loadedResources(String urlPart) {
        return (List<?>) executeScript(
                "return performance.getEntriesByType('resource')"
                        + ".map(e => e.name).filter(n => n.includes(arguments[0]))",
                urlPart);
    }

    private void waitForServerCounterAbove(int value) {
        waitUntil(driver -> getServerCounter() > value, 10);
    }

    private int getServerCounter() {
        return getCounter("serverCounter");
    }

    private int getCounter(String id) {
        return Integer.parseInt(findElement(By.id(id)).getText());
    }
}
