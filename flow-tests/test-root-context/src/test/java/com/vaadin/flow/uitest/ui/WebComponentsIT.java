/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.StreamSupport;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.parallel.BrowserUtil;

public class WebComponentsIT extends ChromeBrowserTest {

    @Test
    public void testPolyfillLoaded() {
        open();

        if (BrowserUtil.isIE(getDesiredCapabilities())) {
            // Console logs are not available from IE11
            return;
        }

        LogEntries logs = driver.manage().logs().get("browser");
        if (logs != null) {
            Optional<LogEntry> anyError = StreamSupport
                    .stream(logs.spliterator(), true)
                    .filter(entry -> entry.getLevel().intValue() > Level.INFO
                            .intValue())
                    .filter(entry -> !entry.getMessage()
                            .contains("favicon.ico"))
                    .filter(entry -> !entry.getMessage()
                            .contains("sockjs-node"))
                    .filter(entry -> !entry.getMessage()
                            .contains("[WDS] Disconnected!"))
                    .findAny();
            anyError.ifPresent(entry -> Assert.fail(entry.getMessage()));
        }
    }
}