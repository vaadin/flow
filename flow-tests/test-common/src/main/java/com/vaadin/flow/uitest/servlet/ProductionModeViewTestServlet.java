/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.servlet;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServletConfiguration;

@WebServlet(asyncSupported = true, urlPatterns = { "/view-production/*" })
@VaadinServletConfiguration(productionMode = true)
public class ProductionModeViewTestServlet extends ViewTestServlet {

}
