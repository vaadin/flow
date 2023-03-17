/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.List;

import com.vaadin.flow.testutil.DevModeGizmoElement;
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
        DevModeGizmoElement liveReload = $(DevModeGizmoElement.class)
                .waitForFirst();

        TestBenchElement window = liveReload.$("*")
                .attributeContains("class", "window").first();
        Assert.assertFalse(window.isDisplayed());

        // After clicking the icon in the indicator, the live-reload message
        // window should appear
        WebElement liveReloadIcon = liveReload.$("*")
                .attributeContains("class", "gizmo").first();
        liveReloadIcon.click();

        waitForElementPresent(By.tagName("vaadin-devmode-gizmo"));

        WebElement window2 = liveReload.$("*")
                .attributeContains("class", "gizmo").first();
        Assert.assertTrue(window2.isDisplayed());
    }

    @Test
    public void splashMessageShownOnAutoReloadAndClosedOnBodyClick() {
        open();

        WebElement liveReloadTrigger = findElement(
                By.id(JavaLiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        liveReloadTrigger.click();

        waitForLiveReload();

        DevModeGizmoElement liveReload = $(DevModeGizmoElement.class)
                .waitForFirst();
        WebElement gizmo1 = liveReload.$("*")
                .attributeContains("class", "gizmo").first();

        Assert.assertTrue(gizmo1.getAttribute("class").contains("active"));

        findElement(By.tagName("body")).click();

        DevModeGizmoElement liveReload2 = $(DevModeGizmoElement.class)
                .waitForFirst();
        Assert.assertNotNull(liveReload2);
        WebElement gizmo2 = liveReload2.$("*")
                .attributeContains("class", "gizmo").first();
        Assert.assertFalse(gizmo2.getAttribute("class").contains("active"));
        Assert.assertTrue(gizmo2.getAttribute("class").contains("gizmo"));
    }

    @Test
    public void deactivateLiveReload() {
        open();

        // given: live reload is deactivated
        DevModeGizmoElement gizmo = $(DevModeGizmoElement.class).waitForFirst();

        gizmo.setLiveReload(false);

        // when: live reload is triggered
        WebElement liveReloadTrigger = findElement(
                By.id(JavaLiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        liveReloadTrigger.click();

        // then: page is not reloaded
        DevModeGizmoElement liveReload2 = $(DevModeGizmoElement.class)
                .waitForFirst();
        WebElement gizmo2 = liveReload2.$("*")
                .attributeContains("class", "gizmo").first();
        Assert.assertFalse(gizmo2.getAttribute("class").contains("active"));
        Assert.assertTrue(gizmo2.getAttribute("class").contains("gizmo"));
    }
}
