/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.server.VaadinServlet;

@WebServlet("/frontend/*")
public class WebJarsServlet extends VaadinServlet {
    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        // Only serve static and webjar requests, don't delegate anything to
        // VaadinService
        if (!serveStaticOrWebJarRequest(request, response)) {
            response.sendError(404);
        }
    }
}
