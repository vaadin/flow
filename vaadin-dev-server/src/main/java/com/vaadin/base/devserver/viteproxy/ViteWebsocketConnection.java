/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.ViteHandler;

/**
 * Communicates with a Vite server through a websocket connection.
 */
public class ViteWebsocketConnection implements Listener {

    private Consumer<String> onMessage;

    /**
     * Established a connection with a Vite server running on the given port,
     * using the given sub protocol.
     *
     * @param port
     *            the port Vite is running on
     * @param subProtocol
     *            the sub protocol to use
     * @param onMessage
     *            a callback to invoke when a message arrives.
     *
     * @throws InterruptedException
     *             if there is a problem with the connection
     * @throws ExecutionException
     *             if there is a problem with the connection
     */
    public ViteWebsocketConnection(int port, String subProtocol,
            Consumer<String> onMessage)
            throws InterruptedException, ExecutionException {
        this.onMessage = onMessage;
        String wsHost = ViteHandler.DEV_SERVER_HOST.replace("http://", "ws://");
        URI uri = URI.create(wsHost + ":" + port + "/VAADIN/");
        WebSocket clientWebSocket = HttpClient.newHttpClient()
                .newWebSocketBuilder().subprotocols(subProtocol)
                .buildAsync(uri, this).get();
        getLogger().debug("Connecting to " + uri + " using the "
                + clientWebSocket.getSubprotocol() + " protocol");
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        getLogger().debug("Connected using the " + webSocket.getSubprotocol()
                + " protocol");
        Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data,
            boolean last) {
        // Message from Vite
        String msg = data.toString();
        getLogger().debug("Message from Vite: " + msg);
        onMessage.accept(msg);
        return Listener.super.onText(webSocket, data, last);
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }
}
