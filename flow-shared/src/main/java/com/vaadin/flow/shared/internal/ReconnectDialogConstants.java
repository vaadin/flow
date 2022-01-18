package com.vaadin.flow.shared.internal;

/**
 * Constants for the reconnect dialog.
 */
public final class ReconnectDialogConstants {

    public static final String DIALOG_TEXT_KEY = "dialogText";
    public static final String DIALOG_TEXT_DEFAULT = "Server connection lost, trying to reconnect...";
    public static final String DIALOG_TEXT_GAVE_UP_KEY = "dialogTextGaveUp";
    public static final String DIALOG_TEXT_GAVE_UP_DEFAULT = "Server connection lost.";
    public static final String RECONNECT_ATTEMPTS_KEY = "reconnectAttempts";
    public static final int RECONNECT_ATTEMPTS_DEFAULT = 10000;
    public static final String RECONNECT_INTERVAL_KEY = "reconnectInterval";
    public static final int RECONNECT_INTERVAL_DEFAULT = 5000;

    private ReconnectDialogConstants() {
        // Only static
    }

}
