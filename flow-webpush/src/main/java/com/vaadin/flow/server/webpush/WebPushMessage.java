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
