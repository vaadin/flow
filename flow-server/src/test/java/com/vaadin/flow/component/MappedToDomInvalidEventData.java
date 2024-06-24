/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;

@DomEvent("dom-event")
public class MappedToDomInvalidEventData extends ComponentEvent<Component> {

    public MappedToDomInvalidEventData(Component source, boolean fromClient,
            @EventData("") int strangeServerSideParam) {
        super(source, fromClient);
    }

}
