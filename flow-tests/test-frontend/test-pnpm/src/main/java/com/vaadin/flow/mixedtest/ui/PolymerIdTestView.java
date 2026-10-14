/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.mixedtest.ui;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

@Route(value = "route-path-polymer")
@Tag("my-polymer-component")
@JsModule("./my-polymer-component.js")
public class PolymerIdTestView extends PolymerTemplate<TemplateModel> {

    @Id
    NativeButton button;
    @Id
    Div content;

    public PolymerIdTestView() {
        button.addClickListener(e -> {
            String s = content.getText();
            Integer val = 1
                    + Integer.parseInt(s == null || s.isEmpty() ? "0" : s);
            content.setText(String.valueOf(val));
        });
    }
}
