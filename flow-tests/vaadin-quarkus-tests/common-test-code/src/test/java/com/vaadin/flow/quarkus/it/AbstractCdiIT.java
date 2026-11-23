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
package com.vaadin.flow.quarkus.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.test.AbstractChromeIT;

abstract public class AbstractCdiIT extends AbstractChromeIT {

    private static final int COUNT_TIMEOUT_SECONDS = 10;

    protected void click(String elementId) {
        findElement(By.id(elementId)).click();
    }

    protected void follow(String linkText) {
        findElement(By.linkText(linkText)).click();
    }

    protected String getText(String id) {
        waitForElementPresent(By.id(id));
        return findElement(By.id(id)).getText();
    }

    /**
     * Waits for a counter to reach the expected value.
     * <p>
     * Use it for counters updated when a session is destroyed: Flow destroys
     * the session at the end of the request, after the response has been sent,
     * so the client can be idle before the counter is updated.
     */
    protected void waitForCount(int expectedCount, String counter)
            throws IOException {
        getCommandExecutor().waitForVaadin();
        try {
            waitUntil(driver -> {
                try {
                    return expectedCount == readCount(counter);
                } catch (IOException ex) {
                    return false;
                }
            }, COUNT_TIMEOUT_SECONDS);
        } catch (TimeoutException ex) {
            Assert.assertEquals(expectedCount, readCount(counter));
        }
    }

    protected void assertCountEquals(int expectedCount, String counter)
            throws IOException {
        Assert.assertEquals(expectedCount, getCount(counter));
    }

    protected void assertTextEquals(String expectedText, String elementId) {
        Assert.assertEquals(expectedText, getText(elementId));
    }

    protected void resetCounts() throws IOException {
        slurp("?resetCounts");
    }

    protected int getCount(String id) throws IOException {
        getCommandExecutor().waitForVaadin();
        return readCount(id);
    }

    private int readCount(String id) throws IOException {
        return Integer.parseInt(slurp("?getCount=" + id));
    }

    private String slurp(String uri) throws IOException {
        URL url = URI.create(getRootURL() + uri).toURL();
        InputStream is = url.openConnection().getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = reader.readLine();
        reader.close();
        return line;
    }
}
