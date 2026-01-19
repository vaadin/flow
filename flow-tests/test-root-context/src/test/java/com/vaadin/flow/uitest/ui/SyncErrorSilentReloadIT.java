/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration test for sync error handling with silent reload (no
 * notification).
 */
public class SyncErrorSilentReloadIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/sync-error-silent/com.vaadin.flow.uitest.ui.SyncErrorView";
    }

    @Test
    public void syncError_notificationDisabled_silentReload() {
        open();

        // Enable desync simulation
        $(NativeButtonElement.class).id("enable-desync").click();
        waitForElementPresent(
                By.xpath("//*[contains(text(),'Desync simulation enabled')]"));

        // Trigger sync error
        $(NativeButtonElement.class).id("trigger-action").click();

        // Page should reload silently without showing notification
        // Wait for the page to reload - the buttons should be available again
        waitUntil(driver -> {
            // After silent reload, the page state is reset
            return isElementPresent(By.id("enable-desync"));
        });

        // Verify NO error notification was shown (it would still be visible if
        // shown)
        Assert.assertFalse(
                "Error notification should not appear for silent reload",
                isElementPresent(By.className("v-system-error")));
    }
}
