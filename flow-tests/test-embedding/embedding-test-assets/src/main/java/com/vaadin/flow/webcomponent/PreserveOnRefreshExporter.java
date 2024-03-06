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
import com.vaadin.flow.router.PreserveOnRefresh;

@PreserveOnRefresh
public class PreserveOnRefreshExporter
        extends WebComponentExporter<PreserveOnRefreshComponent> {

    public PreserveOnRefreshExporter() {
        super("preserve-on-refresh");
    }

    @Override
    public void configureInstance(
            WebComponent<PreserveOnRefreshComponent> webComponent,
            PreserveOnRefreshComponent component) {
    }
}
