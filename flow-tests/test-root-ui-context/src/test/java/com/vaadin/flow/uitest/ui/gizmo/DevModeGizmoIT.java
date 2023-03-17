/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.gizmo;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DevModeGizmoIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/view-disabled-gizmo/com.vaadin.flow.uitest.gizmo.DevModeGizmoView";
    }

    @Test
    public void devMode_devModeGizmo_disabled_shouldNotRenderDevModeGizmo() {
        open();
        Assert.assertTrue(
                "No dev mode gizmo popup expected when it is disabled",
                findElements(By.tagName("vaadin-devmode-gizmo")).isEmpty());

        Assert.assertTrue(
                "Live reload is expected to be disabled when the gizmo "
                        + "disabled",
                findElements(By.id("vaadin-live-reload-indicator")).isEmpty());
    }
}
