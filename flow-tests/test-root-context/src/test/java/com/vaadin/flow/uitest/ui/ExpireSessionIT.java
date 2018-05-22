/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * @author Vaadin Ltd.
 */
public class ExpireSessionIT extends ChromeBrowserTest {

    private static final String UPDATE = "update";
    private static final String CLOSE_SESSION = "close-session";

    @Test
    public void sessionExpired_refreshByDefault() {
        open();

        clickButton(UPDATE);
        clickButton(CLOSE_SESSION);

        // Just click on any button to make a request after killing the session
        clickButton(CLOSE_SESSION);

        try {
            waitUntil(driver -> !isMessageUpdated());
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

    private boolean isMessageUpdated() {
        return "Updated".equals(findElement(By.id("message")).getText());
    }

    private boolean isSessionExpiredNotificationPresent() {
        if (!isElementPresent(By.className("v-system-error"))) {
            return false;
        }
        return findElement(By.className("v-system-error"))
                .getAttribute("innerHTML").contains("Session Expired");
    }

    private void clickButton(String id) {
        findElement(By.id(id)).click();
    }
}
