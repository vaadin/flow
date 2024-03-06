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
 * Event fired when a request starts.
 *
 * @see RequestResponseTracker#startRequest()
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class RequestStartingEvent extends Event<RequestStartingEvent.Handler> {

    /**
     * Handler for {@link RequestStartingEvent}s.
     */
    @FunctionalInterface
    public interface Handler extends EventHandler {
        /**
         * Called when a request is starting.
         *
         * @param requestStartingEvent
         *            the event object
         */
        void onRequestStarting(RequestStartingEvent requestStartingEvent);
    }

    private static Type<Handler> type = null;

    /**
     * Creates a new event.
     */
    public RequestStartingEvent() {
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
        handler.onRequestStarting(this);
    }

}
