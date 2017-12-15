package com.vaadin.flow.uitest.ui.push;

import com.vaadin.ui.Push;
import com.vaadin.flow.nodefeature.PushConfigurationMap;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.shared.ui.Transport;

@Push(transport = Transport.WEBSOCKET)
public class BasicPushWebsocketUI extends BasicPushUI {

    @Override
    public void init(VaadinRequest request) {
        super.init(request);
        // Don't use fallback so we can easier detect failures
        getPushConfiguration().setParameter(
                PushConfigurationMap.FALLBACK_TRANSPORT_KEY, "none");
    }

}
