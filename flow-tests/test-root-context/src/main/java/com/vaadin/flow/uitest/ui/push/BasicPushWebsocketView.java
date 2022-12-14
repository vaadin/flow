package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.internal.nodefeature.PushConfigurationMap;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@CustomPush(transport = Transport.WEBSOCKET)
@Route(value = "com.vaadin.flow.uitest.ui.push.BasicPushWebsocketView", layout = ViewTestLayout.class)
public class BasicPushWebsocketView extends BasicPushView {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Don't use fallback so we can easier detect failures
        attachEvent.getUI().getPushConfiguration().setParameter(
                PushConfigurationMap.FALLBACK_TRANSPORT_KEY, "none");
    }

}
