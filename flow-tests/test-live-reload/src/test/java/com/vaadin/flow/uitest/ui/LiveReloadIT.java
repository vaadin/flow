/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.testbench.TestBenchElement;

@NotThreadSafe
public class LiveReloadIT extends AbstractLiveReloadIT {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void backupFrontendResources() throws IOException {
        // Save original frontend resources. They will be restored after test
        // execution to avoid failures or false positives on later runs
        FileUtils.copyDirectory(new File("./frontend"),
                temporaryFolder.getRoot());
    }

    @After
    public void restoreFrontendResources() throws IOException {
        FileUtils.copyDirectory(temporaryFolder.getRoot(),
                new File("./frontend"));
    }

    @Test
    public void overlayShouldRender() {
        open();
        // Upon opening, the LiveReloadUI should show the indicator but not the
        // message window
        waitForElementPresent(By.tagName("vaadin-devmode-gizmo"));
        List<TestBenchElement> liveReloads = $("vaadin-devmode-gizmo").all();
        Assert.assertEquals(1, liveReloads.size());
        TestBenchElement liveReload = liveReloads.get(0);

        WebElement window = liveReload.$("*")
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
        waitForElementPresent(
                By.id(LiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        WebElement liveReloadTrigger = findElement(
                By.id(LiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        liveReloadTrigger.click();

        TestBenchElement liveReload = $("vaadin-devmode-gizmo").first();
        Assert.assertNotNull(liveReload);
        WebElement gizmo1 = liveReload.$("*")
                .attributeContains("class", "gizmo").first();
        Assert.assertTrue(gizmo1.getAttribute("class").contains("active"));

        findElement(By.tagName("body")).click();

        TestBenchElement liveReload2 = $("vaadin-devmode-gizmo").first();
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
        waitForElementPresent(By.tagName("vaadin-devmode-gizmo"));
        TestBenchElement liveReload = $("vaadin-devmode-gizmo").first();

        WebElement liveReloadIcon = liveReload.$("*")
                .attributeContains("class", "gizmo").first();
        liveReloadIcon.click();

        WebElement deactivateCheckbox = liveReload.$("*").id("toggle");
        deactivateCheckbox.click();

        // when: live reload is triggered
        WebElement liveReloadTrigger = findElement(
                By.id(LiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        liveReloadTrigger.click();

        // then: page is not reloaded
        TestBenchElement liveReload2 = $("vaadin-devmode-gizmo").first();
        WebElement gizmo2 = liveReload2.$("*")
                .attributeContains("class", "gizmo").first();
        Assert.assertFalse(gizmo2.getAttribute("class").contains("active"));
        Assert.assertTrue(gizmo2.getAttribute("class").contains("gizmo"));
    }

    @Test
    public void liveReloadOnTouchedFrontendFile() {
        open();

        final String initialViewId = findElement(
                By.id(LiveReloadView.INSTANCE_IDENTIFIER)).getText();

        waitForElementPresent(
                By.id(LiveReloadView.WEBPACK_LIVE_RELOAD_TRIGGER_BUTTON));
        WebElement liveReloadTrigger = findElement(
                By.id(LiveReloadView.WEBPACK_LIVE_RELOAD_TRIGGER_BUTTON));
        liveReloadTrigger.click();

        waitForElementPresent(By.id(LiveReloadView.PAGE_RELOADING));
        waitForElementNotPresent(By.id(LiveReloadView.PAGE_RELOADING));

        final String newViewId = findElement(
                By.id(LiveReloadView.INSTANCE_IDENTIFIER)).getText();
        Assert.assertNotEquals(initialViewId, newViewId);
    }
}
