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
 * Event fired when handling of a response ends.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ResponseHandlingEndedEvent
        extends Event<ResponseHandlingEndedEvent.Handler> {

    /**
     * Handler for {@link ResponseHandlingEndedEvent}s.
     */
    @FunctionalInterface
    public interface Handler extends EventHandler {
        /**
         * Called when handling of a response ends.
         *
         * @param responseHandlingEndedEvent
         *            the event object
         */
        void onResponseHandlingEnded(
                ResponseHandlingEndedEvent responseHandlingEndedEvent);
    }

    private static Type<Handler> type;

    /**
     * Creates an event object.
     */
    public ResponseHandlingEndedEvent() {
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
        handler.onResponseHandlingEnded(this);
    }

}
