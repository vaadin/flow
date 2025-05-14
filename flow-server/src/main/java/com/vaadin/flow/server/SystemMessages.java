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

package com.vaadin.flow.server;

import java.io.Serializable;

/**
 * Contains the system messages used to notify the user about various critical
 * situations that can occur.
 * <p>
 * Use {@link VaadinService#setSystemMessagesProvider(SystemMessagesProvider)}
 * to customize.
 * </p>
 * <p>
 * The defaults defined in this class are:
 * <ul>
 * <li><b>sessionExpiredURL</b> = null</li>
 * <li><b>sessionExpiredNotificationEnabled</b> = false</li>
 * <li><b>sessionExpiredCaption</b> = "Session Expired"</li>
 * <li><b>sessionExpiredMessage</b> = "Take note of any unsaved data, and click
 * here or press ESC key to continue."</li>
 * <li><b>internalErrorURL</b> = null</li>
 * <li><b>internalErrorNotificationEnabled</b> = true</li>
 * <li><b>internalErrorCaption</b> = "Internal error"</li>
 * <li><b>internalErrorMessage</b> = "Please notify the administrator. Take note
 * of any unsaved data, and click here or press ESC to continue."</li>
 * <li><b>cookiesDisabledURL</b> = null</li>
 * <li><b>cookiesDisabledNotificationEnabled</b> = true</li>
 * <li><b>cookiesDisabledCaption</b> = "Cookies disabled"</li>
 * <li><b>cookiesDisabledMessage</b> = "This application requires cookies to
 * function. Please enable cookies in your browser and click here or press ESC
 * to try again.</li>
 * </ul>
 *
 * @since 1.0
 */
public class SystemMessages implements Serializable {
    protected String sessionExpiredURL = null;
    protected boolean sessionExpiredNotificationEnabled = false;
    protected String sessionExpiredCaption = "Session Expired";
    protected String sessionExpiredMessage = "Take note of any unsaved data, and click here or press ESC key to continue.";

    protected String internalErrorURL = null;
    protected boolean internalErrorNotificationEnabled = true;
    protected String internalErrorCaption = "Internal error";
    protected String internalErrorMessage = "Please notify the administrator. Take note of any unsaved data, and click here or press ESC to continue.";

    protected String cookiesDisabledURL = null;
    protected boolean cookiesDisabledNotificationEnabled = true;
    protected String cookiesDisabledCaption = "Cookies disabled";
    protected String cookiesDisabledMessage = "This application requires cookies to function. Please enable cookies in your browser and click here or press ESC to try again.";

    /**
     * Private constructor
     */
    SystemMessages() {

    }

    /**
     * Gets the URL the user will be redirected to after dismissing a session
     * expired message.
     *
     * Default value is {@literal null}.
     *
     * @return the URL to redirect to, or null to refresh the page
     */
    public String getSessionExpiredURL() {
        return sessionExpiredURL;
    }

    /**
     * Checks if "session expired" notifications should be shown to the end
     * user. If the notification is disabled the user will be immediately
     * redirected to the URL returned by {@link #getSessionExpiredURL()}.
     *
     * By default, the "session expired" notification is disabled.
     *
     * @return {@code true} to show the notification to the end user,
     *         {@code false} to redirect directly
     */
    public boolean isSessionExpiredNotificationEnabled() {
        return sessionExpiredNotificationEnabled;
    }

    /**
     * Gets the caption to show in a "session expired" notification.
     *
     * Returns {@literal null} if the "session expired" notification is
     * disabled.
     *
     * @return The caption to show or {@code null} to show no caption.
     */
    public String getSessionExpiredCaption() {
        return (sessionExpiredNotificationEnabled ? sessionExpiredCaption
                : null);
    }

    /**
     * Gets the message to show in a "session expired" notification.
     *
     * Returns {@literal null} if the "session expired" notification is
     * disabled.
     *
     * @return The message to show or {@code null} to show no message.
     */
    public String getSessionExpiredMessage() {
        return (sessionExpiredNotificationEnabled ? sessionExpiredMessage
                : null);
    }

    /**
     * Gets the URL the user will be redirected to after dismissing an internal
     * error message.
     *
     * Default value is {@literal null}.
     *
     * @return the URL to redirect to, or null to refresh the page
     */
    public String getInternalErrorURL() {
        return internalErrorURL;
    }

    /**
     * Checks if "internal error" notifications should be shown to the end user.
     * If the notification is disabled the user will be immediately redirected
     * to the URL returned by {@link #getInternalErrorURL()}.
     *
     * By default, the "internal error" notification is enabled.
     *
     * @return {@code true} to show the notification to the end user,
     *         {@code false} to redirect directly
     */
    public boolean isInternalErrorNotificationEnabled() {
        return internalErrorNotificationEnabled;
    }

    /**
     * Gets the caption to show in an "internal error" notification.
     *
     * Returns {@literal null} if the "internal error" notification is disabled.
     *
     * @return The caption to show or {@code null} to show no caption.
     */
    public String getInternalErrorCaption() {
        return (internalErrorNotificationEnabled ? internalErrorCaption : null);
    }

    /**
     * Gets the message to show in a "internal error" notification.
     *
     * Returns {@literal null} if the "internal error" notification is disabled.
     *
     * @return The message to show or {@code null} to show no message.
     */
    public String getInternalErrorMessage() {
        return (internalErrorNotificationEnabled ? internalErrorMessage : null);
    }

    /**
     * Gets the URL the user will be redirected to after dismissing a "cookies
     * disabled" message.
     *
     * Default value is {@literal null}.
     *
     * @return the URL to redirect to, or null to refresh the page
     */
    public String getCookiesDisabledURL() {
        return cookiesDisabledURL;
    }

    /**
     * Checks if "cookies disabled" notifications should be shown to the end
     * user. If the notification is disabled the user will be immediately
     * redirected to the URL returned by {@link #getCookiesDisabledURL()}.
     *
     * By default, the "cookies disable" notification is disabled.
     *
     * @return {@code true} to show the notification to the end user,
     *         {@code false} to redirect directly
     */
    public boolean isCookiesDisabledNotificationEnabled() {
        return cookiesDisabledNotificationEnabled;
    }

    /**
     * Gets the caption to show in a "cookies disabled" notification.
     *
     * Returns {@literal null} if the "cookies disable" notification is
     * disabled.
     *
     * @return The caption to show or {@code null} to show no caption.
     */
    public String getCookiesDisabledCaption() {
        return (cookiesDisabledNotificationEnabled ? cookiesDisabledCaption
                : null);
    }

    /**
     * Gets the message to show in a "cookies disabled" notification.
     *
     * Returns {@literal null} if the "cookies disable" notification is
     * disabled.
     *
     * @return The message to show or {@code null} to show no message.
     */
    public String getCookiesDisabledMessage() {
        return (cookiesDisabledNotificationEnabled ? cookiesDisabledMessage
                : null);
    }

}
