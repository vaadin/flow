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
package com.vaadin.flow.pushclose;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.testutil.AbstractTestBenchTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Closes push connections from the client side while the server is still
 * writing push messages to them, the way a browser tab that is closed or
 * reloaded during a large push does, and checks that the server does not log
 * the close as an error.
 * <p>
 * No browser is involved: the connection is opened with a plain websocket
 * client, because the client has to stop reading so that the server's write is
 * still in progress when the close arrives, and a browser always reads.
 */
public class PushClientCloseIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /**
     * How many connections are closed. What the server does when the close
     * arrives depends on where its writes are at that moment, so a single close
     * does not reliably reproduce a problem.
     */
    private static final int CLOSES = 30;

    private static final Pattern UI_ID = Pattern
            .compile("\"v-uiId\"\\s*:\\s*(\\d+)");
    private static final Pattern PUSH_ID = Pattern
            .compile("\"Vaadin-Push-ID\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * Part of the text the server pushes, to tell its pushes apart from the
     * messages that set up the connection.
     */
    private static final String PUSHED_TEXT = String.valueOf(PushFlooder.FILLER)
            .repeat(100);

    private final String rootUrl = "http://localhost:"
            + AbstractTestBenchTest.SERVER_PORT;

    @Test
    public void clientClosesPushWhileServerWrites_noErrorLogged()
            throws Exception {
        String errorsBefore = errorLog();

        for (int i = 0; i < CLOSES; i++) {
            openFloodAndClose();
        }

        String errorsAfter = errorLog();
        assertTrue(errorsAfter.startsWith(errorsBefore),
                "The error log should only grow");
        assertEquals("", errorsAfter.substring(errorsBefore.length()).strip(),
                "Closing push connections from the client side while the "
                        + "server is writing to them should not log errors");
    }

    /**
     * Opens a UI and its push connection the way the client does, lets the
     * server's pushes back up by not reading them, and then closes the
     * connection.
     */
    private void openFloodAndClose() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager()).connectTimeout(TIMEOUT)
                .build();

        String init = client.send(HttpRequest
                .newBuilder(URI.create(rootUrl + "/?v-r=init&location="))
                .build(), HttpResponse.BodyHandlers.ofString()).body();
        String uiId = find(UI_ID, init);
        String pushId = find(PUSH_ID, init);

        PushListener listener = new PushListener();
        WebSocket webSocket = client.newWebSocketBuilder()
                .header("X-Atmosphere-Transport", "websocket")
                .buildAsync(URI.create(pushUrl(uiId, pushId)), listener)
                .get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        try {
            assertTrue(
                    listener.pushReceived.await(TIMEOUT.toSeconds(),
                            TimeUnit.SECONDS),
                    "The server should push updates once the push connection "
                            + "is open, received: " + listener.received);
            // The listener stops asking for more as soon as a push has
            // arrived, so the pushes that follow fill the network buffers and
            // the server ends up waiting for a write to complete
            Thread.sleep(500);
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "")
                    .get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            // Give the server time to handle the close
            Thread.sleep(200);
        } finally {
            webSocket.abort();
        }
    }

    private String pushUrl(String uiId, String pushId) {
        return rootUrl.replaceFirst("^http", "ws") + "/VAADIN/push?v-r=push"
                + "&v-uiId=" + uiId + "&v-pushId=" + pushId
                + "&X-Atmosphere-tracking-id=0&X-Atmosphere-Framework=3.0.5"
                + "&X-Atmosphere-Transport=websocket"
                + "&X-Atmosphere-TrackMessageSize=true&X-atmo-protocol=true";
    }

    private String errorLog() throws Exception {
        return HttpClient.newHttpClient()
                .send(HttpRequest
                        .newBuilder(URI.create(rootUrl + ErrorLogServlet.PATH))
                        .build(), HttpResponse.BodyHandlers.ofString())
                .body();
    }

    private static String find(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        assertTrue(matcher.find(),
                "Expected " + pattern + " in the bootstrap response: " + text);
        return matcher.group(1);
    }

    private static final class PushListener implements WebSocket.Listener {

        private final CountDownLatch pushReceived = new CountDownLatch(1);
        private final StringBuilder received = new StringBuilder();

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode,
                String reason) {
            received.append("closed " + statusCode + " " + reason);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            received.append("error " + error);
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data,
                boolean last) {
            received.append(data, 0, Math.min(data.length(), 300)).append('|');
            if (data.toString().contains(PUSHED_TEXT)) {
                pushReceived.countDown();
            } else {
                webSocket.request(1);
            }
            return null;
        }
    }
}
