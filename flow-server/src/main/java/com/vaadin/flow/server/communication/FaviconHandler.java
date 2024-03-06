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

import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;

/**
 * Handles the favicon request explicitly and return 404 for it.
 * <p>
 * It allows to not produce the same content for the favicon as for any other
 * resource if servlet mapping is "/*".
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class FaviconHandler implements RequestHandler {

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        VaadinServletRequest httpRequest = (VaadinServletRequest) request;
        boolean isFavicon = httpRequest.getContextPath().isEmpty()
                && httpRequest.getServletPath().isEmpty()
                && "/favicon.ico".equals(httpRequest.getPathInfo());
        if (isFavicon) {
            response.setStatus(HttpStatusCode.NOT_FOUND.getCode());
        }
        return isFavicon;
    }

}
