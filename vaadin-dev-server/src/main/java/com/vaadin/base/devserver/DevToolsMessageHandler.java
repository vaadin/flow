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
package com.vaadin.base.devserver;

import tools.jackson.databind.JsonNode;

/**
 * Handles dev tools messages from the client.
 */
public interface DevToolsMessageHandler {

    /**
     * Called when a browser connects.
     * <p>
     * This happens on each refresh but also when the application is opened in a
     * new browser tab or window.
     *
     * @param devToolsInterface
     *            for interaction with the development tools, e.g. sending a
     *            message
     */
    void handleConnect(DevToolsInterface devToolsInterface);

    /**
     * Called when a message from the browser arrives.
     *
     * @param command
     *            the command received
     * @param data
     *            the data received
     * @param devToolsInterface
     *            for interaction with the development tools, e.g. sending a
     *            message
     * @return {@code true} if the message was handled and should not be passed
     *         on to further handlers
     */
    default boolean handleMessage(String command, JsonNode data,
            DevToolsInterface devToolsInterface) {
        return this.handleMessage(command, data, devToolsInterface);
    }

    /**
     * Called when the browser connection disconnects.
     *
     * @param devToolsInterface
     *            for interaction with the development tools, e.g. sending a
     *            message
     */
    default void handleDisconnect(DevToolsInterface devToolsInterface) {
    }
}
