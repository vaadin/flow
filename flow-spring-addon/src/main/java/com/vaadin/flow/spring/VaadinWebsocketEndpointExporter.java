package com.vaadin.flow.spring;

import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.vaadin.flow.server.communication.JSR356WebsocketInitializer;

/**
 * Handles registration of JSR-356 websocket endpoints when the Spring Boot
 * application is run in an embedded container. Also triggered when running in a
 * real server but is not necessary in this scenario as
 * JSR356WebsocketInitializer is triggered by the servlet container.
 */
public class VaadinWebsocketEndpointExporter extends ServerEndpointExporter {

    @Override
    protected void registerEndpoints() {
        super.registerEndpoints();
        if (!JSR356WebsocketInitializer.isAtmosphereAvailable()) {
            return;
        }

        if (getServerContainer() != null) {
            // ServerContainer (i.e. the websocket server provided by Jetty,
            // Tomcat etc) can be null at this point when running in a real
            // server (as opposed to embedded). At least Jetty uses a
            // ServletContainerInitializer to initialize its websocket support
            // and that one might or might not have been run before this code.
            // Need to bail out and let JSR356WebsocketInitializer handle it
            // through its listener when the websocket support is definitely
            // available.
            //
            // This feels like a Spring Boot bug.
            return;
        }

        new JSR356WebsocketInitializer().init(getServletContext());
    }

}
