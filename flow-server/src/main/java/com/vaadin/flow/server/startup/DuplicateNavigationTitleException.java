/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.PageTitle;

/**
 * Exception indicating a conflict between a route target implementing
 * {@link HasDynamicTitle} while also having a {@link PageTitle} annotation.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
public class DuplicateNavigationTitleException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     */
    public DuplicateNavigationTitleException(String message) {
        super(message);
    }
}
