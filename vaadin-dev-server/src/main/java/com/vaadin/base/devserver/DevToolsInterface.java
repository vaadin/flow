/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import com.vaadin.flow.internal.JacksonUtils;

import elemental.json.JsonObject;

/**
 * For interfacing with the development tools by plugins.
 */
public interface DevToolsInterface {

    /**
     * Sends the given message to the client side.
     *
     * @param command
     *            the command to send
     * @param data
     *            data, specific to the command
     */
    @Deprecated
    default void send(String command, JsonObject data) {
        send(command, JacksonUtils.mapElemental(data));
    }

    /**
     * Sends the given message to the client side.
     *
     * @param command
     *            the command to send
     * @param data
     *            data, specific to the command
     */
    void send(String command, Object data);
}
