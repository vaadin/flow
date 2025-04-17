/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.ThrowingConsumer;

import com.vaadin.flow.internal.ReflectTools;

public class ViteWebsocketConnectionTest {

    private HttpServer httpServer;

    private ThrowingConsumer<HttpExchange> handlerSupplier;

    @Before
    public void reservePort() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0),
                10);
        httpServer.createContext("/VAADIN",
                exchange -> handlerSupplier.accept(exchange));
        httpServer.start();
    }

    public void tearDown() throws Exception {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test(timeout = 7000)
    public void waitForConnection_clientWebsocketAvailable_blocksUntilConnectionIsEstablished()
            throws ExecutionException, InterruptedException {
        CountDownLatch connectionLatch = new CountDownLatch(1);
        CountDownLatch closeLatch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        handlerSupplier = (exchange) -> {
            // Simulate connection delay
            Thread.sleep(500);
            handshake(exchange);
        };
        long startTime = System.nanoTime();
        ViteWebsocketConnection viteConnection = new ViteWebsocketConnection(
                httpServer.getAddress().getPort(), "/VAADIN", "proto", x -> {
                }, closeLatch::countDown, err -> {
                    error.set(err);
                    connectionLatch.countDown();
                }) {
            @Override
            public void onOpen(WebSocket webSocket) {
                super.onOpen(webSocket);
                connectionLatch.countDown();
            }
        };
        boolean established = connectionLatch.await(5, TimeUnit.SECONDS);
        long elapsedTime = Duration.ofNanos(System.nanoTime() - startTime)
                .toMillis();
        if (error.get() != null) {
            throw new AssertionError(
                    "Websocket connection failed: " + error.get().getMessage(),
                    error.get());
        }
        Assert.assertTrue("Connection NOT established. Elapsed time "
                + elapsedTime + " ms", established);
        Assert.assertTrue(
                "Should have waited for connection to be established (elapsed time: "
                        + elapsedTime + ")",
                elapsedTime > 500);
        Assert.assertTrue(
                "Should not have been blocked too long after connection (elapsed time: "
                        + elapsedTime + ")",
                elapsedTime < 2500);
        if (!closeLatch.await(500, TimeUnit.MILLISECONDS)) {
            viteConnection.close();
            closeLatch.await(500, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void waitForConnection_clientWebsocketNotAvailable_fails()
            throws InterruptedException {
        // Immediately closing connection to simulate connection failure
        handlerSupplier = HttpExchange::close;
        CountDownLatch errorLatch = new CountDownLatch(1);
        new ViteWebsocketConnection(httpServer.getAddress().getPort(),
                "/VAADIN", "proto", x -> {
                }, () -> {
                }, err -> errorLatch.countDown());
        if (!errorLatch.await(5, TimeUnit.SECONDS)) {
            Assert.fail(
                    "Expecting connection failure, but not happened in 5 seconds");
        }
    }

    @Test(timeout = 5000)
    public void close_clientWebsocketNotAvailable_dontBlock()
            throws ExecutionException, InterruptedException {
        AtomicReference<Throwable> connectionError = new AtomicReference<>();
        CountDownLatch suspendConnectionLatch = new CountDownLatch(1);
        handlerSupplier = exchange -> {
            suspendConnectionLatch.await();
        };
        ViteWebsocketConnection connection = new ViteWebsocketConnection(
                httpServer.getAddress().getPort(), "/VAADIN", "proto", x -> {
                }, () -> {
                }, connectionError::set);
        connection.close();
        suspendConnectionLatch.countDown();
        Assert.assertNull("Websocket connection failed", connectionError.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void close_clientWebsocketClose_dontBlockIndefinitely()
            throws ExecutionException, InterruptedException,
            NoSuchFieldException, InvocationTargetException,
            IllegalAccessException {
        handlerSupplier = ViteWebsocketConnectionTest::handshake;
        AtomicReference<Throwable> connectionError = new AtomicReference<>();
        ViteWebsocketConnection connection = new ViteWebsocketConnection(
                httpServer.getAddress().getPort(), "/VAADIN", "proto", x -> {
                }, () -> {
                }, connectionError::set);

        // Replace websocket with spy to mock close behavior
        Field clientWebsocketField = ViteWebsocketConnection.class
                .getDeclaredField("clientWebsocket");
        CompletableFuture<WebSocket> clientWebsocketFuture = (CompletableFuture<WebSocket>) ReflectTools
                .getJavaFieldValue(connection, clientWebsocketField);
        WebSocket mockWebSocket = Mockito.spy(clientWebsocketFuture.get());
        Mockito.when(mockWebSocket.sendClose(ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyString())).then(i -> {
                    CompletableFuture<?> closeFuture = (CompletableFuture<?>) i
                            .callRealMethod();
                    return closeFuture.thenRunAsync(() -> {
                        try {
                            // Wait longer than test timeout.
                            // Close should not wait that much
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });
        ReflectTools.setJavaFieldValue(connection, clientWebsocketField,
                CompletableFuture.completedFuture(mockWebSocket));
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
            connection.close();
            return ReflectTools.getJavaFieldValue(connection,
                    clientWebsocketField) == null;
        });
        Assert.assertNull("Websocket connection failed", connectionError.get());
    }

    private static void handshake(HttpExchange exchange) throws IOException {
        Headers requestHeaders = exchange.getRequestHeaders();
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && "upgrade"
                .equalsIgnoreCase(requestHeaders.getFirst("Connection"))) {
            String wsKey = requestHeaders.getFirst("Sec-websocket-key");
            String wsAcceptKey;
            try {
                wsAcceptKey = Base64.getEncoder().encodeToString(
                        MessageDigest.getInstance("SHA-1").digest(
                                (wsKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                        .getBytes(StandardCharsets.UTF_8)));
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
