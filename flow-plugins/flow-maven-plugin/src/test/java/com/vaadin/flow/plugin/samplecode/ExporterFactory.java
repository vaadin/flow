/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.plugin.samplecode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.webcomponent.WebComponent;

public class ExporterFactory implements WebComponentExporterFactory<Component> {

    private class InnerExporter extends WebComponentExporter<Component> {

        private InnerExporter(String tag) {
            super(tag);
        }

        @Override
        protected void configureInstance(WebComponent<Component> webComponent,
                Component component) {
        }

    }

    @Override
    public WebComponentExporter<Component> create() {
        return new InnerExporter("wc-foo-bar");
    }

}
