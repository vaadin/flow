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
        // message
        Assert.assertEquals(1,
                findElements(By.id("vaadin-live-reload-indicator")).size());
        WebElement reloadOverlay = findElement(
                By.id("vaadin-live-reload-overlay"));
        Assert.assertNotNull(reloadOverlay.getAttribute("hidden"));

        // After clicking the icon in the indicator, the live-reload message
        // should appear
        WebElement liveReloadIcon = findElement(
                By.id("vaadin-live-reload-icon"));
        liveReloadIcon.click();
        Assert.assertNotEquals("true", reloadOverlay.getAttribute("hidden"));
    }

    @Test
    public void overlayShouldNotBeRenderedAfterDisable() {
        open();

        WebElement liveReloadIcon = findElement(
                By.id("vaadin-live-reload-icon"));
        liveReloadIcon.click();
        WebElement button = findElement(By.tagName("input"));
        button.click();

        Assert.assertEquals(0,
                findElements(By.id("vaadin-live-reload-indicator")).size());

        driver.navigate().refresh();

        Assert.assertEquals(0,
                findElements(By.id("vaadin-live-reload-indicator")).size());
    }
}
