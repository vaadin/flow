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
package com.vaadin.flow.quarkus.it.sseaddon;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.testutil.ReverseProxy;

/**
 * A client that opens the event stream but stops reading it must not keep the
 * session locked, see https://github.com/vaadin/flow/issues/12316
 */
@QuarkusIntegrationTest
public class StalledClientIT {

    private static final int SERVER_PORT = Integer
            .parseInt(System.getProperty("server.port", "8888"));
    // Through the reverse proxy when the tests run through one, which then
    // also sits between the stalled client and the server
    private static final URI SERVER = URI.create(
            ReverseProxy.isEnabled() ? ReverseProxy.getRootUrl(SERVER_PORT)
                    : "http://localhost:" + SERVER_PORT);
    private static final String HOST = SERVER.getHost();
    private static final int PORT = SERVER.getPort();
    private static final boolean HTTPS = "https".equals(SERVER.getScheme());
    private static final String PATH = "";
    private static final String ROOT = SERVER + PATH;

    private final CookieManager cookies = new CookieManager();
    private final HttpClient http = HttpClient.newBuilder()
            .cookieHandler(cookies).sslContext(trustingSslContext())
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Test
    public void stalledClient_otherRequestsOfSessionAreServed()
            throws Exception {
        get("/");
        String init = get("/?v-r=init&location=flood&query=");
        String uiId = find(init, "\"v-uiId\"\\s*:\\s*(\\d+)");
        String pushId = find(init, "\"Vaadin-Push-ID\"\\s*:\\s*\"([^\"]+)\"");

        try (Socket stalled = openStalledSocket()) {
            OutputStream out = stalled.getOutputStream();
            out.write(("GET " + PATH + "/VAADIN/push?v-r=sse&v-uiId=" + uiId
                    + "&v-pushId=" + pushId + " HTTP/1.1\r\nHost: " + HOST + ":"
                    + PORT + "\r\nCookie: " + sessionCookie()
                    + "\r\nAccept: text/event-stream\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.flush();
            String head = readHead(stalled);
            Assertions.assertTrue(head.startsWith("HTTP/1.1 200"), head);
            Assertions.assertTrue(head.contains("text/event-stream"), head);
            // FloodStarter pushes half a megabyte every 50 ms
            Thread.sleep(3000);

            // Opening another UI needs the lock of the session
            long start = System.nanoTime();
            get("/?v-r=init&location=&query=");
            long millis = Duration.ofNanos(System.nanoTime() - start)
                    .toMillis();

            Assertions.assertTrue(millis < 5000,
                    "The session should not wait for the stalled client, waited "
                            + millis + " ms");

            // The client was really stalled with pushed messages, not with
            // an idle stream
            Assertions.assertTrue(readFor(stalled, 2000) > 1024 * 1024,
                    "Too little was pushed to stall the client");
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
     * Opens a connection that reads nothing beyond what is asked, so that the
     * buffers on the way fill up.
     */
    private static Socket openStalledSocket() throws Exception {
        Socket socket = new Socket();
        socket.setReceiveBufferSize(4096);
        socket.connect(new InetSocketAddress(HOST, PORT));
        if (!HTTPS) {
            return socket;
        }
        // HTTP/1.1, as no protocol is offered for the TLS handshake
        SSLSocket tls = (SSLSocket) trustingSslContext().getSocketFactory()
                .createSocket(socket, HOST, PORT, true);
        tls.startHandshake();
        return tls;
    }

    /**
     * Trusts the self-signed certificate of the reverse proxy, like the browser
     * of the tests.
     */
    private static SSLContext trustingSslContext() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain,
                        String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain,
                        String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } }, null);
            return context;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
        Assertions.assertEquals(200, response.statusCode(), path);
        return response.body();
    }

    private String sessionCookie() {
        return cookies.getCookieStore().getCookies().stream()
                .map(HttpCookie::toString).reduce((a, b) -> a + "; " + b)
                .orElseThrow();
    }

    private static String find(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        Assertions.assertTrue(matcher.find(), regex + " not found");
        return matcher.group(1);
    }
}
