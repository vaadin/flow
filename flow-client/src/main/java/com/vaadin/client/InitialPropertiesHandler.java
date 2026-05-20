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
package com.vaadin.client;

import jsinterop.annotations.JsType;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.TreeChangeProcessor;
import com.vaadin.client.flow.nodefeature.MapProperty;

/**
 * Handles server initial property values with the purpose to prevent change
 * their values from the client side. Pure {@code @JsType(isNative=true)}
 * binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/InitialPropertiesHandler.ts}.
 *
 * <p>
 * Construction takes the {@link StateTree} directly rather than the
 * {@link Registry} so the TS class can drive
 * {@link StateTree#sendNodePropertySyncToServer(MapProperty)} via the already
 * TS-migrated surface without dispatching back through the Java registry.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @see StateTree#sendNodePropertySyncToServer(MapProperty)
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "InitialPropertiesHandler")
public class InitialPropertiesHandler {

    public InitialPropertiesHandler(StateTree tree) {
        // Defined by the TS class constructor.
    }

    /**
     * Flushes collected property update queue (requested to be sent from the
     * client to the server). Supposed to be called in the end of
     * {@link TreeChangeProcessor} changes processing.
     */
    public native void flushPropertyUpdates();

    /**
     * Notifies the handler about registered node.
     *
     * @param node
     *            the registered node
     */
    public native void nodeRegistered(StateNode node);

    /**
     * Handles {@code property} update request before it's sent to the server
     * via RPC. Returns {@code true} when the property is queued for later
     * handling (and therefore must not be forwarded now), {@code false}
     * otherwise.
     */
    public native boolean handlePropertyUpdate(MapProperty property);
}
