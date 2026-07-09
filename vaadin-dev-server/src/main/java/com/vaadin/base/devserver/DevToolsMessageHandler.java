/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import com.fasterxml.jackson.databind.JsonNode;

import com.vaadin.flow.internal.JacksonUtils;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

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
    @Deprecated
    boolean handleMessage(String command, JsonObject data,
            DevToolsInterface devToolsInterface);

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
        return this.handleMessage(command, Json.parse(data.toString()),
                devToolsInterface);
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
