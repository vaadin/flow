/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Web Push message object containing an information to be shown in the
 * notification.
 *
 * @since 24.2
 */
public record WebPushMessage(String title, String body) implements Serializable {

    /**
     * Creates a new Web Push notification message with title and body.
     *
     * @param title notification title
     * @param body  notification body
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
