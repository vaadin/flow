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
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponent;

import elemental.json.Json;
import elemental.json.JsonValue;

public class PropertyUpdateExporter
        extends WebComponentExporter<PropertyUpdateComponent> {

    private PropertyConfiguration<PropertyUpdateComponent, Integer> property;
    private PropertyConfiguration<PropertyUpdateComponent, JsonValue> jsonProperty;

    public PropertyUpdateExporter() {
        super("property-update");

        property = addProperty("clicks", 0);
        jsonProperty = addProperty("clicksJson", Json.createNull());
    }

    @Override
    public void configureInstance(
            WebComponent<PropertyUpdateComponent> webComponent,
            PropertyUpdateComponent component) {
        component.addListener(number -> {
            webComponent.setProperty(property, number);
            webComponent.setProperty(jsonProperty, component.getNumberJson());
        });
    }
}
