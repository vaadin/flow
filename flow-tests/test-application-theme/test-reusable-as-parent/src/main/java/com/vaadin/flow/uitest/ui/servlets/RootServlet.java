/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.servlets;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServlet;

// This overrides the servlet mapping of some test dependency
@WebServlet("/*")
public class RootServlet extends VaadinServlet {

}