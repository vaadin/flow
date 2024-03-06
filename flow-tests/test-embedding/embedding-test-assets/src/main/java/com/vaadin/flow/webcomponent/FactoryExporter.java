/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.webcomponent.WebComponent;

public class FactoryExporter implements
        WebComponentExporterFactory<FactoryExporter.InterfaceBasedComponent> {

    private class NotEligibleExporter extends
            WebComponentExporter<FactoryExporter.InterfaceBasedComponent> {

        private NotEligibleExporter() {
            super("interface-based");
        }

        @Override
        public Class<InterfaceBasedComponent> getComponentClass() {
            return InterfaceBasedComponent.class;
        }

        @Override
        protected void configureInstance(
                WebComponent<InterfaceBasedComponent> webComponent,
                InterfaceBasedComponent component) {
        }

    }

    @Override
    public WebComponentExporter<InterfaceBasedComponent> create() {
        return new NotEligibleExporter();
    }

    public static class InterfaceBasedComponent extends Div {
        public InterfaceBasedComponent() {
            Paragraph paragraph = new Paragraph("Hello world");
            paragraph.setId("paragraph");
            add(paragraph);
        }
    }
}
