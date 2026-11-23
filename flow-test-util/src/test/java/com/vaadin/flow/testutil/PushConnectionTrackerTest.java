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
package com.vaadin.flow.testutil;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import com.vaadin.flow.shared.ui.Transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PushConnectionTrackerTest {

    private static final String PUSH_URL = "http://localhost:8888/VAADIN/push?v-r=push&v-uiId=0&X-Atmosphere-Transport=";

    private final WebDriver driver = mock(WebDriver.class, RETURNS_DEEP_STUBS);

    @Test
    void getTransports_connectionAttempts_recordedInOrder() {
        givenLogs(webSocketCreated(
                "ws://localhost:8888/VAADIN/push?v-r=push&v-uiId=0&X-Atmosphere-Transport=websocket"),
                request(PUSH_URL + "long-polling"), request(PUSH_URL + "sse"));

        assertEquals(List.of("websocket", "long-polling", "sse"),
                new PushConnectionTracker(driver).getTransports());
    }

    @Test
    void getTransports_requestsThatOpenNoPushConnection_ignored() {
        givenLogs(request(PUSH_URL + "close"), request(PUSH_URL + "polling"),
                // Development tools connection
                webSocketCreated(
                        "ws://localhost:8888/VAADIN/push?v-r=push&debug_window&X-Atmosphere-Transport=websocket"),
                request("http://localhost:8888/?v-r=uidl&X-Atmosphere-Transport=long-polling"),
                request("http://localhost:8888/VAADIN/push?v-r=push"),
                entry("Network.responseReceived", "{\"response\":{\"url\":\""
                        + PUSH_URL + "long-polling\"}}"));

        assertEquals(List.of(),
                new PushConnectionTracker(driver).getTransports());
    }

    @Test
    void getTransports_logReadAgain_attemptsAccumulate() {
        givenLogs(request(PUSH_URL + "long-polling"));
        PushConnectionTracker tracker = new PushConnectionTracker(driver);
        assertEquals(1, tracker.getAttemptCount());

        givenLogs(request(PUSH_URL + "long-polling"));

        assertEquals(List.of("long-polling", "long-polling"),
                tracker.getTransports());
    }

    @Test
    void assertOnlyTransport_websocketXhr_matchesWebsocket() {
        givenLogs(
                webSocketCreated(PUSH_URL.replace("http", "ws") + "websocket"));

        new PushConnectionTracker(driver)
                .assertOnlyTransport(Transport.WEBSOCKET_XHR);
    }

    @Test
    void assertOnlyTransport_fallbackToOtherTransport_fails() {
        givenLogs(
                webSocketCreated(PUSH_URL.replace("http", "ws") + "websocket"),
                request(PUSH_URL + "long-polling"));

        AssertionError error = assertThrows(AssertionError.class,
                () -> new PushConnectionTracker(driver)
                        .assertOnlyTransport(Transport.WEBSOCKET));
        assertEquals(
                "Push should only use websocket, but it used [websocket, long-polling]",
                error.getMessage());
    }

    @Test
    void assertOnlyTransport_noAttempts_fails() {
        givenLogs();

        assertThrows(AssertionError.class,
                () -> new PushConnectionTracker(driver)
                        .assertOnlyTransport(Transport.LONG_POLLING));
    }

    @Test
    void getTransports_performanceLogNotRecorded_failsWithReason() {
        when(driver.manage().logs().getAvailableLogTypes())
                .thenReturn(Set.of(LogType.BROWSER));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new PushConnectionTracker(driver).getTransports());
        assertTrue(error.getMessage().contains("performance log"),
                error.getMessage());
    }

    @Test
    void assertOnlyTransport_performanceLogNotRecorded_skipped() {
        when(driver.manage().logs().getAvailableLogTypes())
                .thenReturn(Set.of(LogType.BROWSER));

        new PushConnectionTracker(driver)
                .assertOnlyTransport(Transport.WEBSOCKET);
    }

    private void givenLogs(LogEntry... entries) {
        when(driver.manage().logs().getAvailableLogTypes())
                .thenReturn(Set.of(LogType.BROWSER, LogType.PERFORMANCE));
        when(driver.manage().logs().get(LogType.PERFORMANCE))
                .thenReturn(new LogEntries(Arrays.asList(entries)));
    }

    private static LogEntry webSocketCreated(String url) {
        return entry("Network.webSocketCreated", "{\"url\":\"" + url + "\"}");
    }

    private static LogEntry request(String url) {
        return entry("Network.requestWillBeSent",
                "{\"request\":{\"url\":\"" + url + "\"}}");
    }

    private static LogEntry entry(String method, String params) {
        return new LogEntry(Level.INFO, 0, "{\"message\":{\"method\":\""
                + method + "\",\"params\":" + params + "}}");
    }
}
