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

import jsinterop.annotations.JsType;

import com.vaadin.client.Command;

import elemental.json.JsonObject;

/**
 * The default {@link PushConnection} implementation that uses Atmosphere for
 * handling the communication channel. Pure {@code @JsType(isNative=true)}
 * binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/communication/AtmospherePushConnection.ts}.
 *
 * <p>
 * Construction takes an {@link AtmospherePushConnectionCallbacks} adapter so
 * the TS class does not need to reach back through the Java {@code Registry}
 * facade.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "AtmospherePushConnection")
public class AtmospherePushConnection implements PushConnection {

    public AtmospherePushConnection(
            AtmospherePushConnectionCallbacks callbacks) {
        // Defined by the TS class constructor.
    }

    @Override
    public native void push(JsonObject payload);

    @Override
    public native boolean isActive();

    @Override
    public native void disconnect(Command command);

    @Override
    public native String getTransportType();

    @Override
    public native boolean isBidirectional();

    /** Checks whether the Atmosphere push JS library is loaded. */
    public static native boolean isAtmosphereLoaded();

    /**
     * Gets the Atmosphere push JS library version. Returns {@code null} if the
     * library has not been loaded.
     */
    public static native String getAtmosphereJSVersion();
}
