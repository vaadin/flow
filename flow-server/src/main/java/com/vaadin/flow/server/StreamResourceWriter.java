/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Output stream consumer. Implementation of this interface should write data
 * into {@link OutputStream} instance provided as an argument to its
 * {@link #accept(OutputStream, VaadinSession)} method.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@FunctionalInterface
public interface StreamResourceWriter extends Serializable {

    /**
     * Handles {@code stream} (writes data to it) using {@code session} as a
     * context.
     * <p>
     * Note that the method is not called under the session lock. It means that
     * if implementation requires access to the application/session data then
     * the session has to be locked explicitly.
     *
     * @param stream
     *            data output stream
     * @param session
     *            vaadin session
     * @throws IOException
     *             if an IO error occurred
     */
    void accept(OutputStream stream, VaadinSession session) throws IOException;
}
