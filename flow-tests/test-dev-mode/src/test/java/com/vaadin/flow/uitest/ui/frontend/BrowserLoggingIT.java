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

package com.vaadin.flow.uitest.ui.frontend;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertThat;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
@Ignore
public class BrowserLoggingIT extends ChromeBrowserTest {

    @Test
    public void productionModeHasNoLogEntries() {
        openProduction();
        waitForElementPresent(By.id("elementId"));

        assertThat(
                "Flow in production mode should output nothing into the console",
                getLogEntriesCount(), is(0L));
    }

    @Test
    public void nonProductionModeHasLogEntries() {
        open();
        waitForElementPresent(By.id("elementId"));

        assertThat(
                "Flow in production mode should output nothing into the console",
                getLogEntriesCount(), greaterThan(0L));
    }

    private Long getLogEntriesCount() {
        // see java script imported in corresponding test view
        return (Long) executeScript("return window.allLogMessages.length;");
    }
}
