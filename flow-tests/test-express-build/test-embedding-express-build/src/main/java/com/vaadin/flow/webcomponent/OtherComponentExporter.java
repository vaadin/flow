/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponent;

public class OtherComponentExporter
        extends WebComponentExporter<OtherExportedComponent> {

    public OtherComponentExporter() {
        super("exported-component-other");
    }

    @Override
    public void configureInstance(
            WebComponent<OtherExportedComponent> webComponent,
            OtherExportedComponent component) {

    }
}
