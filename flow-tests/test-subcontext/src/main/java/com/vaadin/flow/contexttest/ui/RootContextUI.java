/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.contexttest.ui;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletConfiguration;

public class RootContextUI extends DependencyUI {

    /**
     * The main servlet for the application.
     */
    @WebServlet(urlPatterns = {
            "/*" }, name = "UIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = RootContextUI.class, productionMode = false)
    public static class Servlet extends NoRouterServlet {
    }

    @Override
    protected void init(VaadinRequest request) {
        getElement().appendChild(ElementFactory.createDiv("Root Context UI")
                .setAttribute("id", "root"));
        super.init(request);
    }

}
