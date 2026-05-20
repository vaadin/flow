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

import com.vaadin.client.Command;

import elemental.events.EventRemover;

/**
 * Handles global features related to reactivity, such as keeping track of the
 * current {@link Computation}, providing a lazy flush cycle and registering
 * reactive event collectors. The state-management surface lives in the
 * TypeScript module at
 * {@code src/main/frontend/internal/client/flow/reactive/Reactive.ts}, reached
 * through {@link NativeReactive}.
 *
 * @see Computation
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Reactive {

    private Reactive() {
        // Only static stuff in this class
    }

    /**
     * Adds a listener that will be invoked the next time {@link #flush()} is
     * invoked.
     */
    public static void addFlushListener(FlushListener flushListener) {
        NativeReactive.addFlushListener(flushListener::flush);
    }

    /**
     * Adds a listener that will be invoked during the next {@link #flush()},
     * after all regular flush listeners have been invoked.
     */
    public static void addPostFlushListener(FlushListener postFlushListener) {
        NativeReactive.addPostFlushListener(postFlushListener::flush);
    }

    /**
     * Flushes all flush listeners and post flush listeners.
     */
    public static void flush() {
        NativeReactive.flush();
    }

    /**
     * Gets the currently active computation.
     */
    public static Computation getCurrentComputation() {
        return (Computation) NativeReactive.getCurrentComputation();
    }

    /**
     * Runs a task with the given computation set as
     * {@link #getCurrentComputation()}.
     */
    public static void runWithComputation(Computation computation,
            Command command) {
        NativeReactive.runWithComputation(computation, command::execute);
    }

    /**
     * Adds a reactive change listener that will be invoked whenever a reactive
     * change event is fired from any reactive event router.
     */
    public static EventRemover addEventCollector(
            ReactiveValueChangeListener reactiveValueChangeListener) {
        NativeReactive.JsRemover remover = NativeReactive
                .addEventCollector(event -> reactiveValueChangeListener
                        .onValueChange((ReactiveValueChangeEvent) event));
        return remover::remove;
    }

    /**
     * Fires a reactive change event to all registered event collectors.
     */
    public static void notifyEventCollectors(ReactiveValueChangeEvent event) {
        NativeReactive.notifyEventCollectors(event);
    }

    /**
     * Evaluates the given command whenever there is a change in any
     * {@link ReactiveValue} used in the command.
     */
    public static Computation runWhenDependenciesChange(Command command) {
        return new Computation() {
            @Override
            protected void doRecompute() {
                command.execute();
            }
        };
    }

    /**
     * Resets Reactive to the initial state.
     */
    public static void reset() {
        NativeReactive.reset();
    }
}
