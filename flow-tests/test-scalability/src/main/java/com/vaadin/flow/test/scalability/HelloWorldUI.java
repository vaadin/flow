package com.vaadin.flow.test.scalability;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletConfiguration;

/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

public class HelloWorldUI extends UI {

    public static final String PATH = "/helloworld/";

    /**
     * The main servlet for the application.
     */
    @WebServlet(urlPatterns = PATH
            + "*", name = "UIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = HelloWorldUI.class, productionMode = false)
    public static class Servlet extends VaadinServlet {
    }

    @Override
    protected void init(VaadinRequest request) {
        NativeButton b = new NativeButton("Hello", e -> {
            add(new Text("Hello!"));
        });
        add(b);
    }
}
