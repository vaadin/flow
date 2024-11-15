/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * RequestHandler which takes care of locking and unlocking of the VaadinSession
 * automatically. The session is locked before
 * {@link #synchronizedHandleRequest(VaadinSession, VaadinRequest, VaadinResponse)}
 * is called and unlocked after it has completed.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class SynchronizedRequestHandler implements RequestHandler {

    private static final int MAX_BUFFER_SIZE = 64 * 1024;

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        if (!canHandleRequest(request)) {
            return false;
        }

        try {
            if (isReadRequestBodyFirstEnabled()) {
                BufferedReader reader = request.getReader();
                String requestBody = reader == null ? null
                        : getRequestBody(reader);
                session.lock();
                return synchronizedHandleRequest(session, request, response,
                        requestBody);
            } else {
                session.lock();
                return synchronizedHandleRequest(session, request, response);
            }
        } finally {
            if (session.hasLock()) {
                session.unlock();
            }
        }
    }

    /**
     * Identical to
     * {@link #handleRequest(VaadinSession, VaadinRequest, VaadinResponse)}
     * except the {@link VaadinSession} is locked before this is called and
     * unlocked after this has completed.
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
     * @see #handleRequest(VaadinSession, VaadinRequest, VaadinResponse)
     */
    public abstract boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException;

    /**
     * Returns {@literal true} if
     * {@link #synchronizedHandleRequest(VaadinSession, VaadinRequest, VaadinResponse, String)}
     * should be called. Returns {@literal false} if
     * {@link #synchronizedHandleRequest(VaadinSession, VaadinRequest, VaadinResponse)}
     * should be called.
     *
     * @return if request body should be read before calling
     *         synchronizedHandleRequest
     */
    public boolean isReadRequestBodyFirstEnabled() {
        return false;
    }

    /**
     * Identical to
     * {@link #synchronizedHandleRequest(VaadinSession, VaadinRequest, VaadinResponse)}
     * except the {@link VaadinSession} is locked before this is called and the
     * response requestBody has been read before locking the session and is
     * provided as a separate parameter. Implementations should also take care
     * to unlock the session before writing to the response object.
     *
     * @param session
     *            The session for the request
     * @param request
     *            The request to handle
     * @param response
     *            The response object to which a response can be written.
     * @param requestBody
     *            Request body pre-read from the request object
     * @return true if a response has been written and no further request
     *         handlers should be called, otherwise false
     * @throws IOException
     *             If an IO error occurred
     * @see #handleRequest(VaadinSession, VaadinRequest, VaadinResponse)
     */
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response, String requestBody)
            throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Check whether a request may be handled by this handler. This can be used
     * as an optimization to avoid locking the session just to investigate some
     * method property. The default implementation just returns
     * <code>true</code> which means that all requests will be handled by
     * calling
     * {@link #synchronizedHandleRequest(VaadinSession, VaadinRequest, VaadinResponse)}
     * with the session locked.
     *
     * @param request
     *            the request to handle
     * @return <code>true</code> if the request handling should continue once
     *         the session has been locked; <code>false</code> if there's no
     *         need to lock the session since the request would still not be
     *         handled.
     */
    protected boolean canHandleRequest(VaadinRequest request) {
        return true;
    }

    public static String getRequestBody(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder(MAX_BUFFER_SIZE);
        char[] buffer = new char[MAX_BUFFER_SIZE];

        while (true) {
            int read = reader.read(buffer);
            if (read == -1) {
                break;
            }
            sb.append(buffer, 0, read);
        }

        return sb.toString();
    }
}
