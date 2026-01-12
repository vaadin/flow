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
package com.vaadin.flow.misc.ui;

import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ExceptionLoggingIT extends ChromeBrowserTest {

    @Test
    public void productionModeExceptionViaServerJsShouldLogException() {
        open();
        findElement(By.id("exception")).click();

        // Checking that the log count is exactly 1 also ensures that no info or
        // debug level messages were logged before the exception message.
        assertThat("Flow in production mode should output exception to console",
                getLogEntriesCount(), is(1L));
        assertThat("Error message should have contained 'foo'",
                checkFirstErrorMessageContains("foo"));
    }

    @Test
    public void productionModeExceptionViaExternalJsShouldLogException() {
        open();
        findElement(By.id("externalException")).click();

        // Checking that the log count is exactly 1 also ensures that no info or
        // debug level messages were logged before the exception message.
        assertThat("Flow in production mode should output exception to console",
                getLogEntriesCount(), is(1L));
        assertThat("Error message should have contained 'bar'",
                checkFirstErrorMessageContains("bar"));
    }

    private Long getLogEntriesCount() {
        return (Long) executeScript("return window.allLogMessages.length;");
    }

    private boolean checkFirstErrorMessageContains(String shouldContain) {
        return ((String) executeScript("return window.allLogMessages[0];"))
                .contains(shouldContain);
    }

    @Override
    protected String getTestPath() {
        return "/exception-logging";
    }
}
