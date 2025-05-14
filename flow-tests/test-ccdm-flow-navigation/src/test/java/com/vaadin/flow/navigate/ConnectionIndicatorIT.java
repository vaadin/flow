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
package com.vaadin.flow.navigate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeDeviceTest;
import com.vaadin.testbench.commands.TestBenchCommandExecutor;

public class ConnectionIndicatorIT extends ChromeDeviceTest {

    @Before
    @Override
    public void setup() throws Exception {
        super.setup();
        getDriver().get(getRootURL() + "/connection-indicator");
        waitForDevServer();
        waitForServiceWorkerReady();
    }

    @Test
    public void online_goingOffline_customisedMessageShown() throws Exception {
        getDevTools().setOfflineEnabled(true);
        try {
            expectConnectionState("connection-lost");
            Assert.assertEquals("Custom offline",
                    getConnectionIndicatorStatusText());
        } finally {
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void offline_goingOnline_customisedMessageShown() throws Exception {
        getDevTools().setOfflineEnabled(true);
        try {
            expectConnectionState("connection-lost");
            getDevTools().setOfflineEnabled(false);
            expectConnectionState("connected");
            Assert.assertEquals("Custom online",
                    getConnectionIndicatorStatusText());
        } finally {
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void offline_serverConnectionAttempted_customisedMessageShown()
            throws Exception {
        getDevTools().setOfflineEnabled(true);
        try {
            expectConnectionState("connection-lost");
            findElement(By.id(ConnectionIndicatorView.CONNECT_SERVER)).click();
            testBench().disableWaitForVaadin(); // offline - do not run the
                                                // WAIT_FOR_VAADIN script
            expectConnectionState("reconnecting");
            Assert.assertEquals("Custom reconnecting",
                    getConnectionIndicatorStatusText());
        } finally {
            getDevTools().setOfflineEnabled(false);
        }
    }

    @Test
    public void offline_serverConnectionAttempted_javaCustomisedMessagesShown()
            throws Exception {
        findElement(By.id(ConnectionIndicatorView.SET_CUSTOM_MESSAGES)).click();
        ((TestBenchCommandExecutor) testBench()).waitForVaadin();
        getDevTools().setOfflineEnabled(true);
        try {
            expectConnectionState("connection-lost");
            Assert.assertEquals(ConnectionIndicatorView.CUSTOM_OFFLINE_MESSAGE,
                    getConnectionIndicatorStatusText());
            findElement(By.id(ConnectionIndicatorView.CONNECT_SERVER)).click();
            testBench().disableWaitForVaadin(); // offline - do not run the
                                                // WAIT_FOR_VAADIN script
            expectConnectionState("reconnecting");
            Assert.assertEquals(
                    ConnectionIndicatorView.CUSTOM_RECONNECTING_MESSAGE,
                    getConnectionIndicatorStatusText());
        } finally {
            getDevTools().setOfflineEnabled(false);
            testBench().enableWaitForVaadin();
        }
    }

    private String getConnectionIndicatorStatusText() {
        By statusMessageLocator = By.xpath(
                "//vaadin-connection-indicator/div[contains(@class,'v-status-message')]/span");
        waitUntil(driver -> {
            WebElement statusTextElement = findElement(statusMessageLocator);
            return statusTextElement != null
                    && !statusTextElement.getText().isEmpty();
        });
        WebElement statusTextElement = findElement(statusMessageLocator);
        Assert.assertNotNull("Unable to find connection indicator status text",
                statusTextElement);
        return statusTextElement.getText();
    }

    private void expectConnectionState(String state) {
        waitUntil(driver -> ((JavascriptExecutor) driver).executeScript(
                "return window.Vaadin.connectionState.state === '" + state
                        + "'"),
                2);
    }

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/context-path";
    }
}
