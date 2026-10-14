/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;

/**
 * Used to provide an around-like aspect option around request processing.
 *
 * @author Marcin Grzejszczak
 * @since 24.2
 */
public interface VaadinRequestInterceptor extends Serializable {

    /**
     * Called when request is about to be processed.
     *
     * @param request
     *            request
     * @param response
     *            response
     */
    void requestStart(VaadinRequest request, VaadinResponse response);

    /**
     * Called when an exception occurred
     *
     * @param request
     *            request
     * @param response
     *            response
     * @param vaadinSession
     *            session
     * @param t
     *            exception
     */
    void handleException(VaadinRequest request, VaadinResponse response,
            VaadinSession vaadinSession, Exception t);

    /**
     * Called in the finally block of processing a request. Will be called
     * regardless of whether there was an exception or not.
     *
     * @param request
     *            request
     * @param response
     *            response
     * @param session
     *            session
     */
    void requestEnd(VaadinRequest request, VaadinResponse response,
            VaadinSession session);
}
