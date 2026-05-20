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

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

/**
 * GWT-side binding for the connection indicator globals set up by
 * {@code @vaadin/common-frontend}. Pure {@code @JsType(isNative=true)} binding
 * to {@code src/main/frontend/internal/client/ConnectionIndicator.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "ConnectionIndicator")
public final class ConnectionIndicator {

    /**
     * Application is connected to server: last transaction over the wire (XHR /
     * heartbeat / endpoint call) was successful.
     */
    @JsOverlay
    public static final String CONNECTED = "connected";

    /**
     * Application is connected and Flow is loading application state from the
     * server, or Fusion is waiting for an endpoint call to return.
     */
    @JsOverlay
    public static final String LOADING = "loading";

    /**
     * Application has been temporarily disconnected from the server.
     */
    @JsOverlay
    public static final String RECONNECTING = "reconnecting";

    /**
     * Application has been permanently disconnected.
     */
    @JsOverlay
    public static final String CONNECTION_LOST = "connection-lost";

    private ConnectionIndicator() {
    }

    /**
     * Sets the connection state.
     */
    public static native void setState(String state);

    /**
     * Gets the connection state.
     */
    public static native String getState();

    /**
     * Sets a property of the connection indicator component.
     */
    public static native void setProperty(String property, Object value);

    /**
     * Notifies the connection state indicator that a loading operation has
     * started.
     */
    public static native void loadingStarted();

    /**
     * Notifies the connection state indicator that a loading operation has
     * completed successfully.
     */
    public static native void loadingFinished();

    /**
     * Notifies the connection state indicator that a loading operation has
     * encountered an error or failed.
     */
    public static native void loadingFailed();
}
