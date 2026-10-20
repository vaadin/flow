/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server;

import java.io.IOException;

/**
 * A request handler for requests that do not need a {@link VaadinSession}, such
 * as a resource that is the same for every user.
 * <p>
 * The service calls {@link #handleRequest(VaadinRequest, VaadinResponse)}
 * before it looks up the session of the request, so the handler runs the same
 * way whether or not the request has a session, never creates one and never
 * waits for the session lock. {@link VaadinSession#getCurrent()} is
 * {@code null} while it runs.
 * <p>
 * Not creating a session matters when a browser that has no session cookie yet
 * sends several requests in parallel, for example a page that embeds an
 * exported web component: each new session would replace the cookie of the
 * previous one, leaving the browser with a cookie for a session other than the
 * one its UI was created in.
 * <p>
 * Register the handler like any other request handler, for example with
 * {@link ServiceInitEvent#addRequestHandler(RequestHandler)}. All registered
 * sessionless handlers are asked before any handler that gets a session, in the
 * order of {@link VaadinService#getRequestHandlers()}. A request that none of
 * them handles continues to the session lookup and the other handlers.
 *
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface SessionlessRequestHandler extends RequestHandler {

    /**
     * Called for every request before the service looks up its session. If a
     * response is written, this method should return {@code true} to indicate
     * that no more request handlers should be invoked for the request.
     *
     * @param request
     *            the request to handle
     * @param response
     *            the response object to which a response can be written
     * @return {@code true} if a response has been written and no further
     *         request handlers should be called, otherwise {@code false}
     * @throws IOException
     *             if an IO error occurred
     */
    boolean handleRequest(VaadinRequest request, VaadinResponse response)
            throws IOException;

    /**
     * Calls {@link #handleRequest(VaadinRequest, VaadinResponse)}, ignoring the
     * session.
     * <p>
     * The service only calls the sessionless method of the handlers registered
     * with it. This method is used when the handler is called as a regular
     * request handler, for example when it is added to a session with
     * {@link VaadinSession#addRequestHandler(RequestHandler)}.
     */
    @Override
    default boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        return handleRequest(request, response);
    }
}
