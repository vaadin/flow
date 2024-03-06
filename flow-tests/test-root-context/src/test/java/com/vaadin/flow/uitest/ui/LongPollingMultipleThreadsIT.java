/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class LongPollingMultipleThreadsIT extends ChromeBrowserTest {

    @Test
    public void openPage_startUpdatingLabelThrowThreads_thereAreNoErrorsInTheConsole() {
        open();
        WebElement button = findElement(By.id("start-button"));
        for (int i = 0; i < 10; i++) {
            button.click();
            /*
             * SockJS client may try to connect to sockjs node server:
             * https://github.com/sockjs/sockjs-node/blob/master/README.md.
             *
             * This entry may be ignored.
             */

            checkLogsForErrors(msg -> msg.contains("sockjs-node")
                    || msg.contains("[WDS] Disconnected!"));
        }

    }
}
