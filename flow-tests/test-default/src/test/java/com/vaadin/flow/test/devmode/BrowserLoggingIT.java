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
package com.vaadin.flow.test.devmode;

import java.util.ArrayList;

import org.openqa.selenium.By;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(BrowserLoggingView.class)
public class BrowserLoggingIT extends AbstractDefaultIT {

    @BrowserTest
    @SuppressWarnings("unchecked")
    public void developmentModeHasLogEntries() {
        open();
        waitForElementPresent(By.id("exception"));

        findElement(By.id("exception")).click();

        ArrayList<Object> logMessages = (ArrayList<Object>) executeScript(
                "return window.allLogMessages;");

        assertFalse(logMessages.isEmpty(),
                "Flow in development mode should output something into the console");

        // Check for "Scheduling heartbeat in" msg (= debug level)
        assertTrue(
                logMessages.stream()
                        .anyMatch(msg -> String.valueOf(msg)
                                .contains("Scheduling heartbeat in")),
                "Expected debug message not found in log");

        // Check for "Setting heartbeat interval to" msg (= info level)
        assertTrue(
                logMessages.stream()
                        .anyMatch(msg -> String.valueOf(msg)
                                .contains("Setting heartbeat interval to")),
                "Expected info message not found in log");

        // Check for exception thrown msg (= error level)
        assertTrue(logMessages.stream()
                .anyMatch(msg -> String.valueOf(msg).contains(
                        "Exception is thrown during JavaScript execution.")),
                "Expected error message not found in log");
    }
}
