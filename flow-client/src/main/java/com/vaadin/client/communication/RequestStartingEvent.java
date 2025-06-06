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
