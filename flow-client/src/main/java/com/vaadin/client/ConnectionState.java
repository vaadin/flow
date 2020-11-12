/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 * GWT interface to window.Vaadin.Flow.connectionState.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ConnectionState {

    /**
     * Shared with ConnectionState.ts: ConnectionState
     */
    public enum State {
        /**
         * Application is connected to server: last transaction over the wire (XHR /
         * heartbeat / endpoint call) was successful.
         */
        CONNECTED("connected"),

        /**
         * Application is connected and Flow is loading application state from the
         * server, or Fusion is waiting for an endpoint call to return.
         */
        LOADING("loading"),

        /**
         * Application has been temporarily disconnected from the server because the
         * last transaction over the write (XHR / heartbeat / endpoint call) resulted
         * in a network error. Flow is attempting to reconnect.
         */
        RECONNECTING("reconnecting"),

        /**
         * Application has been permanently disconnected due to browser going offline,
         * or the server not being reached after a number of reconnect attempts
         * (see ReconnectDialogConfiguration.java: RECONNECT_ATTEMPTS_KEY).
         */
        CONNECTION_LOST("connection-lost");


        private final String value;

        State(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

    }

    /**
     * Set the connection state to be displayed by the loading indicator.
     * @param state
     *      one of "connected", "loading", "reconnecting" or "connection-lost"
     */
    public native void setState(State state)
    /*-{
        $wnd.Vaadin.Flow.connectionState.state = state;
    }-*/;
}
