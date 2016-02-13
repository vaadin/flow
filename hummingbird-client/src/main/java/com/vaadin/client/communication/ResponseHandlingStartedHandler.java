package com.vaadin.client.communication;

import com.google.gwt.event.shared.EventHandler;

public interface ResponseHandlingStartedHandler extends EventHandler {
    void onResponseHandlingStarted(ResponseHandlingStartedEvent e);
}
