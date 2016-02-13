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
