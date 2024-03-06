/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

@DomEvent("dom-event")
public class MappedToDomNoDataEvent extends ComponentEvent<Component> {

    public MappedToDomNoDataEvent(Component source, boolean fromClient) {
        super(source, fromClient);
    }

    public MappedToDomNoDataEvent(Component source, boolean fromClient,
            int strangeServerSideParam) {
        super(source, fromClient);
    }

}
