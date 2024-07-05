/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.memoryleaks.ui;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletConfiguration;

public class MemoryLeakUI extends UI {

    @WebServlet(asyncSupported = true, urlPatterns = { "/*" })
    @VaadinServletConfiguration(productionMode = false, ui = MemoryLeakUI.class)
    public static class MemoryLeakServlet extends VaadinServlet {

    }

    @Override
    protected void init(VaadinRequest request) {
        NativeButton button = new NativeButton("Hello",
                e -> add(new Text("Hello")));
        button.setId("hello");
        add(button);
    }
}
