/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client;

import com.vaadin.client.bootstrap.ErrorMessage;

import elemental.client.Browser;

/**
 * Class handling system errors in the application.
 *
 * @author Vaadin
 * @since
 */
public class SystemErrorHandler {

    private Registry registry;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public SystemErrorHandler(Registry registry) {
        this.registry = registry;
    }

    /**
     * Shows the communication error notification.
     *
     * @param details
     *            message details or null if there are no details
     * @param statusCode
     *            The status code returned for the request
     */
    public void showCommunicationError(String details, int statusCode) {
        Console.error("Communication error: " + details);
        showError(details,
                registry.getApplicationConfiguration().getCommunicationError());
    }

    /**
     * Shows the authentication error notification.
     *
     * @param details
     *            message details or null if there are no details
     */
    public void showAuthenticationError(String details) {
        Console.error("Authentication error: " + details);
        showError(details,
                registry.getApplicationConfiguration().getAuthorizationError());
    }

    /**
     * Shows the session expiration notification.
     *
     * @param details
     *            message details or null if there are no details
     */
    public void showSessionExpiredError(String details) {
        Console.error("Session expired: " + details);
        showError(details, registry.getApplicationConfiguration()
                .getSessionExpiredError());
    }

    /**
     * Shows an error notification.
     *
     * @param details
     *            message details or null if there are no details
     * @param message
     *            an ErrorMessage describing the error
     */
    protected void showError(String details, ErrorMessage message) {
        showError(message.getCaption(), message.getMessage(), details,
                message.getUrl());
    }

    /**
     * Shows a error notification using the given parameters.
     *
     * @param caption
     *            the caption of the message
     * @param message
     *            the message body
     * @param details
     *            message details or null if there are no details
     * @param url
     *            a URL to redirect to when the user clicks the message or null
     *            if no redirection should take place
     */
    public void showError(String caption, String message, String details,
            String url) {
        // FIXME Not like this
        Browser.getWindow().alert(caption + "\n" + message + "\n" + details);
        if (url != null) {
            WidgetUtil.redirect(url);
        }
    }

}
