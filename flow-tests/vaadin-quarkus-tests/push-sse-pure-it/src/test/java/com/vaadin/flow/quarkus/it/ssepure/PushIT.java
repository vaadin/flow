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
package com.vaadin.flow.quarkus.it.ssepure;

import java.util.List;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.test.AbstractChromeIT;

@QuarkusIntegrationTest
public class PushIT extends AbstractChromeIT {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @BeforeEach
    public void openView() {
        open();
        waitForElementPresent(By.id("transport"));
    }

    @Test
    public void pushFallsBackToSsePureWithoutAtmosphere() {
        Assertions.assertEquals("SSE_PURE",
                findElement(By.id("transport")).getText());
        Assertions.assertEquals(Boolean.TRUE,
                executeScript("return window.vaadinPush === undefined"),
                "Atmosphere should not be loaded");
        Assertions.assertEquals(List.of(), loadedResources("vaadinPush"));
    }

    @Test
    public void runsInProductionMode() {
        Assertions.assertTrue(
                findElements(By.tagName("vaadin-dev-tools")).isEmpty(),
                "Dev tools should not be loaded");
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
        Assertions.assertEquals(expected, executeScript(
                "return document.getElementById('message').textContent"));
        waitForServerCounterAbove(getServerCounter());
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
