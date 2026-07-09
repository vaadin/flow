/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.slf4j.LoggerFactory;

public class RouteBasicIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Test
    public void testServletDeployed() throws Exception {
        open();

        Assert.assertTrue(isElementPresent(By.id("main")));

        findElement(By.id("foo")).click();

        waitUntil(driver -> isElementPresent(By.id("singleton-in-ui")));
    }

    @Test
    public void uiScopedProxiedTargetView_shouldUseSameViewInstance()
            throws Exception {
        getDriver().get(getTestURL() + "proxied");
        waitForDevServer();

        String prevUuid = null;
        AtomicReference<String> prevCounter = new AtomicReference<>("");
        for (int i = 0; i < 5; i++) {
            String uuid = waitUntil(d -> d.findElement(By.id("COMPONENT_ID")))
                    .getText();

            waitUntil(d -> !prevCounter.get()
                    .equals(d.findElement(By.id("CLICK_COUNTER")).getText()));

            if (prevUuid != null) {
                Assert.assertEquals("UUID should not have been changed",
                        prevUuid, uuid);
            } else {
                prevUuid = uuid;
            }
            String counter = findElement(By.id("CLICK_COUNTER")).getText();
            Assert.assertEquals(
                    "Parameter and counter should have the same value",
                    "P:" + i + ", C:" + i, counter);

            prevCounter.set(counter);

            $("a").first().click();
        }

    }
}
