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
package com.vaadin.flow.test.routing;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.fail;

@TestFor(QueuedHistoryUpdateView.class)
public class QueuedHistoryUpdateIT extends AbstractDefaultIT {

    @BrowserTest
    public void queuedHistoryUpdates_onlyUpdatesWithCallbackEnterView()
            throws InterruptedException {
        open();
        waitForEntered(QueuedHistoryUpdateView.ROUTE);

        $(NativeButtonElement.class)
                .id(QueuedHistoryUpdateView.UPDATE_BUTTON_ID).click();

        int rounds = QueuedHistoryUpdateView.ROUNDS;
        waitUntil(driver -> driver.getCurrentUrl()
                .endsWith(QueuedHistoryUpdateView.ROUTE + "?second=" + rounds));
        checkLogsForErrors();

        List<String> expected = new ArrayList<>();
        expected.add(QueuedHistoryUpdateView.ROUTE);
        for (int round = 1; round <= rounds; round++) {
            expected.add(QueuedHistoryUpdateView.ROUTE + "?callback=" + round);
        }
        String expectedText = String.join(",", expected);
        waitForEntered("?callback=" + rounds);
        // A late server navigation for an update without callback would
        // arrive after the last expected one
        Thread.sleep(500);
        Assertions.assertEquals(expectedText, getEntered(),
                "Only the updates with callback should enter the view");
    }

    private void waitForEntered(String location) {
        try {
            waitUntil(driver -> getEntered().endsWith(location));
        } catch (TimeoutException e) {
            fail("Expected the view to be entered last for '" + location
                    + "' but it was entered for '" + getEntered() + "'");
        }
    }

    private String getEntered() {
        return findElement(By.id(QueuedHistoryUpdateView.ENTERED_ID)).getText();
    }
}
