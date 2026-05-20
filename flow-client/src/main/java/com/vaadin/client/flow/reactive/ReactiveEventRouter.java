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

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

import elemental.events.EventRemover;

/**
 * Event router providing integration with reactive features in {@link Reactive}
 * and {@link Computation}. Pure {@code @JsType(isNative=true)} binding to the
 * TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/reactive/ReactiveEventRouter.ts}.
 *
 * <p>
 * The Java abstract-class shape (override {@code wrap} / {@code dispatchEvent})
 * is replaced by passing two callbacks to the constructor.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <L>
 *            the listener type of this router
 * @param <E>
 *            the reactive event type of this router
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.reactive", name = "ReactiveEventRouter")
public class ReactiveEventRouter<L, E extends ReactiveValueChangeEvent> {

    /** Wraps a generic change listener into the router's listener type. */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    public interface WrapFn<L> {
        L wrap(ReactiveValueChangeListener listener);
    }

    /** Dispatches an event to a listener of the router's listener type. */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    public interface DispatchFn<L, E> {
        void dispatch(L listener, E event);
    }

    /**
     * Creates a new event router. Takes the reactive value the router fires
     * events for and two callbacks that supply what were previously the
     * abstract methods {@code wrap} and {@code dispatchEvent}.
     */
    public ReactiveEventRouter(ReactiveValue reactiveValue, WrapFn<L> wrap,
            DispatchFn<L, E> dispatch) {
        // Defined by the TS class constructor.
    }

    public native EventRemover addListener(L listener);

    public native EventRemover addReactiveListener(
            ReactiveValueChangeListener listener);

    public native void fireEvent(E event);

    public native void registerRead();

    public native ReactiveValue getReactiveValue();
}
