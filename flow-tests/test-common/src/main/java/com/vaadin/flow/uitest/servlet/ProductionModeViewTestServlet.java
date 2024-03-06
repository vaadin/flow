/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.servlet;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

@WebServlet(asyncSupported = true, urlPatterns = {
        "/view-production/*" }, initParams = @WebInitParam(name = "productionMode", value = "true"))
public class ProductionModeViewTestServlet extends ViewTestServlet {

}
