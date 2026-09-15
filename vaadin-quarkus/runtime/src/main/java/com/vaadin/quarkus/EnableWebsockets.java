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
package com.vaadin.quarkus;

import java.util.Collections;
import java.util.Set;

import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Only purpose of this class is to automatically enable quarkus WebSocket
 * deployment, in order to make Atmosphere JSR365Endpoint work.
 * 
 * Quarkus enables WebSocket deployment only if it finds annotated endpoints
 * (@{@link jakarta.websocket.server.ServerEndpoint}) or implementors of
 * {@link ServerApplicationConfig} interface.
 * 
 * Unfortunately, if at least one implementation of
 * {@link ServerApplicationConfig} is found, annotated endpoints are not
 * deployed automatically.
 *
 * To circumvent this problem, implementation of
 * {@link #getAnnotatedEndpointClasses(Set)} method will return all the provided
 * scanned annotated endpoints. Although Javadocs says that the passed set of
 * scanned classes contains all the annotated endpoint classes in the JAR or WAR
 * file containing the implementation of this interface, Quarkus will instead
 * provide all available annotated endpoints found at build time.
 *
 */
public class EnableWebsockets implements ServerApplicationConfig {

    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(
            Set<Class<? extends Endpoint>> endpointClasses) {
        return Collections.emptySet();
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        return scanned;
    }
}
