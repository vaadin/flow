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

public class InvalidateHttpSessionIT extends ChromeBrowserTest {

    @Test
    public void invalidateHttpSession_vaadinSessionIsClosed() {
        open();

        findElement(By.id("invalidate-session")).click();

        waitForElementPresent(By.id("invalidated-session-id"));

        String invalidatedSessionId = findElement(
                By.id("invalidated-session-id")).getText();

        String sessionId = findElement(By.id("current-session-id")).getText();
        Assert.assertNotEquals(sessionId, invalidatedSessionId);
    }

}
