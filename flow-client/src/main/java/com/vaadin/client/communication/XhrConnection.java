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

import elemental.json.JsonObject;

/**
 * Provides a connection to the UIDL request handler on the server. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/communication/XhrConnection.ts}.
 *
 * <p>
 * Construction takes an {@link XhrConnectionCallbacks} adapter so the TS class
 * does not need to reach back through the Java {@code Registry} facade.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "XhrConnection")
public class XhrConnection {

    public XhrConnection(XhrConnectionCallbacks callbacks) {
        // Defined by the TS class constructor.
    }

    /** Sends an asynchronous UIDL request to the server. */
    public native void send(JsonObject payload);

    /** Returns the URI to use when sending RPCs to the server. */
    public native String getUri();
}
