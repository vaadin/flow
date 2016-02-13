package com.vaadin.client.communication;

import com.google.gwt.event.shared.EventHandler;

public interface ResponseHandlingEndedHandler extends EventHandler {
    void onResponseHandlingEnded(ResponseHandlingEndedEvent e);
}
