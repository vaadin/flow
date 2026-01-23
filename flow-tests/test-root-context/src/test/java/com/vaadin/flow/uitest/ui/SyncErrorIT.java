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

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for sync error handling with default messages.
 */
public class SyncErrorIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/sync-error/com.vaadin.flow.uitest.ui.SyncErrorView";
    }

    @Test
    public void syncError_clientAheadOfServer_showsNotification() {
        open();

        // Enable desync simulation - next request will throw
        // MessageIdSyncException
        $(NativeButtonElement.class).id("enable-desync").click();
        waitForElementPresent(
                By.xpath("//*[contains(text(),'Desync simulation enabled')]"));

        // Trigger an action - the MessageIdSyncException will be thrown
        $(NativeButtonElement.class).id("trigger-action").click();

        // Verify sync error notification appears
        waitUntil(driver -> isElementPresent(By.className("v-system-error")));

        WebElement errorNotification = findElement(
                By.className("v-system-error"));
        assertThat(errorNotification.getText(),
                containsString("Synchronization Error"));
        assertThat(errorNotification.getText(),
                containsString("Your session needs to be refreshed"));
    }

    @Test
    public void syncError_pageRefreshesAfterDismiss() {
        open();

        // Enable desync simulation
        $(NativeButtonElement.class).id("enable-desync").click();
        waitForElementPresent(
                By.xpath("//*[contains(text(),'Desync simulation enabled')]"));

        // Trigger sync error
        $(NativeButtonElement.class).id("trigger-action").click();

        // Wait for error notification
        waitUntil(driver -> isElementPresent(By.className("v-system-error")));

        // Click the error notification to dismiss
        findElement(By.className("v-system-error")).click();

        // Page should reload - desync flag was reset to false by the view
        // reload, so notification and desync simulation enabled messages should
        // not be visible
        waitForElementNotPresent(By.className("v-system-error"));
        waitForElementNotPresent(
                By.xpath("//*[contains(text(),'Desync simulation enabled')]"));
    }
}
