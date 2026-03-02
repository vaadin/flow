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
package com.vaadin.base.devserver.viteproxy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.vaadin.flow.shared.Registration;

/**
 * Tracks HTTP session closures and notifies registered listeners.
 * <p>
 * This allows WebSocket connections to be notified when their associated HTTP
 * session is invalidated, enabling them to close the WebSocket with an
 * appropriate close code that the client can detect.
 * <p>
 * This class is meant only for testing purposes. Do not use it in production.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ViteSessionTracker {

    /**
     * Listener interface for HTTP session close events.
     */
    @FunctionalInterface
    public interface SessionCloseListener {
        /**
         * Called when an HTTP session is closed.
         *
         * @param httpSessionId
         *            the ID of the HTTP session that was closed
         * @param closeCode
         *            the WebSocket close code to use (e.g., 1008 for
         *            VIOLATED_POLICY)
         * @param closeMessage
         *            the close message to send with the WebSocket close
         */
        void onSessionClose(String httpSessionId, int closeCode,
                String closeMessage);
    }

    private final List<SessionCloseListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a listener that will be notified when an HTTP session is closed.
     *
     * @param listener
     *            a listener that receives the HTTP session ID, close code, and
     *            close message
     * @return a registration that can be used to remove the listener
     */
    public Registration addListener(SessionCloseListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Notifies all registered listeners that the given HTTP session has closed.
     *
     * @param httpSessionId
     *            the ID of the HTTP session that was closed
     * @param closeCode
     *            the WebSocket close code to use (e.g., 1008 for
     *            VIOLATED_POLICY)
     * @param closeMessage
     *            the close message to send with the WebSocket close
     */
    public void close(String httpSessionId, int closeCode,
            String closeMessage) {
        for (SessionCloseListener listener : listeners) {
            listener.onSessionClose(httpSessionId, closeCode, closeMessage);
        }
    }
}
