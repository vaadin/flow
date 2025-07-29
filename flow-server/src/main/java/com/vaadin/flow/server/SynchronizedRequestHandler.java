/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.Optional;

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

    public static final int MAX_BUFFER_SIZE = 64 * 1024;

    /**
     * ResponseWriter is optionally returned by request handlers which implement
     * {@link SynchronizedRequestHandler#synchronizedHandleRequest(VaadinSession, VaadinRequest, VaadinResponse, String)}
     *
     * The ResponseWriter will be executed by
     * {@link #handleRequest(VaadinSession, VaadinRequest, VaadinResponse)}
     * without holding Vaadin session lock.
     */
    @FunctionalInterface
    public interface ResponseWriter extends Serializable {
        void writeResponse() throws IOException;
    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        if (!canHandleRequest(request)) {
            return false;
        }

        try {
            if (isReadAndWriteOutsideSessionLock()) {
                BufferedReader reader = request.getReader();
                String requestBody = reader == null ? null
                        : getRequestBody(reader);
                session.lock();
                Optional<ResponseWriter> responseWriter = synchronizedHandleRequest(
                        session, request, response, requestBody);
                session.unlock();
                if (responseWriter.isPresent()) {
                    responseWriter.get().writeResponse();
                }
                return responseWriter.isPresent();
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
     * @see #handleRequest(VaadinSession, VaadinRequest, VaadinResponse)
     * @param session
     *            The session for the request
     * @param request
     *            The request to handle
     * @param response
     *            The response object to which a response can be written.
     * @return true if a response has been written and no further request
     *         handlers should be called, otherwise false
     *
     * @throws IOException
     *             If an IO error occurred
     */
    public abstract boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException;

    /**
     * Gets if request body should be read and the response written without
     * holding {@link VaadinSession} lock
     *
     * @return {@literal true} if
     *         {@link #synchronizedHandleRequest(VaadinSession, VaadinRequest, VaadinResponse, String)}
     *         should be called. Returns {@literal false} if
     *         {@link #synchronizedHandleRequest(VaadinSession, VaadinRequest, VaadinResponse)}
     *         should be called.
     */
    public boolean isReadAndWriteOutsideSessionLock() {
        return false;
    }

    /**
     * Identical to
     * {@link #synchronizedHandleRequest(VaadinSession, VaadinRequest, VaadinResponse)}
     * except the {@link VaadinSession} is locked before this is called and the
     * response requestBody has been read before locking the session and is
     * provided as a separate parameter.
     *
     * @param session
     *            The session for the request
     * @param request
     *            The request to handle
     * @param response
     *            The response object to which a response can be written.
     * @param requestBody
     *            Request body pre-read from the request object
     * @return a ResponseWriter wrapped into an Optional, if this handler will
     *         write the response and no further request handlers should be
     *         called, otherwise an empty Optional. The ResponseWriter will be
     *         executed after the VaadinSession is unlocked.
     *
     * @throws IOException
     *             If an IO error occurred
     * @see #handleRequest(VaadinSession, VaadinRequest, VaadinResponse)
     */
    public Optional<ResponseWriter> synchronizedHandleRequest(
            VaadinSession session, VaadinRequest request,
            VaadinResponse response, String requestBody)
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
