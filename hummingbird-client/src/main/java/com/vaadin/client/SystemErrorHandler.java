package com.vaadin.client;

import com.vaadin.client.bootstrap.ErrorMessage;

import elemental.client.Browser;

public class SystemErrorHandler {

    private Registry registry;

    public SystemErrorHandler(Registry registry) {
        this.registry = registry;
    }

    /**
     * Shows the communication error notification.
     *
     * @param details
     *            Optional details.
     * @param statusCode
     *            The status code returned for the request
     *
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
     *            Optional details.
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
     *            Optional details.
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
     *            Optional details.
     * @param message
     *            An ErrorMessage describing the error.
     */
    protected void showError(String details, ErrorMessage message) {
        showError(message.getCaption(), message.getMessage(), details,
                message.getUrl());
    }

    public void showError(String caption, String message, String details,
            String url) {
        // FIXME Not like this
        Browser.getWindow().alert(caption + "\n" + message + "\n" + details);
        if (url != null) {
            WidgetUtil.redirect(url);
        }
    }

}
