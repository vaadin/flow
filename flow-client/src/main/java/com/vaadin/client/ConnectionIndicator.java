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

import com.google.gwt.core.client.GWT;

/**
 * GWT interface to the connection indicator globals set up by
 * {@code @vaadin/common-frontend}. Under GWT, the calls are forwarded to the
 * TypeScript implementation at
 * {@code src/main/frontend/internal/client/ConnectionIndicator.ts}; on the JVM
 * (unit tests) the methods are no-ops and {@link #getState()} returns
 * {@code null}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ConnectionIndicator {

    /**
     * Application is connected to server: last transaction over the wire (XHR /
     * heartbeat / endpoint call) was successful.
     */
    public static final String CONNECTED = "connected";

    /**
     * Application is connected and Flow is loading application state from the
     * server, or Fusion is waiting for an endpoint call to return.
     */
    public static final String LOADING = "loading";

    /**
     * Application has been temporarily disconnected from the server because the
     * last transaction over the wire (XHR / heartbeat / endpoint call) resulted
     * in a network error, or the browser has received the 'online' event and
     * needs to verify reconnection with the server. Flow is attempting to
     * reconnect a configurable number of times before giving up.
     */
    public static final String RECONNECTING = "reconnecting";

    /**
     * Application has been permanently disconnected due to browser receiving
     * the 'offline' event, or the server not being reached after a number of
     * reconnect attempts.
     */
    public static final String CONNECTION_LOST = "connection-lost";

    private ConnectionIndicator() {
        // No instance should ever be created
    }

    /**
     * Set the connection state to be displayed by the loading indicator.
     */
    public static void setState(String state) {
        if (GWT.isScript()) {
            NativeConnectionIndicator.setState(state);
        }
    }

    /**
     * Get the connection state.
     */
    public static String getState() {
        if (GWT.isScript()) {
            return NativeConnectionIndicator.getState();
        }
        return null;
    }

    /**
     * Set a property of the connection indicator component.
     */
    public static void setProperty(String property, Object value) {
        if (GWT.isScript()) {
            NativeConnectionIndicator.setProperty(property, value);
        }
    }

    /**
     * Notifies the client-side connection state indicator that a loading
     * operation has started.
     */
    public static void loadingStarted() {
        if (GWT.isScript()) {
            NativeConnectionIndicator.loadingStarted();
        }
    }

    /**
     * Notifies the client-side connection state indicator that a loading
     * operation has completed successfully.
     */
    public static void loadingFinished() {
        if (GWT.isScript()) {
            NativeConnectionIndicator.loadingFinished();
        }
    }

    /**
     * Notifies the client-side connection state indicator that a loading
     * operation has encountered an error or failed.
     */
    public static void loadingFailed() {
        if (GWT.isScript()) {
            NativeConnectionIndicator.loadingFailed();
        }
    }
}
