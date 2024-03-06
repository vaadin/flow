/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.List;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
@Ignore("Fails in Containers")
public class WebpackDevServerPortIT extends ChromeBrowserTest {
    @Override
    protected String getTestPath() {
        return super.getTestPath().replace("/view", "");
    }

    @Test
    public void webpackDevServerPortShouldBeReusedOnReload()
            throws InterruptedException {
        open();

        String initialUUID = findElement(
                By.id(WebpackDevServerPortView.UUID_ID)).getText();
        Assert.assertEquals(36, initialUUID.length());

        String initialPort = findElement(
                By.id(WebpackDevServerPortView.WEBPACK_PORT_ID)).getText();
        Assert.assertTrue(NumberUtils.isDigits(initialPort));

        // trigger jetty reload
        findElement(By.id(WebpackDevServerPortView.TRIGGER_RELOAD_ID)).click();

        // keep refreshing until page comes back (the UUID has changed)
        int totalWaitTime = 20000;
        boolean reconnected = false;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < startTime + totalWaitTime
                && !reconnected) {
            Thread.sleep(2000);
            getDriver().navigate().refresh();
            List<WebElement> elements = findElements(
                    By.id(WebpackDevServerPortView.UUID_ID));
            if (!elements.isEmpty()
                    && !elements.get(0).getText().equals(initialUUID)) {
                reconnected = true;
            }
        }
        Assert.assertTrue("unable to verify connection to new Jetty instance",
                reconnected);

        // then the port number is unchanged
        String port = findElement(
                By.id(WebpackDevServerPortView.WEBPACK_PORT_ID)).getText();
        Assert.assertEquals(initialPort, port);

    }
}
