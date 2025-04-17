/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import com.fasterxml.jackson.databind.JsonNode;
import org.atmosphere.cpr.AtmosphereResource;

import com.vaadin.flow.server.communication.FragmentedMessageHolder;

/**
 * Provides a way to reload browser tabs via web socket connection passed as a
 * {@link AtmosphereResource}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 *
 */
public interface BrowserLiveReload extends FragmentedMessageHolder {

    /**
     * Live reload enabling technology detected.
     */
    enum Backend {
        HOTSWAP_AGENT, JREBEL, SPRING_BOOT_DEVTOOLS;
    }

    /**
     * Detects and return enabling live reload backend technology.
     *
     * @return enabling technology, or <code>null</code> if none
     */
    Backend getBackend();

    /**
     * Sets the live reload backend technology explicitly.
     *
     * @param backend
     *            enabling technology, not <code>null</code>.
     */
    void setBackend(Backend backend);

    /**
     * Sets the web socket connection resource when it's established.
     *
     * @param resource
     *            a web socket connection resource, not <code>null</code>.
     */
    void onConnect(AtmosphereResource resource);

    /**
     * Removes the web socket connection resource, not <code>null</code>.
     *
     * @param resource
     *            a web socket connection resource
     */
    void onDisconnect(AtmosphereResource resource);

    /**
     * Returns whether the passed connection is a browser live-reload
     * connection.
     *
     * @param resource
     *            a web socket connection resource, not <code>null</code>.
     * @return whether the web socket connection is for live reload
     */
    boolean isLiveReload(AtmosphereResource resource);

    /**
     * Requests reload via the resource provided via
     * {@link #onConnect(AtmosphereResource)} call.
     */
    void reload();

    /**
     * Requests a refresh of the current view in the browser, without reloading
     * the whole page.
     *
     * @param refreshLayouts
     *            {@code true} to refresh all layouts in the route chain,
     *            {@code false} to only refresh the route component
     */
    default void refresh(boolean refreshLayouts) {
        reload();
    }

    /**
     * Request an update of the resource with the given path.
     *
     * @param path
     *            the path of the file to update, relative to the servlet path
     * @param content
     *            the new content of the file
     */
    void update(String path, String content);

    /**
     * Called when any message is received through the connection.
     *
     * @param resource
     *            the atmosphere resource that received the message
     * @param msg
     *            the received message
     */
    void onMessage(AtmosphereResource resource, String msg);

    /**
     * Send a client side HMR event.
     *
     * @param event
     *            the event name
     * @param eventData
     *            the event data
     */
    void sendHmrEvent(String event, JsonNode eventData);

}
