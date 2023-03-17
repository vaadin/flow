/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.navigate;

import com.vaadin.flow.testutil.ChromeDeviceTest;
import com.vaadin.testbench.commands.TestBenchCommandExecutor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.mobile.NetworkConnection;

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
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);
        try {
            expectConnectionState("connection-lost");
            Assert.assertEquals("Custom offline",
                    getConnectionIndicatorStatusText());
        } finally {
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void offline_goingOnline_customisedMessageShown() throws Exception {
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);
        try {
            expectConnectionState("connection-lost");
            setConnectionType(NetworkConnection.ConnectionType.ALL);
            expectConnectionState("connected");
            Assert.assertEquals("Custom online",
                    getConnectionIndicatorStatusText());
        } finally {
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void offline_serverConnectionAttempted_customisedMessageShown()
            throws Exception {
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);
        try {
            expectConnectionState("connection-lost");
            findElement(By.id(ConnectionIndicatorView.CONNECT_SERVER)).click();
            testBench().disableWaitForVaadin(); // offline - do not run the
                                                // WAIT_FOR_VAADIN script
            expectConnectionState("reconnecting");
            Assert.assertEquals("Custom reconnecting",
                    getConnectionIndicatorStatusText());
        } finally {
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void offline_serverConnectionAttempted_javaCustomisedMessagesShown()
            throws Exception {
        findElement(By.id(ConnectionIndicatorView.SET_CUSTOM_MESSAGES)).click();
        ((TestBenchCommandExecutor) testBench()).waitForVaadin();
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);
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
            setConnectionType(NetworkConnection.ConnectionType.ALL);
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
