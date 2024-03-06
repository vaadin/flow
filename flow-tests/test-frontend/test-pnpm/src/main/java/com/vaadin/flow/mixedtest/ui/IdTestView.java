/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.mixedtest.ui;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.templatemodel.TemplateModel;

@Route(value = "route-path")
@Tag("my-component")
@JsModule("./my-component.js")
public class IdTestView extends PolymerTemplate<TemplateModel> {

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
