package com.vaadin.client.communication;

import com.google.web.bindery.event.shared.Event;
import com.vaadin.client.Registry;

public class ResponseHandlingStartedEvent
        extends Event<ResponseHandlingStartedHandler> {

    public static final Type<ResponseHandlingStartedHandler> TYPE = new Type<ResponseHandlingStartedHandler>();
    private Registry registry;

    public ResponseHandlingStartedEvent(Registry registry) {
        this.registry = registry;
    }

    @Override
    public Type<ResponseHandlingStartedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ResponseHandlingStartedHandler handler) {
        handler.onResponseHandlingStarted(this);
    }

    public Registry getRegistry() {
        return registry;
    }
}
