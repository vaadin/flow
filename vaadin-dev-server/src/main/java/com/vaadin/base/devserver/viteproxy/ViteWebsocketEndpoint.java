package com.vaadin.base.devserver.viteproxy;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

@WebListener
public class ViteWebsocketEndpoint extends Endpoint {

    static int vitePort = -1;

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        getLogger().debug("Browser connected to Vite proxy");
        ViteProxy proxy;
        try {
            proxy = new ViteProxy(session);
            session.addMessageHandler(proxy);
        } catch (Exception e) {
            getLogger().debug("Error creating Vite proxy connection", e);
            try {
                session.close();
            } catch (IOException e1) {
            }
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    public static int getVitePort() {
        return vitePort;
    }
}
