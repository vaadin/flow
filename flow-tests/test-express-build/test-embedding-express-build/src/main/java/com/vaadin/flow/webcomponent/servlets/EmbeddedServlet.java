/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent.servlets;

import jakarta.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServlet;

@WebServlet(urlPatterns = { "/*", "/vaadin/*" }, asyncSupported = true)
public class EmbeddedServlet extends VaadinServlet {
}
