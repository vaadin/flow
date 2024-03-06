/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.io.Serializable;

import com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap;

/**
 * Provides methods for configuring the reconnect dialog.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ReconnectDialogConfiguration extends Serializable {
    /**
     * Gets the text to show in the reconnect dialog when trying to re-establish
     * the server connection.
     * <p>
     * The default is
     * {@value ReconnectDialogConfigurationMap#DIALOG_TEXT_DEFAULT}
     *
     * @return the text to show in the reconnect dialog
     */
    String getDialogText();

    /**
     * Sets the text to show in the reconnect dialog when trying to re-establish
     * the server connection.
     *
     * @param dialogText
     *            the text to show in the reconnect dialog
     */
    void setDialogText(String dialogText);

    /**
     * Gets the text to show in the reconnect dialog after giving up trying to
     * reconnect ({@link #getReconnectAttempts()} reached).
     * <p>
     * The default is
     * {@value ReconnectDialogConfigurationMap#DIALOG_TEXT_GAVE_UP_DEFAULT}
     *
     * @return the text to show in the reconnect dialog after giving up
     */
    String getDialogTextGaveUp();

    /**
     * Sets the text to show in the reconnect dialog after giving up trying to
     * reconnect ({@link #getReconnectAttempts()} reached).
     *
     * @param dialogTextGaveUp
     *            the text to show in the reconnect dialog after giving up
     */
    void setDialogTextGaveUp(String dialogTextGaveUp);

    /**
     * Gets the number of times to try to reconnect to the server before giving
     * up.
     * <p>
     * The default is
     * {@value ReconnectDialogConfigurationMap#RECONNECT_ATTEMPTS_DEFAULT}
     *
     * @return the number of times to try to reconnect
     */
    int getReconnectAttempts();

    /**
     * Sets the number of times to try to reconnect to the server before giving
     * up.
     *
     * @param reconnectAttempts
     *            the number of times to try to reconnect
     */
    void setReconnectAttempts(int reconnectAttempts);

    /**
     * Gets the interval (in milliseconds) between reconnect attempts.
     * <p>
     * The default is
     * {@value ReconnectDialogConfigurationMap#RECONNECT_INTERVAL_DEFAULT}
     *
     * @return the interval (in ms) between reconnect attempts
     */
    int getReconnectInterval();

    /**
     * Sets the interval (in milliseconds) between reconnect attempts.
     *
     * @param reconnectInterval
     *            the interval (in ms) between reconnect attempts
     */
    void setReconnectInterval(int reconnectInterval);
}
