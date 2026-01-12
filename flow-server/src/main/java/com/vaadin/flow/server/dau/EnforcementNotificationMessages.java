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
package com.vaadin.flow.server.dau;

import java.io.Serializable;
import java.util.Objects;

/**
 * Contains the system messages used to notify the user about various critical
 * situations that can occur.
 *
 * @param caption
 *            the caption to show in an enforcement notification, not
 *            {@literal null}.
 * @param message
 *            the message to show in an enforcement notification, not
 *            {@literal null}.
 * @param details
 *            additional details to show in an enforcement notification.
 * @param url
 *            the URL the user will be redirected to after dismissing an
 *            enforcement message.
 * @since 24.5
 */
public record EnforcementNotificationMessages(String caption, String message,
        String details, String url) implements Serializable {

    public EnforcementNotificationMessages {
        Objects.requireNonNull(caption, "Caption cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");
    }

    /**
     * Default enforcement messages.
     */
    public static final EnforcementNotificationMessages DEFAULT = new EnforcementNotificationMessages(
            "Service Unavailable",
            "Please notify the administrator. Take note of any unsaved data, and click here or press ESC to continue.",
            null, null);
}
