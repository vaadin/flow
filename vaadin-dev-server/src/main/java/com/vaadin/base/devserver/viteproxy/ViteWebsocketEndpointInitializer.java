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

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Creates the websocket endpoint that the Vite client JS connects to.
 */
public class ViteWebsocketEndpointInitializer
        implements ServletContextListener {

    /**
     * Creates the websocket endpoint that Vite connects to.
     *
     * @param context
     *            the web context
     * @param path
     *            the path Vite will connect to
     * @param port
     *            the port Vite dev server is running on
     */
    public ViteWebsocketEndpointInitializer(VaadinContext context, String path,
            int port) {
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
            List<String> subProtocols = Collections.singletonList("vite-hmr");
            ServerEndpointConfig endpointConfig = ServerEndpointConfig.Builder
                    .create(ViteWebsocketEndpoint.class, path)
                    .subprotocols(subProtocols).build();
            endpointConfig.getUserProperties()
                    .put(ViteWebsocketEndpoint.VITE_PORT, port);
            container.addEndpoint(endpointConfig);
        } catch (DeploymentException e) {
            getLogger().error("Error deploying Vite websocket proxy endpoint",
                    e);
        }

    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }
}
