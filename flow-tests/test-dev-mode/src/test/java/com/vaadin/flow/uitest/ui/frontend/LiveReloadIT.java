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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class LiveReloadIT extends ChromeBrowserTest {

    @Test
    public void overlayShouldRender() {
        open();

        // Upon opening, the LiveReloadUI should show the indicator but not the
        // message window
        List<WebElement> gizmos = findElements(
                By.tagName("vaadin-devmode-gizmo"));
        Assert.assertEquals(1, gizmos.size());
        WebElement gizmo = gizmos.get(0);

        WebElement window = findInShadowRoot(gizmo, By.className("window"))
                .get(0);
        Assert.assertFalse(window.isDisplayed());

        // After clicking the icon in the indicator, the live-reload message
        // window should appear
        WebElement liveReloadIcon = findInShadowRoot(gizmo,
                By.className("vaadin-logo")).get(0);
        liveReloadIcon.click();
        Assert.assertTrue(window.isDisplayed());
    }

    @Test
    public void overlayShouldNotBeRenderedAfterDisable() {
        open();

        WebElement gizmo = findElement(By.tagName("vaadin-devmode-gizmo"));
        gizmo.click();

        WebElement liveReloadIcon = findInShadowRoot(gizmo,
                By.className("vaadin-logo")).get(0);
        liveReloadIcon.click();

        WebElement button = findInShadowRoot(gizmo, By.id("disable")).get(0);
        button.click();

        Assert.assertEquals(0,
                findElements(By.tagName("vaadin-devmode-gizmo")).size());

        driver.navigate().refresh();

        Assert.assertEquals(0,
                findElements(By.tagName("vaadin-devmode-gizmo")).size());
    }
}
