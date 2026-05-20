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
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

import com.vaadin.client.Command;
import com.vaadin.client.JsRunnable;

import elemental.events.EventRemover;

/**
 * Reactive state-management binding. Pure {@code @JsType(isNative=true)} link
 * to {@code src/main/frontend/internal/client/flow/reactive/Reactive.ts}, with
 * 
 * @JsOverlay helpers for the Java-side adapter shapes (FlushListener,
 *            ReactiveValueChangeListener, Computation subclass construction).
 *
 * @see Computation
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.reactive", name = "Reactive")
public final class Reactive {

    /** Native event-collector consumer shape. */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    interface JsEventConsumer {
        void accept(Object event);
    }

    /** Native unregister handle. */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    interface JsRemover {
        void remove();
    }

    private Reactive() {
        // Native, not instantiated from Java
    }

    @JsMethod(name = "addFlushListener")
    static native void addFlushListenerImpl(JsRunnable listener);

    @JsMethod(name = "addPostFlushListener")
    static native void addPostFlushListenerImpl(JsRunnable listener);

    /** Flushes all flush listeners and post flush listeners. */
    public static native void flush();

    @JsMethod(name = "getCurrentComputation")
    static native Object getCurrentComputationImpl();

    @JsMethod(name = "runWithComputation")
    static native void runWithComputationImpl(Object computation,
            JsRunnable command);

    @JsMethod(name = "addEventCollector")
    static native JsRemover addEventCollectorImpl(JsEventConsumer listener);

    @JsMethod(name = "notifyEventCollectors")
    static native void notifyEventCollectorsImpl(Object event);

    /** Resets Reactive to the initial state. */
    public static native void reset();

    /** Adds a listener invoked on the next flush. */
    @JsOverlay
    public static void addFlushListener(FlushListener flushListener) {
        addFlushListenerImpl(flushListener::flush);
    }

    /** Adds a listener invoked after the next flush. */
    @JsOverlay
    public static void addPostFlushListener(FlushListener postFlushListener) {
        addPostFlushListenerImpl(postFlushListener::flush);
    }

    /** Gets the currently active computation. */
    @JsOverlay
    public static Computation getCurrentComputation() {
        return (Computation) getCurrentComputationImpl();
    }

    /**
     * Runs a task with the given computation set as
     * {@link #getCurrentComputation()}.
     */
    @JsOverlay
    public static void runWithComputation(Computation computation,
            Command command) {
        runWithComputationImpl(computation, command::execute);
    }

    /** Registers a global event-change collector. */
    @JsOverlay
    public static EventRemover addEventCollector(
            ReactiveValueChangeListener listener) {
        JsRemover remover = addEventCollectorImpl(event -> listener
                .onValueChange((ReactiveValueChangeEvent) event));
        return remover::remove;
    }

    /** Fires a reactive change event to all registered event collectors. */
    @JsOverlay
    public static void notifyEventCollectors(ReactiveValueChangeEvent event) {
        notifyEventCollectorsImpl(event);
    }

    /**
     * Evaluates the given command whenever there is a change in any
     * {@link ReactiveValue} used in the command.
     */
    @JsOverlay
    public static Computation runWhenDependenciesChange(Command command) {
        return new Computation() {
            @Override
            protected void doRecompute() {
                command.execute();
            }
        };
    }
}
