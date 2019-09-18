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

package com.vaadin.flow.server;

/**
 * Contains the system messages used to notify the user about various critical
 * situations that can occur.
 * <p>
 * Vaadin gets the SystemMessages from your application by calling a static
 * getSystemMessages() method. By default the Application.getSystemMessages() is
 * used. You can customize this by defining a static
 * MyApplication.getSystemMessages() and returning CustomizedSystemMessages.
 * Note that getSystemMessages() is static - changing the system messages will
 * by default change the message for all users of the application.
 * <p>
 * The default behavior is to show a notification, and restart the application
 * the user clicks the message.<br>
 * Instead of restarting the application, you can set a specific URL that the
 * user is taken to.<br>
 * Setting both caption and message to null will restart the application (or go
 * to the specified URL) without displaying a notification.
 * set*NotificationEnabled(false) will achieve the same thing.
 * <p>
 * The situations are:
 * <ul>
 * <li>Session expired: the user session has expired, usually due to inactivity.
 * <li>Communication error: the client failed to contact the server, or the
 * server returned an invalid response.
 * <li>Internal error: unhandled critical server error (e.g out of memory,
 * database crash)
 * </ul>
 *
 * @since 1.0
 */
public class CustomizedSystemMessages extends SystemMessages {

    /**
     * Sets the URL the user will be redirected to after dismissing a "session
     * expired" message.
     *
     * @param sessionExpiredURL
     *            the URL to redirect to, or null to refresh the page
     */
    public void setSessionExpiredURL(String sessionExpiredURL) {
        this.sessionExpiredURL = sessionExpiredURL;
    }

    /**
     * Sets whether a "session expired" notification should be shown to the end
     * user. If the notification is disabled the user will be immediately
     * redirected to the URL returned by {@link #getSessionExpiredURL()}.
     *
     * @param sessionExpiredNotificationEnabled
     *            {@code true} to show the notification to the end user,
     *            {@code false} to redirect directly
     */
    public void setSessionExpiredNotificationEnabled(
            boolean sessionExpiredNotificationEnabled) {
        this.sessionExpiredNotificationEnabled = sessionExpiredNotificationEnabled;
    }

    /**
     * Sets the caption to show in a "session expired" notification.
     * <p>
     * If both {@link #getSessionExpiredCaption()} and
     * {@link #getSessionExpiredMessage()} return null, the user will
     * automatically be forwarded to the URL returned by
     * {@link #getSessionExpiredURL()} when the session expires.
     *
     * @param sessionExpiredCaption
     *            The caption to show or {@code null} to show no caption.
     */
    public void setSessionExpiredCaption(String sessionExpiredCaption) {
        this.sessionExpiredCaption = sessionExpiredCaption;
    }

    /**
     * Sets the message to show in a "session expired" notification.
     * <p>
     * If both {@link #getSessionExpiredCaption()} and
     * {@link #getSessionExpiredMessage()} return null, the user will
     * automatically be forwarded to the URL returned by
     * {@link #getSessionExpiredURL()} when the session expires.
     *
     * @param sessionExpiredMessage
     *            The message to show or {@code null} to show no message.
     */
    public void setSessionExpiredMessage(String sessionExpiredMessage) {
        this.sessionExpiredMessage = sessionExpiredMessage;
    }

    /**
     * Sets the URL the user will be redirected to after dismissing an "internal
     * error" message.
     *
     * @param internalErrorURL
     *            the URL to redirect to, or null to refresh the page
     */
    public void setInternalErrorURL(String internalErrorURL) {
        this.internalErrorURL = internalErrorURL;
    }

    /**
     * Sets whether an "internal error" notification should be shown to the end
     * user. If the notification is disabled the user will be immediately
     * redirected to the URL returned by {@link #getInternalErrorURL()}.
     *
     * @param internalErrorNotificationEnabled
     *            {@code true} to show the notification to the end user,
     *            {@code false} to redirect directly
     */
    public void setInternalErrorNotificationEnabled(
            boolean internalErrorNotificationEnabled) {
        this.internalErrorNotificationEnabled = internalErrorNotificationEnabled;
    }

    /**
     * Sets the caption to show in an "internal error" notification.
     *
     * @param internalErrorCaption
     *            The caption to show or {@code null} to show no caption.
     */
    public void setInternalErrorCaption(String internalErrorCaption) {
        this.internalErrorCaption = internalErrorCaption;
    }

    /**
     * Sets the message to show in an "internal error" notification.
     *
     * @param internalErrorMessage
     *            The message to show or {@code null} to show no message.
     */
    public void setInternalErrorMessage(String internalErrorMessage) {
        this.internalErrorMessage = internalErrorMessage;
    }

    /**
     * Sets the URL the user will be redirected to after dismissing a
     * "cookies disabled" message.
     *
     * @param cookiesDisabledURL
     *            the URL to redirect to, or null to refresh the page
     */
    public void setCookiesDisabledURL(String cookiesDisabledURL) {
        this.cookiesDisabledURL = cookiesDisabledURL;
    }

    /**
     * Sets whether a "cookies disabled" notification should be shown to the end
     * user. If the notification is disabled the user will be immediately
     * redirected to the URL returned by {@link #getCookiesDisabledURL()}.
     *
     * @param cookiesDisabledNotificationEnabled
     *            {@code true} to show the notification to the end user,
     *            {@code false} to redirect directly
     */
    public void setCookiesDisabledNotificationEnabled(
            boolean cookiesDisabledNotificationEnabled) {
        this.cookiesDisabledNotificationEnabled = cookiesDisabledNotificationEnabled;
    }

    /**
     * Sets the caption to show in an "cookies disabled" notification.
     *
     * @param cookiesDisabledCaption
     *            The caption to show or {@code null} to show no caption.
     */
    public void setCookiesDisabledCaption(String cookiesDisabledCaption) {
        this.cookiesDisabledCaption = cookiesDisabledCaption;
    }

    /**
     * Sets the message to show in a "cookies disabled" notification.
     *
     * @param cookiesDisabledMessage
     *            The message to show or {@code null} to show no message.
     */
    public void setCookiesDisabledMessage(String cookiesDisabledMessage) {
        this.cookiesDisabledMessage = cookiesDisabledMessage;
    }

}
