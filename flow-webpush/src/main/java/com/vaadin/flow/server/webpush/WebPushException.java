/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.webpush;

/**
 * Checked exception which is thrown when Web Push initialisation or sending a
 * Web Push notification fails.
 *
 * @author Vaadin Ltd
 * @since 24.2
 */
public class WebPushException extends RuntimeException {

    public WebPushException() {
        super();
    }

    public WebPushException(String message) {
        super(message);
    }

    public WebPushException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebPushException(Throwable cause) {
        super(cause);
    }

    public WebPushException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
