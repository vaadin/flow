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
