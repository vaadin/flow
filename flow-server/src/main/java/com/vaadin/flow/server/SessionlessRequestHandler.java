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
import java.io.Serializable;

/**
 * A request handler for requests that do not need a {@link VaadinSession}, such
 * as a resource that is the same for every user.
 * <p>
 * The service calls {@link #handleRequest(VaadinRequest, VaadinResponse)}
 * before it looks up the session of the request, so the handler runs the same
 * way whether or not the request has a session, never creates one and does not
 * lock it. {@link VaadinSession#getCurrent()} is {@code null} while it runs.
 * <p>
 * {@link VaadinRequestInterceptor Request interceptors} still run for the
 * request before the handler. An interceptor that uses an existing session,
 * such as the daily active user tracking, may lock that session in
 * {@link VaadinRequestInterceptor#requestStart(VaadinRequest, VaadinResponse)},
 * so the request can then wait for the session lock before the handler is
 * called.
 * <p>
 * Not creating a session matters when a browser that has no session cookie yet
 * sends several requests in parallel, for example a page that embeds an
 * exported web component: each new session would replace the cookie of the
 * previous one, leaving the browser with a cookie for a session other than the
 * one its UI was created in.
 * <p>
 * Register the handler with
 * {@link ServiceInitEvent#addSessionlessRequestHandler(SessionlessRequestHandler)}.
 * All registered sessionless handlers are asked before any
 * {@link RequestHandler}, in the order of
 * {@link VaadinService#getSessionlessRequestHandlers()}. A request that none of
 * them handles continues to the session lookup and the request handlers.
 *
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface SessionlessRequestHandler extends Serializable {

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

}
