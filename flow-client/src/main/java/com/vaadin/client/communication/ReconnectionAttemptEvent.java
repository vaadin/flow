/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
