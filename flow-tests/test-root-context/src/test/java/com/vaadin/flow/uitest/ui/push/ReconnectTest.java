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
package com.vaadin.flow.uitest.ui.push;

import java.io.IOException;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.flow.testutil.PushConnectionTracker;

public abstract class ReconnectTest extends PushChromeBrowserTestWithProxy {

    private PushConnectionTracker pushConnections;

    @Override
    protected void updateHeadlessChromeOptions(ChromeOptions chromeOptions) {
        PushConnectionTracker.enable(chromeOptions);
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        pushConnections = new PushConnectionTracker(getDriver());

        open((String[]) null);

        startTimer();
        waitUntilServerCounterChanges();

        testBench().disableWaitForVaadin();
    }

    @Test
    public void messageIsQueuedOnDisconnect() throws IOException {
        disconnectProxy();

        clickButtonAndWaitForTwoReconnectAttempts();

        connectAndVerifyConnectionEstablished();
        waitUntilClientCounterChanges(1);
    }

    @Test
    public void messageIsNotSentBeforeConnectionIsEstablished()
            throws IOException, InterruptedException {
        disconnectProxy();

        waitForNextReconnectionAttempt();
        clickButtonAndWaitForTwoReconnectAttempts();

        connectAndVerifyConnectionEstablished();
        waitUntilClientCounterChanges(1);
    }

    @Override
    protected DesiredCapabilities getDesiredCapabilities() {
        return super.getDesiredCapabilities();
    }

    private void clickButtonAndWaitForTwoReconnectAttempts() {
        clickClientButton();

        // Reconnection attempt is where pending messages can
        // falsely be sent to server.
        waitForNextReconnectionAttempt();

        // Waiting for the second reconnection attempt makes sure that the
        // first attempt has been completed or aborted.
        waitForNextReconnectionAttempt();
    }

    private void clickClientButton() {
        getIncrementClientCounterButton().click();
    }

    private void waitForNextReconnectionAttempt() {
        int attempts = pushConnections.getAttemptCount();
        waitUntil(driver -> pushConnections.getAttemptCount() > attempts);
    }

    private void connectAndVerifyConnectionEstablished() throws IOException {
        connectProxy();
        waitUntilServerCounterChanges();
    }

    private WebElement getIncrementClientCounterButton() {
        return BasicPushIT.getIncrementButton(this);
    }

    private void waitUntilServerCounterChanges() {
        final int counter = BasicPushIT.getServerCounter(this);
        waitUntil(input -> {
            try {
                return BasicPushIT
                        .getServerCounter(ReconnectTest.this) > counter;
            } catch (NoSuchElementException e) {
                return false;
            }
        }, 30);
        ensureNoSystemErrorFromServer();
    }

    private void ensureNoSystemErrorFromServer() {
        // Make sure there is no error caused by messages sync lost
        waitForElementNotPresent(By.cssSelector("div.v-system-error"));
    }

    private void waitUntilClientCounterChanges(final int expectedValue) {
        waitUntil(
                input -> BasicPushIT
                        .getClientCounter(ReconnectTest.this) == expectedValue,
                5);
    }

    private void startTimer() {
        BasicPushIT.getServerCounterStartButton(this).click();
    }

}
