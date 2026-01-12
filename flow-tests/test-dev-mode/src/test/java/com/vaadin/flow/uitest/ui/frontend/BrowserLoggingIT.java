/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.frontend;

import java.util.ArrayList;

import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class BrowserLoggingIT extends ChromeBrowserTest {

    @Test
    public void developmentModeHasLogEntries() {
        open();
        waitForElementPresent(By.id("exception"));

        findElement(By.id("exception")).click();

        ArrayList<Object> logMessages = (ArrayList<Object>) executeScript(
                "return window.allLogMessages;");

        assertThat(
                "Flow in development mode should output something into the console",
                logMessages.size(), greaterThan(0));

        // Check for "Scheduling heartbeat in" msg (= debug level)
        assertThat("Expected debug message not found in log",
                logMessages.stream().anyMatch(msg -> String.valueOf(msg)
                        .contains("Scheduling heartbeat in")));

        // Check for "Setting heartbeat interval to" msg (= info level)
        assertThat("Expected info message not found in log",
                logMessages.stream().anyMatch(msg -> String.valueOf(msg)
                        .contains("Setting heartbeat interval to")));

        // Check for exception thrown msg (= error level)
        assertThat("Expected error message not found in log", logMessages
                .stream().anyMatch(msg -> String.valueOf(msg).contains(
                        "Exception is thrown during JavaScript execution.")));
    }
}
