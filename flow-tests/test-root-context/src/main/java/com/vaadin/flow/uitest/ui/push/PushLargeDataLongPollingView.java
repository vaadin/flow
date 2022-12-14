package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.internal.nodefeature.PushConfigurationMap;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

@CustomPush(transport = Transport.LONG_POLLING)
@Route("com.vaadin.flow.uitest.ui.push.PushLargeDataLongPollingView")
public class PushLargeDataLongPollingView extends PushLargeData {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().getPushConfiguration().setParameter(
                PushConfigurationMap.FALLBACK_TRANSPORT_KEY, "none");
    }
}
