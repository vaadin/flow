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
 * A specialized RequestHandler which is capable of sending session expiration
 * messages to the user.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface SessionExpiredHandler extends RequestHandler {

    /**
     * Called when the a session expiration has occurred and a notification
     * needs to be sent to the user. If a response is written, this method
     * should return <code>true</code> to indicate that no more
     * {@link SessionExpiredHandler} handlers should be invoked for the request.
     *
     * @param request
     *            The request to handle
     * @param response
     *            The response object to which a response can be written.
     * @return true if a response has been written and no further request
     *         handlers should be called, otherwise false
     * @throws IOException
     *             If an IO error occurred
     */
    boolean handleSessionExpired(VaadinRequest request, VaadinResponse response)
            throws IOException;

    /**
     * Checks whether this handler serves the request without a session.
     * <p>
     * A request that does not need a session, such as a resource that is the
     * same for every user, should not create one: a browser that has no session
     * cookie yet may send several such requests in parallel, and each new
     * session would replace the cookie of the previous one. When a registered
     * handler returns {@code true}, the request does not create a session, and
     * when there is none, its
     * {@link #handleSessionExpired(VaadinRequest, VaadinResponse)} is called
     * before that of any other handler.
     *
     * @param request
     *            the request to check
     * @return {@code true} if this handler serves the request without a
     *         session, {@code false} by default
     * @since 25.4
     */
    default boolean canHandleWithoutSession(VaadinRequest request) {
        return false;
    }

}
