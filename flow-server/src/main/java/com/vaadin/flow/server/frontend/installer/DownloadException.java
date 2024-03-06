/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.installer;

/**
 * Exception indicating a failure during file download.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since
 */
final class DownloadException extends Exception {

    /**
     * Exceptioon with message.
     *
     * @param message
     *            exception message
     */
    public DownloadException(String message) {
        super(message);
    }

    /**
     * Exceptioon with message and cause.
     *
     * @param message
     *            exception message
     * @param cause
     *            cause for exception
     */
    DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
