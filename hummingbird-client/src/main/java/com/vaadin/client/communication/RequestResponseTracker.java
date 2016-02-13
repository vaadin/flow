package com.vaadin.client.communication;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.vaadin.client.Console;
import com.vaadin.client.Registry;
import com.vaadin.client.UILifecycle.UIState;
import com.vaadin.client.gwt.com.google.web.bindery.event.shared.SimpleEventBus;

public class RequestResponseTracker {

    private boolean hasActiveRequest = false;
    private Registry registry;
    private EventBus eventBus = new SimpleEventBus();

    public RequestResponseTracker(Registry registry) {
        this.registry = registry;
    }

    public void startRequest() {
        if (hasActiveRequest) {
            Console.error(
                    "Trying to start a new request while another is active");
        }
        hasActiveRequest = true;
        fireEvent(new RequestStartingEvent(registry));
    }

    /**
     * Fires the given event using the event bus for this class.
     *
     * @param event
     *            the event to fire
     */
    private void fireEvent(Event<?> event) {
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

    public void endRequest() {
        if (!hasActiveRequest) {
            Console.error("No active request");
        }
        // After sendInvocationsToServer() there may be a new active
        // request, so we must set hasActiveRequest to false before, not after,
        // the call.
        hasActiveRequest = false;

        if (registry.getUILifecycle().getState() == UIState.RUNNING) {
            if (registry.getServerRpcQueue().isFlushPending()) {
                registry.getMessageSender().sendInvocationsToServer();
            }
        }

        // deferring to avoid flickering
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
                if ((registry.getUILifecycle().getState() != UIState.RUNNING)
                        || !(hasActiveRequest() || registry.getServerRpcQueue()
                                .isFlushPending())) {
                    registry.getLoadingIndicator().hide();
                }
            }
        });
        fireEvent(new ResponseHandlingEndedEvent(registry));
    }

    public HandlerRegistration addRequestStartingHandler(
            RequestStartingHandler handler) {
        return eventBus.addHandler(RequestStartingEvent.TYPE, handler);
    }

    public HandlerRegistration addResponseHandlingStartedHandler(
            ResponseHandlingStartedHandler handler) {
        return eventBus.addHandler(ResponseHandlingStartedEvent.TYPE, handler);
    }

    public HandlerRegistration addResponseHandlingEndedHandler(
            ResponseHandlingEndedHandler handler) {
        return eventBus.addHandler(ResponseHandlingEndedEvent.TYPE, handler);
    }

}
