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
        DevModeGizmoElement liveReload = $(DevModeGizmoElement.class).waitForFirst();

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
        waitForElementPresent(By.id(LiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        WebElement liveReloadTrigger = findElement(
                By.id(LiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        liveReloadTrigger.click();

        waitForLiveReload();

        DevModeGizmoElement liveReload = $(DevModeGizmoElement.class).waitForFirst();
        WebElement gizmo1 = liveReload.$("*")
                .attributeContains("class", "gizmo").first();

        Assert.assertTrue(gizmo1.getAttribute("class").contains("active"));

        findElement(By.tagName("body")).click();

        DevModeGizmoElement liveReload2 = $(DevModeGizmoElement.class).waitForFirst();
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
        DevModeGizmoElement liveReload = $(DevModeGizmoElement.class).waitForFirst();
        WebElement liveReloadIcon = liveReload.$("*").attributeContains("class", "gizmo").first();
        liveReloadIcon.click();

        WebElement deactivateCheckbox = liveReload.$("*").id("toggle").first();
        deactivateCheckbox.click();

        // when: live reload is triggered
        WebElement liveReloadTrigger = findElement(
                By.id(LiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        liveReloadTrigger.click();

        // then: page is not reloaded
        DevModeGizmoElement liveReload2 = $("vaadin-devmode-gizmo").first();
        WebElement gizmo2 = .attributeContains("class", "gizmo").first();
        Assert.assertFalse(
                gizmo2.getAttribute("class").contains("active"));
        Assert.assertTrue(gizmo2.getAttribute("class").contains("gizmo"));
    }

    @Test
    public void liveReloadOnTouchedFrontendFile() {
        open();

        final String initialViewId = findElement(
                By.id(LiveReloadView.INSTANCE_IDENTIFIER)).getText();

        waitForElementPresent(By.id(LiveReloadView.WEBPACK_LIVE_RELOAD_TRIGGER_BUTTON));
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
