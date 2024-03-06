/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.bytecodescanning;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FallbackChunk;

/**
 * This is a hacky code since it removes {@link FallbackChunk} instance
 * <b><u>per request</u></b>.
 * <p>
 * Once being removed the {@link FallbackChunk} instance can'be restored for the
 * whole application. So one (arbitrary) request changes the behavior of the
 * whole application (singleton). This is generally a terrible way of doing
 * things but in <u>this specific test case</u> it doesn't cause issues
 * <b><u>currently</u></b> with the tests (only one in fact) that we have. The
 * situation might change in the future.
 * <p>
 * This hack works <u>only</u> if all tests uses "drop-fallback" query parameter
 * or don't use it.
 */
public class RemoveFallbackChunkInfo implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addRequestHandler(this::handleRequest);
    }

    boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) {
        VaadinServletRequest servletRequest = (VaadinServletRequest) request;
        HttpServletRequest httpRequest = servletRequest.getHttpServletRequest();
        String query = httpRequest.getQueryString();
        if ("drop-fallback".equals(query)) {
            // self check
            FallbackChunk chunk = session.getService().getContext()
                    .getAttribute(FallbackChunk.class);

            if (chunk == null) {
                throw new RuntimeException(
                        "Vaadin context has no fallback chunk data");
            }

            // remove fallback chunk data to that the chunk won't be loaded
            session.getService().getContext()
                    .removeAttribute(FallbackChunk.class);
        }
        return false;
    }

}
