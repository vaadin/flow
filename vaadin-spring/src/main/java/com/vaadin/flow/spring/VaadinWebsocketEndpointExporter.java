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

        if (getServerContainer() == null) {
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

    @Override
    public void afterPropertiesSet() {
        // avoid call super method which may throw IllegalStateException
    }

}
