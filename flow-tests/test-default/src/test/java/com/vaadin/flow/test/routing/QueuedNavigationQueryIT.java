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

import org.junit.jupiter.api.Assertions;

import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(QueuedNavigationQueryView.class)
public class QueuedNavigationQueryIT extends AbstractDefaultIT {

    @BrowserTest
    public void navigateWhileNavigationInProgress_queryAndHashPreserved() {
        open();

        AnchorElement first = $(AnchorElement.class)
                .id(QueuedNavigationQueryView.FIRST_ANCHOR_ID);
        AnchorElement second = $(AnchorElement.class)
                .id(QueuedNavigationQueryView.SECOND_ANCHOR_ID);
        SpanElement queryLog = $(SpanElement.class)
                .id(QueuedNavigationQueryView.QUERY_LOG_ID);
        // Start the second navigation while the first one is still waiting
        // for the server, and record the log at that moment to verify it
        executeScript("""
                arguments[0].click();
                setTimeout(() => {
                    window.queryLogAtSecondClick = arguments[2].textContent;
                    arguments[1].click();
                }, 100);
                """, first, second, queryLog);

        waitUntil(driver -> queryLog.getText().contains(","));

        Assertions.assertEquals("",
                executeScript("return window.queryLogAtSecondClick"),
                "Second navigation should start while the first one is in progress");
        Assertions.assertEquals("first,second", queryLog.getText());
        waitUntil(driver -> driver.getCurrentUrl()
                .endsWith("/second?qp=second#fragment"));
    }
}
