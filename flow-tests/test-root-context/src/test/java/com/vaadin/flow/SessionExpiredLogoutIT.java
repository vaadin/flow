/*
 * Copyright 2000-2019 Vaadin Ltd.
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
 *
 */

package com.vaadin.flow;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.vaadin.flow.component.html.testbench.LabelElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

public class SessionExpiredLogoutIT extends ChromeBrowserTest {

    @Test
    public void changeOnClient() throws InterruptedException {
        open();

        $(NativeButtonElement.class).first().click();

        String sessionExpiredText = $(LabelElement.class).first().getText();
        Assert.assertEquals("Missing session expired label", "Session expired!",
                sessionExpiredText);

        analyzeLog();
    }

    public void analyzeLog() {

        // TODO make sure there are no errors.

        List<LogEntry> logEntries = getLogEntries(Level.ALL);
        System.out.println("logEntries: " + logEntries.size());
        logEntries.forEach(entry -> {
            System.out.println(new Date(entry.getTimestamp()) + " " + entry.getLevel() + " " + entry.getMessage());
        });
    }

}
