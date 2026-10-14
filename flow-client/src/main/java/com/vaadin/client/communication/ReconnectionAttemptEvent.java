/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.communication;

import com.google.web.bindery.event.shared.Event;

import com.google.gwt.event.shared.EventHandler;

/**
 * Event fired when a reconnection attempt is requested.
 *
 * @author Vaadin Ltd
 * @since 24.7
 */
public class ReconnectionAttemptEvent
        extends Event<ReconnectionAttemptEvent.Handler> {

    /**
     * Handler for {@link ReconnectionAttemptEvent}s.
     */
    @FunctionalInterface
    public interface Handler extends EventHandler {
        /**
         * Called when handling of a reconnection attempt starts.
         *
         * @param event
         *            the event object
         */
        void onReconnectionAttempt(ReconnectionAttemptEvent event);
    }

    private static Type<Handler> type = null;

    private final int attempt;

    /**
     * Creates an event object.
     */
    public ReconnectionAttemptEvent(int attempt) {
        this.attempt = attempt;
    }

    /**
     * Gets the number of the current reconnection attempt.
     *
     * @return the number of the current reconnection attempt.
     */
    public int getAttempt() {
        return attempt;
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
        handler.onReconnectionAttempt(this);
    }

}
