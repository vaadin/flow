/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Handles a request by passing it to each registered {@link RequestHandler} in
 * the session in turn until one produces a response. This method is used for
 * requests that have not been handled by any specific functionality in the
 * servlet.
 * <p>
 * The request handlers are invoked in the reverse order in which they were
 * added to the session until a response has been produced. This means that the
 * most recently added handler is used first and the first request handler that
 * was added to the session is invoked towards the end unless any previous
 * handler has already produced a response.
 * </p>
 * <p>
 * The session is not locked during execution of the request handlers. The
 * request handler can itself decide if it needs to lock the session or not.
 * </p>
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see VaadinSession#addRequestHandler(RequestHandler)
 * @see RequestHandler
 * @since 1.0
 */
public class SessionRequestHandler implements RequestHandler {

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        // Use a copy to avoid ConcurrentModificationException
        session.lock();
        List<RequestHandler> requestHandlers;
        try {
            requestHandlers = new ArrayList<>(session.getRequestHandlers());
        } finally {
            session.unlock();
        }
        for (RequestHandler handler : requestHandlers) {
            if (handler.handleRequest(session, request, response)) {
                return true;
            }
        }
        // If not handled
        return false;
    }
}
