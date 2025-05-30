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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import jakarta.websocket.CloseReason.CloseCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.ViteHandler;

/**
 * Communicates with a Vite server through a websocket connection.
 */
public class ViteWebsocketConnection implements Listener {

    private final Consumer<String> onMessage;
    private final Runnable onClose;
    private final List<CharSequence> parts = new ArrayList<>();
    private CompletableFuture<WebSocket> clientWebsocket = new CompletableFuture<>();

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ViteWebsocketConnection.class);
    }

    /**
     * Established a connection with a Vite server running on the given port,
     * using the given sub protocol.
     *
     * @param port
     *            the port Vite is running on
     * @param path
     *            the path Vite is using
     * @param subProtocol
     *            the sub protocol to use
     * @param onMessage
     *            a callback to invoke when a message arrives.
     * @param onClose
     *            a callback to invoke if the connection to Vite is closed
     *
     */
    public ViteWebsocketConnection(int port, String path, String subProtocol,
            Consumer<String> onMessage, Runnable onClose,
            Consumer<Throwable> onConnectionFailure) {
        this.onMessage = onMessage;
        this.onClose = onClose;
        String wsHost = ViteHandler.DEV_SERVER_HOST.replace("http://", "ws://");
        URI uri = URI.create(wsHost + ":" + port + path);
        HttpClient.newHttpClient().newWebSocketBuilder()
                .subprotocols(subProtocol).buildAsync(uri, this)
                .whenComplete(((webSocket, failure) -> {
                    if (failure == null) {
                        getLogger().debug(
                                "Connection to {} using the {} protocol established",
                                uri, webSocket.getSubprotocol());
                        if (clientWebsocket.complete(webSocket)) {
                            getLogger().trace(
                                    "Websocket future completed in client build completion");
                        }
                    } else {
                        getLogger().debug("Failed to connect to {}", uri);
                        clientWebsocket.completeExceptionally(failure);
                        onConnectionFailure.accept(failure);
                    }
                }));
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        getLogger().debug("Connected using the {} protocol",
                webSocket.getSubprotocol());
        if (clientWebsocket.complete(webSocket)) {
            getLogger().trace("Websocket future completed in onOpen");
        }
        Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode,
            String reason) {
        onClose.run();
        return Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data,
            boolean last) {
        // Message from Vite
        if (!last) {
            getLogger().debug("Partial message from Vite: {}", data);
            parts.add(data);
        } else {
            String msg = "";
            while (!parts.isEmpty()) {
                msg += parts.remove(0);
            }
            msg += data;
            getLogger().debug("Message from Vite: {}", msg);
            onMessage.accept(msg);
        }
        return Listener.super.onText(webSocket, data, last);
    }

    /**
     * Sends the given message to the Vite server.
     *
     * @param message
     *            the message to send
     * @throws InterruptedException
     *             if there is a problem with the connection
     * @throws ExecutionException
     *             if there is a problem with the connection
     */
    public void send(String message)
            throws InterruptedException, ExecutionException {
        CompletableFuture<WebSocket> send = clientWebsocket.get()
                .sendText(message, true);
        send.get();
    }

    /**
     * Closes the connection.
     *
     * @throws ExecutionException
     *             if there is a problem with the connection
     * @throws InterruptedException
     *             if there is a problem with the connection
     */
    public void close() throws InterruptedException, ExecutionException {
        if (clientWebsocket == null) {
            getLogger().debug("Connection already closed");
            return;
        }
        getLogger().debug("Closing the connection");
        if (clientWebsocket.isDone()) {
            WebSocket client = clientWebsocket.get();
            if (!client.isOutputClosed()) {
                CompletableFuture<WebSocket> closeRequest = client
                        .sendClose(CloseCodes.NORMAL_CLOSURE.getCode(), "");
                try {
                    closeRequest.get(500, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    getLogger().debug("Timed out waiting for close request");
                }
            }
        } else {
            // Websocket client connection has not been established
            clientWebsocket.cancel(true);
        }
        // release HttpClient reference, so its internal executor can be
        // properly stopped
        clientWebsocket = null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        getLogger().debug("Error from Vite websocket connection", error);
        Listener.super.onError(webSocket, error);
    }
}
