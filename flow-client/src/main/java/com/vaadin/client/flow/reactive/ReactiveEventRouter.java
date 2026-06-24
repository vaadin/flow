/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.client.flow.reactive;

import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsSet;

import elemental.events.EventRemover;

/**
 * Event router providing integration with reactive features in {@link Reactive}
 * and {@link Computation}. Listeners can be added both for a specific event
 * type and for the generic value change. All events are fired to both types of
 * listeners, as well as to event collectors registered using
 * {@link Reactive#addEventCollector(ReactiveValueChangeListener)}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <L>
 *            the listener type of this router
 * @param <E>
 *            the reactive event type of this router
 */
public class ReactiveEventRouter<L, E extends ReactiveValueChangeEvent> {

    /**
     * Wraps a generic reactive change listener as the listener type natively
     * supported by an event router.
     *
     * @param <L>
     *            the native listener type
     */
    @FunctionalInterface
    public interface ListenerWrapper<L> {
        L wrap(ReactiveValueChangeListener reactiveValueChangeListener);
    }

    /**
     * Dispatches an event to a listener of the type natively supported by an
     * event router.
     *
     * @param <L>
     *            the native listener type
     * @param <E>
     *            the reactive event type
     */
    @FunctionalInterface
    public interface EventDispatcher<L, E> {
        void dispatch(L listener, E event);
    }

    private final JsSet<L> listeners = JsCollections.set();

    private final ReactiveValue reactiveValue;

    private final ListenerWrapper<L> wrapper;

    private final EventDispatcher<L, E> dispatcher;

    /**
     * Creates a new event router for a reactive value.
     *
     * @param reactiveValue
     *            the reactive value, not <code>null</code>
     * @param wrapper
     *            callback for wrapping a generic reactive change listener as
     *            the native listener type
     * @param dispatcher
     *            callback for dispatching an event to a native listener
     */
    public ReactiveEventRouter(ReactiveValue reactiveValue,
            ListenerWrapper<L> wrapper, EventDispatcher<L, E> dispatcher) {
        assert reactiveValue != null;

        this.reactiveValue = reactiveValue;
        this.wrapper = wrapper;
        this.dispatcher = dispatcher;
    }

    /**
     * Adds a listener to this event router.
     *
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return an event remover that can be used for removing the added listener
     */
    public EventRemover addListener(L listener) {
        assert listener != null;

        listeners.add(listener);
        EventRemover remover = () -> listeners.delete(listener);

        Computation computation = Reactive.getCurrentComputation();
        if (computation != null) {
            computation.onNextInvalidate(e -> remover.remove());
        }

        return remover;
    }

    /**
     * Adds a generic reactive change listener to this router.
     *
     * @param reactiveValueChangeListener
     *            the change listener to add, not <code>null</code>
     * @return an event remover that can be used for removing the added listener
     */
    public EventRemover addReactiveListener(
            ReactiveValueChangeListener reactiveValueChangeListener) {
        assert reactiveValueChangeListener != null;
        return addListener(wrapper.wrap(reactiveValueChangeListener));
    }

    /**
     * Fires an event to all listeners added to this router using
     * {@link #addListener(Object)} or
     * {@link #addReactiveListener(ReactiveValueChangeListener)} as well as all
     * global event collectors added using
     * {@link Reactive#addEventCollector(ReactiveValueChangeListener)}.
     *
     * @param event
     *            the event to fire
     */
    public void fireEvent(E event) {
        assert event.getSource() == reactiveValue;

        JsSet<L> copy = JsCollections.set(listeners);

        copy.forEach(listener -> dispatcher.dispatch(listener, event));

        Reactive.notifyEventCollectors(event);
    }

    /**
     * Registers access to the data for which this event router fires event.
     * This registers the event source of this event router to be set as a
     * dependency of the current computation if there is one.
     */
    public void registerRead() {
        Computation computation = Reactive.getCurrentComputation();
        if (computation != null) {
            computation.addDependency(reactiveValue);
        }
    }

    /**
     * Gets the reactive value for which this router fires event.
     *
     * @return the reactive value
     */
    public ReactiveValue getReactiveValue() {
        return reactiveValue;
    }
}
