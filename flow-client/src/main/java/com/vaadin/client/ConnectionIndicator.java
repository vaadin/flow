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

/**
 * GWT-side facade for the connection indicator globals set up by
 * {@code @vaadin/common-frontend}. Always delegates to the TypeScript
 * implementation at
 * {@code src/main/frontend/internal/client/ConnectionIndicator.ts} via
 * {@link NativeConnectionIndicator}. JVM-side use throws — no JUnit test
 * reaches this class transitively.
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
     * Application has been temporarily disconnected from the server.
     */
    public static final String RECONNECTING = "reconnecting";

    /**
     * Application has been permanently disconnected.
     */
    public static final String CONNECTION_LOST = "connection-lost";

    private ConnectionIndicator() {
    }

    /**
     * Set the connection state to be displayed by the loading indicator.
     */
    public static void setState(String state) {
        NativeConnectionIndicator.setState(state);
    }

    /**
     * Get the connection state.
     */
    public static String getState() {
        return NativeConnectionIndicator.getState();
    }

    /**
     * Set a property of the connection indicator component.
     */
    public static void setProperty(String property, Object value) {
        NativeConnectionIndicator.setProperty(property, value);
    }

    /**
     * Notifies the client-side connection state indicator that a loading
     * operation has started.
     */
    public static void loadingStarted() {
        NativeConnectionIndicator.loadingStarted();
    }

    /**
     * Notifies the client-side connection state indicator that a loading
     * operation has completed successfully.
     */
    public static void loadingFinished() {
        NativeConnectionIndicator.loadingFinished();
    }

    /**
     * Notifies the client-side connection state indicator that a loading
     * operation has encountered an error or failed.
     */
    public static void loadingFailed() {
        NativeConnectionIndicator.loadingFailed();
    }
}
