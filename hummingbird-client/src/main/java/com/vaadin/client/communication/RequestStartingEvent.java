package com.vaadin.client.communication;

import com.google.web.bindery.event.shared.Event;
import com.vaadin.client.Registry;

public class RequestStartingEvent extends Event<RequestStartingHandler> {

    public static final Type<RequestStartingHandler> TYPE = new Type<RequestStartingHandler>();
    private Registry registry;

    public RequestStartingEvent(Registry registry) {
        this.registry = registry;
    }

    @Override
    public Type<RequestStartingHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(RequestStartingHandler handler) {
        handler.onRequestStarting(this);
    }

    public Registry getRegistry() {
        return registry;
    }
}
