/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.quarkus.graal;

import jakarta.servlet.ServletContext;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.Recorder;
import io.undertow.servlet.api.DeploymentManager;
import org.atmosphere.container.JSR356AsyncSupport;
import org.atmosphere.container.JSR356Endpoint;
import org.atmosphere.cpr.AtmosphereFramework;

import com.vaadin.flow.internal.BootstrapHandlerHelper;
import com.vaadin.flow.server.communication.JSR356WebsocketInitializer;
import com.vaadin.flow.server.communication.JSR356WebsocketInitializer.FakeServletConfig;

/**
 * Sets up Atmosphere for the Vaadin servlets of a native image.
 * <p>
 * STATIC_INIT runs while the native image is built, so Atmosphere, which starts
 * thread pools when it is initialized, is initialized at RUNTIME_INIT instead,
 * before the Vaadin servlets that use it. The push WebSocket endpoints still
 * have to be registered at STATIC_INIT, as the WebSocket container refuses new
 * endpoints once its request handlers are created at RUNTIME_INIT, so each
 * endpoint looks up the Atmosphere instance of its servlet when a WebSocket
 * connects.
 * <p>
 * This class is meant for internal use only.
 */
@Recorder
public class AtmosphereRecorder {

    /**
     * Registers the push WebSocket endpoint of each of the given servlets.
     *
     * @param deploymentManager
     *            the manager of the servlet deployment
     * @param servletNames
     *            the names of the Vaadin servlets
     */
    public void registerPushEndpoints(DeploymentManager deploymentManager,
            List<String> servletNames) {
        ServletContext servletContext = deploymentManager.getDeployment()
                .getServletContext();
        ServerContainer container = (ServerContainer) servletContext
                .getAttribute(ServerContainer.class.getName());
        for (String servletName : servletNames) {
            String path = BootstrapHandlerHelper
                    .getPushEndpointPath(new FakeServletConfig(
                            servletContext.getServletRegistration(servletName),
                            servletContext));
            try {
                container.addEndpoint(ServerEndpointConfig.Builder
                        .create(JSR356Endpoint.class, path)
                        .extensions(new ArrayList<>(
                                container.getInstalledExtensions()))
                        .configurator(new DeferredAtmosphereConfigurator(
                                servletContext, servletName))
                        .build());
            } catch (DeploymentException e) {
                throw new IllegalStateException(
                        "Unable to register the push endpoint " + path
                                + " of the Vaadin servlet " + servletName,
                        e);
            }
        }
    }

    /**
     * Initializes Atmosphere for each of the given servlets.
     *
     * @param deploymentManager
     *            the manager of the servlet deployment
     * @param servletNames
     *            the names of the Vaadin servlets
     */
    public void initAtmosphere(DeploymentManager deploymentManager,
            List<String> servletNames) {
        ServletContext servletContext = deploymentManager.getDeployment()
                .getServletContext();
        for (String servletName : servletNames) {
            JSR356WebsocketInitializer.initAtmosphereForVaadinServlet(
                    servletContext.getServletRegistration(servletName),
                    servletContext);
        }
    }

    /**
     * Hands the WebSocket connections of a push endpoint to the Atmosphere
     * instance of the servlet, which is created after the endpoint is
     * registered.
     */
    static final class DeferredAtmosphereConfigurator
            extends ServerEndpointConfig.Configurator {

        private final ServletContext servletContext;
        private final String servletName;
        private ServerEndpointConfig.Configurator delegate;

        DeferredAtmosphereConfigurator(ServletContext servletContext,
                String servletName) {
            this.servletContext = servletContext;
            this.servletName = servletName;
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig config,
                HandshakeRequest request, HandshakeResponse response) {
            getDelegate().modifyHandshake(config, request, response);
        }

        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass)
                throws InstantiationException {
            return getDelegate().getEndpointInstance(endpointClass);
        }

        // The delegate pairs each handshake with the endpoint created for it,
        // so all connections have to go through the same instance
        private synchronized ServerEndpointConfig.Configurator getDelegate() {
            if (delegate == null) {
                Object framework = servletContext
                        .getAttribute(JSR356WebsocketInitializer
                                .getAttributeName(servletName));
                if (!(framework instanceof AtmosphereFramework atmosphere)) {
                    throw new IllegalStateException(
                            "Atmosphere is not initialized for the Vaadin servlet "
                                    + servletName);
                }
                delegate = new JSR356AsyncSupport.QuarkusAtmosphereConfigurator(
                        atmosphere);
            }
            return delegate;
        }
    }
}
