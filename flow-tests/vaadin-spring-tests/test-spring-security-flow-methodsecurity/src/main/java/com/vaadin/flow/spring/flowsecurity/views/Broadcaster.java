/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.flowsecurity.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventBus;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;

public class Broadcaster {

    private static Broadcaster instance = new Broadcaster();

    private ComponentEventBus router = new ComponentEventBus(new Div());

    public static class RefreshEvent extends ComponentEvent<Component> {
        public RefreshEvent() {
            super(new Div(), false);
        }
    }

    public static void sendMessage() {
        instance.router.fireEvent(new RefreshEvent());
    }

    public static synchronized Registration addMessageListener(
            ComponentEventListener<RefreshEvent> listener) {
        return instance.router.addListener(RefreshEvent.class, listener);
    }

}
