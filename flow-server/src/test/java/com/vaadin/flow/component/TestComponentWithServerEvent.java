/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import com.vaadin.flow.component.ComponentTest.TestComponent;
import com.vaadin.flow.shared.Registration;

public class TestComponentWithServerEvent extends TestComponent {
    public Registration addServerEventListener(
            ComponentEventListener<ServerEvent> listener) {
        return super.addListener(ServerEvent.class, listener);
    }
}
