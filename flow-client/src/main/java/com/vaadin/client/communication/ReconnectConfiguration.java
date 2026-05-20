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
package com.vaadin.client.communication;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.reactive.Reactive;

/**
 * Tracks the reconnect configuration stored in the root node and provides it
 * with an easier to use API. Pure {@code @JsType(isNative=true)} binding to the
 * TypeScript implementation at
 * {@code src/main/frontend/internal/client/communication/ReconnectConfiguration.ts}.
 *
 * <p>
 * The {@link #bind(ConnectionStateHandler)} static helper stays on the Java
 * side as an {@code @JsOverlay} because it wires
 * {@link Reactive#runWhenDependenciesChange(com.vaadin.client.Command)}, which
 * builds a Java {@code Computation} subclass not expressible from TS.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "ReconnectConfiguration")
public class ReconnectConfiguration {

    public ReconnectConfiguration(StateTree tree) {
        // Defined by the TS class constructor.
    }

    /**
     * Binds this ReconnectDialogConfiguration to the given
     * {@link ConnectionStateHandler} so that
     * {@link ConnectionStateHandler#configurationUpdated()} is run whenever a
     * relevant part of {@link ReconnectConfiguration} changes.
     *
     * @param connectionStateHandler
     *            the connection state handler to bind to
     */
    @JsOverlay
    public static void bind(ConnectionStateHandler connectionStateHandler) {
        Reactive.runWhenDependenciesChange(
                () -> connectionStateHandler.configurationUpdated());
    }

    /**
     * Gets the text to show in the reconnect dialog.
     *
     * @deprecated The API for configuring the connection indicator has changed.
     */
    @Deprecated
    public native String getDialogText();

    /**
     * Gets the text to show in the reconnect dialog when no longer trying to
     * reconnect.
     *
     * @deprecated The API for configuring the connection indicator has changed.
     */
    @Deprecated
    public native String getDialogTextGaveUp();

    /**
     * Gets the number of reconnect attempts that should be performed before
     * giving up.
     */
    public native int getReconnectAttempts();

    /**
     * Gets the interval in milliseconds to wait between reconnect attempts.
     */
    public native int getReconnectInterval();
}
