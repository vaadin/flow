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
package com.vaadin.flow.spring.sseaddondev;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * In development mode, the dev tools keep Atmosphere on the classpath, but the
 * push of the application uses the transport of the add-on.
 */
public class DevModePushIT extends ChromeBrowserTest {

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
    public void pushUsesAddOnTransportNextToDevTools() {
        Assert.assertEquals("SsePushConnection",
                findElement(By.id("transport")).getText());
        Assert.assertFalse("Dev tools should be loaded",
                findElements(By.tagName("vaadin-dev-tools")).isEmpty());
        Assert.assertEquals(Boolean.TRUE,
                executeScript("return !!window.Vaadin.Flow.pushTransport"));
        // The dev tools load Atmosphere for their own connection, with a
        // script of their own that may still be loading
        waitUntil(
                driver -> Boolean.TRUE
                        .equals(executeScript("return !!window.vaadinPush")),
                10);
    }

    @Test
    public void serverPushesUpdates() {
        int counter = getServerCounter();
        waitUntil(driver -> getServerCounter() > counter, 10);
    }

    @Test
    public void clientRequestsWorkWhilePushing() {
        findElement(By.id("incrementClientCounter")).click();
        waitUntil(
                driver -> "1"
                        .equals(findElement(By.id("clientCounter")).getText()),
                10);
    }

    private int getServerCounter() {
        return Integer.parseInt(findElement(By.id("serverCounter")).getText());
    }
}
