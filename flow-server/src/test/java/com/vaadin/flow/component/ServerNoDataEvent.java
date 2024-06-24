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

public class ServerNoDataEvent extends ComponentEvent<Component> {

    public ServerNoDataEvent(Component source, boolean fromClient) {
        super(source, fromClient);
    }

}
