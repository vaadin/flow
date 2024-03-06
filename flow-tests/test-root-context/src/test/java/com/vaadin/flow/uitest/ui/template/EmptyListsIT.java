/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import java.util.Optional;
import java.util.stream.StreamSupport;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class EmptyListsIT extends ChromeBrowserTest {

    @Test
    public void emptyListsAreProperlyHandled() {
        open();

        TestBenchElement template = $("*").id("template");

        Assert.assertTrue(
                template.$("*").attributeContains("class", "item").exists());

        findElement(By.id("set-empty")).click();

        LogEntries logs = driver.manage().logs().get("browser");
        if (logs != null) {
            Optional<LogEntry> anyError = StreamSupport
                    .stream(logs.spliterator(), true)
                    .filter(entry -> entry.getLevel()
                            .intValue() > java.util.logging.Level.INFO
                                    .intValue())
                    .filter(entry -> !entry.getMessage()
                            .contains("favicon.ico"))
                    .filter(entry -> !entry.getMessage()
                            .contains("HTML Imports is deprecated"))
                    .filter(entry -> !entry.getMessage()
                            .contains("sockjs-node"))
                    .filter(entry -> !entry.getMessage()
                            .contains("[WDS] Disconnected!"))
                    // Web Socket error when trying to connect to Spring Dev
                    // Tools live-reload server.
                    .filter(entry -> !entry.getMessage()
                            .contains("WebSocket connection to 'ws://"))
                    .findAny();

            anyError.ifPresent(entry -> Assert.fail(entry.getMessage()));
        }
    }
}
