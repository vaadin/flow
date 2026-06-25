/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServlet;

@WebServlet(urlPatterns = "/*", asyncSupported = true, initParams = {
        @WebInitParam(name = "heartbeatInterval", value = "5") })
public class CustomServlet extends VaadinServlet {
    public static long HEARTBEAT_INTERVAL = 5;
}
