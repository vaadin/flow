/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServlet;

@WebServlet(asyncSupported = true, urlPatterns = { "/view/*" })
public class ViewTestServlet extends VaadinServlet {

    private static ViewClassLocator viewLocator;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        if (getService() != null) {
            viewLocator = new ViewClassLocator(getService().getClassLoader());
        }
    }

    static ViewClassLocator getViewLocator() {
        return viewLocator;
    }

}
