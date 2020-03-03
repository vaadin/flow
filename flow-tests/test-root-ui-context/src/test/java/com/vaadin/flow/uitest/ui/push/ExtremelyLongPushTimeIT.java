/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.List;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.parallel.ExcludeFromSuite;

@ExcludeFromSuite
public class ExtremelyLongPushTimeIT extends ChromeBrowserTest {

    private static final int ONE_HOUR_IN_MS = 20 * 1000;

    @Test
    public void test24HourPush() throws Exception {
        open();

        // Without this there is a large chance that we will wait for all pushes
        // to complete before moving on
        getCommandExecutor().disableWaitForVaadin();

        // Wait for startButton to be present
        waitForElementVisible(By.id("startButton"));

        // Start the test
        findElement(By.id("startButton")).click();

        // Wait for push to start. Should take 60s
        waitUntil(ExpectedConditions.textToBePresentInElement(getLog(),
                "Package "), 120);

        // Check every hour that push is still going on
        for (int i = 0; i < 24; i++) {
            Thread.sleep(ONE_HOUR_IN_MS);
            ensureStillPushing();
        }

    }

    private WebElement getLog() {
        List<WebElement> elements = findElements(By.className("log"));
        return elements.get(elements.size() - 1);
    }

    private void ensureStillPushing() {
        String logValue = getLog().getText();
        // Wait for the log value to change. Should take max 60s
        waitUntilNot(
                ExpectedConditions.textToBePresentInElement(getLog(), logValue),
                120);
    }
}
