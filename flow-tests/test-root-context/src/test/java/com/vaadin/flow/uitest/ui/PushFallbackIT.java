/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.uitest.ui.PushFallbackView.CONTAINER_ID;
import static com.vaadin.flow.uitest.ui.PushFallbackView.MESSAGE_COUNT;
import static com.vaadin.flow.uitest.ui.PushFallbackView.PUSH_BUTTON_ID;

public class PushFallbackIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/push-fallback/com.vaadin.flow.uitest.ui.PushFallbackView";
    }

    @Test
    public void pushFallback_websocketsFailure_fallbackToLonPolling() {
        open();

        waitForElementPresent(By.id(PUSH_BUTTON_ID));

        checkPushWarningPresent();

        findElement(By.id(PUSH_BUTTON_ID)).click();

        waitUntil(driver -> {
            List<WebElement> messages = getPushMessages();
            return messages != null && messages.size() == MESSAGE_COUNT;
        });

        List<WebElement> pushMessages = getPushMessages();
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            Assert.assertEquals("Unexpected push message received",
                    "Push message " + i,
                    pushMessages.get(i).getText());
        }
    }

    private void checkPushWarningPresent() {
        Optional<LogEntry> websocketFailedMessage = getLogEntries(Level.SEVERE).stream().filter(logEntry ->
                logEntry.getMessage().contains("WebSocket connection to") &&
                logEntry.getMessage().contains("failed:")).findAny();

        Assert.assertTrue("Expected websockets connection to be failed",
                websocketFailedMessage.isPresent());

        Optional<LogEntry> longPollingEstablishedMessage =
                getLogEntries(Level.INFO).stream().filter(logEntry ->
                logEntry.getMessage().contains("Push connection established using long-polling")).findAny();

        Assert.assertTrue("Expected long polling connection to be established",
                longPollingEstablishedMessage.isPresent());
    }

    private List<WebElement> getPushMessages() {
        WebElement container = findElement(By.id(CONTAINER_ID));
        return container.findElements(By.tagName("div"));
    }
}
