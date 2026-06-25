/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.flowsecuritywebsocket;

import org.springframework.stereotype.Component;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.ListenerPriority;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.shared.ui.Transport;

@Component
public class PushWebsocketConfigurer implements VaadinServiceInitListener {

    private final PushTransportSetter pushTransportSetter = new PushTransportSetter();

    @Override
    public void serviceInit(ServiceInitEvent event) {

        event.getSource().addUIInitListener(uiInitEvent -> {
            // Transport cannot be set directly in UI listener because
            // BootstrapHandler overrides it with @Push annotation value.
            uiInitEvent.getUI().addBeforeEnterListener(pushTransportSetter);
        });
    }

    @ListenerPriority(10)
    private static class PushTransportSetter implements BeforeEnterListener {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.getUI().getPushConfiguration()
                    .setTransport(Transport.WEBSOCKET);
        }
    }
}
