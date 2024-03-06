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

public abstract class AbstractExporter extends WebComponentExporter<Component> {

    protected AbstractExporter(String tag) {
        super(tag);
    }

}
