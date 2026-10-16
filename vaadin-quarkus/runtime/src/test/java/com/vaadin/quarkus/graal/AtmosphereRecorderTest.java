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

import jakarta.servlet.ServletRegistration;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.List;
import java.util.Map;

import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.spec.ServletContextImpl;
import org.atmosphere.container.JSR356Endpoint;
import org.atmosphere.cpr.AtmosphereFramework;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.vaadin.flow.server.communication.JSR356WebsocketInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtmosphereRecorderTest {

    @Test
    void registerPushEndpoints_endpointAtPushPathOfServlet_connectsToAtmosphereInitializedLater()
            throws Exception {
        ServletRegistration registration = mock(ServletRegistration.class);
        when(registration.getName()).thenReturn("app");
        when(registration.getMappings()).thenReturn(List.of("/app/*"));
        ServerContainer container = mock(ServerContainer.class);
        ServletContextImpl context = mock(ServletContextImpl.class);
        when(context.getServletRegistration("app")).thenReturn(registration);
        when(context.getServletRegistrations())
                .thenAnswer(invocation -> Map.of("app", registration));
        when(context.getAttribute(ServerContainer.class.getName()))
                .thenReturn(container);
        DeploymentManager deploymentManager = mock(DeploymentManager.class,
                RETURNS_DEEP_STUBS);
        when(deploymentManager.getDeployment().getServletContext())
                .thenReturn(context);

        new AtmosphereRecorder().registerPushEndpoints(deploymentManager,
                List.of("app"));

        ArgumentCaptor<ServerEndpointConfig> endpoint = ArgumentCaptor
                .forClass(ServerEndpointConfig.class);
        verify(container).addEndpoint(endpoint.capture());
        assertEquals("/app/VAADIN/push", endpoint.getValue().getPath());
        assertEquals(JSR356Endpoint.class,
                endpoint.getValue().getEndpointClass());
        // Atmosphere of the servlet is initialized after the endpoint is
        // registered, and looked up when a WebSocket connects
        ServerEndpointConfig.Configurator configurator = endpoint.getValue()
                .getConfigurator();
        assertThrows(IllegalStateException.class,
                () -> configurator.getEndpointInstance(JSR356Endpoint.class));

        AtmosphereFramework atmosphere = new AtmosphereFramework(false, false);
        when(context.getAttribute(
                JSR356WebsocketInitializer.getAttributeName("app")))
                .thenReturn(atmosphere);
        try {
            // The handshake and the endpoint created for it are paired by the
            // same Atmosphere configurator
            configurator.modifyHandshake(endpoint.getValue(),
                    mock(HandshakeRequest.class),
                    mock(HandshakeResponse.class));
            assertInstanceOf(JSR356Endpoint.class,
                    configurator.getEndpointInstance(JSR356Endpoint.class));
        } finally {
            atmosphere.destroy();
        }
    }
}
