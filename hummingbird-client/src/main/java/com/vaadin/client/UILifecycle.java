package com.vaadin.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.vaadin.client.gwt.com.google.web.bindery.event.shared.SimpleEventBus;

/**
 * Class managing the lifecycle of a UI.
 *
 * @author Vaadin
 * @since
 */
public class UILifecycle {

    /**
     * Enum describing the state of a UI.
     *
     * @author Vaadin
     * @since
     */
    public enum UIState {
        INITIALIZING(0), RUNNING(1), TERMINATED(2);
        private int order;

        private UIState(int order) {
            this.order = order;
        }

    }

    private UIState state = UIState.INITIALIZING;
    private EventBus eventBus = new SimpleEventBus();

    /**
     * Gets the state of the UI.
     *
     * @return the current state of the UI
     */
    public UIState getState() {
        return state;
    }

    /**
     * Sets the state of the UI to the given value.
     * <p>
     * Only allows state changes in one direction: {@link UIState#INITIALIZING}
     * -> {@link UIState#RUNNING} -> {@link UIState#TERMINATED}.
     *
     * @param state
     *            the new UI state
     *
     */
    public void setState(UIState state) {
        if (state.order != this.state.order + 1) {
            Console.warn("Tried to move from state " + this.state.name()
                    + " to " + state.name() + " which is not allowed");
            return;
        }

        this.state = state;
        eventBus.fireEvent(new StateChangeEvent(this));
    }

    /**
     * Adds a state change event handler.
     *
     * @param type
     * @param handler
     * @return
     */
    public <H extends StateChangeHandler> HandlerRegistration addHandler(
            H handler) {
        return eventBus.addHandler(StateChangeEvent.TYPE, handler);
    }

    /**
     * Event triggered when the lifecycle state of a UI is changed.
     * <p>
     * To listen for the event add a {@link StateChangeHandler} using
     * {@link UILifecycle#addHandler(StateChangeHandler)}.
     *
     * @since
     * @author Vaadin Ltd
     */
    public static class StateChangeEvent extends Event<StateChangeHandler> {

        public static final Type<StateChangeHandler> TYPE = new Type<StateChangeHandler>();

        public StateChangeEvent(UILifecycle lifecycle) {
            setSource(lifecycle);
        }

        @Override
        public Type<StateChangeHandler> getAssociatedType() {
            return TYPE;
        }

        /**
         * Gets the {@link UILifecycle} instance which triggered this event.
         *
         * @return the {@link UILifecycle} which triggered the event
         */
        @Override
        public UILifecycle getSource() {
            return (UILifecycle) super.getSource();
        }

        @Override
        protected void dispatch(StateChangeHandler listener) {
            listener.onUIStateChanged(this);
        }
    }

    /**
     * A listener for listening to UI lifecycle events.
     *
     * @since
     * @author Vaadin Ltd
     */
    public interface StateChangeHandler extends EventHandler {

        /**
         * Triggered when state of a UI if changed. To get the current state,
         * call {@link UILifecycle#getState()}.
         *
         * @param event
         *            the event object
         */
        void onUIStateChanged(StateChangeEvent event);
    }

}
