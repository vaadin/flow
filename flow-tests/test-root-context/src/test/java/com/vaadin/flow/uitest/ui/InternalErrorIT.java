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

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Tests for handling internal errors and session expiration
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class InternalErrorIT extends ChromeBrowserTest {

    private static final String UPDATE = "update";
    private static final String CLOSE_SESSION = "close-session";
    private int count;

    @Test
    public void sessionExpired_refreshByDefault() {
        open();

        clickButton(UPDATE);
        clickButton(CLOSE_SESSION);

        // Just click on any button to make a request after killing the session
        clickButton(CLOSE_SESSION);

        try {
            waitUntil(driver -> {
                if (!isMessageUpdated()) {
                    return true;
                }
                if (count % 2 == 0) {
                    clickButton(UPDATE);
                } else {
                    clickButton(CLOSE_SESSION);
                }
                count++;
                return false;
            });
        } catch (TimeoutException e) {
            Assert.fail(
                    "After killing the session, the page should be refreshed, "
                            + "resetting the state of the UI.");
        }

        Assert.assertFalse(
                "By default, the 'Session Expired' notification "
                        + "should not be used",
                isSessionExpiredNotificationPresent());
    }

    @Test
    public void enableSessionExpiredNotification_sessionExpired_notificationShown() {
        open();

        clickButton("enable-notification");

        // Refresh to take the new config into use
        getDriver().navigate().refresh();

        clickButton(UPDATE);
        clickButton(CLOSE_SESSION);

        // Just click on any button to make a request after killing the session
        clickButton(CLOSE_SESSION);

        Assert.assertTrue("After enabling the 'Session Expired' notification, "
                + "the page should not be refreshed "
                + "after killing the session", isMessageUpdated());
        Assert.assertTrue("After enabling the 'Session Expired' notification "
                + "and killing the session, the notification should be displayed",
                isSessionExpiredNotificationPresent());
    }

    @Test
    public void internalError_showNotification_clickNotification_refresh() {
        open();

        clickButton(UPDATE);

        clickButton("cause-exception");

        Assert.assertTrue("The page should not be immediately refreshed after "
                + "a server-side exception", isMessageUpdated());
        Assert.assertTrue(
                "'Internal error' notification should be present after "
                        + "a server-side exception",
                isInternalErrorNotificationPresent());

        getErrorNotification().click();
        try {
            waitUntil(driver -> !isMessageUpdated());
        } catch (TimeoutException e) {
            Assert.fail("After internal error, clicking the notification "
                    + "should refresh the page, resetting the state of the UI.");
        }
        Assert.assertFalse(
                "'Internal error' notification should be gone after refreshing",
                isInternalErrorNotificationPresent());
    }

    @Test
    public void internalError_showNotification_clickEsc_refresh() {
        open();

        clickButton(UPDATE);

        clickButton("cause-exception");

        Assert.assertTrue("The page should not be immediately refreshed after "
                + "a server-side exception", isMessageUpdated());
        Assert.assertTrue(
                "'Internal error' notification should be present after "
                        + "a server-side exception",
                isInternalErrorNotificationPresent());

        new Actions(getDriver()).sendKeys(Keys.ESCAPE).build().perform();
        try {
            waitUntil(driver -> !isMessageUpdated());
        } catch (TimeoutException e) {
            Assert.fail(
                    "After internal error, pressing esc-key should refresh the page, "
                            + "resetting the state of the UI.");
        }
        Assert.assertFalse(
                "'Internal error' notification should be gone after refreshing",
                isInternalErrorNotificationPresent());
    }

    @After
    public void resetSystemMessages() {
        clickButton("reset-system-messages");
    }

    private boolean isMessageUpdated() {
        return "Updated".equals(findElement(By.id("message")).getText());
    }

    private boolean isSessionExpiredNotificationPresent() {
        return isErrorNotificationPresent("Session Expired");
    }

    private boolean isInternalErrorNotificationPresent() {
        return isErrorNotificationPresent("Internal error");
    }

    private boolean isErrorNotificationPresent(String text) {
        if (!isElementPresent(By.className("v-system-error"))) {
            return false;
        }
        return getErrorNotification().getAttribute("innerHTML").contains(text);
    }

    private WebElement getErrorNotification() {
        return findElement(By.className("v-system-error"));
    }

    private void clickButton(String id) {
        findElement(By.id(id)).click();
    }
}
