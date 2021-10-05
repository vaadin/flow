/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import java.util.Set;

import com.google.web.bindery.event.shared.UmbrellaException;
import com.vaadin.client.bootstrap.ErrorMessage;

/**
 * Handles system errors in the application.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
     * Shows the session expiration notification.
     *
     * @param details
     *            message details or null if there are no details
     */
    public void handleSessionExpiredError(String details) {
        handleUnrecoverableError(details, registry.getApplicationConfiguration()
                .getSessionExpiredError());
    }

    /**
     * Shows an error notification for an error which is unrecoverable.
     *
     * @param details
     *            message details or null if there are no details
     * @param message
     *            an ErrorMessage describing the error
     */
    protected void handleUnrecoverableError(String details,
            ErrorMessage message) {
        handleUnrecoverableError(message.getCaption(), message.getMessage(),
                details, message.getUrl());
    }

    /**
     * Shows an error notification for an error which is unrecoverable, using
     * the given parameters.
     *
     * @param caption
     *            the caption of the message
     * @param message
     *            the message body
     * @param details
     *            message details or {@code null} if there are no details
     * @param url
     *            a URL to redirect to when the user clicks the message or
     *            {@code null} to refresh on click
     */
    public void handleUnrecoverableError(String caption, String message,
            String details, String url) {
        if (caption == null && message == null && details == null) {
            if (!isWebComponentMode()) {
                WidgetUtil.redirect(url);
            }
            return;
        }

        handleError(caption, message, details);
    }

    /**
     * Shows an error notification for an error which is unrecoverable, using
     * the given parameters.
     *
     * @param caption
     *            the caption of the message
     * @param message
     *            the message body
     * @param details
     *            message details or {@code null} if there are no details
     * @param url
     *            a URL to redirect to when the user clicks the message or
     *            {@code null} to refresh on click
     * @param querySelector
     *            query selector to find the element under which the error will
     *            be added . If element is not found or the selector is
     *            {@code null}, body will be used
     * 
     * @deprecated the querySelector parameter is no longer used, use
     *             {@link #handleUnrecoverableError(String, String, String, String)}
     *             instead
     */
    @Deprecated
    public void handleUnrecoverableError(String caption, String message,
            String details, String url, String querySelector) {
        handleUnrecoverableError(caption, message, details, url);
    }

    /**
     * Shows the given error message if not running in production mode and logs
     * it to the console if running in production mode.
     *
     * @param errorMessage
     *            the error message to show
     */
    public void handleError(String errorMessage) {
        Console.error(errorMessage);
    }

    /**
     * Shows an error message if not running in production mode and logs it to
     * the console if running in production mode.
     *
     * @param throwable
     *            the throwable which occurred
     */
    public void handleError(Throwable throwable) {
        Throwable unwrappedThrowable = unwrapUmbrellaException(throwable);
        if (unwrappedThrowable instanceof AssertionError) {
            handleError("Assertion error: " + unwrappedThrowable.getMessage());
        } else {
            handleError(unwrappedThrowable.getMessage());
        }
    }

    private void handleError(String caption, String message, String details) {
        Console.error(caption + " " + message + " " + details);
    }

    private static Throwable unwrapUmbrellaException(Throwable e) {
        if (e instanceof UmbrellaException) {
            Set<Throwable> causes = ((UmbrellaException) e).getCauses();
            if (causes.size() == 1) {
                return unwrapUmbrellaException(causes.iterator().next());
            }
        }
        return e;
    }

    private boolean isWebComponentMode() {
        return registry.getApplicationConfiguration().isWebComponentMode();
    }

}
