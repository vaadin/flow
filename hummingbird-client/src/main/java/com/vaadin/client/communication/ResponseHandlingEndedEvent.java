package com.vaadin.client.communication;

import com.google.web.bindery.event.shared.Event;
import com.vaadin.client.Registry;

public class ResponseHandlingEndedEvent
        extends Event<ResponseHandlingEndedHandler> {

    public static final Type<ResponseHandlingEndedHandler> TYPE = new Type<ResponseHandlingEndedHandler>();
    private Registry registry;

    public ResponseHandlingEndedEvent(Registry registry) {
        this.registry = registry;
    }

    @Override
    public Type<ResponseHandlingEndedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ResponseHandlingEndedHandler handler) {
        handler.onResponseHandlingEnded(this);
    }

    public Registry getRegistry() {
        return registry;
    }
}
