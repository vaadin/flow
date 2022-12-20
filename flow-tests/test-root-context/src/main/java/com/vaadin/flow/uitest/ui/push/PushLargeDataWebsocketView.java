package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.internal.nodefeature.PushConfigurationMap;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

@CustomPush(transport = Transport.WEBSOCKET)
@Route("com.vaadin.flow.uitest.ui.push.PushLargeDataWebsocketView")
public class PushLargeDataWebsocketView extends PushLargeData {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().getPushConfiguration().setParameter(
                PushConfigurationMap.FALLBACK_TRANSPORT_KEY, "none");
    }
}
