package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.internal.nodefeature.PushConfigurationMap;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@CustomPush(transport = Transport.WEBSOCKET_XHR)
@Route(value = "com.vaadin.flow.uitest.ui.push.BasicPushWebsocketXhrView", layout = ViewTestLayout.class)
public class BasicPushWebsocketXhrView extends BasicPushView {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Don't use fallback so we can easier detect failures
        attachEvent.getUI().getPushConfiguration().setParameter(
                PushConfigurationMap.FALLBACK_TRANSPORT_KEY, "none");
    }

}
