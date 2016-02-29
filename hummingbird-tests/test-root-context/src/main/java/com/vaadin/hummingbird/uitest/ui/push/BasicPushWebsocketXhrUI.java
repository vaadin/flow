package com.vaadin.hummingbird.uitest.ui.push;

import com.vaadin.annotations.Push;
import com.vaadin.hummingbird.namespace.PushConfigurationMap;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ui.Transport;

@Push(transport = Transport.WEBSOCKET_XHR)
public class BasicPushWebsocketXhrUI extends BasicPushUI {

    @Override
    public void init(VaadinRequest request) {
        super.init(request);
        // Don't use fallback so we can easier detect failures
        getPushConfiguration().setParameter(
                PushConfigurationMap.FALLBACK_TRANSPORT_KEY, "none");
    }

}
