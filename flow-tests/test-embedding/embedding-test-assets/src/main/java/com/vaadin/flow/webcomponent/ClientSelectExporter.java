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
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.server.PWA;

@PWA(name = "Client select exporter", shortName = "Client select")
public class ClientSelectExporter
        extends WebComponentExporter<ClientSelectComponent>
        implements AppShellConfigurator {

    public ClientSelectExporter() {
        super("client-select");
        addProperty("show", false)
                .onChange(ClientSelectComponent::setMessageVisible);
    }

    @Override
    public void configureInstance(
            WebComponent<ClientSelectComponent> webComponent,
            ClientSelectComponent component) {

    }
}
