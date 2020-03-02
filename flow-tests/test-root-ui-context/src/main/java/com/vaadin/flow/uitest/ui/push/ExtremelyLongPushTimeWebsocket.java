package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.ui.Transport;

@Push(transport = Transport.WEBSOCKET)
public class ExtremelyLongPushTimeWebsocket extends ExtremelyLongPushTime {

    @Override
    public void init(VaadinRequest request) {
        super.init(request);
    }

}