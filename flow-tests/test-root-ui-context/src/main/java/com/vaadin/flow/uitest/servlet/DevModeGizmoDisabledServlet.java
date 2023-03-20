/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.servlet;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServlet;

// Custom servlet for disabled dev gizmo test
@WebServlet(asyncSupported = true, urlPatterns = {
        "/view-disabled-gizmo/*" }, initParams = {
                @WebInitParam(name = "devmode.gizmo.enabled", value = "false") })
public class DevModeGizmoDisabledServlet extends VaadinServlet {
}
