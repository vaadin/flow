/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.base.devserver.viteproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ThrowingConsumer;

public class ViteWebsocketConnectionTest {

    private HttpServer httpServer;

    private ThrowingConsumer<HttpExchange> handlerSupplier;

    @Before
    public void reservePort() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 10);
        httpServer.createContext("/VAADIN", exchange -> handlerSupplier.accept(exchange));
        httpServer.start();
    }

    public void tearDown() throws Exception {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test(timeout = 2000)
    public void waitForConnection_clientWebsocketAvailable_blocksUntilConnectionIsEstablished()
            throws ExecutionException, InterruptedException {
        CountDownLatch closeLatch = new CountDownLatch(1);
        handlerSupplier = (exchange) -> {
            // Simulate connection delay
            Thread.sleep(500);
            handshake(exchange);
        };
        long startTime = System.nanoTime();
        new ViteWebsocketConnection(httpServer.getAddress().getPort(), "/VAADIN", "proto", x -> {
        }, () -> {
            closeLatch.countDown();
        }, err -> {
        });
        closeLatch.await(2, TimeUnit.SECONDS);
        long elapsedTime = Duration.ofNanos(System.nanoTime() - startTime).toMillis();
        Assert.assertTrue("Should have waited for connection to be established (elapsed time: " + elapsedTime + ")",
                elapsedTime > 500);
        Assert.assertTrue("Should not have been blocked too long after connection (elapsed time: " + elapsedTime + ")",
                elapsedTime < 1000);

    }

    @Test
    public void waitForConnection_clientWebsocketNotAvailable_fails() throws InterruptedException {
        // Immediately closing connection to simulate connection failure
        handlerSupplier = HttpExchange::close;
        CountDownLatch errorLatch = new CountDownLatch(1);
        new ViteWebsocketConnection(httpServer.getAddress().getPort(), "/VAADIN", "proto", x -> {
        }, () -> {
        }, err -> errorLatch.countDown());
        errorLatch.await(2, TimeUnit.SECONDS);
    }

    private static void handshake(HttpExchange exchange) throws IOException {
        Headers requestHeaders = exchange.getRequestHeaders();
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())
                && "upgrade".equalsIgnoreCase(requestHeaders.getFirst("Connection"))) {
            String wsKey = requestHeaders.getFirst("Sec-websocket-key");
            String wsAcceptKey;
            try {
                wsAcceptKey = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
                        .digest((wsKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add("Connection", "Upgrade");
            responseHeaders.add("Upgrade", "websocket");
            responseHeaders.add("Sec-WebSocket-Accept", wsAcceptKey);
            exchange.sendResponseHeaders(101, -1);
        }
    }
}
