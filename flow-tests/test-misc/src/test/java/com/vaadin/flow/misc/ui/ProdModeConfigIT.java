/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ProdModeConfigIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/prod-mode-config-test";
    }

    @Test
    public void prodMode_liveReload_alwaysDisabled_shouldNotRenderIndicator() {
        open();

        Assert.assertEquals("productionMode is expected to be true", "true",
                findElement(By.id("productionMode")).getText());
        Assert.assertEquals(
                "devModeLiveReloadEnabled is supposed to be false"
                        + " by default in production mode",
                "false",
                findElement(By.id("devModeLiveReloadEnabled")).getText());

        Assert.assertTrue(
                findElements(By.id("vaadin-live-reload-indicator")).isEmpty());
    }

    @Test
    public void prodMode_devTools_alwaysDisabled_shouldNotRenderDevToolsPopup() {
        open();

        Assert.assertEquals(
                "devToolsEnabled is supposed to be false "
                        + "by default in production mode",
                "false", findElement(By.id("devToolsEnabled")).getText());

        Assert.assertTrue(
                findElements(By.tagName("vaadin-dev-tools")).isEmpty());
    }
}
