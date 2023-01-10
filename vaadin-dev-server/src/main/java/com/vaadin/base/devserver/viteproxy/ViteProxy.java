package com.vaadin.base.devserver.viteproxy;

import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

public class ViteProxy implements MessageHandler.Whole<String> {

    private CompletableFuture<WebSocket> clientWebSocket;
    private Session session;

    public ViteProxy(Session browserSession) throws Exception {
        this.session = browserSession;
        new ViteWebsocketConnection(ViteWebsocketEndpoint.vitePort,
                browserSession.getNegotiatedSubprotocol(), msg -> {
                    try {
                        session.getBasicRemote().sendText(msg);
                        getLogger().debug("Message sent to browser: " + msg);
                    } catch (Exception e) {
                        getLogger().debug("Error sending message to browser",
                                e);
                    }
                });
    }

    protected Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    @Override
    public void onMessage(String message) {
        getLogger().debug("Got message from browser: " + message);
        try {
            clientWebSocket.get().sendText(message, false);
            getLogger().debug("Sent message to Vite: " + message);
        } catch (InterruptedException | ExecutionException e) {
            getLogger().debug("Error sending message (" + message + ") to Vite",
                    e);
        }

    }

}
