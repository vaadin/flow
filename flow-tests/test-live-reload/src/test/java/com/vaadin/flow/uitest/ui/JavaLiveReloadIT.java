/*
 * Copyright 2000-2022 Vaadin Ltd.
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
