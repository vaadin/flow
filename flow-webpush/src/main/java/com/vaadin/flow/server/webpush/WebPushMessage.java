/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.webpush;

import java.io.Serializable;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Web Push message object containing an information to be shown in the
 * notification.
 *
 * @since 24.2
 */
public record WebPushMessage(String title,
        String body) implements Serializable {

    /**
     * Creates a new Web Push notification message with title and body.
     *
     * @param title
     *            notification title
     * @param body
     *            notification body
     */
    public WebPushMessage {
    }

    @Override
    public String toString() {
        return toJson();
    }

    /**
     * Converts this Web Push message into JSON format string.
     *
     * @return JSON representation of this message
     */
    public String toJson() {
        JsonObject json = Json.createObject();
        json.put("title", title);
        json.put("body", body);
        return json.toJson();
    }
}
