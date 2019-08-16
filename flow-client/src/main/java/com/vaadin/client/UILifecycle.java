/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.vaadin.client.gwt.com.google.web.bindery.event.shared.SimpleEventBus;

/**
 * Manages the lifecycle of a UI.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UILifecycle {

    /**
     * Describes the state of a UI.
     *
     */
    public enum UIState {
        // Do not change the order of the enums as it is used to determine what
        // transitions are allowed.
        INITIALIZING, RUNNING, TERMINATED;
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
     * -&gt; {@link UIState#RUNNING} -&gt; {@link UIState#TERMINATED}.
     * <p>
     * Changing the state fires a {@link StateChangeEvent}.
     *
     * @param state
     *            the new UI state
     */
    public void setState(UIState state) {
        if (state.ordinal() != this.state.ordinal() + 1) {
            throw new IllegalArgumentException(
                    "Tried to move from state " + this.state.name() + " to "
                            + state.name() + " which is not allowed");
        }

        this.state = state;
        eventBus.fireEvent(new StateChangeEvent(this));
    }

    /**
     * Check if the state is {@link UIState#RUNNING}.
     *
     * @return {@code true} if the status is {@link UIState#RUNNING},
     *         {@code false} otherwise
     */
    public boolean isRunning() {
        return getState() == UIState.RUNNING;
    }

    /**
     * Check if the state is {@link UIState#TERMINATED}.
     *
     * @return {@code true} if the status is {@link UIState#TERMINATED},
     *         {@code false} otherwise
     */
    public boolean isTerminated() {
        return getState() == UIState.TERMINATED;
    }

    /**
     * Adds a state change event handler.
     *
     * @param handler
     *            the handler to add
     * @param <H>
     *            the handler type
     * @return a handler registration object which can be used to remove the
     *         handler
     */
    public <H extends StateChangeHandler> HandlerRegistration addHandler(
            H handler) {
        return eventBus.addHandler(StateChangeEvent.getType(), handler);
    }

    /**
     * Event triggered when the lifecycle state of a UI is changed.
     * <p>
     * To listen for the event add a {@link StateChangeHandler} using
     * {@link UILifecycle#addHandler(StateChangeHandler)}.
     */
    public static class StateChangeEvent extends Event<StateChangeHandler> {

        private static Type<StateChangeHandler> type = null;

        private UILifecycle uiLifecycle;

        /**
         * Creates a new event connected to the given lifecycle instance.
         *
         * @param uiLifecycle
         *            the lifecycle instance
         */
        public StateChangeEvent(UILifecycle uiLifecycle) {
            this.uiLifecycle = uiLifecycle;
        }

        /**
         * Gets the type of the event after ensuring the type has been created.
         *
         * @return the type for the event
         */
        public static Type<StateChangeHandler> getType() {
            if (type == null) {
                type = new Type<>();
            }
            return type;
        }

        @Override
        public Type<StateChangeHandler> getAssociatedType() {
            return type;
        }

        /**
         * Gets the {@link UILifecycle} instance which triggered this event.
         *
         * @return the {@link UILifecycle} which triggered the event
         */
        public UILifecycle getUiLifecycle() {
            return uiLifecycle;
        }

        @Override
        protected void dispatch(StateChangeHandler listener) {
            listener.onUIStateChanged(this);
        }
    }

    /**
     * A listener for listening to UI lifecycle events.
     */
    @FunctionalInterface
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
