package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.ui.Transport;

@Push(transport = Transport.LONG_POLLING)
public class ExtremelyLongPushTimeUI extends PushLargeData {

    private static final int DURATION_MS = 48 * 60 * 60 * 1000; // 48 H
    private static int INTERVAL_MS = 60 * 1000; // 1 minute
    private static int PAYLOAD_SIZE = 100 * 1024; // 100 KB

    @Override
    protected void init(VaadinRequest request) {
        super.init(request);
        duration.setValue(DURATION_MS + "");
        interval.setValue(INTERVAL_MS + "");
        dataSize.setValue(PAYLOAD_SIZE + "");
    }

}