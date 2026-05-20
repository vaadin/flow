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

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

import com.google.gwt.core.client.JavaScriptObject;

import com.vaadin.client.communication.AtmospherePushConnection.AtmosphereConfiguration;

/**
 * JsInterop binding for the TypeScript {@code AtmospherePushConnection} module
 * published at
 * {@code window.Vaadin.Flow.internal.client.communication.AtmospherePushConnection}.
 * Source lives in
 * {@code src/main/frontend/internal/client/communication/AtmospherePushConnection.ts}.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "AtmospherePushConnection")
final class NativeAtmospherePushConnection {

    private NativeAtmospherePushConnection() {
        // Native, not instantiated from Java
    }

    static native AtmosphereConfiguration createConfig(int messageDelimiter);

    static native JavaScriptObject doConnect(String uri,
            AtmosphereConfiguration config,
            AtmosphereConnectCallbacks callbacks);

    static native void doPush(JavaScriptObject socket, String message);

    static native void doDisconnect(String url);

    static native boolean isAtmosphereLoaded();
}

/** Callback for Atmosphere events that deliver a single response object. */
@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsAtmosphereResponseConsumer {
    void accept(Object response);
}

/** Callback for Atmosphere events that deliver a (request, response) pair. */
@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsAtmosphereReconnectConsumer {
    void accept(Object request, Object response);
}

/** Supplier returning a JS-visible value (used for the sync-id header). */
@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsAtmosphereObjectSupplier {
    Object get();
}

/**
 * Bundle of the nine Java callbacks the TS bridge wires into the Atmosphere
 * config.
 */
@JsType
final class AtmosphereConnectCallbacks {
    public JsAtmosphereResponseConsumer onOpen;
    public JsAtmosphereResponseConsumer onReopen;
    public JsAtmosphereResponseConsumer onMessage;
    public JsAtmosphereResponseConsumer onError;
    public JsAtmosphereResponseConsumer onTransportFailure;
    public JsAtmosphereResponseConsumer onClose;
    public JsAtmosphereReconnectConsumer onReconnect;
    public JsAtmosphereResponseConsumer onClientTimeout;
    public JsAtmosphereObjectSupplier getLastSeenServerSyncId;
}
