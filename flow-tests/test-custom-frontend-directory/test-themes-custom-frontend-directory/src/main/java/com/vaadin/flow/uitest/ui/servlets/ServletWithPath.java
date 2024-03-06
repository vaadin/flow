/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.servlets;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServlet;

// Load on startup is set to 1 to ensure this servlet is used as the frontend servlet to paths are always the same in the tests
@WebServlet(urlPatterns = "/path/*", loadOnStartup = 1)
public class ServletWithPath extends VaadinServlet {

}
