/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.PolymerModelPropertiesView", layout = ViewTestLayout.class)
@Tag("model-properties")
@JsModule("./PolymerModelProperties.js")
public class PolymerModelPropertiesView extends PolymerTemplate<Message> {

    @DomEvent("text-changed")
    public static class ValueChangeEvent
            extends ComponentEvent<PolymerModelPropertiesView> {
        public ValueChangeEvent(PolymerModelPropertiesView source,
                boolean fromClient) {
            super(source, fromClient);
        }
    }

    public PolymerModelPropertiesView() {
        setId("template");
        getModel().setText("foo");

        getElement().addPropertyChangeListener("text", "text-changed",
                event -> {
                });

        addListener(ValueChangeEvent.class, event -> {
            getUI().get().add(addUpdateElement("property-update-event"));
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getUI().get().add(addUpdateElement("property-value"));
    }

    @EventHandler
    private void valueUpdated() {
        getUI().get().add(addUpdateElement("value-update"));
    }

    private Div addUpdateElement(String id) {
        Div div = new Div();
        div.setText("Property value:" + getElement().getProperty("text")
                + ", model value: " + getModel().getText());
        div.setId(id);
        return div;
    }
}
