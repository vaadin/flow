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

import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connects a brower-server websocket connection with a server-Vite websocket
 * connection.
 * <p>
 * Forwards all messages from one to the other.
 */
public class ViteWebsocketProxy implements MessageHandler.Whole<String> {

    private final ViteWebsocketConnection viteConnection;

    /**
     * Creates a new proxy for the given browser-server websocket connection.
     * <p>
     * Opens a connection to the Vite server running on the given port and
     * starts forwarding messages.
     *
     * @param browserSession
     *            the websocket connection from the browser
     * @param vitePort
     *            the port the Vite server is running on
     * @param vitePath
     *            the path Vite is using
     */
    public ViteWebsocketProxy(Session browserSession, Integer vitePort,
            String vitePath) {
        viteConnection = new ViteWebsocketConnection(vitePort, vitePath,
                browserSession.getNegotiatedSubprotocol(), msg -> {
                    try {
                        browserSession.getBasicRemote().sendText(msg);
                        getLogger().debug("Message sent to browser: {}", msg);
                    } catch (IOException e) {
                        getLogger().debug("Error sending message to browser",
                                e);
                    }
                }, () -> {
                    try {
                        browserSession.close();
                    } catch (IOException e) {
                        getLogger().debug("Error closing browser connection",
                                e);
                    }
                }, err -> {
                    getLogger().error("Error creating Vite proxy connection",
                            err);
                    try {
                        browserSession.close();
                    } catch (IOException e1) {
                        getLogger().debug("Error closing browser connection",
                                e1);
                    }
                });
    }

    protected static Logger getLogger() {
        return LoggerFactory.getLogger(ViteWebsocketProxy.class);
    }

    @Override
    public void onMessage(String message) {
        getLogger().debug("Got message from browser: {}", message);
        try {
            viteConnection.send(message);
            getLogger().debug("Sent message to Vite: {}", message);
        } catch (InterruptedException | ExecutionException e) {
            getLogger().debug("Error sending message ({}) to Vite", message, e);
        }

    }

    /**
     * Terminates the connection.
     */
    public void close() {
        try {
            viteConnection.close();
        } catch (InterruptedException | ExecutionException e) {
            getLogger().debug("Error closing connection to Vite", e);
        }
    }

}
