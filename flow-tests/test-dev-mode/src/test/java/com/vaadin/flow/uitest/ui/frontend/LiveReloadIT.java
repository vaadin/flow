/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.uitest.ui.frontend;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

// These tests are not parallelizable, nor should they be run at the same time
// as other tests in the same module, due to live-reload affecting the whole
// application
@NotThreadSafe
public class LiveReloadIT extends ChromeBrowserTest {

    private static final Lock lock = new ReentrantLock();

    @Before
    @Override
    public void setup() throws Exception {
        lock.lock();
        super.setup();
    }

    @After
    public void tearDown() {
        lock.unlock();
    }

    @Test
    public void overlayShouldRender() {
        open();
        // Upon opening, the LiveReloadUI should show the indicator but not the
        // message window
        waitForElementPresent(By.tagName("vaadin-devmode-gizmo"));
        List<WebElement> liveReloads = findElements(
                By.tagName("vaadin-devmode-gizmo"));
        Assert.assertEquals(1, liveReloads.size());
        WebElement liveReload = liveReloads.get(0);

        WebElement window = findInShadowRoot(liveReload, By.className("window"))
                .get(0);
        Assert.assertFalse(window.isDisplayed());

        // After clicking the icon in the indicator, the live-reload message
        // window should appear
        WebElement liveReloadIcon = findInShadowRoot(liveReload,
                By.className("gizmo-container")).get(0);
        liveReloadIcon.click();

        waitForElementPresent(By.tagName("vaadin-devmode-gizmo"));

        WebElement window2 = findInShadowRoot(liveReload, By.className("gizmo-container"))
                .get(0);
        Assert.assertTrue(window2.isDisplayed());
    }

    @Test
    @Ignore
    public void overlayShouldNotBeRenderedAfterDisable() {
        open();
        waitForElementPresent(By.tagName("vaadin-devmode-gizmo"));
        WebElement liveReload = findElement(By.tagName("vaadin-devmode-gizmo"));
        liveReload.click();

        WebElement liveReloadIcon = findInShadowRoot(liveReload,
                By.className("gizmo-container")).get(0);
        liveReloadIcon.click();

        WebElement button = findInShadowRoot(liveReload, By.id("disable"))
                .get(0);
        button.click();

        Assert.assertEquals(0,
                findElements(By.tagName("vaadin-devmode-gizmo")).size());

        driver.navigate().refresh();

        Assert.assertEquals(0,
                findElements(By.tagName("vaadin-devmode-gizmo")).size());
    }

    @Test
    @Ignore
    public void liveReloadShouldNotTriggerAfterDisable() {
        open();
        waitForElementPresent(By.tagName("vaadin-devmode-gizmo"));
        WebElement liveReload = findElement(By.tagName("vaadin-devmode-gizmo"));
        liveReload.click();

        String instanceId = findElement(By.id("elementId")).getText();

        WebElement liveReloadIcon = findInShadowRoot(liveReload,
                By.className("gizmo-container")).get(0);
        liveReloadIcon.click();

        WebElement button = findInShadowRoot(liveReload, By.id("disable"))
                .get(0);
        button.click();

        WebElement liveReloadTrigger = findElement(
                By.id("live-reload-trigger-button"));
        liveReloadTrigger.click();

        String instanceId2 = findElement(By.id("elementId")).getText();

        Assert.assertEquals(instanceId, instanceId2);
    }

    @Test
    public void notificationShownOnAutoReloadAndClosedOnBodyClick() {
        open();
        waitForElementPresent(By.id("live-reload-trigger-button"));
        WebElement liveReloadTrigger = findElement(
                By.id("live-reload-trigger-button"));
        liveReloadTrigger.click();

        WebElement liveReload = findElement(By.tagName("vaadin-devmode-gizmo"));
        Assert.assertNotNull(liveReload);
        WebElement gizmo1 = findInShadowRoot(liveReload, By.className("gizmo-container"))
                .get(0);
        Assert.assertTrue(
                gizmo1.getAttribute("class").contains("active"));

        findElement(By.tagName("body")).click();

        WebElement liveReload2 = findElement(
                By.tagName("vaadin-devmode-gizmo"));
        Assert.assertNotNull(liveReload2);
        WebElement gizmo2 = findInShadowRoot(liveReload2, By.className("gizmo-container"))
                .get(0);
        Assert.assertFalse(
                gizmo2.getAttribute("class").contains("active"));
        Assert.assertTrue(gizmo2.getAttribute("class").contains("gizmo-container"));
    }

    @Test
    public void deactivateLiveReload() {
        open();

        // given: live reload is deactivated
        waitForElementPresent(By.tagName("vaadin-devmode-gizmo"));
        WebElement liveReload = findElement(By.tagName("vaadin-devmode-gizmo"));

        WebElement liveReloadIcon = findInShadowRoot(liveReload,
                By.className("gizmo-container")).get(0);
        liveReloadIcon.click();

        WebElement deactivateCheckbox = findInShadowRoot(liveReload,
                By.id("toggle")).get(0);
        deactivateCheckbox.click();

        // when: live reload is triggered
        WebElement liveReloadTrigger = findElement(
                By.id("live-reload-trigger-button"));
        liveReloadTrigger.click();

        // then: page is not reloaded
        WebElement liveReload2 = findElement(
                By.tagName("vaadin-devmode-gizmo"));
        WebElement gizmo2 = findInShadowRoot(liveReload2, By.className("gizmo-container"))
                .get(0);
        Assert.assertFalse(
                gizmo2.getAttribute("class").contains("active"));
        Assert.assertTrue(gizmo2.getAttribute("class").contains("gizmo-container"));
    }
}
