package com.vaadin.client.communication;

import com.google.gwt.event.shared.EventHandler;

public interface RequestStartingHandler extends EventHandler {
    void onRequestStarting(RequestStartingEvent e);
}
