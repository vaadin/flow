/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * This servlet handles static resources and webjars. It's purpose to handle
 * "/frontend" URI if the main Vaadin servlet has non-root mapping.
 *
 * @author Vaadin Ltd
 * @since 1.4
 */
public class FrontendVaadinServlet extends VaadinServlet {

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        if (serveStaticOrWebJarRequest(request, response)) {
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND,
                "Couldn't handle the request with any static resource");
    }
}
