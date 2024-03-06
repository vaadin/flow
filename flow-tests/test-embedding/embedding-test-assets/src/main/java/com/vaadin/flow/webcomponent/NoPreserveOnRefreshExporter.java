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

public class NoPreserveOnRefreshExporter
        extends WebComponentExporter<PreserveOnRefreshComponent> {

    public NoPreserveOnRefreshExporter() {
        super("no-preserve-on-refresh");
    }

    @Override
    public void configureInstance(
            WebComponent<PreserveOnRefreshComponent> webComponent,
            PreserveOnRefreshComponent component) {
    }
}
