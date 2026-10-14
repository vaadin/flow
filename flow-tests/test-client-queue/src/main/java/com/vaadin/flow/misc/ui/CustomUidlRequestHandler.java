/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.UidlRequestHandler;

public class CustomUidlRequestHandler extends UidlRequestHandler {

    public static Set<VaadinSession> emptyResponse = new HashSet();

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        if (emptyResponse.contains(session)) {
            emptyResponse.remove(session);
            commitEmptyResponse(response);
            return true;
        }
        return super.synchronizedHandleRequest(session, request, response);
    }

    @Override
    public Optional<ResponseWriter> synchronizedHandleRequest(
            VaadinSession session, VaadinRequest request,
            VaadinResponse response, String requestBody)
            throws IOException, UnsupportedOperationException {

        if (emptyResponse.contains(session)) {
            emptyResponse.remove(session);
            return Optional.of(() -> commitEmptyResponse(response));
        }
        return super.synchronizedHandleRequest(session, request, response,
                requestBody);
    }

    private void commitEmptyResponse(VaadinResponse response)
            throws IOException {
        commitJsonResponse(response, "for(;;);[{}]");
    }
}
