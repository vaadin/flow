package com.vaadin.base.devserver;

import elemental.json.JsonObject;

/**
 * Handles dev tools messages from the client.
 */
public interface DevToolsMessageHandler {
    /**
     * Called when a message from the browser arrives.
     *
     * @param command
     *            the command received
     * @param data
     *            the data received
     * @return {@code true} if the message was handled and should not be passed
     *         on to further handlers
     */
    public boolean handleDevToolsMessage(String command, JsonObject data);

}
