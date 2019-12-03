/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import com.google.gwt.core.client.Scheduler;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.vaadin.client.Registry;
import com.vaadin.client.gwt.com.google.web.bindery.event.shared.SimpleEventBus;

/**
 * Tracks active server UIDL requests.
 * <p>
 * Ensures that there is only one outgoing server request active at a given
 * time.
 * <p>
 * Fires events when a requests starts, response handling starts and when
 * response handling ends.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class RequestResponseTracker {

    private boolean hasActiveRequest = false;
    private final Registry registry;
    private EventBus eventBus = new SimpleEventBus();

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public RequestResponseTracker(Registry registry) {
        this.registry = registry;
    }

    /**
     * Marks that a new request has started.
     * <p>
     * Should not be called when a request is in progress, i.e.
     * {@link #startRequest()} has been called but not {@link #endRequest()}.
     * <p>
     * Fires a {@link RequestStartingEvent}.
     */
    public void startRequest() {
        if (hasActiveRequest) {
            throw new IllegalStateException(
                    "Trying to start a new request while another is active");
        }
        hasActiveRequest = true;
        fireEvent(new RequestStartingEvent());
    }

    /**
     * Fires the given event using the event bus for this class.
     *
     * @param event
     *            the event to fire
     */
    void fireEvent(Event<?> event) {
        eventBus.fireEvent(event);
    }

    /**
     * Checks is there is an active UIDL request.
     *
     * @return true if there is an active request, false otherwise
     */
    public boolean hasActiveRequest() {
        return hasActiveRequest;
    }

    /**
     * Marks that the current request has ended.
     * <p>
     * Should not be called unless a request is in progress, i.e.
     * {@link #startRequest()} has been called but not {@link #endRequest()}.
     * <p>
     * Will trigger sending of any pending invocations to the server.
     * <p>
     * Fires a {@link ResponseHandlingEndedEvent}.
     */
    public void endRequest() {
        if (!hasActiveRequest) {
            throw new IllegalStateException(
                    "endRequest called when no request is active");
        }
        // After sendInvocationsToServer() there may be a new active
        // request, so we must set hasActiveRequest to false before, not after,
        // the call.
        hasActiveRequest = false;

        if (registry.getUILifecycle().isRunning()
                && registry.getServerRpcQueue().isFlushPending()) {
            // Send the pending RPCs immediately.
            // This might be an unnecessary optimization as ServerRpcQueue has a
            // finally scheduled command which trigger the send if we do not do
            // it here
            registry.getMessageSender().sendInvocationsToServer();
        }

        // deferring to avoid hiding the loading indicator and showing it again
        // shortly thereafter
        Scheduler.get().scheduleDeferred(() -> {
            boolean terminated = registry.getUILifecycle().isTerminated();
            boolean requestNowOrSoon = hasActiveRequest()
                    || registry.getServerRpcQueue().isFlushPending();

            if (terminated || !requestNowOrSoon) {
                registry.getLoadingIndicator().hide();
            }
        });

        fireEvent(new ResponseHandlingEndedEvent());
    }

    /**
     * Adds a handler for {@link RequestStartingEvent}s.
     *
     * @param handler
     *            the handler to add
     * @return a registration object which can be used to remove the handler
     */
    public HandlerRegistration addRequestStartingHandler(
            RequestStartingEvent.Handler handler) {
        return eventBus.addHandler(RequestStartingEvent.getType(), handler);
    }

    /**
     * Adds a handler for {@link ResponseHandlingStartedEvent}s.
     *
     * @param handler
     *            the handler to add
     * @return a registration object which can be used to remove the handler
     */
    public HandlerRegistration addResponseHandlingStartedHandler(
            ResponseHandlingStartedEvent.Handler handler) {
        return eventBus.addHandler(ResponseHandlingStartedEvent.getType(),
                handler);
    }

    /**
     * Adds a handler for {@link ResponseHandlingEndedEvent}s.
     *
     * @param handler
     *            the handler to add
     * @return a registration object which can be used to remove the handler
     */
    public HandlerRegistration addResponseHandlingEndedHandler(
            ResponseHandlingEndedEvent.Handler handler) {
        return eventBus.addHandler(ResponseHandlingEndedEvent.getType(),
                handler);
    }

}
