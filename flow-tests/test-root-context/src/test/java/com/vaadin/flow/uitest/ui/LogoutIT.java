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

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class LogoutIT extends ChromeBrowserTest {

    @Test
    public void setLocationWithNotificationDisabled_noErrorMessages() {
        open();

        $(NativeButtonElement.class).first().click();

        // There can be "Session Expired" message because of heartbeat
        // Strings defined in com.vaadin.flow.server.SystemMessages
        checkLogsForErrors(
                msg -> msg.contains("Session Expired") || msg.contains(
                        "Take note of any unsaved data, and click here or press ESC key to continue."));

        // There can't be any error dialog
        Assert.assertFalse(isElementPresent(By.className("v-system-error")));

        // The base href view should be shown
        waitForElementPresent(By.tagName("a"));
    }
}
