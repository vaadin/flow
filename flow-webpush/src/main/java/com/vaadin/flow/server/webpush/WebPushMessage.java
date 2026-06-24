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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Web Push message object containing an information to be shown in the
 * notification.
 *
 * @since 24.2
 */
public record WebPushMessage(String title,
        ObjectNode options) implements Serializable {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a new Web Push notification message with the specified title and
     * various options fetched from a given Java object.
     *
     * @param title
     *            the notification title
     * @param options
     *            any {@code Serializable} Java object representing custom
     *            settings to apply to the notification
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/ServiceWorkerRegistration/showNotification#parameters">
     *      showNotification parameters</a>
     */
    public WebPushMessage(String title, Serializable options) {
        this(title, objectMapper.convertValue(options, ObjectNode.class));
    }

    /**
     * Creates a new Web Push notification message with just a title and body.
     *
     * @param title
     *            notification title
     * @param body
     *            notification body
     */
    public WebPushMessage(String title, String body) {
        this(title, getBodyOption(body));
    }

    /**
     * Creates a new Web Push notification message with just a title.
     *
     * @param title
     *            notification title
     */
    public WebPushMessage(String title) {
        this(title, (ObjectNode) null);
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
        ObjectNode json = objectMapper.createObjectNode();
        json.put("title", title);
        if (options != null) {
            json.set("options", options);
        }
        return json.toString();
    }

    private static ObjectNode getBodyOption(String body) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        if (body != null) {
            objectNode.put("body", body);
        }
        return objectNode;
    }
}
