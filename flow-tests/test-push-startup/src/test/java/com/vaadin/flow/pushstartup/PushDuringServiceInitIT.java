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
package com.vaadin.flow.pushstartup;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.testutil.AbstractTestBenchTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Reproduces a push connection arriving while the Vaadin service is still
 * initializing, the way a browser tab left open against a previous server does
 * when it keeps retrying its push connection across a restart.
 * <p>
 * No browser is involved: the connection is opened with a plain websocket
 * client, because what is being tested is what the server does with a
 * connection it cannot serve yet.
 */
public class PushDuringServiceInitIT {

    /**
     * Part of the response a push connection gets when it belongs to no
     * session.
     * <p>
     * The connection opened here deliberately belongs to none, so once it is
     * finally handled it takes the ordinary path, finds no session, and is
     * answered with this. Receiving it is what shows the held connection was
     * served rather than dropped, and that the request was started for it,
     * which is the step that used to fail while the service was starting. It is
     * also exactly what a browser tab left open across a server restart gets.
     */
    private static final String SESSION_EXPIRED = "sessionExpired";

    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    @Test
    public void pushConnectsWhileServiceInitializing_connectionServedOnceReady()
            throws Exception {
        String rootUrl = "http://localhost:"
                + AbstractTestBenchTest.SERVER_PORT;
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build();

        // The servlet is initialized on this first request, and
        // SlowServiceInitListener holds initialization open while it runs
        CompletableFuture<HttpResponse<String>> initTrigger = client.sendAsync(
                HttpRequest.newBuilder(URI.create(rootUrl + "/")).build(),
                HttpResponse.BodyHandlers.ofString());

        // Let initialization get past the point where the push endpoint is
        // wired to the service, which happens before the service reports
        // itself as initialized
        Thread.sleep(SlowServiceInitListener.INIT_DELAY_MS / 8);
        assertFalse(initTrigger.isDone(),
                "Service initialization should still be running when the push "
                        + "connection is opened, otherwise this test is not "
                        + "exercising the startup window at all");

        MessageCollector collector = new MessageCollector();
        WebSocket webSocket = client.newWebSocketBuilder()
                // Atmosphere resolves the transport from this header, and
                // treats the connection as undefined without it, which would
                // take a different path through the push handler than a real
                // websocket does
                .header("X-Atmosphere-Transport", "websocket")
                .header("X-Atmosphere-Framework", "3.0.5")
                .header("X-Atmosphere-tracking-id", "0")
                .header("X-Atmosphere-TrackMessageSize", "true")
                .buildAsync(URI.create(pushUrl(rootUrl)), collector)
                .get(RESPONSE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        try {
            // Waiting for this particular response rather than for any message
            // at all: holding the connection suspends it, which already makes
            // the server send the atmosphere handshake, and that alone would
            // not say whether the connection was ever handled
            String response = collector.awaitMessageContaining(SESSION_EXPIRED,
                    RESPONSE_TIMEOUT);
            assertNotNull(response,
                    "A push connection opened while the service was still "
                            + "initializing should be held open and handled "
                            + "once initialization completes, but no response "
                            + "was received. " + collector.describe());
        } finally {
            webSocket.abort();
        }

        assertEquals(200,
                initTrigger.get(RESPONSE_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                        .statusCode(),
                "The request that triggered service initialization should have "
                        + "succeeded");
    }

    /**
     * Builds the push URL the way the client does, including the atmosphere
     * protocol parameters, with identifiers that belong to no live session.
     */
    private static String pushUrl(String rootUrl) {
        return rootUrl.replaceFirst("^http", "ws") + "/VAADIN/push?v-r=push"
                + "&v-uiId=0&v-pushId=00000000-0000-0000-0000-000000000000"
                + "&X-Atmosphere-tracking-id=0&X-Atmosphere-Framework=3.0.5"
                + "&X-Atmosphere-Transport=websocket"
                + "&X-Atmosphere-TrackMessageSize=true&X-atmo-protocol=true";
    }

    private static final class MessageCollector implements WebSocket.Listener {

        private final List<String> messages = new CopyOnWriteArrayList<>();
        private final StringBuilder partial = new StringBuilder();
        private volatile String connectionEnd;

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data,
                boolean last) {
            partial.append(data);
            if (last) {
                messages.add(partial.toString());
                partial.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode,
                String reason) {
            connectionEnd = "was closed with status " + statusCode + " ("
                    + reason + ")";
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            connectionEnd = "failed with " + error;
        }

        private String awaitMessageContaining(String text, Duration timeout)
                throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            while (System.currentTimeMillis() < deadline) {
                String match = messages.stream()
                        .filter(message -> message.contains(text)).findFirst()
                        .orElse(null);
                if (match != null) {
                    return match;
                }
                Thread.sleep(100);
            }
            return null;
        }

        private String describe() {
            return "Received messages: " + messages
                    + (connectionEnd == null ? ""
                            : ", the connection " + connectionEnd);
        }
    }
}
