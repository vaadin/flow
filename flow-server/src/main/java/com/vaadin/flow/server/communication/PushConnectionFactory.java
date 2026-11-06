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
package com.vaadin.flow.server.communication;

import java.util.ServiceLoader;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Factory that produces instances of {@link PushConnection}.
 *
 * Produces a {@link PushConnection} for the provided {@link UI}.
 *
 * Factory instances are by default discovered and instantiated using
 * {@link ServiceLoader}. This means that all implementations must have a
 * zero-argument constructor and the fully qualified name of the implementation
 * class must be listed on a separate line in a
 * META-INF/services/com.vaadin.flow.server.communication.PushConnectionFactory
 * file present in the jar file containing the implementation class. At most one
 * implementation can be present.
 * <p>
 * An add-on can provide push without Atmosphere with a factory that returns
 * {@code false} from {@link #isAtmosphereRequired()}. Push then works without
 * {@code flow-push} on the classpath, and the bootstrap does not load the
 * Atmosphere script. The client side of such a transport is a JavaScript module
 * in the frontend bundle of the application, which registers itself as
 * {@code window.Vaadin.Flow.pushTransport} with the methods of the Atmosphere
 * JavaScript API the client engine uses: {@code subscribe(request)} and
 * {@code unsubscribeUrl(url)}. Messages to the server are sent as regular
 * requests, as with {@link com.vaadin.flow.shared.ui.Transport#WEBSOCKET_XHR},
 * and the add-on handles the requests of its transport with a
 * {@link com.vaadin.flow.server.RequestHandler} serving {@code VAADIN/push}
 * with a request type of its own.
 *
 * @since 1.0
 */
public interface PushConnectionFactory
        extends SerializableFunction<UI, PushConnection> {

    /**
     * Gets whether the push connections of this factory need Atmosphere, on the
     * server and in the browser.
     *
     * @return {@code true} if push needs Atmosphere, {@code false} if the
     *         factory provides push without it
     */
    default boolean isAtmosphereRequired() {
        return true;
    }
}
