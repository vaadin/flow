/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.mixedtest.ui;

import jakarta.servlet.annotation.WebServlet;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServlet;

@Route(value = "route-path")
@Tag("my-component")
@JsModule("./my-component.js")
public class IdTestView extends LitTemplate {

    @WebServlet("/servlet-path/*")
    public static class MyServlet extends VaadinServlet {
    }

    @Id
    NativeButton button;
    @Id
    Div content;

    public IdTestView() {
        button.addClickListener(e -> {
            String s = content.getText();
            Integer val = 1
                    + Integer.parseInt(s == null || s.isEmpty() ? "0" : s);
            content.setText(String.valueOf(val));
        });
    }
}
