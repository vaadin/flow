/*
 * Copyright 2000-2016 Vaadin Ltd.
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
import com.vaadin.client.Console;
import com.vaadin.client.Registry;
import com.vaadin.client.gwt.com.google.web.bindery.event.shared.SimpleEventBus;

/**
 * Class responsible for tracking that the active server request.
 * <p>
 * Ensures that there is only one server request active at a given time.
 * <p>
 * Fires events when a requests starts, response handling starts and when
 * response handling ends.
 *
 * @author Vaadin
 * @since
 */
public class RequestResponseTracker {

    private boolean hasActiveRequest = false;
    private Registry registry;
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
     */
    public void startRequest() {
        if (hasActiveRequest) {
            Console.error(
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
     * Indicates whether or not there are currently active UIDL requests. Used
     * internally to sequence requests properly, seldom needed in Widgets.
     *
     * @return true if there are active requests
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
     */
    public void endRequest() {
        if (!hasActiveRequest) {
            Console.error("No active request");
        }
        // After sendInvocationsToServer() there may be a new active
        // request, so we must set hasActiveRequest to false before, not after,
        // the call.
        hasActiveRequest = false;

        if (registry.getUILifecycle().isRunning()
                && registry.getServerRpcQueue().isFlushPending()) {
            registry.getMessageSender().sendInvocationsToServer();
        }

        // deferring to avoid flickering
        Scheduler.get().scheduleDeferred(() -> {
            if (!registry.getUILifecycle().isRunning() || !(hasActiveRequest()
                    || registry.getServerRpcQueue().isFlushPending())) {
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
