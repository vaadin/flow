/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.DevToolsElement;
import com.vaadin.testbench.TestBenchElement;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@NotThreadSafe
public class JavaLiveReloadIT extends AbstractLiveReloadIT {

    @Test
    public void overlayShouldRender() {
        open();

        // Upon opening, the LiveReloadUI should show the indicator but not the
        // message window
        DevToolsElement devTools = $(DevToolsElement.class).waitForFirst();

        TestBenchElement window = devTools.$("*")
                .attributeContains("class", "window").first();
        Assert.assertFalse(window.isDisplayed());

        // After clicking the icon in the indicator, the live-reload message
        // window should appear
        WebElement liveReloadIcon = devTools.$("*")
                .attributeContains("class", "dev-tools").first();
        liveReloadIcon.click();

        waitForElementPresent(By.tagName("vaadin-dev-tools"));

        WebElement window2 = devTools.$("*")
                .attributeContains("class", "dev-tools").first();
        Assert.assertTrue(window2.isDisplayed());
    }

    @Test
    public void splashMessageShownOnAutoReloadAndClosedOnBodyClick() {
        open();

        WebElement liveReloadTrigger = findElement(
                By.id(JavaLiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        liveReloadTrigger.click();

        waitForLiveReload();

        DevToolsElement liveReload = $(DevToolsElement.class).waitForFirst();
        WebElement devTools1 = liveReload.$("*")
                .attributeContains("class", "dev-tools").first();

        Assert.assertTrue(devTools1.getAttribute("class").contains("active"));

        findElement(By.tagName("body")).click();

        DevToolsElement liveReload2 = $(DevToolsElement.class).waitForFirst();
        Assert.assertNotNull(liveReload2);
        WebElement devTools2 = liveReload2.$("*")
                .attributeContains("class", "dev-tools").first();
        Assert.assertFalse(devTools2.getAttribute("class").contains("active"));
        Assert.assertTrue(
                devTools2.getAttribute("class").contains("dev-tools"));
    }

    @Test
    public void deactivateLiveReload() {
        open();

        // given: live reload is deactivated
        DevToolsElement devTools = $(DevToolsElement.class).waitForFirst();

        devTools.setLiveReload(false);

        // when: live reload is triggered
        WebElement liveReloadTrigger = findElement(
                By.id(JavaLiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        liveReloadTrigger.click();

        // then: page is not reloaded
        DevToolsElement liveReload2 = $(DevToolsElement.class).waitForFirst();
        WebElement devTools2 = liveReload2.$("*")
                .attributeContains("class", "dev-tools").first();
        Assert.assertFalse(devTools2.getAttribute("class").contains("active"));
        Assert.assertTrue(
                devTools2.getAttribute("class").contains("dev-tools"));
    }
}
