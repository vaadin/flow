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
import com.vaadin.flow.component.webcomponent.WebComponent;

public class DefaultValueInitializationExporter
        extends WebComponentExporter<DefaultValueInitializationComponent> {

    public DefaultValueInitializationExporter() {
        super("default-value-init");
        addProperty("value", 1)
                .onChange(DefaultValueInitializationComponent::updateValue);
    }

    @Override
    protected void configureInstance(
            WebComponent<DefaultValueInitializationComponent> webComponent,
            DefaultValueInitializationComponent component) {

    }
}
