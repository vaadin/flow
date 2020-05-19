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
package com.vaadin.flow.uitest.ui;

import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@NotThreadSafe
public class LiveReloadIT extends AbstractLiveReloadIT {

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
                By.className("gizmo")).get(0);
        liveReloadIcon.click();

        waitForElementPresent(By.tagName("vaadin-devmode-gizmo"));

        WebElement window2 = findInShadowRoot(liveReload, By.className("gizmo"))
                .get(0);
        Assert.assertTrue(window2.isDisplayed());
    }

    @Test
    public void splashMessageShownOnAutoReloadAndClosedOnBodyClick() {
        open();
        waitForElementPresent(By.id("live-reload-trigger-button"));
        WebElement liveReloadTrigger = findElement(
                By.id("live-reload-trigger-button"));
        liveReloadTrigger.click();

        WebElement liveReload = findElement(By.tagName("vaadin-devmode-gizmo"));
        Assert.assertNotNull(liveReload);
        WebElement gizmo1 = findInShadowRoot(liveReload, By.className("gizmo"))
                .get(0);
        Assert.assertTrue(
                gizmo1.getAttribute("class").contains("active"));

        findElement(By.tagName("body")).click();

        WebElement liveReload2 = findElement(
                By.tagName("vaadin-devmode-gizmo"));
        Assert.assertNotNull(liveReload2);
        WebElement gizmo2 = findInShadowRoot(liveReload2, By.className("gizmo"))
                .get(0);
        Assert.assertFalse(
                gizmo2.getAttribute("class").contains("active"));
        Assert.assertTrue(gizmo2.getAttribute("class").contains("gizmo"));
    }

    @Test
    public void deactivateLiveReload() {
        open();

        // given: live reload is deactivated
        waitForElementPresent(By.tagName("vaadin-devmode-gizmo"));
        WebElement liveReload = findElement(By.tagName("vaadin-devmode-gizmo"));

        WebElement liveReloadIcon = findInShadowRoot(liveReload,
                By.className("gizmo")).get(0);
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
        WebElement gizmo2 = findInShadowRoot(liveReload2, By.className("gizmo"))
                .get(0);
        Assert.assertFalse(
                gizmo2.getAttribute("class").contains("active"));
        Assert.assertTrue(gizmo2.getAttribute("class").contains("gizmo"));
    }

}
