/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DevModeConfigIT extends ChromeBrowserTest {
    @Test
    public void liveReload_disabled_shouldNotRenderIndicator() {
        open();

        Assert.assertEquals("productionMode is expected to be false", "false",
                findElement(By.id("productionMode")).getText());
        Assert.assertEquals("devModeLiveReloadEnabled is expected to be false",
                "false",
                findElement(By.id("devModeLiveReloadEnabled")).getText());

        Assert.assertTrue(
                findElements(By.id("vaadin-live-reload-indicator")).isEmpty());
    }
}
