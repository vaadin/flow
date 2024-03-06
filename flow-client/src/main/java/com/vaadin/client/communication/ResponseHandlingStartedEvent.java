/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.communication;

import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.Event;

/**
 * Event fired when handling of a response starts.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ResponseHandlingStartedEvent
        extends Event<ResponseHandlingStartedEvent.Handler> {

    /**
     * Handler for {@link ResponseHandlingStartedEvent}s.
     */
    @FunctionalInterface
    public interface Handler extends EventHandler {
        /**
         * Called when handling of a response starts.
         *
         * @param responseHandlingStartedEvent
         *            the event object
         */
        void onResponseHandlingStarted(
                ResponseHandlingStartedEvent responseHandlingStartedEvent);
    }

    private static Type<Handler> type = null;

    /**
     * Creates an event object.
     */
    public ResponseHandlingStartedEvent() {
        // Default constructor
    }

    /**
     * Gets the type of the event after ensuring the type has been created.
     *
     * @return the type for the event
     */
    public static Type<Handler> getType() {
        if (type == null) {
            type = new Type<>();
        }
        return type;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return type;
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onResponseHandlingStarted(this);
    }

}
