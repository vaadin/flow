/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class IFrameIT extends ChromeBrowserTest {

    @Test
    public void testIFrameReload() {
        open();

        waitForElementPresent(By.id("frame1"));
        getDriver().switchTo().frame("frame1");
        Assert.assertEquals("A", findElement(By.id("Friday")).getText());

        getDriver().switchTo().parentFrame();
        findElement(By.id("Reload")).click();

        getDriver().switchTo().frame("frame1");
        waitUntil(webDriver -> "B"
                .equals(findElement(By.id("Friday")).getText()));
    }
}
