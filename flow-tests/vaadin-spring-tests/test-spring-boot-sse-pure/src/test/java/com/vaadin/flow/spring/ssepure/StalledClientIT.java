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
package com.vaadin.flow.spring.ssepure;

import java.io.OutputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

/**
 * A client that opens the event stream but stops reading it must not keep the
 * session locked, see https://github.com/vaadin/flow/issues/12316
 */
public class StalledClientIT {

    private static final String HOST = "localhost";
    private static final int PORT = Integer
            .parseInt(System.getProperty("server.port", "8888"));
    // Vaadin is mapped to /ui/* in this application
    private static final String PATH = "/ui";
    private static final String ROOT = "http://" + HOST + ":" + PORT + PATH;

    private final CookieManager cookies = new CookieManager();
    private final HttpClient http = HttpClient.newBuilder()
            .cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    public void stalledClient_otherRequestsOfSessionAreServed()
            throws Exception {
        get("/");
        String init = get("/?v-r=init&location=flood&query=");
        String uiId = find(init, "\"v-uiId\"\\s*:\\s*(\\d+)");
        String pushId = find(init, "\"Vaadin-Push-ID\"\\s*:\\s*\"([^\"]+)\"");

        try (Socket stalled = new Socket()) {
            // Reads nothing, so the buffers on the way fill up
            stalled.setReceiveBufferSize(4096);
            stalled.connect(new InetSocketAddress(HOST, PORT));
            OutputStream out = stalled.getOutputStream();
            out.write(("GET " + PATH + "/VAADIN/push?v-r=sse&v-uiId=" + uiId
                    + "&v-pushId=" + pushId + " HTTP/1.1\r\nHost: " + HOST + ":"
                    + PORT + "\r\nCookie: " + sessionCookie()
                    + "\r\nAccept: text/event-stream\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.flush();
            String head = readHead(stalled);
            Assert.assertTrue(head, head.startsWith("HTTP/1.1 200"));
            Assert.assertTrue(head, head.contains("text/event-stream"));
            // FloodStarter pushes half a megabyte every 50 ms
            Thread.sleep(3000);

            // Opening another UI needs the lock of the session
            long start = System.nanoTime();
            get("/?v-r=init&location=&query=");
            long millis = Duration.ofNanos(System.nanoTime() - start)
                    .toMillis();

            Assert.assertTrue(
                    "The session should not wait for the stalled client, waited "
                            + millis + " ms",
                    millis < 5000);

            // The client was really stalled with pushed messages, not with
            // an idle stream
            Assert.assertTrue("Too little was pushed to stall the client",
                    readFor(stalled, 2000) > 1024 * 1024);
        }
    }

    private static long readFor(Socket socket, int millis) throws Exception {
        socket.setSoTimeout(millis);
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        long end = System.currentTimeMillis() + millis;
        try {
            int read;
            while (System.currentTimeMillis() < end
                    && (read = socket.getInputStream().read(buffer)) > 0) {
                total += read;
            }
        } catch (java.net.SocketTimeoutException e) {
            // Nothing more arrived
        }
        return total;
    }

    /**
     * Reads the response headers of the stream, and nothing more.
     */
    private static String readHead(Socket socket) throws Exception {
        StringBuilder head = new StringBuilder();
        var in = socket.getInputStream();
        while (head.indexOf("\r\n\r\n") < 0) {
            int b = in.read();
            if (b < 0) {
                break;
            }
            head.append((char) b);
        }
        return head.toString();
    }

    private String get(String path) throws Exception {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create(ROOT + path))
                        .timeout(Duration.ofSeconds(60)).build(),
                HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(path, 200, response.statusCode());
        return response.body();
    }

    private String sessionCookie() {
        return cookies.getCookieStore().getCookies().stream()
                .map(HttpCookie::toString).reduce((a, b) -> a + "; " + b)
                .orElseThrow();
    }

    private static String find(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        Assert.assertTrue(regex + " not found", matcher.find());
        return matcher.group(1);
    }
}
