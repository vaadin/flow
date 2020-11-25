/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.testutil.ChromeDeviceTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.mobile.NetworkConnection;

import static com.vaadin.flow.navigate.HelloWorldView.NAVIGATE_ABOUT;

public class ConnectionIndicatorIT extends ChromeDeviceTest {

    @Test
    public void online_goingOffline_customisedMessageShown() throws Exception {
        getDriver().get(getRootURL() + "/");
        waitForServiceWorkerReady();
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);
        try {
            expectConnectionState("connection-lost");
            Assert.assertEquals("Custom offline", getConnectionIndicatorStatusText());
        } finally {
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void offline_goingOnline_customisedMessageShown() throws Exception {
        getDriver().get(getRootURL() + "/");
        waitForServiceWorkerReady();
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);
        try {
            expectConnectionState("connection-lost");
            setConnectionType(NetworkConnection.ConnectionType.ALL);
            expectConnectionState("connected");
            Assert.assertEquals("Custom online", getConnectionIndicatorStatusText());
        } finally {
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void offline_serverConnectionAttempted_customisedMessageShown() throws Exception {
        getDriver().get(getRootURL() + "/hello");
        waitForServiceWorkerReady();
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);
        try {
            expectConnectionState("connection-lost");
            findElement(By.id(NAVIGATE_ABOUT)).click();
            testBench().disableWaitForVaadin(); // offline - do run the WAIT_FOR_VAADIN script
            expectConnectionState("reconnecting");
            Assert.assertEquals("Custom reconnecting", getConnectionIndicatorStatusText());
        } finally {
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    private String getConnectionIndicatorStatusText() {
        WebElement statusTextElement = findElement(By.xpath("//vaadin-connection-indicator/div[contains(@class,'v-status-message')]/span"));
        Assert.assertNotNull("Unable to find connection indicator status text", statusTextElement);
        return statusTextElement.getText();
    }

    private void expectConnectionState(String state) {
        waitUntil(driver -> ((JavascriptExecutor) driver)
                .executeScript("return window.Vaadin.connectionState.state === '" + state + "'"), 2);
    }

}
