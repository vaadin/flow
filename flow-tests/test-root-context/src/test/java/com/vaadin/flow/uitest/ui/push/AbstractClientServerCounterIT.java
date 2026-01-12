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
package com.vaadin.flow.uitest.ui.push;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.uitest.ui.push.components.ClientServerCounter;
import com.vaadin.testbench.TestBenchTestCase;

public abstract class AbstractClientServerCounterIT extends ChromeBrowserTest {

    @Test
    public void testServerInitiatedCommunication() throws InterruptedException {
        open();

        getIncrementButton().click();
        testBench().disableWaitForVaadin();

        waitUntilClientCounterChanges(1);

        getIncrementButton().click();
        getIncrementButton().click();
        getIncrementButton().click();
        waitUntilClientCounterChanges(4);

        // Test server initiated push
        getServerCounterStartButton().click();
        waitUntilServerCounterChanges();
    }

    public static int getClientCounter(TestBenchTestCase t) {
        WebElement clientCounterElem = t
                .findElement(By.id(ClientServerCounter.CLIENT_COUNTER_ID));
        return Integer.parseInt(clientCounterElem.getText());
    }

    protected WebElement getIncrementButton() {
        return getIncrementButton(this);
    }

    protected WebElement getServerCounterStartButton() {
        return getServerCounterStartButton(this);
    }

    public static int getServerCounter(TestBenchTestCase t) {
        WebDriverException lastException = null;
        for (int i = 0; i < 10; i++) {
            try {
                WebElement serverCounterElem = t.findElement(
                        By.id(ClientServerCounter.SERVER_COUNTER_ID));
                return Integer.parseInt(serverCounterElem.getText());
            } catch (WebDriverException e) {
                lastException = e;
            }
        }

        throw lastException;
    }

    public static WebElement getServerCounterStartButton(TestBenchTestCase t) {
        return t.findElement(By.id(ClientServerCounter.START_TIMER_ID));
    }

    public static WebElement getServerCounterStopButton(TestBenchTestCase t) {
        return t.findElement(By.id(ClientServerCounter.STOP_TIMER_ID));
    }

    public static WebElement getIncrementButton(TestBenchTestCase t) {
        return t.findElement(By.id(ClientServerCounter.INCREMENT_BUTTON_ID));
    }

    protected void waitUntilClientCounterChanges(final int expectedValue) {
        waitUntil(new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply(WebDriver input) {
                return AbstractClientServerCounterIT.getClientCounter(
                        AbstractClientServerCounterIT.this) == expectedValue;
            }
        }, 10);
    }

    protected void waitUntilServerCounterChanges() {
        final int counter = AbstractClientServerCounterIT
                .getServerCounter(this);
        waitUntil(new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply(WebDriver input) {
                return AbstractClientServerCounterIT.getServerCounter(
                        AbstractClientServerCounterIT.this) > counter;
            }
        }, 10);
    }

}
