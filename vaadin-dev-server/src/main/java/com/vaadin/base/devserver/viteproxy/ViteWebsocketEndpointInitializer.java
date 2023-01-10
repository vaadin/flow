package com.vaadin.base.devserver.viteproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

public class ViteWebsocketEndpointInitializer
        implements ServletContextListener {

    public ViteWebsocketEndpointInitializer(VaadinContext context, int port) {
        ViteWebsocketEndpoint.vitePort = port;
        ServletContext servletContext = ((VaadinServletContext) context)
                .getContext();
        ServerContainer container = (ServerContainer) servletContext
                .getAttribute(ServerContainer.class.getName());
        try {
            ServerEndpointConfig endpointConfig = ServerEndpointConfig.Builder
                    .create(ViteWebsocketEndpoint.class, "/VAADIN/").build();
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
