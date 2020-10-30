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
 *
 */
package com.vaadin.flow.ccdmtest;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.mobile.NetworkConnection;

import com.vaadin.flow.testutil.ChromeDeviceTest;

public class OfflineServerViewIT extends ChromeDeviceTest {

    @Test
    public void should_showOfflineStub_whenNavigatingToServerSideView()
            throws IOException {
        open();
        waitForServiceWorkerReady();

        waitForElementPresent(By.id("button3"));

        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);
        try {
            findElement(By.id("pathname")).sendKeys("serverview");
            findElement(By.id("button3")).click();

            waitForElementPresent(By.tagName("vaadin-offline-stub"));
            WebElement offlineStub = findElement(
                    By.tagName("vaadin-offline-stub"));

            Assert.assertFalse(
                    "vaadin-offline-stub shadow root expected to contain an element with class offline",
                    findInShadowRoot(offlineStub, By.className("offline"))
                            .isEmpty());
        } finally {
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Override
    protected String getTestPath() {
        return "/foo";
    }
}
