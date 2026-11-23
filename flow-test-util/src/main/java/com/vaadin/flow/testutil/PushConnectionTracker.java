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

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Assume;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Records the push connections that a Chrome browser opens, as seen on the
 * network, so that a test can check which transport push really used.
 * <p>
 * A test that only checks that a pushed update arrives cannot tell a working
 * websocket from one that failed: the client falls back to long polling without
 * an error and the update still arrives. The tracker reads the websocket and
 * HTTP requests to the push endpoint from the browser's performance log, which
 * does not depend on the push client library or on anything the application
 * does.
 * <p>
 * The performance log has to be enabled with {@link #enable(ChromeOptions)}
 * when the browser is created, e.g. in
 * {@link ChromeBrowserTest#updateHeadlessChromeOptions(ChromeOptions)}. Only a
 * local Chrome is supported, and {@link ChromeBrowserTest} does not apply those
 * options when it runs on a hub or with a debugger attached; see
 * {@link #isAvailable()}.
 */
public class PushConnectionTracker {

    private static final String PUSH_REQUEST = "v-r=push";
    private static final String TRANSPORT_PARAMETER = "X-Atmosphere-Transport";
    private static final String NOT_AVAILABLE = "the browser does not record "
            + "the performance log. Push connections can only be tracked in a "
            + "local Chrome created with PushConnectionTracker.enable(), "
            + "which ChromeBrowserTest skips on a hub or with a debugger "
            + "attached.";

    private final WebDriver driver;
    private final List<String> transports = new ArrayList<>();

    /**
     * Creates a tracker for the given browser.
     *
     * @param driver
     *            the driver of a browser created with options passed to
     *            {@link #enable(ChromeOptions)}, not {@code null}
     */
    public PushConnectionTracker(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Enables the browser performance log that the tracker reads, with network
     * events only. Browser console logging stays enabled.
     *
     * @param options
     *            the options of the browser to create, not {@code null}
     */
    public static void enable(ChromeOptions options) {
        LoggingPreferences logs = new LoggingPreferences();
        logs.enable(LogType.BROWSER, Level.ALL);
        logs.enable(LogType.PERFORMANCE, Level.ALL);
        options.setCapability("goog:loggingPrefs", logs);
        options.setExperimentalOption("perfLoggingPrefs",
                Map.of("enableNetwork", true, "enablePage", false));
    }

    /**
     * Checks whether the browser records the performance log that the tracker
     * reads, i.e. whether it was created with the options passed to
     * {@link #enable(ChromeOptions)}.
     *
     * @return {@code true} if push connections can be tracked, {@code false}
     *         otherwise
     */
    public boolean isAvailable() {
        return driver.manage().logs().getAvailableLogTypes()
                .contains(LogType.PERFORMANCE);
    }

    /**
     * Gets the transports of all push connection attempts the browser has made
     * so far, in the order it made them. Requests that open no connection, such
     * as messages sent to the server over the push URL, are left out. A
     * websocket counts when the browser starts to open it, whether or not it
     * opens. Each long polling request counts on its own, so long polling adds
     * an entry whenever a request is renewed.
     *
     * @return the Atmosphere transport names, such as {@code websocket},
     *         {@code long-polling} or {@code sse}
     * @throws IllegalStateException
     *             if the browser does not record the performance log
     * @see #isAvailable()
     */
    public List<String> getTransports() {
        if (!isAvailable()) {
            throw new IllegalStateException(NOT_AVAILABLE);
        }
        for (LogEntry entry : driver.manage().logs().get(LogType.PERFORMANCE)) {
            JsonNode message = JsonMapper.shared().readTree(entry.getMessage())
                    .path("message");
            String method = message.path("method").asString("");
            JsonNode params = message.path("params");
            String url;
            if ("Network.webSocketCreated".equals(method)) {
                url = params.path("url").asString("");
            } else if ("Network.requestWillBeSent".equals(method)) {
                url = params.path("request").path("url").asString("");
            } else {
                continue;
            }
            String transport = getTransport(url);
            // Atmosphere sends a closed connection notice and, with a
            // unidirectional transport, messages to the server as requests of
            // their own, which open no connection
            if (transport != null && !"close".equals(transport)
                    && !"polling".equals(transport)) {
                transports.add(transport);
            }
        }
        return List.copyOf(transports);
    }

    /**
     * Gets the number of push connection attempts the browser has made so far.
     *
     * @return the number of attempts
     * @throws IllegalStateException
     *             if the browser does not record the performance log
     * @see #getTransports()
     */
    public int getAttemptCount() {
        return getTransports().size();
    }

    /**
     * Asserts that the browser made at least one push connection attempt and
     * used only the given transport for all of them, i.e. that it did not fall
     * back to another transport. If the browser does not record the performance
     * log, e.g. on a hub or with a debugger attached, the test is reported as
     * skipped by a failed assumption instead of passing without the check.
     *
     * @param expected
     *            the transport push should use, not {@code null}
     * @see #isAvailable()
     */
    public void assertOnlyTransport(Transport expected) {
        Assume.assumeTrue("Not checking the push transport: " + NOT_AVAILABLE,
                isAvailable());
        String name = getAtmosphereTransport(expected);
        List<String> actual = getTransports();
        Assert.assertTrue(
                "Push should only use " + name + ", but it used " + actual,
                !actual.isEmpty() && actual.stream().allMatch(name::equals));
    }

    /**
     * Gets the name Atmosphere uses on the network for the given transport.
     * {@link Transport#WEBSOCKET_XHR} pushes over a websocket, too; only the
     * messages to the server go over XHR, to the regular request handler.
     *
     * @param transport
     *            the transport, not {@code null}
     * @return the Atmosphere transport name
     */
    public static String getAtmosphereTransport(Transport transport) {
        return transport == Transport.WEBSOCKET_XHR
                ? Transport.WEBSOCKET.getIdentifier()
                : transport.getIdentifier();
    }

    private static String getTransport(String url) {
        if (!url.contains(PUSH_REQUEST)) {
            return null;
        }
        String query = URI.create(url).getRawQuery();
        if (query == null) {
            return null;
        }
        String transport = null;
        for (String parameter : query.split("&")) {
            String[] nameAndValue = parameter.split("=", 2);
            if (ApplicationConstants.DEBUG_WINDOW_CONNECTION
                    .equals(nameAndValue[0])) {
                // The development tools connection, not push
                return null;
            }
            if (TRANSPORT_PARAMETER.equals(nameAndValue[0])
                    && nameAndValue.length == 2) {
                transport = URLDecoder.decode(nameAndValue[1],
                        StandardCharsets.UTF_8);
            }
        }
        return transport;
    }
}
