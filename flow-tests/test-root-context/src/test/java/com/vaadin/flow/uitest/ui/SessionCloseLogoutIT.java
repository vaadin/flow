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

public class SessionCloseLogoutIT extends ChromeBrowserTest {

    @Test
    public void changeOnClient() throws InterruptedException {
        open();

        // clean all messages: if there is an error in the console it won't
        // appear anymore
        getLogEntries(java.util.logging.Level.ALL).forEach(logEntry -> {
            // no-op
        });

        $(NativeButtonElement.class).first().click();

        asserNoErrors();

        waitUntil(driver -> !findElements(By.tagName("a")).isEmpty());
        String sessionExpiredText = $("a").first().getText();
        Assert.assertEquals(
                "Unexpected view after navigation with closed session",
                "My link", sessionExpiredText);

        asserNoErrors();
    }

    private void asserNoErrors() {
        checkLogsForErrors(msg -> msg
                .matches("^.*((VAADIN/static/client|FlowClient.js).*|\"\")$"));
    }

}
