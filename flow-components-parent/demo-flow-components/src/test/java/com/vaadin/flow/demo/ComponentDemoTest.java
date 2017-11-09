/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.demo;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.Before;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.By;

/**
 * Base class for the integration tests of this project.
 *
 */
public abstract class ComponentDemoTest extends ChromeBrowserTest {
    protected WebElement layout;

    @Override
    protected int getDeploymentPort() {
        return 8080;
    }

    @Before
    public void openDemoPageAndCheckForErrors() {
        open();
        waitForElementPresent(By.tagName("main-layout"));
        layout = findElement(By.tagName("main-layout"));
        checkLogsForErrors();
    }

    protected Stream<LogEntry> getLogErrors() {
        return driver.manage().logs().get(LogType.BROWSER).getAll().stream()
                .filter(logEntry -> logEntry.getLevel().intValue() > Level.INFO
                        .intValue())
                // we always have this error
                .filter(logEntry -> !logEntry.getMessage()
                        .contains("favicon.ico"));
    }

    private void checkLogsForErrors() {
        getLogErrors().forEach(this::processWarningOrError);
    }

    private void processWarningOrError(LogEntry logEntry) {
        if (Objects.equals(logEntry.getLevel(), Level.SEVERE)
                || logEntry.getMessage().contains("404")) {
            throw new AssertionError(String.format(
                    "Received error message in browser log console right after opening the page, message: %s",
                    logEntry));
        } else {
            Logger.getLogger(ComponentDemoTest.class.getName())
                    .warning(String.format(
                            "This message in browser log console may be a potential error: '%s'",
                            logEntry));
        }
    }
}
