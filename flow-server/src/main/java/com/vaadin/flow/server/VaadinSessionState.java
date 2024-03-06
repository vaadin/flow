/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import com.vaadin.flow.component.UI;

/**
 * The lifecycle state of a VaadinSession.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public enum VaadinSessionState {
    /**
     * The session is active and accepting client requests.
     */
    OPEN,
    /**
     * The {@link VaadinSession#close() close} method has been called; the
     * session will be closed as soon as the current request ends.
     */
    CLOSING,
    /**
     * The session is closed; all the {@link UI}s have been removed and
     * {@link SessionDestroyListener}s have been called.
     */
    CLOSED;

}
