/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.servlets;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServlet;

/**
 * This is a temporary workaround until #5740 is fixed.
 *
 * @since 2.0
 */
@WebServlet("/*")
public class WorkaroundServlet extends VaadinServlet {

}
