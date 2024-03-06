/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp.views.empty;

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;

@Route("")
@JsModule("@testscope/all")
public class MainView extends Div {
    public MainView() {
        add(new H2("Hello world!"), new HtmlComponent("testscope-button"),
                new HtmlComponent("testscope-map"));
    }
}
