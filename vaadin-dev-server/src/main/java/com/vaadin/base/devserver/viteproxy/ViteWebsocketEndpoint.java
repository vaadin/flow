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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.ViteHandler;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;

import jakarta.servlet.ServletContext;
import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * The websocket endpoint for Vite.
 */
public class ViteWebsocketEndpoint extends Endpoint {

    public static final String VITE_HANDLER = "viteServer";

    private final static Map<String, ViteWebsocketProxy> proxies = Collections
            .synchronizedMap(new HashMap<>());

    /**
     * Creates the websocket endpoint that Vite connects to.
     *
     * @param context
     *            the web context
     * @param viteHandler
     *            the Vite handler instance to connect to
     */
    public static void init(VaadinContext context, ViteHandler viteHandler) {
        ServletContext servletContext = ((VaadinServletContext) context)
                .getContext();
        ServerContainer container = (ServerContainer) servletContext
                .getAttribute(ServerContainer.class.getName());
        if (container == null) {
            getLogger().error(
                    "Unable to deploy Vite websocket endpoint, no container value is available");
            return;
        }
        try {
            List<String> subProtocols = List.of("vite-hmr", "vite-ping");
            ServerEndpointConfig endpointConfig = ServerEndpointConfig.Builder
                    .create(ViteWebsocketEndpoint.class,
                            viteHandler.getPathToVaadinInContext())
                    .subprotocols(subProtocols).build();
            endpointConfig.getUserProperties()
                    .put(ViteWebsocketEndpoint.VITE_HANDLER, viteHandler);
            container.addEndpoint(endpointConfig);
        } catch (DeploymentException e) {
            getLogger().error("Error deploying Vite websocket proxy endpoint",
                    e);
        }

    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        getLogger().debug("Browser ({}) connected to Vite proxy",
                session.getId());
        // Vite pings every 30s but while you debug in the browser, the pings
        // will be prevented.
        // When you resume after debugging, the page will reload if the timeout
        // was hit, so it is 0 == disabled
        session.setMaxIdleTimeout(0);

        ViteHandler viteHandler = (ViteHandler) config.getUserProperties()
                .get(VITE_HANDLER);
        ViteWebsocketProxy proxy;
        try {
            proxy = new ViteWebsocketProxy(session, viteHandler.getPort(),
                    viteHandler.getPathToVaadin());
            proxies.put(session.getId(), proxy);
            session.addMessageHandler(proxy);
        } catch (Exception e) {
            getLogger().error("Error creating Vite proxy connection", e);
            try {
                session.close();
            } catch (IOException e1) {
                getLogger().debug("Error closing connection", e1);
            }
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        getLogger().debug("Browser ({}) closed the connection",
                session.getId());
        ViteWebsocketProxy proxy = proxies.remove(session.getId());
        if (proxy != null) {
            proxy.close();
        }
        super.onClose(session, closeReason);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ViteWebsocketEndpoint.class);
    }

}
