/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.theme.Theme;

public class BasicComponent extends Div {
    @Theme("my-theme")
    public static class Exporter extends WebComponentExporter<BasicComponent> {
        public Exporter() {
            super("basic-component");
        }

        @Override
        protected void configureInstance(
                WebComponent<BasicComponent> webComponent,
                BasicComponent component) {
        }
    }

    public BasicComponent() {
        H1 h1 = new H1();
        h1.setText("Basic Component");
        add(h1);
    }
}
