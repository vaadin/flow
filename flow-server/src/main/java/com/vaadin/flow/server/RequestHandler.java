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
import java.io.Serializable;

import com.vaadin.flow.component.UI;

/**
 * Handler for producing a response to HTTP requests. Handlers can be either
 * added on a {@link VaadinService service} level, common for all users, or on a
 * {@link VaadinSession session} level for only a single user.
 *
 * @since 1.0
 */
public interface RequestHandler extends Serializable {

    /**
     * Called when a request needs to be handled. If a response is written, this
     * method should return <code>true</code> to indicate that no more request
     * handlers should be invoked for the request.
     * <p>
     * Note that request handlers by default do not lock the session. If you are
     * using VaadinSession or anything inside the VaadinSession you must ensure
     * the session is locked. This can be done by extending
     * {@link SynchronizedRequestHandler} or by using
     * {@link VaadinSession#accessSynchronously(Command)} or
     * {@link UI#accessSynchronously(Command)}.
     * </p>
     *
     * @param session
     *            The session for the request
     * @param request
     *            The request to handle
     * @param response
     *            The response object to which a response can be written.
     * @return true if a response has been written and no further request
     *         handlers should be called, otherwise false
     * @throws IOException
     *             If an IO error occurred
     */
    boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException;

}
