package com.vaadin.flow.contexttest.ui;

import java.util.List;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

public class PushUtil {
    private PushUtil() {
    }

    public static void setupPush() {
        // server-side bootstrap
        String transportName = VaadinRequest.getCurrent().getParameter("transport");

        // client-side bootstrap
        if (transportName == null) {
            List<String> list = UI.getCurrent().getInternals()
                    .getLastHandledLocation().getQueryParameters()
                    .getParameters().get("transport");
            if (list != null && !list.isEmpty()) {
                transportName = list.get(0);
            }
        }

        Transport transport = Transport.getByIdentifier(transportName);
        if (transport != null) {
            PushConfiguration pushConfiguration = UI.getCurrent().getPushConfiguration();
            pushConfiguration.setPushMode(PushMode.MANUAL);
            pushConfiguration.setTransport(transport);
            Transport fallbackTransport = transport == Transport.WEBSOCKET_XHR ? Transport.WEBSOCKET: transport;
            pushConfiguration.setFallbackTransport(fallbackTransport);
        }
    }
}
