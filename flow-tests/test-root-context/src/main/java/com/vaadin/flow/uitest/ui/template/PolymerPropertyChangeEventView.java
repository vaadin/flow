/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.PropertyChangeEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.PolymerPropertyChangeEventView")
public class PolymerPropertyChangeEventView extends AbstractDivView {

    @Tag("property-change")
    @JsModule("./PolymerPropertyChange.js")
    public static class PolymerPropertyChange extends PolymerTemplate<Message> {

    }

    public PolymerPropertyChangeEventView() {
        PolymerPropertyChange template = new PolymerPropertyChange();
        template.setId("template");
        template.getElement().addPropertyChangeListener("text",
                this::propertyChanged);
        add(template);
    }

    private void propertyChanged(PropertyChangeEvent event) {
        Div div = new Div();
        div.setText("New property value: '" + event.getValue()
                + "', old property value: '" + event.getOldValue() + "'");
        div.addClassName("change-event");
        add(div);
    }
}
