/*
 * Copyright 2000-2018 Vaadin Ltd.
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

    /**
     * Gets the timeout (in milliseconds) between noticing a loss of connection
     * and showing the dialog.
     * <p>
     * The default is
     * {@value ReconnectDialogConfigurationMap#DIALOG_GRACE_PERIOD_DEFAULT}
     *
     * @return the time to wait before showing a dialog
     */
    int getDialogGracePeriod();

    /**
     * Sets the timeout (in milliseconds) between noticing a loss of connection
     * and showing the dialog.
     *
     * @param dialogGracePeriod
     *            the time to wait before showing a dialog
     */
    void setDialogGracePeriod(int dialogGracePeriod);

    /**
     * Sets the modality of the dialog.
     * <p>
     * If the dialog is set to modal, it will prevent the usage of the
     * application while the dialog is being shown. If not modal, the user can
     * continue to use the application as normally and all server events will be
     * queued until connection has been re-established.
     *
     * @param dialogModal
     *            true to make the dialog modal, false otherwise
     */
    void setDialogModal(boolean dialogModal);

    /**
     * Checks the modality of the dialog.
     * <p>
     * The default is
     * {@value ReconnectDialogConfigurationMap#DIALOG_MODAL_DEFAULT}
     *
     * @see #setDialogModal(boolean)
     * @return true if the dialog is modal, false otherwise
     */
    boolean isDialogModal();
}
