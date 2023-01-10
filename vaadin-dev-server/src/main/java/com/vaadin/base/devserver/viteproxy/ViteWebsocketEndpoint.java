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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

/**
 * The websocket endpoint for Vite, initialized by the servlet container through
 * {@link ViteWebsocketEndpointInitializer}.
 */
@WebListener
public class ViteWebsocketEndpoint extends Endpoint {

    static final String VITE_PORT = "vitePort";

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        getLogger().debug("Browser connected to Vite proxy");
        ViteWebsocketProxy proxy;
        try {
            Integer vitePort = (Integer) config.getUserProperties()
                    .get(VITE_PORT);
            proxy = new ViteWebsocketProxy(session, vitePort);
            session.addMessageHandler(proxy);
        } catch (Exception e) {
            getLogger().debug("Error creating Vite proxy connection", e);
            try {
                session.close();
            } catch (IOException e1) {
                getLogger().debug("Error closing connection", e1);
            }
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

}
